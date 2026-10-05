package com.example.ragchatbot.controller;

import com.example.ragchatbot.model.ChatRequest;
import com.example.ragchatbot.model.ChatResponse;
import com.example.ragchatbot.model.User;
import com.example.ragchatbot.service.AuthService;
import com.example.ragchatbot.service.ChatHistoryService;
import com.example.ragchatbot.service.RagService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Chat Controller
 * 
 * REST API endpoint for chat functionality.
 * Now includes conversation history persistence.
 * 
 * ENDPOINT: POST /api/chat
 * 
 * REQUEST:
 * {
 *   "question": "What is machine learning?",
 *   "sessionId": "uuid-session-id",
 *   "conversationHistory": [...]
 * }
 * 
 * RESPONSE:
 * {
 *   "answer": "Machine learning is...",
 *   "sources": ["document1.pdf"],
 *   "contextChunksUsed": 3,
 *   "responseTimeMs": 1250
 * }
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class ChatController {

    private final RagService ragService;
    private final ChatHistoryService chatHistoryService;
    private final AuthService authService;

    public ChatController(RagService ragService, 
                         ChatHistoryService chatHistoryService,
                         AuthService authService) {
        this.ragService = ragService;
        this.chatHistoryService = chatHistoryService;
        this.authService = authService;
    }

    /**
     * Main chat endpoint with RAG
     * 
     * Flow:
     * 1. Receive question from frontend
     * 2. Save user message to history
     * 3. RagService embeds question
     * 4. Search pgvector for similar chunks (with MMR)
     * 5. Build prompt with context
     * 6. LLM generates response
     * 7. Save assistant message to history
     * 8. Return response with sources
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("📨 Received chat request");
        System.out.println("   Question: " + request.getQuestion());
        System.out.println("   Session: " + (request.getSessionId() != null ? request.getSessionId() : "none"));
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        try {
            // Get user if authenticated (optional)
            User user = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // In a real app, extract from SecurityContext
                // For now, we allow anonymous chat
            }
            
            // Create session if not provided
            String sessionId = request.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = chatHistoryService.createSession();
            }
            
            // Save user message to history
            chatHistoryService.saveUserMessage(user, sessionId, request.getQuestion());
            
            // Process with RAG
            ChatResponse response = ragService.chat(request);
            
            // Save assistant message to history
            chatHistoryService.saveAssistantMessage(
                user, 
                sessionId, 
                response.getAnswer(),
                response.getSources(),
                response.getResponseTimeMs(),
                response.getContextChunksUsed()
            );
            
            // Include sessionId in response
            response.setSessionId(sessionId);
            
            System.out.println("\n✅ Response generated successfully");
            System.out.println("   Time: " + response.getResponseTimeMs() + "ms");
            System.out.println("   Context chunks: " + response.getContextChunksUsed());
            System.out.println("   Session: " + sessionId);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ Error processing chat: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Sorry, I encountered an error. Please try again."));
        }
    }

    /**
     * Simple chat endpoint (no RAG, for testing LLM directly)
     */
    @PostMapping("/simple")
    public ResponseEntity<ChatResponse> simpleChat(@RequestBody ChatRequest request) {
        System.out.println("\n📨 Simple chat (no RAG): " + request.getQuestion());
        
        try {
            String answer = ragService.simpleChat(request.getQuestion());
            return ResponseEntity.ok(new ChatResponse(answer));
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(new ChatResponse("Error: " + e.getMessage()));
        }
    }
}
