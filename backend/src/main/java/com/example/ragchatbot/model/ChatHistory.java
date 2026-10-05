package com.example.ragchatbot.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * ChatHistory Entity
 * 
 * Persists conversation history for each user.
 * Enables conversation continuity across sessions.
 * 
 * KEY INTERVIEW POINTS:
 * - Stateful conversations require persistence
 * - Links messages to users for multi-tenant support
 * - Stores metadata like sources and response time for analytics
 */
@Entity
@Table(name = "chat_history", indexes = {
    @Index(name = "idx_user_session", columnList = "user_id, session_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class ChatHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Column(nullable = false)
    private String role; // "user" or "assistant"
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    @Column(columnDefinition = "TEXT")
    private String sources; // JSON array of source filenames
    
    @Column(name = "response_time_ms")
    private Long responseTimeMs;
    
    @Column(name = "context_chunks_used")
    private Integer contextChunksUsed;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Constructors
    public ChatHistory() {}
    
    public ChatHistory(User user, String sessionId, String role, String content) {
        this.user = user;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getSources() { return sources; }
    public void setSources(String sources) { this.sources = sources; }
    
    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
    
    public Integer getContextChunksUsed() { return contextChunksUsed; }
    public void setContextChunksUsed(Integer contextChunksUsed) { this.contextChunksUsed = contextChunksUsed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
