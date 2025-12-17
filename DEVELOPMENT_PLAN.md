# Piano di Sviluppo WikiChat - Sistema RAG

## Panoramica
Sistema RAG (Retrieval-Augmented Generation) che permette di caricare documenti (MD, HTML, PDF) e porre domande sul loro contenuto tramite chat interface. Il sistema utilizza embeddings vettoriali per recuperare informazioni rilevanti e Google Gemini per generare risposte contestualizzate.

**Stack**: Java 25, Spring Boot 4.x, PostgreSQL+pgvector, LangChain4j, Gemini API

---

## Fase 1: Setup Ambiente e Configurazione Base

### 1.1 Configurazione Docker e PostgreSQL
- Creare `docker-compose.yml` con PostgreSQL 15 + estensione pgvector
- Creare script SQL iniziale `init.sql` per:
  - Abilitare estensione pgvector
  - Creare schema database (tabelle: documents, chunks, conversations, messages)
  - Creare indici vettoriali HNSW per performance
- Avviare container e verificare connessione

### 1.2 Configurazione Maven e Dipendenze 
Aggiornare `pom.xml` con tutte le dipendenze necessarie:
- Spring Boot Web, Data JPA, Validation
- PostgreSQL driver + pgvector-java
- LangChain4j + vertex-ai-gemini
- Parser: PDFBox, Jsoup, CommonMark
- Lombok per ridurre boilerplate

### 1.3 Configurazione Application Properties
Aggiornare `application.properties`:
- Database connection (PostgreSQL)
- Gemini API configuration
- File upload limits (50MB)
- RAG parameters (chunk size: 500, overlap: 50, top-k: 5)
- Logging levels

### 1.4 Setup Gemini API
- Creare account Google Cloud / Google AI Studio
- Ottenere API Key gratuita
- Configurare variabile ambiente `GEMINI_API_KEY`
- Creare file `.env` per development locale

---

## Fase 2: Layer Model e Database

### 2.1 Implementazione JPA Entities
Creare package `model.entity` con:

**`Document.java`**: Entità per documenti caricati
```java
@Entity @Table(name = "documents")
- id (UUID), filename, contentType, fileSize
- status (PROCESSING/COMPLETED/FAILED)
- metadata (JSONB), timestamps
```

**`Chunk.java`**: Frammenti di testo con embeddings
```java
@Entity @Table(name = "chunks")
- id (UUID), documentId (FK), content
- chunkIndex, embedding (vector type pgvector)
- metadata (JSONB)
```

**`Conversation.java`** e **`Message.java`**: Chat history
```java
@Entity - conversazioni con sessionId
@Entity - messaggi con role (USER/ASSISTANT) e sources (JSONB)
```

### 2.2 Configurazione Custom Types
- Creare `VectorType.java` per supporto pgvector in Hibernate
- Registrare type handler personalizzato per `vector` PostgreSQL

### 2.3 Implementazione Repositories
Creare interfaces in `repository`:
- `DocumentRepository extends JpaRepository<Document, UUID>`
- `ChunkRepository` con metodo custom per vector search:
  ```java
  @Query(native = true, value = "SELECT *, embedding <=> :queryVector AS distance 
                                  FROM chunks ORDER BY distance LIMIT :k")
  List<Chunk> findSimilarChunks(String queryVector, int k);
  ```
- `ConversationRepository` e `MessageRepository`

---

## Fase 3: Layer Integration - Parser e Gemini

### 3.1 Document Parsers
Creare package `integration.parser`:

**`DocumentParser.java`**: Interface
```java
String parse(InputStream inputStream, String contentType);
```

**Implementazioni**:
- `MarkdownParser.java`: usa CommonMark per parsing MD
- `HtmlParser.java`: usa Jsoup per estrarre testo da HTML
- `PdfParser.java`: usa PDFBox per estrarre testo da PDF
- `ParserFactory.java`: factory pattern per selezione parser

### 3.2 Gemini API Client
Creare package `integration.gemini`:

**`GeminiConfig.java`**: Configurazione LangChain4j
```java
@Configuration
- Bean per VertexAiGeminiChatModel
- Bean per VertexAiGeminiEmbeddingModel
- Caricamento API key e parametri da properties
```

