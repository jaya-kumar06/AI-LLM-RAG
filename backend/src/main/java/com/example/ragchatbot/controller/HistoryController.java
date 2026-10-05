package com.example.ragchatbot.controller;

import com.example.ragchatbot.model.ChatHistory;
import com.example.ragchatbot.model.User;
import com.example.ragchatbot.service.AuthService;
import com.example.ragchatbot.service.ChatHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat History Controller
 * 
 * Manages conversation history for users.
 * 
 * ENDPOINTS:
 * - GET /api/history/sessions - Get all sessions for user
 * - GET /api/history/{sessionId} - Get messages in a session
 * - POST /api/history/session - Create new session
 * - DELETE /api/history/{sessionId} - Delete a session
 */
@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class HistoryController {
    
    private final ChatHistoryService chatHistoryService;
    private final AuthService authService;
    
    public HistoryController(ChatHistoryService chatHistoryService, AuthService authService) {
        this.chatHistoryService = chatHistoryService;
        this.authService = authService;
    }
    
    /**
     * Create a new chat session
     */
    @PostMapping("/session")
    public ResponseEntity<?> createSession() {
        String sessionId = chatHistoryService.createSession();
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all sessions for current user
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessions", new ArrayList<>());
        
        // For now, return empty if no auth (guest mode)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(response);
        }
        
        try {
            // In a real implementation, get user from SecurityContext
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * Get messages for a specific session
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSessionHistory(@PathVariable String sessionId) {
        List<ChatHistory> history = chatHistoryService.getSessionHistory(sessionId);
        
        // Convert to response format
        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatHistory msg : history) {
            Map<String, Object> msgMap = new HashMap<>();
            msgMap.put("id", msg.getId());
            msgMap.put("role", msg.getRole());
            msgMap.put("content", msg.getContent());
            msgMap.put("sources", msg.getSources() != null ? msg.getSources() : "[]");
            msgMap.put("responseTimeMs", msg.getResponseTimeMs() != null ? msg.getResponseTimeMs() : 0L);
            msgMap.put("createdAt", msg.getCreatedAt().toString());
            messages.add(msgMap);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("messages", messages);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete a session and all its messages
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable String sessionId) {
        chatHistoryService.deleteSession(sessionId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Session deleted");
        return ResponseEntity.ok(response);
    }
}
