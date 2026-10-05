package com.example.ragchatbot.model;

import java.util.List;

/**
 * Chat Response Model
 * 
 * Represents the response sent back to the frontend.
 * Includes the AI's answer and the context sources used.
 */
public class ChatResponse {
    
    private String answer;
    private List<String> sources;
    private int contextChunksUsed;
    private long responseTimeMs;
    private String sessionId; // For conversation persistence
    
    // Constructors
    public ChatResponse() {}
    
    public ChatResponse(String answer) {
        this.answer = answer;
    }
    
    public ChatResponse(String answer, List<String> sources, int contextChunksUsed, long responseTimeMs) {
        this.answer = answer;
        this.sources = sources;
        this.contextChunksUsed = contextChunksUsed;
        this.responseTimeMs = responseTimeMs;
    }
    
    // Getters and Setters
    public String getAnswer() {
        return answer;
    }
    
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    
    public List<String> getSources() {
        return sources;
    }
    
    public void setSources(List<String> sources) {
        this.sources = sources;
    }
    
    public int getContextChunksUsed() {
        return contextChunksUsed;
    }
    
    public void setContextChunksUsed(int contextChunksUsed) {
        this.contextChunksUsed = contextChunksUsed;
    }
    
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
