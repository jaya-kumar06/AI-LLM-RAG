package com.example.ragchatbot.service;

import com.example.ragchatbot.model.ChatRequest;
import com.example.ragchatbot.model.ChatResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG (Retrieval-Augmented Generation) Service
 * 
 * This is the CORE of the RAG application!
 * 
 * RAG FLOW:
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │                                                                      │
 * │  1. USER QUESTION                                                    │
 * │     "What is machine learning?"                                      │
 * │                        │                                             │
 * │                        ▼                                             │
 * │  2. EMBED QUESTION                                                   │
 * │     → [0.023, -0.156, ..., 0.089] (384 dimensions)                   │
 * │                        │                                             │
 * │                        ▼                                             │
 * │  3. MMR SEARCH (Maximal Marginal Relevance)                          │
 * │     Find diverse, relevant chunks (not just most similar)            │
 * │     Balances relevance and diversity                                 │
 * │                        │                                             │
 * │                        ▼                                             │
 * │  4. BUILD PROMPT WITH CONTEXT                                        │
 * │     System: You are a helpful assistant...                           │
 * │     Context: [retrieved chunks]                                      │
 * │     Question: What is machine learning?                              │
 * │                        │                                             │
 * │                        ▼                                             │
 * │  5. LLM GENERATES RESPONSE                                           │
 * │     "Machine learning is a subset of AI that..."                     │
 * │                                                                      │
 * └──────────────────────────────────────────────────────────────────────┘
 * 
 * WHY RAG?
 * - LLMs have knowledge cutoff dates
 * - LLMs can hallucinate (make up facts)
 * - RAG grounds responses in YOUR documents
 * - Reduces hallucination by providing real context
 * 
 * WHY MMR?
 * - Basic similarity search returns redundant chunks
 * - MMR balances relevance + diversity
 * - Better coverage of different aspects of the answer
 */
