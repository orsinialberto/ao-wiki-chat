# Architettura WikiChat - Sistema RAG

## Overview
WikiChat è un sistema RAG (Retrieval-Augmented Generation) che permette di caricare documenti e porre domande sul loro contenuto. Il sistema combina ricerca vettoriale semantica con un LLM per fornire risposte accurate e contestualizzate.

---

## Stack Tecnologico

- **Backend**: Java 25, Spring Boot 4.x
- **Database**: PostgreSQL 15+ con estensione pgvector
- **LLM & Embeddings**: Google Gemini API (free tier)
- **Framework AI**: LangChain4j
- **Containerizzazione**: Docker & Docker Compose

---

## Architettura a Livelli

```
┌─────────────────────────────────────────────────────────────┐
│                   CLIENT (Future: React)                    │
│              - Upload documenti                             │
│              - Chat Interface                               │
│              - Visualizzazione sorgenti                     │
└──────────────────────┬──────────────────────────────────────┘
                       │ REST API
┌──────────────────────┴──────────────────────────────────────┐
│                   BACKEND (Spring Boot)                     │
│ ┌──────────────────────────────────────────────────────────┐│
│ │          PRESENTATION LAYER                              ││
│ │  DocumentController | ChatController | HealthController  ││
│ └──────────────────────────────────────────────────────────┘│
│ ┌──────────────────────────────────────────────────────────┐│
│ │           SERVICE LAYER                                  ││
│ │  DocumentService | ChunkingService | EmbeddingService    ││
│ │  VectorSearchService | RAGService (orchestrator)         ││
│ └──────────────────────────────────────────────────────────┘│
│ ┌──────────────────────────────────────────────────────────┐│
│ │           INTEGRATION LAYER                              ││
│ │  LangChain4j | Gemini API Client | Document Parsers      ││
│ └──────────────────────────────────────────────────────────┘│
│ ┌──────────────────────────────────────────────────────────┐│
│ │           DATA LAYER                                     ││
│ │  DocumentRepo | ChunkRepo | ConversationRepo             ││
│ └──────────────────────────────────────────────────────────┘│
└──────────────────────┬──────────────────────────────────────┘
                       │ JPA/JDBC
┌──────────────────────┴──────────────────────────────────────┐
│              PostgreSQL + pgvector                          │
│  - Metadata documenti                                       │
│  - Chunks di testo + embeddings vettoriali                  │
│  - Cronologia conversazioni                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## Struttura Package

```
com.example.ao_wiki_chat/
├── AoWikiChatApplication.java
│
├── config/                          # Configurazioni
│   ├── LangChain4jConfig.java      
│   ├── GeminiConfig.java            
│   ├── WebConfig.java               # CORS
│   └── AsyncConfig.java             # Async processing
│
├── controller/                      # REST Controllers
│   ├── DocumentController.java      
│   ├── ChatController.java          
│   └── HealthController.java        
│
├── service/                         # Business Logic
│   ├── DocumentService.java         
│   ├── ChunkingService.java         # Splitting documenti
│   ├── EmbeddingService.java        # Generazione embeddings
│   ├── VectorSearchService.java     # Ricerca similarità
│   ├── ChatService.java             
│   └── RAGService.java              # Orchestratore RAG
│
├── integration/                     # Integrazioni esterne
│   ├── gemini/
│   │   ├── GeminiClient.java        
│   │   └── GeminiEmbeddingService.java
│   ├── parser/
│   │   ├── DocumentParser.java      # Interface
│   │   ├── MarkdownParser.java
│   │   ├── HtmlParser.java
│   │   └── PdfParser.java
│   └── langchain/
│       └── LangChain4jService.java  
│
├── repository/                      # Data Access
│   ├── DocumentRepository.java
│   ├── ChunkRepository.java
│   └── ConversationRepository.java
│
├── model/                           # Domain Models
│   ├── entity/                      # JPA Entities
│   │   ├── Document.java
│   │   ├── Chunk.java
│   │   ├── Conversation.java
│   │   └── Message.java
│   └── dto/                         # Data Transfer Objects
│       ├── DocumentUploadRequest.java
│       ├── DocumentResponse.java
│       ├── ChatRequest.java
│       ├── ChatResponse.java
│       └── SourceReference.java
│
├── exception/                       # Exception Handling
│   ├── GlobalExceptionHandler.java
│   ├── DocumentProcessingException.java
│   └── EmbeddingException.java
│
└── util/                           # Utilities
    ├── TextProcessor.java          
    └── VectorUtils.java            
