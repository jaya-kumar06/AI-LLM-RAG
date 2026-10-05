package com.example.ragchatbot.model;

import java.time.LocalDateTime;

/**
 * Document Information Model
 * 
 * Represents metadata about an uploaded and processed document.
 */
public class DocumentInfo {
    
    private String id;
    private String filename;
    private String fileType;
    private long fileSize;
    private int chunkCount;
    private LocalDateTime uploadedAt;
    private String status; // "processing", "completed", "failed"
    
    // Constructors
    public DocumentInfo() {}
    
    public DocumentInfo(String id, String filename, String fileType, long fileSize) {
        this.id = id;
        this.filename = filename;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadedAt = LocalDateTime.now();
        this.status = "processing";
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public void setFilename(String filename) {
        this.filename = filename;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
    
    public int getChunkCount() {
        return chunkCount;
    }
    
    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
}
