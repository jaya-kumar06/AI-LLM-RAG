package com.example.ragchatbot.controller;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health Check Controller
 * 
 * Provides endpoints to check the status of various components:
 * - Application status
 * - Database connection
 * - LLM availability
 * - Embedding model status
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class HealthController {

    private final EmbeddingModel embeddingModel;
    private final ChatLanguageModel chatLanguageModel;

    @Value("${llm.provider:ollama}")
    private String llmProvider;

    @Value("${ollama.model:mistral}")
    private String ollamaModel;

    public HealthController(EmbeddingModel embeddingModel, 
                           ChatLanguageModel chatLanguageModel) {
        this.embeddingModel = embeddingModel;
        this.chatLanguageModel = chatLanguageModel;
    }

    /**
     * Basic health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("application", "RAG Chatbot");
        health.put("version", "1.0.0");
        
        // Check components
        Map<String, String> components = new LinkedHashMap<>();
        
        // Check embedding model
        try {
            embeddingModel.embed("test");
            components.put("embeddingModel", "UP");
        } catch (Exception e) {
            components.put("embeddingModel", "DOWN: " + e.getMessage());
        }
        
        // Check LLM
        try {
            chatLanguageModel.generate("Hi");
            components.put("llm", "UP (" + llmProvider + "/" + ollamaModel + ")");
        } catch (Exception e) {
            components.put("llm", "DOWN: " + e.getMessage());
        }
        
        health.put("components", components);
        
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed system info
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        
        info.put("application", Map.of(
                "name", "RAG Chatbot",
                "description", "Java Spring Boot RAG application with LangChain4j",
                "version", "1.0.0"
        ));
        
        info.put("technology", Map.of(
                "framework", "Spring Boot 3.2.5",
                "language", "Java 17",
                "aiLibrary", "LangChain4j 0.31.0",
                "vectorDatabase", "PostgreSQL + pgvector",
                "embeddingModel", "AllMiniLmL6V2 (384 dimensions)",
                "llmProvider", llmProvider
        ));
        
        info.put("endpoints", Map.of(
                "chat", "POST /api/chat",
                "uploadDocument", "POST /api/documents",
                "listDocuments", "GET /api/documents",
                "health", "GET /api/health"
        ));
        
        return ResponseEntity.ok(info);
    }
}