```

---

## Schema Database PostgreSQL

```sql
-- Estensione pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Tabella documenti
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PROCESSING',
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella chunks (frammenti di testo)
CREATE TABLE chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding vector(768),  -- Dimensione embedding Gemini
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);

-- Indice per ricerca vettoriale (HNSW)
CREATE INDEX chunks_embedding_idx ON chunks 
USING hnsw (embedding vector_cosine_ops);

-- Tabella conversazioni
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella messaggi
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,  -- USER, ASSISTANT
    content TEXT NOT NULL,
    sources JSONB,  -- Riferimenti ai chunks utilizzati
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indici per performance
CREATE INDEX idx_chunks_document_id ON chunks(document_id);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_conversations_session_id ON conversations(session_id);
```

---

## Pipeline RAG

### A) INGESTION PIPELINE (Caricamento Documenti)

```
1. Upload Documento (DocumentController)
   ↓
2. Validazione (tipo, dimensione)
   ↓
3. Salvataggio metadata in DB (status: PROCESSING)
   ↓
4. Async Processing:
   ├─ Parsing (MD/HTML/PDF → testo)
   ├─ Chunking (split ~500 token con overlap 50)
   ├─ Generazione Embeddings (Gemini API)
   └─ Salvataggio chunks + embeddings in DB
   ↓
5. Aggiornamento status: COMPLETED/FAILED
```

### B) RETRIEVAL PIPELINE (Query Chat)

```
1. Domanda utente (ChatController)
   ↓
2. Generazione embedding domanda (GeminiEmbeddingService)
   ↓
3. Vector Search su pgvector (VectorSearchService)
   ├─ Cosine similarity
   └─ Top-k chunks (k=5-10)
   ↓
4. Costruzione prompt con contesto (RAGService)
   ↓
5. Chiamata LLM Gemini (GeminiChatService)
   ↓
6. Salvataggio conversazione in DB
   ↓
7. Risposta + riferimenti sorgenti
```

---

## Flusso Dati RAG Dettagliato

```
[Utente] → "Cos'è WikiChat?"
    ↓
[ChatController.processQuery()]
    ↓
[RAGService.processQuery()]
    ├─ 1. EmbeddingService.generateEmbedding(query)
    │      └→ Gemini API → vector[768]
    │
    ├─ 2. VectorSearchService.findSimilar(vector)
    │      └→ PostgreSQL pgvector → top 5 chunks
    │
    ├─ 3. buildContext(chunks)
    │      └→ "Contesto:\n[chunk1]\n[chunk2]..."
    │
    ├─ 4. buildPrompt(context, query)
    │      └→ "Dato il seguente contesto, rispondi..."
    │
    ├─ 5. GeminiChatService.generate(prompt)
    │      └→ Gemini API → risposta generata
    │
    └─ 6. saveConversation(...)
           └→ PostgreSQL → messages table

[Risposta] ← { answer, sources[] }
```

---

## Componenti Chiave

### RAGService (Orchestratore)
Il cervello del sistema. Coordina tutte le operazioni:
- Riceve query utente
- Genera embedding della query
- Cerca chunks rilevanti nel database
- Costruisce prompt con contesto
- Chiama LLM per generare risposta
- Salva cronologia conversazione
- Ritorna risposta con sorgenti citate

### VectorSearchService
Gestisce la ricerca semantica:
- Query: `SELECT *, embedding <=> :vector AS distance FROM chunks ORDER BY distance LIMIT k`
- Utilizza operatore `<=>` (cosine distance) di pgvector
- Filtra per similarity threshold (> 0.7)
- Ritorna chunks più rilevanti ordinati per score

### ChunkingService
Intelligente splitting del testo:
- Divide testo in frammenti ~500 token
- Overlap di 50 token tra chunks per continuità
- Preserva integrità semantica (non spezza frasi)
- Gestisce metadata (numero pagina, sezione, etc.)

### EmbeddingService
Genera rappresentazioni vettoriali:
- Batch processing per efficienza (max 100 chunks)
- Rate limiting per rispettare quota Gemini API
- Retry con exponential backoff su errori
- Caching opzionale per chunks duplicati

---

## Integrazioni Esterne

### Google Gemini API
**Modelli utilizzati**:
- `text-embedding-004`: generazione embeddings (768 dimensioni)
- `gemini-pro`: generazione risposte chat

**Free Tier Limits**:
- 2M token input/giorno
- 32K token/minuto
- Rate limiting gestito con retry automatico

### LangChain4j
Framework per semplificare integrazione LLM:
- Abstraction layer per Gemini API
- Gestione automatica prompt templates
- Streaming responses (opzionale)
- Memory management per conversazioni

---

## Scalabilità e Performance

### Database Optimization
- **Indice HNSW** su embeddings per ricerca O(log n)
- **Connection pooling** HikariCP (default Spring Boot)
- **Batch inserts** per chunks durante ingestion
- **Prepared statements** per prevenire SQL injection

### Async Processing
- **ThreadPoolTaskExecutor** per processing documenti
- Core pool: 4 thread, Max: 8 thread
- Non blocca endpoint REST durante processing
- Status polling via GET /api/documents/{id}

### Caching Strategy (Future)
- Redis per cache embeddings frequenti
- Cache risultati vector search (TTL: 1h)
- Cache risposte LLM per query identiche

---

## Security (Post-MVP)

### Autenticazione & Autorizzazione
- JWT tokens per autenticazione
- Spring Security integration
- Role-based access control (USER, ADMIN)

### Input Validation
- Whitelisting content-type upload
- Max file size enforcement (50MB)
- Sanitizzazione input utente
- SQL injection prevention (prepared statements)

### Rate Limiting
- Bucket4j per limit requests/user
- Protezione endpoint pubblici
- Monitoraggio usage Gemini API

---

## Monitoring & Logging

### Logging Strategy
```properties
logging.level.com.example.ao_wiki_chat=DEBUG
logging.level.dev.langchain4j=INFO
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