**`GeminiEmbeddingService.java`**: Wrapper per embeddings
```java
float[] generateEmbedding(String text)
- Chiamata a Gemini text-embedding-004
- Gestione rate limiting (2M token/giorno)
- Retry logic con exponential backoff
```

**`GeminiChatService.java`**: Wrapper per generazione testo
```java
String generate(String prompt)
- Chiamata a gemini-pro model
- Configurazione temperatura, max tokens
- Streaming response (opzionale)
```

---

## Fase 4: Layer Service - Business Logic

### 4.1 ChunkingService
Creare `service.ChunkingService`:
```java
List<String> splitIntoChunks(String text, int chunkSize, int overlap)
- Split testo in frammenti ~500 token
- Overlap di 50 token tra chunks
- Preservare integrità frasi/paragrafi
- Gestire casi edge (testi brevi)
```

### 4.2 EmbeddingService
Creare `service.EmbeddingService`:
```java
List<float[]> generateEmbeddings(List<String> chunks)
- Batch processing per efficienza
- Gestione rate limiting Gemini API
- Caching opzionale per chunks duplicati
- Logging progressi per documenti grandi
```

### 4.3 DocumentService
Creare `service.DocumentService`:
```java
UUID uploadDocument(MultipartFile file)
- Validazione file (tipo, dimensione)
- Salvataggio metadata in DB (status: PROCESSING)
- Trigger async processing pipeline
- Ritorno immediato document ID

void processDocument(UUID documentId)
- Parsing documento → testo
- Chunking → frammenti
- Generazione embeddings
- Salvataggio chunks in DB
- Aggiornamento status: COMPLETED/FAILED
```

### 4.4 VectorSearchService
Creare `service.VectorSearchService`:
```java
List<Chunk> findSimilarChunks(float[] queryEmbedding, int topK)
- Ricerca cosine similarity su pgvector
- Filtering per similarity threshold (>0.7)
- Ritorno top-k chunks più rilevanti
- Ordinamento per score
```

### 4.5 RAGService (Orchestratore)
Creare `service.RAGService`:
```java
ChatResponse processQuery(String query, String sessionId)
1. Genera embedding della domanda (GeminiEmbeddingService)
2. Cerca chunks rilevanti (VectorSearchService)
3. Costruisci contesto da chunks
4. Genera prompt strutturato:
   "Contesto: {chunks}\nDomanda: {query}\nRisposta:"
5. Chiama LLM (GeminiChatService)
6. Salva conversazione in DB
7. Ritorna risposta + source references
```

---

## Fase 5: Layer Presentation - REST API

### 5.1 DTOs (Data Transfer Objects)
Creare package `model.dto`:
- `DocumentUploadRequest/Response`
- `ChatRequest`: query, sessionId
- `ChatResponse`: answer, sources (List<SourceReference>)
- `SourceReference`: documentName, chunkContent, similarityScore

### 5.2 Controllers
Creare package `controller`:

**`DocumentController.java`**:
```java
POST   /api/documents/upload          → uploadDocument()
GET    /api/documents                 → listDocuments()
GET    /api/documents/{id}            → getDocument()
DELETE /api/documents/{id}            → deleteDocument()
GET    /api/documents/{id}/chunks     → getDocumentChunks()
```

**`ChatController.java`**:
```java
POST   /api/chat/query                → processQuery()
GET    /api/chat/history/{sessionId}  → getHistory()
DELETE /api/chat/{sessionId}          → deleteHistory()
```

**`HealthController.java`**:
```java
GET    /api/health                    → healthCheck()
GET    /api/health/db                 → checkDatabase()
GET    /api/health/gemini             → checkGeminiAPI()
```

### 5.3 Exception Handling
Creare `exception.GlobalExceptionHandler`:
- `@ControllerAdvice` per gestione errori centralizzata
- Custom exceptions: `DocumentProcessingException`, `EmbeddingException`, `VectorSearchException`
- Response standardizzate con HTTP status appropriati

---

## Fase 6: Configurazioni Avanzate

