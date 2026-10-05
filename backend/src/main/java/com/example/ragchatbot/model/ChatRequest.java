package com.example.ragchatbot.model;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Chat Request Model
 * 
 * Represents an incoming chat request from the frontend.
 * Contains the user's question and optional conversation history.
 */
public class ChatRequest {
    
    @NotBlank(message = "Question cannot be empty")
    private String question;
    
    private String sessionId; // For conversation persistence
    
    private List<Message> conversationHistory;
    
    // Getters and Setters
    public String getQuestion() {
        return question;
    }
    
    public void setQuestion(String question) {
        this.question = question;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public List<Message> getConversationHistory() {
        return conversationHistory;
    }
    
    public void setConversationHistory(List<Message> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }
    
    /**
     * Inner class representing a single message in conversation history
     */
    public static class Message {
        private String role; // "user" or "assistant"
        private String content;
        
        public String getRole() {
            return role;
        }
        
        public void setRole(String role) {
            this.role = role;
        }
        
        public String getContent() {
            return content;
        }
        
        public void setContent(String content) {
            this.content = content;
        }
    }
}