### Metriche da Monitorare
- Latenza vector search (target: <100ms)
- Tempo generazione embeddings
- Rate limiting Gemini API (token usage)
- Dimensione database chunks
- Numero richieste/secondo

### Health Checks
- `GET /api/health` - application status
- `GET /api/health/db` - PostgreSQL connectivity
- `GET /api/health/gemini` - Gemini API availability

---

## Diagramma ER Database

```
┌──────────────┐
│  documents   │
│──────────────│
│ id (PK)      │──┐
│ filename     │  │
│ content_type │  │
│ status       │  │
│ metadata     │  │
└──────────────┘  │
                  │ 1:N
                  │
               ┌──┴──────────┐
               │   chunks    │
               │─────────────│
               │ id (PK)     │
               │ document_id │ (FK)
               │ content     │
               │ embedding   │ ← vector(768)
               │ chunk_index │
               └─────────────┘

┌─────────────────┐
│ conversations   │
│─────────────────│
│ id (PK)         │──┐
│ session_id      │  │
└─────────────────┘  │
                     │ 1:N
                     │
                  ┌──┴─────────┐
                  │  messages  │
                  │────────────│
                  │ id (PK)    │
                  │ conv_id    │ (FK)
                  │ role       │
                  │ content    │
                  │ sources    │ ← JSONB
                  └────────────┘
```

---

## API Endpoints

### Documents
```
POST   /api/documents/upload          # Upload documento
GET    /api/documents                 # Lista documenti
GET    /api/documents/{id}            # Dettaglio documento
DELETE /api/documents/{id}            # Elimina documento
GET    /api/documents/{id}/chunks     # Visualizza chunks
```

### Chat
```
POST   /api/chat/query                # Invia domanda
GET    /api/chat/history/{sessionId}  # Cronologia conversazione
DELETE /api/chat/{sessionId}          # Cancella conversazione
```

### Health
```
GET    /api/health                    # Status generale
GET    /api/health/db                 # Check database
GET    /api/health/gemini             # Check Gemini API
```

---

## Decisioni Architetturali

### Perché pgvector?
- Native PostgreSQL extension
- Performance eccellenti con indice HNSW
- No necessità di database vettoriale separato
- Semplicità deployment

### Perché LangChain4j?
- Abstraction layer per LLM
- Support nativo per Gemini API
- Community attiva, ben documentato
- Facilita switch tra provider LLM

### Perché Async Processing?
- Upload documenti non blocca endpoint
- User experience migliore (risposta immediata)
- Gestione errori centralizzata
- Scalabilità (code processing)

### Perché Chunking con Overlap?
- Evita perdita informazioni ai confini
- Migliora retrieval accuracy
- Context continuity tra chunks
- Best practice RAG systems

---

## Future Enhancements

### Fase 2 (Post-MVP)
- Frontend React completo
- Autenticazione utenti
- Multi-tenancy support
- Caching Redis

### Fase 3 (Scalabilità)
- Kubernetes deployment
- Message queue (RabbitMQ) per processing
- Distributed tracing (Jaeger)
- Metrics dashboard (Grafana)

### Fase 4 (Features Avanzate)
- OCR per immagini in PDF
- Multi-modal embeddings
- Conversational memory
- Query expansion/rewriting

