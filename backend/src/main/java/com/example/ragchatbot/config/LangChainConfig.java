package com.example.ragchatbot.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j Configuration with PostgreSQL + pgvector
 * 
 * COMPONENTS:
 * 1. EmbeddingModel - Converts text to vectors (384 dimensions)
 * 2. EmbeddingStore - PostgreSQL + pgvector for vector storage
 * 3. ChatLanguageModel - Ollama (local) or OpenAI (cloud)
 */
@Configuration
public class LangChainConfig {

    @Value("${llm.provider:ollama}")
    private String llmProvider;

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String openaiModel;

    @Value("${ollama.base.url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:mistral}")
    private String ollamaModel;

    @Value("${pgvector.host:localhost}")
    private String pgHost;

    @Value("${pgvector.port:5432}")
    private int pgPort;

    @Value("${pgvector.database:ragchatbot}")
    private String pgDatabase;

    @Value("${pgvector.user:postgres}")
    private String pgUser;

    @Value("${pgvector.password:postgres}")
    private String pgPassword;

    @Value("${pgvector.table:document_embeddings}")
    private String pgTable;

    /**
     * Embedding Model - Converts text to 384-dimensional vectors
     * Runs locally using ONNX runtime, no API key needed
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        System.out.println("🔧 Initializing Embedding Model: AllMiniLmL6V2 (384 dimensions)");
        return new AllMiniLmL6V2QuantizedEmbeddingModel();
    }

    /**
     * Vector Store - PostgreSQL + pgvector
     * Stores document embeddings for similarity search
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        System.out.println("🔧 Connecting to PostgreSQL + pgvector...");
        System.out.println("   Host: " + pgHost + ":" + pgPort);
        System.out.println("   Database: " + pgDatabase);
        System.out.println("   Table: " + pgTable);

        return PgVectorEmbeddingStore.builder()
                .host(pgHost)
                .port(pgPort)
                .database(pgDatabase)
                .user(pgUser)
                .password(pgPassword)
                .table(pgTable)
                .dimension(384)
                .createTable(true)
                .build();
    }

    /**
     * Chat Model - LLM for generating responses
     * Uses Ollama (local) by default, or OpenAI if configured
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if ("openai".equalsIgnoreCase(llmProvider) && !openaiApiKey.isBlank()) {
            System.out.println("🤖 Using OpenAI: " + openaiModel);
            return OpenAiChatModel.builder()
                    .apiKey(openaiApiKey)
                    .modelName(openaiModel)
                    .temperature(0.3)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        } else {
            System.out.println("🤖 Using Ollama (Local): " + ollamaModel);
            System.out.println("   URL: " + ollamaBaseUrl);
            return OllamaChatModel.builder()
                    .baseUrl(ollamaBaseUrl)
                    .modelName(ollamaModel)
                    .temperature(0.3)
                    .timeout(Duration.ofSeconds(120))
                    .build();
        }
    }
}
