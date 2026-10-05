package com.example.ragchatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RAG Chatbot Application - Main Entry Point
 * 
 * This application demonstrates:
 * 1. RAG (Retrieval-Augmented Generation) using LangChain4j
 * 2. Document processing (PDF, DOCX, TXT)
 * 3. Vector embeddings with sentence-transformers
 * 4. In-memory vector store for similarity search
 * 5. LLM integration (OpenAI or Ollama)
 * 
 * @author Your Name
 */
@SpringBootApplication
public class RagChatbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagChatbotApplication.class, args);
        
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║     RAG Chatbot Backend Started Successfully   ║");
        System.out.println("╠════════════════════════════════════════════════╣");
        System.out.println("║  API Base URL:  http://localhost:8080          ║");
        System.out.println("║  H2 Console:    http://localhost:8080/h2-console║");
        System.out.println("║                                                ║");
        System.out.println("║  Endpoints:                                    ║");
        System.out.println("║    POST /api/chat         - Chat with AI       ║");
        System.out.println("║    POST /api/documents    - Upload documents   ║");
        System.out.println("║    GET  /api/documents    - List documents     ║");
        System.out.println("║    GET  /api/health       - Health check       ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
    }
}