### 6.1 Async Processing
Creare `config.AsyncConfig`:
```java
@EnableAsync
ThreadPoolTaskExecutor per processing documenti
- Core pool: 4 thread
- Max pool: 8 thread
- Queue capacity: 100
```

### 6.2 CORS Configuration
Creare `config.WebConfig`:
```java
@Configuration
CorsMapping per frontend (quando sarà implementato)
- Allowed origins: localhost:3000
- Allowed methods: GET, POST, DELETE
```

### 6.3 Validation
- Aggiungere `@Valid` nei controller
- Annotations nei DTO: `@NotBlank`, `@Size`, etc.
- Custom validator per file types supportati

---

## Fase 7: Testing e Verifica

### 7.1 Test Unitari
Per ogni service:
- `DocumentServiceTest`: mock repositories
- `ChunkingServiceTest`: verifica split corretto
- `RAGServiceTest`: mock dependencies, verifica orchestrazione

### 7.2 Test di Integrazione
- `DocumentIntegrationTest`: upload → processing → storage
- `ChatIntegrationTest`: query → retrieval → generation
- Database embedded H2 con testcontainers per PostgreSQL

### 7.3 Test Manuali con curl/Postman
```bash
# Upload documento
curl -X POST -F "file=@test.md" http://localhost:8080/api/documents/upload

# Chat query
curl -X POST -H "Content-Type: application/json" \
  -d '{"query":"Cos è WikiChat?","sessionId":"test-123"}' \
  http://localhost:8080/api/chat/query
```

---

## Fase 8: Documentazione e Deployment

### 8.1 Documentazione API
- Aggiungere SpringDoc OpenAPI (Swagger UI)
- Annotations `@Operation`, `@ApiResponse` nei controller
- Accessibile su `http://localhost:8080/swagger-ui.html`

### 8.2 README e Guide
Creare/aggiornare:
- `README.md`: overview progetto, quick start
- `SETUP.md`: guida setup ambiente dettagliata
- `API.md`: documentazione endpoints REST
- `ARCHITECTURE.md`: diagrammi e spiegazione architettura

### 8.3 Dockerfile per Backend
```dockerfile
FROM eclipse-temurin:25-jdk-alpine
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

### 8.4 Docker Compose Completo
Aggiornare `docker-compose.yml`:
- Service PostgreSQL (pgvector)
- Service backend (build da Dockerfile)
- Volumes per persistenza
- Health checks
- Network dedicata

---

## Checklist Progresso

Ogni fase può essere testata indipendentemente:
- [x] Fase 1: Container PostgreSQL accessibile, ping database OK ✅
- [x] Fase 2: Application si avvia, tabelle create automaticamente ✅
- [ ] Fase 3: Parser estraggono testo, Gemini API risponde
- [ ] Fase 4: Upload documento → chunks salvati con embeddings
- [ ] Fase 5: Endpoint REST rispondono, query funziona end-to-end
- [ ] Fase 6: Processing async non blocca, errori gestiti
- [ ] Fase 7: Test passano, sistema stabile
- [ ] Fase 8: Deploy Docker funzionante

---

## Note Implementative

### Gestione Rate Limiting Gemini API
- Free tier: 2M token input/giorno, 32K token/minuto
- Implementare retry con exponential backoff
- Logging chiamate API per monitoraggio quota

### Ottimizzazioni Performance
- Indice HNSW su pgvector (non IVFFlat) per speed
- Batch embeddings (max 100 chunks per batch)
- Connection pooling PostgreSQL (HikariCP default Spring Boot)

### Security (da implementare post-MVP)
- Autenticazione JWT
- Rate limiting endpoints
- Sanitizzazione input utente
- Validazione content-type upload

---

## Stima Tempi

- Fase 1-2: 1-2 giorni (setup + entities)
- Fase 3: 1-2 giorni (parsers + Gemini)
- Fase 4: 2-3 giorni (business logic core)
- Fase 5: 1-2 giorni (REST API)
- Fase 6-7: 1-2 giorni (config + testing)
- Fase 8: 1 giorno (docs + deploy)

**Totale stimato**: 8-13 giorni lavorativi

