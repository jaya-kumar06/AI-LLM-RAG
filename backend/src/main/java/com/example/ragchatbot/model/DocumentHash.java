package com.example.ragchatbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * DocumentHash Entity
 * 
 * Tracks document content hashes to prevent duplicate indexing.
 * 
 * KEY INTERVIEW POINTS:
 * - Deduplication prevents wasted storage and compute
 * - MD5/SHA-256 hash of content identifies unique documents
 * - Even if filename changes, same content won't be re-indexed
 */
@Entity
@Table(name = "document_hashes", indexes = {
    @Index(name = "idx_content_hash", columnList = "content_hash", unique = true)
})
public class DocumentHash {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "content_hash", unique = true, nullable = false, length = 64)
    private String contentHash; // SHA-256 hash
    
    @Column(nullable = false)
    private String filename;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "chunk_count")
    private Integer chunkCount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public DocumentHash() {}
    
    public DocumentHash(String contentHash, String filename, Long fileSize) {
        this.contentHash = contentHash;
        this.filename = filename;
        this.fileSize = fileSize;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
