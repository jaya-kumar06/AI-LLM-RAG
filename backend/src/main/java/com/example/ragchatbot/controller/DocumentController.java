package com.example.ragchatbot.controller;

import com.example.ragchatbot.model.DocumentInfo;
import com.example.ragchatbot.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Document Controller
 * 
 * REST API endpoints for document management.
 * 
 * ENDPOINTS:
 * - POST   /api/documents          - Upload a document
 * - GET    /api/documents          - List all documents
 * - GET    /api/documents/{id}     - Get document details
 * - DELETE /api/documents/{id}     - Delete a document
 * 
 * SUPPORTED FILE TYPES:
 * - PDF  (.pdf)   - Uses Apache PDFBox
 * - Word (.docx)  - Uses Apache POI
 * - Text (.txt)   - Direct read
 */
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Upload and process a document
     * 
     * This endpoint:
     * 1. Receives the file upload
     * 2. Extracts text from the document
     * 3. Splits into chunks
     * 4. Generates embeddings
     * 5. Stores in PostgreSQL + pgvector
     * 
     * Example cURL:
     * curl -X POST -F "file=@document.pdf" http://localhost:8080/api/documents
     */
    @PostMapping
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File is empty"));
        }

        String filename = file.getOriginalFilename();
        if (filename == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid filename"));
        }

        // Check file type
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!List.of("pdf", "docx", "txt").contains(extension)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Unsupported file type: " + extension,
                            "supported", List.of("pdf", "docx", "txt")
                    ));
        }

        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📤 Document upload received");
        System.out.println("   Filename: " + filename);
        System.out.println("   Size: " + file.getSize() + " bytes");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        try {
            DocumentInfo docInfo = documentService.processDocument(file);
            return ResponseEntity.ok(docInfo);
        } catch (IOException e) {
            System.err.println("❌ Upload failed: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to process document: " + e.getMessage()));
        }
    }

    /**
     * List all uploaded documents
     */
    @GetMapping
    public ResponseEntity<List<DocumentInfo>> listDocuments() {
        List<DocumentInfo> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    /**
     * Get a specific document by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable String id) {
        return documentService.getDocument(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a document and its embeddings
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable String id) {
        boolean deleted = documentService.deleteDocument(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }
}
