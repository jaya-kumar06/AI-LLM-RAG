package com.example.ragchatbot.service;

import com.example.ragchatbot.model.DocumentHash;
import com.example.ragchatbot.model.DocumentInfo;
import com.example.ragchatbot.repository.DocumentHashRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Document Processing Service
 * 
 * RESPONSIBILITIES:
 * 1. Upload and save documents (PDF, DOCX, TXT)
 * 2. Check for duplicates using content hash (DEDUPLICATION)
 * 3. Extract text from documents
 * 4. Chunk text into smaller pieces
 * 5. Generate embeddings for chunks
 * 6. Store embeddings in PostgreSQL (pgvector)
 * 
 * CHUNKING STRATEGY:
 * - Chunk Size: 500 characters (configurable)
 * - Overlap: 50 characters (for context continuity)
 * - Splitter: Recursive character splitter
 * 
 * DEDUPLICATION:
 * - SHA-256 hash of file content
 * - Prevents re-indexing identical documents
 * - Even renamed files are detected as duplicates
 */
@Service
public class DocumentService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentHashRepository documentHashRepository;
    
    @Value("${upload.directory:./uploads}")
    private String uploadDirectory;
    
    @Value("${rag.chunk.size:500}")
    private int chunkSize;
    
    @Value("${rag.chunk.overlap:50}")
    private int chunkOverlap;

    // Track uploaded documents (in production, store in DB)
    private final Map<String, DocumentInfo> documents = new ConcurrentHashMap<>();

    public DocumentService(EmbeddingModel embeddingModel, 
                          EmbeddingStore<TextSegment> embeddingStore,
                          DocumentHashRepository documentHashRepository) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.documentHashRepository = documentHashRepository;
    }
    
    /**
     * Calculate SHA-256 hash of file content
     * 
     * KEY INTERVIEW POINT:
     * - Used for deduplication
     * - Same content = same hash, regardless of filename
     * - Prevents wasted storage and compute on duplicates
     */
    private String calculateFileHash(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
    
    /**
     * Check if document already exists (by content hash)
     */
    public boolean isDuplicate(byte[] content) {
        String hash = calculateFileHash(content);
        return documentHashRepository.existsByContentHash(hash);
    }

    /**
     * Process an uploaded document
     * 
     * FLOW:
     * 1. Save file to disk
     * 2. Extract text based on file type
     * 3. Split into chunks
     * 4. Generate embeddings
     * 5. Store in pgvector
     * 
     * @param file The uploaded file
     * @return DocumentInfo with processing results
     */
    public DocumentInfo processDocument(MultipartFile file) throws IOException {
        String docId = UUID.randomUUID().toString();
        String filename = file.getOriginalFilename();
        String fileType = getFileType(filename);
        
        System.out.println("\n📄 Processing document: " + filename);
        System.out.println("   Type: " + fileType);
        System.out.println("   Size: " + file.getSize() + " bytes");
        
        // DEDUPLICATION CHECK
        byte[] fileContent = file.getBytes();
        String contentHash = calculateFileHash(fileContent);
        System.out.println("   🔑 Content hash: " + contentHash.substring(0, 16) + "...");
        
        if (documentHashRepository.existsByContentHash(contentHash)) {
            DocumentHash existing = documentHashRepository.findByContentHash(contentHash).get();
            System.out.println("   ⚠️ DUPLICATE DETECTED! Already indexed as: " + existing.getFilename());
            
            // Return existing document info instead of re-processing
            DocumentInfo docInfo = new DocumentInfo(docId, filename, fileType, file.getSize());
            docInfo.setStatus("duplicate");
            docInfo.setChunkCount(existing.getChunkCount());
            return docInfo;
        }
        
        // Create document info
        DocumentInfo docInfo = new DocumentInfo(docId, filename, fileType, file.getSize());
        documents.put(docId, docInfo);
        
        try {
            // Step 1: Save file (use absolute path to avoid Tomcat temp directory issues)
            Path uploadPath = Paths.get(uploadDirectory).toAbsolutePath();
            Files.createDirectories(uploadPath);
            Path filePath = uploadPath.resolve(docId + "_" + filename);
            System.out.println("   📁 Upload directory: " + uploadPath);
            file.transferTo(filePath.toFile());
            System.out.println("   ✅ Saved to: " + filePath);
            
            // Step 2: Extract text
            String text = extractText(filePath.toFile(), fileType);
            System.out.println("   ✅ Extracted text: " + text.length() + " characters");
            
            // Step 3: Create document and split into chunks
            Document document = Document.from(text, Metadata.from("source", filename));
            
            DocumentSplitter splitter = DocumentSplitters.recursive(
                chunkSize,    // Max characters per chunk
                chunkOverlap  // Overlap between chunks
            );
            
            List<TextSegment> chunks = splitter.split(document);
            System.out.println("   ✅ Created " + chunks.size() + " chunks");
            
            // Add metadata to each chunk
            List<TextSegment> enrichedChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                TextSegment chunk = chunks.get(i);
                Metadata metadata = Metadata.from("source", filename)
                        .put("documentId", docId)
                        .put("chunkIndex", String.valueOf(i))
                        .put("totalChunks", String.valueOf(chunks.size()));
                enrichedChunks.add(TextSegment.from(chunk.text(), metadata));
            }
            
            // Step 4: Generate embeddings
            System.out.println("   ⏳ Generating embeddings...");
            List<Embedding> embeddings = embeddingModel.embedAll(enrichedChunks).content();
            System.out.println("   ✅ Generated " + embeddings.size() + " embeddings");
            
            // Step 5: Store in pgvector
            System.out.println("   ⏳ Storing in PostgreSQL...");
            embeddingStore.addAll(embeddings, enrichedChunks);
            System.out.println("   ✅ Stored in vector database!");
            
            // Update document info
            docInfo.setChunkCount(chunks.size());
            docInfo.setStatus("completed");
            
            // Save document hash for deduplication
            DocumentHash docHash = new DocumentHash(contentHash, filename, file.getSize());
            docHash.setChunkCount(chunks.size());
            documentHashRepository.save(docHash);
            System.out.println("   ✅ Document hash saved for deduplication");
            
            System.out.println("📄 Document processing complete!\n");
            
            return docInfo;
            
        } catch (Exception e) {
            docInfo.setStatus("failed");
            System.err.println("   ❌ Error: " + e.getMessage());
            throw new IOException("Failed to process document: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from file based on type
     */
    private String extractText(File file, String fileType) throws IOException {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> extractFromPdf(file);
            case "docx" -> extractFromDocx(file);
            case "txt" -> extractFromTxt(file);
            default -> throw new IOException("Unsupported file type: " + fileType);
        };
    }

    /**
     * Extract text from PDF using Apache PDFBox
     * 
     * PDFBox is the most popular Java library for PDF processing.
     * It extracts text while preserving reading order.
     */
    private String extractFromPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Extract text from DOCX using Apache POI
     * 
     * POI (Poor Obfuscation Implementation) handles Microsoft Office formats.
     * XWPF = XML Word Processing Format (for .docx files)
     */
    private String extractFromDocx(File file) throws IOException {
        StringBuilder text = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
        }
        return text.toString();
    }

    /**
     * Extract text from TXT file
     */
    private String extractFromTxt(File file) throws IOException {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8);
    }

    /**
     * Get file type from filename
     */
    private String getFileType(String filename) {
        if (filename == null) return "unknown";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "unknown";
    }

    /**
     * Get all uploaded documents
     */
    public List<DocumentInfo> getAllDocuments() {
        return new ArrayList<>(documents.values());
    }

    /**
     * Get document by ID
     */
    public Optional<DocumentInfo> getDocument(String id) {
        return Optional.ofNullable(documents.get(id));
    }

    /**
     * Delete document and its embeddings
     */
    public boolean deleteDocument(String id) {
        DocumentInfo doc = documents.remove(id);
        if (doc != null) {
            // In production, you would also delete from pgvector
            // embeddingStore.removeAll(filter("documentId", id));
            try {
                Path filePath = Paths.get(uploadDirectory, id + "_" + doc.getFilename());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println("Failed to delete file: " + e.getMessage());
            }
            return true;
        }
        return false;
    }
}
