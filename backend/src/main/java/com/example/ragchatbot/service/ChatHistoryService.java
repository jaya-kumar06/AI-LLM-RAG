package com.example.ragchatbot.service;

import com.example.ragchatbot.model.ChatHistory;
import com.example.ragchatbot.model.User;
import com.example.ragchatbot.repository.ChatHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Chat History Service
 * 
 * Manages conversation persistence for multi-session support.
 * 
 * KEY INTERVIEW POINTS:
 * - Enables conversation continuity across page refreshes
 * - Session-based grouping for conversation context
 * - Stores metadata for analytics (response time, sources)
 */
@Service
public class ChatHistoryService {
    
    private final ChatHistoryRepository chatHistoryRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ChatHistoryService(ChatHistoryRepository chatHistoryRepository) {
        this.chatHistoryRepository = chatHistoryRepository;
    }
    
    /**
     * Create a new session ID
     */
    public String createSession() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Save a user message
     */
    public ChatHistory saveUserMessage(User user, String sessionId, String content) {
        ChatHistory message = new ChatHistory(user, sessionId, "user", content);
        return chatHistoryRepository.save(message);
    }
    
    /**
     * Save an assistant message with metadata
     */
    public ChatHistory saveAssistantMessage(User user, String sessionId, String content, 
                                            List<String> sources, Long responseTimeMs, 
                                            Integer contextChunksUsed) {
        ChatHistory message = new ChatHistory(user, sessionId, "assistant", content);
        
        // Convert sources list to JSON
        if (sources != null && !sources.isEmpty()) {
            try {
                message.setSources(objectMapper.writeValueAsString(sources));
            } catch (JsonProcessingException e) {
                message.setSources("[]");
            }
        }
        
        message.setResponseTimeMs(responseTimeMs);
        message.setContextChunksUsed(contextChunksUsed);
        
        return chatHistoryRepository.save(message);
    }
    
    /**
     * Get conversation history for a session
     */
    public List<ChatHistory> getSessionHistory(String sessionId) {
        return chatHistoryRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
    
    /**
     * Get recent messages for context (used for follow-up questions)
     */
    public List<ChatHistory> getRecentMessages(String sessionId, int limit) {
        return chatHistoryRepository.findRecentBySessionId(sessionId, limit);
    }
    
    /**
     * Get all sessions for a user
     */
    public List<String> getUserSessions(User user) {
        return chatHistoryRepository.findDistinctSessionIdsByUser(user);
    }
    
    /**
     * Delete a session
     */
    @Transactional
    public void deleteSession(String sessionId) {
        chatHistoryRepository.deleteBySessionId(sessionId);
    }
    
    /**
     * Build conversation context string from history
     * (Used for LLM context window)
     */
    public String buildConversationContext(String sessionId, int maxMessages) {
        List<ChatHistory> history = getRecentMessages(sessionId, maxMessages);
        
        StringBuilder context = new StringBuilder();
        for (ChatHistory msg : history) {
            context.append(msg.getRole().toUpperCase())
                   .append(": ")
                   .append(msg.getContent())
                   .append("\n\n");
        }
        
        return context.toString();
    }
}
