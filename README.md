# 🤖 Java RAG Chatbot - Complete Learning Guide

A production-ready **Retrieval-Augmented Generation (RAG)** chatbot built with Java Spring Boot, React, PostgreSQL + pgvector, and LangChain4j. This project demonstrates modern AI/ML concepts for interview preparation and real-world applications.

---

## 📚 Table of Contents

1. [What is RAG?](#-what-is-rag)
2. [Architecture Overview](#-architecture-overview)
3. [Core Concepts Explained](#-core-concepts-explained)
4. [Technology Stack](#-technology-stack)
5. [Project Structure](#-project-structure)
6. [Setup & Installation](#-setup--installation)
7. [How It Works - Step by Step](#-how-it-works---step-by-step)
8. [Database & Vector Search](#-database--vector-search)
9. [Key Algorithms](#-key-algorithms)
10. [Interview Topics Covered](#-interview-topics-covered)
11. [API Reference](#-api-reference)
12. [Troubleshooting](#-troubleshooting)

---

## 🧠 What is RAG?

### The Problem with LLMs

Large Language Models (LLMs) like GPT, Mistral, LLaMA have limitations:

| Problem | Description |
|---------|-------------|
| **Knowledge Cutoff** | LLMs only know data up to their training date |
| **Hallucination** | LLMs can confidently make up false information |
| **No Private Data** | LLMs don't know your company's internal documents |
| **Generic Answers** | Without context, answers are generic, not specific |

### The RAG Solution

**RAG (Retrieval-Augmented Generation)** solves these problems by:

```
┌─────────────────────────────────────────────────────────────────┐
│                         RAG WORKFLOW                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. USER ASKS: "What is our refund policy?"                     │
│                          │                                      │
│                          ▼                                      │
│  2. RETRIEVE: Search your documents for relevant chunks         │
│     → Found: "Refunds are processed within 7 days..."           │
│                          │                                      │
│                          ▼                                      │
│  3. AUGMENT: Add retrieved context to the prompt                │
│     "Based on this context: [chunks], answer: ..."              │
│                          │                                      │
│                          ▼                                      │
│  4. GENERATE: LLM creates accurate, grounded response           │
│     "Our refund policy states that refunds are processed..."    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### Why RAG is Important

| Benefit | Explanation |
|---------|-------------|
| **Accuracy** | Answers are grounded in real documents |
| **Up-to-date** | Add new documents anytime, no retraining needed |
| **Transparency** | Can cite sources for every answer |
| **Privacy** | Your data stays in your database |
| **Cost-effective** | No need to fine-tune expensive models |

---

## 🏗 Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           SYSTEM ARCHITECTURE                           │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────┐         ┌──────────────────────────────────────────┐  │
│  │              │         │           SPRING BOOT BACKEND            │  │
│  │    REACT     │  HTTP   │  ┌─────────────────────────────────────┐ │  │
│  │   FRONTEND   │ ◄─────► │  │         REST Controllers            │ │  │
│  │              │         │  │  • ChatController (/api/chat)       │ │  │
│  │  • Chat UI   │         │  │  • DocumentController (/api/docs)   │ │  │
│  │  • Upload    │         │  │  • AuthController (/api/auth)       │ │  │
│  │  • Auth      │         │  │  • HistoryController (/api/history) │ │  │
│  └──────────────┘         │  └─────────────────────────────────────┘ │  │
│                           │                    │                      │  │
│                           │                    ▼                      │  │
│                           │  ┌─────────────────────────────────────┐ │  │
│                           │  │           SERVICES LAYER            │ │  │
│                           │  │                                     │ │  │
│                           │  │  ┌─────────────┐ ┌───────────────┐  │ │  │
│                           │  │  │ RagService  │ │DocumentService│  │ │  │
│                           │  │  │             │ │               │  │ │  │
│                           │  │  │ • Embed     │ │ • Parse PDF   │  │ │  │
│                           │  │  │ • Search    │ │ • Chunk text  │  │ │  │
│                           │  │  │ • MMR       │ │ • Deduplicate │  │ │  │
│                           │  │  │ • Generate  │ │ • Store       │  │ │  │
│                           │  │  └─────────────┘ └───────────────┘  │ │  │
│                           │  └─────────────────────────────────────┘ │  │
│                           └──────────────────────────────────────────┘  │
│                                            │                            │
│                    ┌───────────────────────┼───────────────────────┐    │
│                    │                       │                       │    │
│                    ▼                       ▼                       ▼    │
│  ┌──────────────────────┐  ┌──────────────────────┐  ┌───────────────┐  │
│  │  PostgreSQL+pgvector │  │   Embedding Model    │  │  Ollama LLM   │  │
│  │                      │  │                      │  │               │  │
│  │  • Users table       │  │  AllMiniLM-L6-v2     │  │  Mistral 7B   │  │
│  │  • Chat history      │  │  384 dimensions      │  │  Local/Free   │  │
│  │  • Document hashes   │  │  Runs locally        │  │               │  │
│  │  • Vector embeddings │  │                      │  │               │  │
│  └──────────────────────┘  └──────────────────────┘  └───────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📖 Core Concepts Explained

### 1. Embeddings (Vector Representations)

**What are Embeddings?**

Embeddings convert text into numerical vectors that capture semantic meaning.

```
Text: "Machine learning is a subset of AI"
      ↓ Embedding Model
Vector: [0.023, -0.156, 0.089, ..., 0.045]  (384 numbers)
```

**Why Embeddings Matter:**

- Similar meanings → Similar vectors
- "Dog" and "Puppy" have similar vectors
- "Dog" and "Refrigerator" have very different vectors

**In Code (RagService.java):**
```java
// Convert question to embedding
Embedding questionEmbedding = embeddingModel.embed(question).content();
// Result: 384-dimensional vector
```

**Embedding Model Used:** `AllMiniLM-L6-v2`
- 384 dimensions
- Fast and lightweight
- Runs locally (no API calls)
- Good for semantic similarity

---

### 2. Vector Database (pgvector)

**What is a Vector Database?**

Traditional databases search by exact matches:
```sql
SELECT * FROM products WHERE name = 'laptop'
```

Vector databases search by **similarity**:
```sql
SELECT * FROM documents 
ORDER BY embedding <-> query_embedding  -- Cosine distance
LIMIT 5
```

**pgvector Operations:**

| Operator | Name | Description |
|----------|------|-------------|
| `<->` | L2 distance | Euclidean distance (smaller = more similar) |
| `<=>` | Cosine distance | Angle between vectors (smaller = more similar) |
| `<#>` | Inner product | Dot product (larger = more similar) |

**Why pgvector?**
- Uses familiar PostgreSQL
- ACID compliant
- Can combine with regular SQL
- No separate vector DB needed

---

### 3. Chunking Strategy

**Why Chunk Documents?**

LLMs have context limits. A 100-page PDF won't fit in a prompt.

**Chunking Process:**
```
Original Document (10,000 words)
         │
         ▼
┌─────────────────────────────────────┐
│         CHUNKING ALGORITHM          │
│  • Chunk size: 500 characters       │
│  • Overlap: 50 characters           │
└─────────────────────────────────────┘
         │
         ▼
Chunk 1: "Machine learning is a subset of artificial..."  [chars 0-500]
Chunk 2: "...artificial intelligence that enables..."     [chars 450-950]
Chunk 3: "...enables computers to learn from data..."     [chars 900-1400]
         ...
```

**Why Overlap?**

Overlap prevents losing context at chunk boundaries:

```
Without overlap: "The company was founded in" | "1995 by John Smith"
                 ↑ Lost connection between sentences

With overlap:    "The company was founded in" | "founded in 1995 by John Smith"
                 ↑ Context preserved
```

**In Code (DocumentService.java):**
```java
DocumentSplitter splitter = DocumentSplitters.recursive(
    chunkSize,    // 500 characters
    chunkOverlap  // 50 characters overlap
);
List<TextSegment> chunks = splitter.split(document);
```

---

### 4. Semantic Search vs Keyword Search

**Keyword Search (Traditional):**
```
Query: "How to fix a broken laptop"
Matches: Documents containing exact words "fix", "broken", "laptop"
Problem: Won't find "repair malfunctioning notebook"
```

**Semantic Search (Vector-based):**
```
Query: "How to fix a broken laptop"
Embedding: [0.23, -0.15, ...]
Matches: Documents with similar MEANING
✓ Finds: "repair malfunctioning notebook"
✓ Finds: "troubleshoot computer issues"
```

**In Code:**
```java
EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
    .queryEmbedding(questionEmbedding)  // Vector of the question
    .maxResults(10)                      // Get top 10 matches
    .minScore(0.5)                       // Minimum similarity threshold
    .build();

EmbeddingSearchResult<TextSegment> results = embeddingStore.search(searchRequest);
```

---

### 5. MMR (Maximal Marginal Relevance)

**The Problem with Simple Similarity:**

If you search for "What is Python?", top results might be:
1. "Python is a programming language" (score: 0.95)
2. "Python is a popular programming language" (score: 0.94)
3. "Python is a widely-used programming language" (score: 0.93)

All three say the same thing! This is **redundant**.

**MMR Solution:**

MMR balances **relevance** and **diversity**:

```
MMR Score = λ × Relevance - (1-λ) × MaxSimilarityToSelected
```

Where:
- `λ = 0.7` (default) - 70% relevance, 30% diversity
- `λ = 1.0` - Only relevance (may be redundant)
- `λ = 0.0` - Only diversity (may miss relevant docs)

**MMR Result:**
1. "Python is a programming language" (most relevant)
2. "Python was created by Guido van Rossum" (different info)
3. "Python is used for web, AI, and scripting" (different info)

**In Code (RagService.java):**
```java
private List<EmbeddingMatch<TextSegment>> applyMMR(
        Embedding queryEmbedding,
        List<EmbeddingMatch<TextSegment>> candidates,
        int k,           // Number to select
        double lambda) { // Balance factor (0.7)
    
    // For each candidate, calculate:
    // MMR = lambda * relevance - (1-lambda) * maxSimilarityToAlreadySelected
    
    for (int i = 0; i < k; i++) {
        double bestScore = Double.NEGATIVE_INFINITY;
        for (candidate : candidates) {
            double relevance = candidate.score();
            double diversity = 1 - maxSimilarityToSelected(candidate, selected);
            double mmrScore = lambda * relevance + (1-lambda) * diversity;
            // Select candidate with best MMR score
        }
    }
}
```

---

### 6. Prompt Engineering

**What is Prompt Engineering?**

Crafting instructions for the LLM to get better responses.

**Bad Prompt:**
```
Answer this: What is machine learning?
```

**Good Prompt (RAG):**
```
You are a helpful AI assistant that answers questions based on provided context.

INSTRUCTIONS:
1. Answer ONLY based on the context below
2. If context doesn't help, say "I don't have that information"
3. Be concise and cite sources
4. Do NOT make up information

CONTEXT:
[Retrieved document chunks here]

QUESTION: What is machine learning?

ANSWER:
```

**Why This Works:**
- Clear role definition
- Explicit constraints
- Provides context
- Reduces hallucination

---

### 7. Document Deduplication (SHA-256)

**Problem:** User uploads same document twice → duplicate embeddings → wasted storage

**Solution:** Hash each document and check before processing

```java
public String calculateHash(byte[] content) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(content);
    return bytesToHex(hash);  // e.g., "a1b2c3d4e5..."
}

// Before processing
Optional<DocumentHash> existing = hashRepository.findByHash(hash);
if (existing.isPresent()) {
    return "Document already exists!";  // Skip processing
}
```

---

### 8. JWT Authentication

**JWT (JSON Web Token) Flow:**

```
1. User Login
   Client: POST /api/auth/login {username, password}
   Server: Validates credentials
   Server: Returns JWT token
   
2. Authenticated Requests
   Client: GET /api/chat
           Header: Authorization: Bearer <jwt-token>
   Server: Validates token
   Server: Returns response

3. Token Structure
   eyJhbGciOiJIUzM4NCJ9.        ← Header (algorithm)
   eyJzdWIiOiJ1c2VyMSIsImlh... ← Payload (user data, expiry)
   2tsaWsge8IFHcF0sVu2AE1P6... ← Signature (verification)
```

**In Code (JwtUtil.java):**
```java
public String generateToken(String username) {
    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey(), SignatureAlgorithm.HS384)
        .compact();
}
```

---

## 🛠 Technology Stack

### Backend

| Technology | Purpose | Why Chosen |
|------------|---------|------------|
| **Java 21** | Language | Enterprise standard, strong typing |
| **Spring Boot 3.2** | Framework | Production-ready, dependency injection |
| **LangChain4j** | AI/RAG Library | Java-native LangChain alternative |
| **PostgreSQL + pgvector** | Vector Database | SQL + vectors in one DB |
| **Ollama + Mistral** | Local LLM | Free, private, no API costs |
| **AllMiniLM-L6-v2** | Embedding Model | Fast, accurate, runs locally |
| **Spring Security + JWT** | Authentication | Stateless, scalable auth |

### Frontend

| Technology | Purpose |
|------------|---------|
| **React 18** | UI Framework |
| **Vite** | Build Tool |
| **Axios** | HTTP Client |
| **CSS3** | Styling |

### Infrastructure

| Technology | Purpose |
|------------|---------|
| **Docker** | PostgreSQL + pgvector |
| **Maven** | Build & Dependencies |
| **Spring DevTools** | Hot Reload |

---

## 📁 Project Structure

```
java-rag-chatbot/
├── backend/
│   ├── src/main/java/com/example/ragchatbot/
│   │   ├── RagChatbotApplication.java      # Main entry point
│   │   ├── config/
│   │   │   ├── LangChainConfig.java        # AI models configuration
│   │   │   ├── CorsConfig.java             # CORS settings
│   │   │   └── WebConfig.java              # Web configuration
│   │   ├── controller/
│   │   │   ├── ChatController.java         # /api/chat endpoints
│   │   │   ├── DocumentController.java     # /api/documents endpoints
│   │   │   ├── AuthController.java         # /api/auth endpoints
│   │   │   └── HistoryController.java      # /api/history endpoints
│   │   ├── service/
│   │   │   ├── RagService.java             # ⭐ Core RAG logic
│   │   │   ├── DocumentService.java        # Document processing
│   │   │   ├── AuthService.java            # Authentication
│   │   │   └── ChatHistoryService.java     # History persistence
│   │   ├── model/
│   │   │   ├── ChatRequest.java            # Request DTOs
│   │   │   ├── ChatResponse.java           # Response DTOs
│   │   │   ├── User.java                   # User entity
│   │   │   ├── ChatHistory.java            # History entity
│   │   │   └── DocumentHash.java           # Deduplication entity
│   │   ├── repository/
│   │   │   ├── UserRepository.java
│   │   │   ├── ChatHistoryRepository.java
│   │   │   └── DocumentHashRepository.java
│   │   └── security/
│   │       ├── SecurityConfig.java         # Security configuration
│   │       ├── JwtUtil.java                # JWT utilities
│   │       └── JwtAuthFilter.java          # JWT filter
│   └── src/main/resources/
│       └── application.properties          # Configuration
│
├── frontend/
│   ├── src/
│   │   ├── App.jsx                         # Main component
│   │   ├── components/
│   │   │   ├── ChatMessage.jsx             # Message display
│   │   │   ├── DocumentUpload.jsx          # File upload
│   │   │   └── AuthModal.jsx               # Login/Register
│   │   └── services/
│   │       └── api.js                      # API client
│   └── package.json
│
├── uploads/                                # Uploaded documents
├── setup-database.sql                      # DB initialization
└── README.md                               # This file
```

---

## 🚀 Setup & Installation

### Prerequisites

- Java 21 (JDK)
- Node.js 18+
- Docker Desktop
- Ollama

### Step 1: Start PostgreSQL + pgvector

```bash
docker run -d \
  --name pgvector \
  -e POSTGRES_PASSWORD=postgres \
  -p 5433:5432 \
  pgvector/pgvector:pg16
```

Create database:
```bash
docker exec -it pgvector psql -U postgres -c "CREATE DATABASE ragchatbot;"
```

### Step 2: Start Ollama (Local LLM)

```bash
# Pull the Mistral model
ollama pull mistral

# Ollama runs automatically on port 11434
```

### Step 3: Start Backend

```bash
cd backend

# Set JAVA_HOME if needed
set JAVA_HOME=C:\Program Files\Java\jdk-21

# Run with Maven
mvn spring-boot:run
```

Backend starts at: http://localhost:8080

### Step 4: Start Frontend

```bash
cd frontend
npm install
npm start
```

Frontend starts at: http://localhost:3000

---

## ⚙️ How It Works - Step by Step

### Document Upload Flow

```
1. User selects PDF file
         │
         ▼
2. Frontend sends to POST /api/documents/upload
         │
         ▼
3. DocumentService receives file
         │
         ▼
4. Calculate SHA-256 hash
         │
         ├── Hash exists? → Return "Already uploaded"
         │
         ▼
5. Parse PDF using Apache PDFBox
         │
         ▼
6. Split into chunks (500 chars, 50 overlap)
         │
         ▼
7. For each chunk:
   ├── Generate embedding (384-dim vector)
   └── Store in pgvector
         │
         ▼
8. Save hash to prevent duplicates
         │
         ▼
9. Return success response
```

### Chat Query Flow

```
1. User asks: "What is the refund policy?"
         │
         ▼
2. RagService.chat() called
         │
         ▼
3. Check if casual greeting
   ├── Yes → Short response, skip RAG
   │
   ▼
4. Embed the question
   "What is the refund policy?" → [0.23, -0.15, ...]
         │
         ▼
5. Search pgvector for similar chunks
   SELECT * FROM embeddings
   ORDER BY embedding <=> query_embedding
   LIMIT 10
         │
         ▼
6. Apply MMR for diversity
   Select top 3 diverse, relevant chunks
         │
         ▼
7. Build prompt with context
   "You are a helpful assistant...
    CONTEXT: [chunks]
    QUESTION: What is the refund policy?"
         │
         ▼
8. Send to Ollama/Mistral LLM
         │
         ▼
9. Return response with sources
   {
     "answer": "The refund policy states...",
     "sources": ["policy.pdf"],
     "chunksUsed": 3
   }
```

---

## 🗄 Database & Vector Search

### Tables

```sql
-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Chat history
CREATE TABLE chat_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    session_id VARCHAR(100),
    role VARCHAR(20),  -- 'user' or 'assistant'
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Document deduplication
CREATE TABLE document_hashes (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255),
    hash VARCHAR(64) UNIQUE,  -- SHA-256
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Vector embeddings (created by LangChain4j)
CREATE TABLE document_embeddings (
    embedding_id UUID PRIMARY KEY,
    embedding VECTOR(384),  -- pgvector type
    text TEXT,
    metadata JSONB
);

-- Vector index for fast similarity search
CREATE INDEX ON document_embeddings 
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

### Vector Search Query

```sql
-- Find top 5 most similar documents
SELECT 
    text,
    metadata->>'source' as source,
    1 - (embedding <=> '[0.23, -0.15, ...]'::vector) as similarity
FROM document_embeddings
WHERE 1 - (embedding <=> '[0.23, -0.15, ...]'::vector) > 0.5
ORDER BY embedding <=> '[0.23, -0.15, ...]'::vector
LIMIT 5;
```

---

## 🔑 Key Algorithms

### 1. Cosine Similarity

Measures angle between two vectors (0 to 1, higher = more similar):

```
similarity = (A · B) / (||A|| × ||B||)

Where:
  A · B = dot product
  ||A|| = magnitude of A
```

```java
private double cosineSimilarity(float[] a, float[] b) {
    double dotProduct = 0.0;
    double normA = 0.0, normB = 0.0;
    
    for (int i = 0; i < a.length; i++) {
        dotProduct += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }
    
    return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
}
```

### 2. MMR Algorithm

```java
for each selection round:
    bestScore = -infinity
    for each candidate not yet selected:
        relevance = similarity(candidate, query)
        diversity = 1 - max(similarity(candidate, each selected))
        mmrScore = λ * relevance + (1-λ) * diversity
        if mmrScore > bestScore:
            bestCandidate = candidate
    select bestCandidate
```

### 3. Text Chunking

```java
public List<String> chunkText(String text, int size, int overlap) {
    List<String> chunks = new ArrayList<>();
    int start = 0;
    
    while (start < text.length()) {
        int end = Math.min(start + size, text.length());
        chunks.add(text.substring(start, end));
        start += (size - overlap);  // Move by (size - overlap)
    }
    
    return chunks;
}
```

---

## 🎯 Interview Topics Covered

### AI/ML Concepts

| Topic | Where in Code |
|-------|--------------|
| RAG Architecture | `RagService.java` |
| Embeddings | `LangChainConfig.java` |
| Vector Similarity | `RagService.cosineSimilarity()` |
| MMR Algorithm | `RagService.applyMMR()` |
| Prompt Engineering | `RagService.buildPrompt()` |
| Chunking Strategies | `DocumentService.java` |
| LLM Integration | `LangChainConfig.java` |

### Backend Concepts

| Topic | Where in Code |
|-------|--------------|
| Spring Boot | Entire backend |
| REST APIs | Controllers |
| Dependency Injection | `@Autowired`, `@Service` |
| JPA/Hibernate | Repositories |
| Spring Security | `SecurityConfig.java` |
| JWT Authentication | `JwtUtil.java`, `JwtAuthFilter.java` |
| Exception Handling | Controllers |
| CORS Configuration | `CorsConfig.java` |

### Database Concepts

| Topic | Where |
|-------|-------|
| PostgreSQL | `application.properties` |
| Vector Indexes | pgvector |
| JSONB for Metadata | Embeddings table |
| Entity Relationships | JPA Entities |

### DevOps Concepts

| Topic | Where |
|-------|-------|
| Docker | pgvector container |
| Environment Config | `application.properties` |
| Hot Reload | Spring DevTools |

---

## 📡 API Reference

### Authentication

```
POST /api/auth/register
Body: { "username": "user1", "email": "user@email.com", "password": "pass123" }
Response: { "message": "Success", "token": "eyJ..." }

POST /api/auth/login
Body: { "username": "user1", "password": "pass123" }
Response: { "message": "Login successful", "token": "eyJ..." }
```

### Chat

```
POST /api/chat
Header: Authorization: Bearer <token>
Body: { 
  "question": "What is machine learning?",
  "conversationHistory": [...]
}
Response: {
  "answer": "Machine learning is...",
  "sources": ["ml-guide.pdf"],
  "chunksUsed": 3,
  "responseTime": 1234
}
```

### Documents

```
POST /api/documents/upload
Header: Authorization: Bearer <token>
Body: FormData with file
Response: { "message": "Processed 45 chunks", "filename": "doc.pdf" }

GET /api/documents
Response: [{ "name": "doc.pdf", "chunks": 45, "uploadedAt": "..." }]
```

### History

```
GET /api/history?sessionId=abc123
Response: [{ "role": "user", "content": "Hello" }, ...]
```

---

## 🔧 Troubleshooting

### Port Already in Use

```bash
# Find and kill process on port 8080
netstat -ano | findstr :8080
taskkill /PID <pid> /F
```

### Ollama Connection Refused

```bash
# Check if Ollama is running
ollama list

# Start Ollama service
ollama serve
```

### pgvector Container Issues

```bash
# Check container status
docker ps -a

# View logs
docker logs pgvector

# Restart container
docker restart pgvector
```

### Java Version Issues

```bash
# Check Java version
java -version

# Set JAVA_HOME (Windows)
set JAVA_HOME=C:\Program Files\Java\jdk-21
```

---

## 📚 Further Learning

### Recommended Reading

1. **LangChain4j Documentation**: https://docs.langchain4j.dev/
2. **pgvector GitHub**: https://github.com/pgvector/pgvector
3. **Ollama Documentation**: https://ollama.ai/
4. **Spring Boot Docs**: https://spring.io/projects/spring-boot

### Advanced Topics to Explore

- [ ] Hybrid Search (Vector + Keyword)
- [ ] Reranking with Cross-Encoders
- [ ] Multi-modal RAG (Images + Text)
- [ ] Agent-based Systems
- [ ] Fine-tuning Embedding Models
- [ ] Evaluation Metrics (RAGAS)

---

## 🏆 Summary

This project demonstrates a **production-ready RAG system** with:

✅ **Document Processing** - PDF parsing, chunking, deduplication  
✅ **Vector Search** - pgvector, embeddings, similarity search  
✅ **MMR Algorithm** - Diverse, relevant results  
✅ **Local LLM** - Ollama + Mistral, no API costs  
✅ **Authentication** - JWT, Spring Security  
✅ **Chat History** - Persistent conversations  
✅ **Modern Stack** - Java 21, Spring Boot 3, React 18  

Perfect for learning AI/ML concepts and preparing for technical interviews!

---

*Built with ❤️ for learning RAG, LLMs, and modern AI applications.*