@Service
public class RagService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatLanguageModel chatModel;

    @Value("${rag.max.results:3}")
    private int maxResults;
    
    @Value("${rag.fetch.k:10}")
    private int fetchK; // Fetch more candidates for MMR
    
    @Value("${rag.lambda.mult:0.7}")
    private double lambdaMult; // Balance between relevance (1.0) and diversity (0.0)

    // Minimum similarity score (0-1). Lower = more results but less relevant
    private static final double MIN_SCORE = 0.5;

    public RagService(EmbeddingModel embeddingModel,
                     EmbeddingStore<TextSegment> embeddingStore,
                     ChatLanguageModel chatModel) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
    }

    /**
     * Process a chat request using RAG
     * 
     * @param request The chat request containing the question
     * @return ChatResponse with answer and sources
     */
    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        String question = request.getQuestion();
        
        System.out.println("\n🔍 RAG Query: " + question);
        
        // Check if this is a casual greeting - skip RAG search
        if (isCasualMessage(question)) {
            System.out.println("   ℹ️ Casual message detected - skipping document search");
            String prompt = buildPrompt(question, "No relevant documents found.", request.getConversationHistory());
            String answer = chatModel.generate(prompt);
            long responseTime = System.currentTimeMillis() - startTime;
            return new ChatResponse(answer, new ArrayList<>(), 0, responseTime);
        }
        
        // Step 1: Embed the question
        // Convert the question to a vector (same dimension as stored documents)
        Embedding questionEmbedding = embeddingModel.embed(question).content();
        System.out.println("   ✅ Question embedded (384 dimensions)");
        
        // Step 2: Search for similar documents in pgvector (fetch more for MMR)
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(questionEmbedding)
                .maxResults(fetchK) // Fetch more candidates
                .minScore(MIN_SCORE)
                .build();
        
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> allMatches = searchResult.matches();
        
        System.out.println("   ✅ Found " + allMatches.size() + " candidate chunks");
        
        // Step 3: Apply MMR (Maximal Marginal Relevance) for diversity
        List<EmbeddingMatch<TextSegment>> matches = applyMMR(
            questionEmbedding, allMatches, maxResults, lambdaMult
        );
        
        System.out.println("   ✅ Selected " + matches.size() + " diverse chunks via MMR");
        
        // Step 3: Build context from retrieved chunks
        String context = buildContext(matches);
        List<String> sources = extractSources(matches);
        
        // Step 4: Build the prompt
        String prompt = buildPrompt(question, context, request.getConversationHistory());
        
        // Step 5: Generate response using LLM
        System.out.println("   ⏳ Generating response...");
        String answer = chatModel.generate(prompt);
        System.out.println("   ✅ Response generated!");
        
        long responseTime = System.currentTimeMillis() - startTime;
        
        return new ChatResponse(answer, sources, matches.size(), responseTime);
    }

    /**
     * Build context string from matched document chunks
     * 
     * Each match contains:
     * - score: Similarity score (0-1)
     * - embedded: The text segment
     * - embeddingId: Unique ID in the store
     */
    private String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches.isEmpty()) {
            return "No relevant documents found.";
        }
        
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            TextSegment segment = match.embedded();
            double score = match.score();
            
            String source = segment.metadata().getString("source");
            
            context.append("--- Document ").append(i + 1);
            context.append(" (").append(source != null ? source : "Unknown");
            context.append(", Score: ").append(String.format("%.2f", score));
            context.append(") ---\n");
            context.append(segment.text());
            context.append("\n\n");
            
            System.out.println("     Chunk " + (i + 1) + ": " + 
                    (source != null ? source : "Unknown") + 
                    " (score: " + String.format("%.3f", score) + ")");
        }
        
        return context.toString();
    }

    /**
     * Extract source filenames from matches
     */
    private List<String> extractSources(List<EmbeddingMatch<TextSegment>> matches) {
        return matches.stream()
                .map(m -> m.embedded().metadata().getString("source"))
                .filter(s -> s != null)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Build the final prompt for the LLM
     * 
     * PROMPT ENGINEERING:
     * - System instruction: Defines AI behavior
     * - Context: Retrieved document chunks
     * - Conversation history: Previous messages (for multi-turn)
     * - Question: Current user question
     * 
     * The key is to instruct the LLM to:
     * 1. Use ONLY the provided context
     * 2. Say "I don't know" if context doesn't help
     * 3. Be concise and accurate
     */
    private String buildPrompt(String question, String context, 
                               List<ChatRequest.Message> history) {
        StringBuilder prompt = new StringBuilder();
        
        // Check if this is a casual greeting (not a real question)
        boolean isCasualGreeting = isCasualMessage(question);
        boolean hasRelevantContext = !context.equals("No relevant documents found.");
        
        if (isCasualGreeting) {
            // For greetings, respond naturally without document context
            prompt.append("""
                You are a friendly AI assistant for a document Q&A chatbot.
                
                The user sent a casual greeting or message. Respond briefly and naturally.
                Keep your response short (1-2 sentences max).
                You can mention that you're here to help answer questions about their uploaded documents.
                
                """);
            prompt.append("USER MESSAGE: ").append(question).append("\n\n");
            prompt.append("RESPONSE: ");
        } else {
            // For actual questions, use RAG with document context
            prompt.append("""
                You are a helpful AI assistant that answers questions based on the provided context.
                
                INSTRUCTIONS:
                1. Answer ONLY based on the context provided below
                2. If the context doesn't contain relevant information, say "I don't have information about that in my documents"
                3. Be concise and direct - avoid lengthy explanations unless asked
                4. If you quote from the context, cite the source briefly
                5. Do NOT make up information
                
                """);
            
            // Add context only if we have relevant documents
            if (hasRelevantContext) {
                prompt.append("CONTEXT:\n");
                prompt.append(context);
                prompt.append("\n");
            } else {
                prompt.append("CONTEXT: No relevant documents found.\n\n");
            }
            
            // Add conversation history if present
            if (history != null && !history.isEmpty()) {
                prompt.append("CONVERSATION HISTORY:\n");
                for (ChatRequest.Message msg : history) {
                    prompt.append(msg.getRole().toUpperCase()).append(": ");
                    prompt.append(msg.getContent()).append("\n");
                }
                prompt.append("\n");
            }
            
            // Add the current question
            prompt.append("QUESTION: ").append(question).append("\n\n");
            prompt.append("ANSWER: ");
        }
        
        return prompt.toString();
    }
    
    /**
     * Check if the message is a casual greeting rather than a real question
     */
    private boolean isCasualMessage(String message) {
        if (message == null) return true;
        
        String lower = message.toLowerCase().trim();
        
        // If message contains question words, it's NOT casual (even if it starts with "hi")
        if (lower.contains("what") || lower.contains("how") || lower.contains("why") || 
            lower.contains("when") || lower.contains("where") || lower.contains("who") ||
            lower.contains("tell") || lower.contains("explain") || lower.contains("describe") ||
            lower.contains("show") || lower.contains("list") || lower.contains("find") ||
            lower.contains("?")) {
            return false;
        }
        
        // Pure greetings only (exact match or very short)
        String[] pureGreetings = {
            "hi", "hello", "hey", "hii", "hiii", "yo", "sup",
            "good morning", "good afternoon", "good evening", "good night",
            "how are you", "what's up", "whats up", "wassup",
            "thanks", "thank you", "thx", "ty",
            "bye", "goodbye", "see you", "cya",
            "ok", "okay", "cool", "nice", "great"
        };
        
        for (String pattern : pureGreetings) {
            if (lower.equals(pattern) || lower.equals(pattern + "!") || lower.equals(pattern + ".")) {
                return true;
            }
        }
        
        // Very short messages (1-2 words) without question indicators
        String[] words = lower.split("\\s+");
        if (words.length <= 2) {
            return true;
        }
        
        return false;
    }

    /**
     * Simple chat without RAG (for testing)
     */
    public String simpleChat(String message) {
        return chatModel.generate(message);
    }
    
    /**
     * Maximal Marginal Relevance (MMR) Selection
     * 
     * MMR balances two factors:
     * 1. Relevance: How similar is the chunk to the query?
     * 2. Diversity: How different is it from already selected chunks?
     * 
     * Formula: MMR = λ * sim(query, doc) - (1-λ) * max(sim(doc, selected_docs))
     * 
     * KEY INTERVIEW POINTS:
     * - λ (lambda) near 1.0 = prioritize relevance
     * - λ near 0.0 = prioritize diversity
     * - λ = 0.7 is a good balance
     * - Prevents returning redundant/similar chunks
     * 
     * @param queryEmbedding The embedded question
     * @param candidates All candidate matches from initial search
     * @param k Number of results to return
     * @param lambda Balance parameter (0-1)
     * @return Selected diverse matches
     */
    private List<EmbeddingMatch<TextSegment>> applyMMR(
            Embedding queryEmbedding,
            List<EmbeddingMatch<TextSegment>> candidates,
            int k,
            double lambda) {
        
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<EmbeddingMatch<TextSegment>> selected = new ArrayList<>();
        Set<Integer> selectedIndices = new HashSet<>();
        
        // Pre-compute query similarities (already in match.score())
        // Pre-compute embeddings for diversity calculation
        List<float[]> candidateVectors = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : candidates) {
            // Embed the text content for diversity calculation
            Embedding emb = embeddingModel.embed(match.embedded().text()).content();
            candidateVectors.add(emb.vector());
        }
        
        float[] queryVector = queryEmbedding.vector();
        
        // Iteratively select k documents
        for (int i = 0; i < Math.min(k, candidates.size()); i++) {
            double bestScore = Double.NEGATIVE_INFINITY;
            int bestIndex = -1;
            
            for (int j = 0; j < candidates.size(); j++) {
                if (selectedIndices.contains(j)) continue;
                
                EmbeddingMatch<TextSegment> candidate = candidates.get(j);
                double relevance = candidate.score();
                
                // Calculate max similarity to already selected documents
                double maxSimilarityToSelected = 0.0;
                for (int selectedIdx : selectedIndices) {
                    double sim = cosineSimilarity(
                        candidateVectors.get(j), 
                        candidateVectors.get(selectedIdx)
                    );
                    maxSimilarityToSelected = Math.max(maxSimilarityToSelected, sim);
                }
                
                // MMR score: balance relevance and diversity
                double mmrScore = lambda * relevance - (1 - lambda) * maxSimilarityToSelected;
                
                if (mmrScore > bestScore) {
                    bestScore = mmrScore;
                    bestIndex = j;
                }
            }
            
            if (bestIndex >= 0) {
                selected.add(candidates.get(bestIndex));
                selectedIndices.add(bestIndex);
            }
        }
        
        return selected;
    }
    
    /**
     * Calculate cosine similarity between two vectors
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
