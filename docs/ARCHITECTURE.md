# WikiChat Architecture Documentation

## Table of Contents

- [System Overview](#system-overview)
- [Architecture Layers](#architecture-layers)
- [Component Diagram](#component-diagram)
- [Data Flow](#data-flow)
- [Technology Stack](#technology-stack)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)
- [Service Layer](#service-layer)
- [Integration Points](#integration-points)
- [Configuration](#configuration)
- [Deployment Architecture](#deployment-architecture)

---

## System Overview

WikiChat is a **Retrieval-Augmented Generation (RAG)** system that enables users to upload documents and query them using natural language. The system uses vector embeddings for semantic search and Google Gemini AI for generating contextualized responses.

### Key Features

- **Document Processing**: Supports Markdown, HTML, and PDF files
- **Vector Search**: Semantic similarity search using pgvector
- **Conversation Management**: Maintains conversation history and context
- **Source Attribution**: Provides references to source document chunks
- **Asynchronous Processing**: Non-blocking document upload and processing
- **CLI Interface**: Command-line tool for system interaction

### System Modules

1. **Backend Module** (`ao-wiki-chat-backend`): Spring Boot REST API
2. **CLI Module** (`ao-wiki-chat-cli`): Command-line interface using Picocli

---

## Architecture Layers

The system follows a **layered architecture** pattern:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (Controllers: REST API + CLI)          │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│         Service Layer                   │
│  (Business Logic & Orchestration)       │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│         Repository Layer                │
│  (Data Access: JPA Repositories)        │
└─────────────────────────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│         Database Layer                  │
│  (PostgreSQL + pgvector)                │
└─────────────────────────────────────────┘
```

### Layer Responsibilities

#### Presentation Layer
- **Controllers**: Handle HTTP requests, validate input, transform DTOs
- **CLI Commands**: Parse command-line arguments, format output
- **Exception Handling**: Global exception handler for consistent error responses

#### Service Layer
- **Business Logic**: Document processing, RAG orchestration, chunking
- **Integration**: External API calls (Gemini), document parsing
- **Transaction Management**: Ensures data consistency

#### Repository Layer
- **Data Access**: JPA repositories for CRUD operations
- **Custom Queries**: Vector similarity search, complex queries
- **Entity Management**: JPA entity lifecycle management

#### Database Layer
- **PostgreSQL**: Relational database with JSONB support
- **pgvector**: Vector extension for embedding storage and similarity search
- **Indexes**: HNSW index for fast vector search, B-tree indexes for lookups

---

## Component Diagram

```
┌──────────────┐
│   Client     │
│  (CLI/HTTP)  │
└──────┬───────┘
       │
       ↓
┌─────────────────────────────────────────────────────────┐
│                    Backend API                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  Document    │  │     Chat     │  │    Health    │   │
│  │  Controller  │  │  Controller  │  │  Controller  │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
└─────────┼─────────────────┼─────────────────┼───────────┘
          │                 │                 │
          ↓                 ↓                 ↓
┌─────────────────────────────────────────────────────────┐
│                    Service Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  Document    │  │    RAG       │  │  Chunking    │   │
│  │  Service     │  │   Service    │  │  Service     │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
│         │                 │                 │           │
│  ┌──────┴───────┐  ┌──────┴───────┐  ┌──────┴───────┐   │
│  │  Embedding   │  │   Vector     │  │     LLM      │   │
│  │  Service     │  │   Search     │  │   Service    │   │
│  │              │  │   Service    │  │              │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
└─────────┼─────────────────┼─────────────────┼───────────┘
          │                 │                 │
          ↓                 ↓                 ↓
┌─────────────────────────────────────────────────────────┐
│              Integration Layer                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │   Parser     │  │    Gemini    │  │   Gemini     │   │
│  │   Factory    │  │  Embedding   │  │     LLM      │   │
│  │              │  │   Client     │  │    Client    │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
└─────────┼─────────────────┼─────────────────┼───────────┘
          │                 │                 │
          ↓                 ↓                 ↓
┌─────────────────────────────────────────────────────────┐
│              Repository Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │  Document    │  │    Chunk     │  │ Conversation │   │
│  │  Repository  │  │  Repository  │  │  Repository  │   │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘   │
└─────────┼─────────────────┼─────────────────┼───────────┘
          │                 │                 │
          └─────────────────┼─────────────────┘
                            ↓
              ┌─────────────────────────┐
              │   PostgreSQL Database   │
              │   (with pgvector)       │
              └─────────────────────────┘
```

---

## Data Flow

### Document Upload Flow

```
1. Client Upload
   ↓
2. DocumentController.uploadDocument()
   - Validates file (type, size, name)
   - Creates Document entity (status: PROCESSING)
   - Saves to database
   - Returns document ID immediately
   ↓
3. DocumentService.processDocumentAsync() [ASYNC]
   - Parses document (Markdown/HTML/PDF)
   - Extracts text content
   ↓
4. ChunkingService.chunkText()
   - Splits text into chunks (300 tokens, 30 overlap)
   - Creates Chunk entities
   ↓
5. GeminiEmbeddingService.generateEmbedding() [BATCH]
   - Generates embeddings for each chunk (768 dimensions)
   - Normalizes embeddings
   ↓
6. Save chunks with embeddings to database
   ↓
7. Update Document status (COMPLETED/FAILED)
```

### Query Processing Flow (RAG Pipeline)

```
1. Client Query
   ↓
2. ChatController.processQuery()
   - Validates request
   - Delegates to RAGService
   ↓
3. RAGService.processQuery()
   ├─ Step 0: Get/Create Conversation
   │  └─ Retrieve previous messages (if any)
   │
   ├─ Step 1: Generate Query Embedding
   │  └─ GeminiEmbeddingService.generateEmbedding(query)
   │
   ├─ Step 2: Vector Search
   │  └─ VectorSearchService.findSimilarChunks(embedding)
   │     └─ PostgreSQL cosine similarity query
   │
   ├─ Step 3: Build Context
   │  └─ Format chunks into context string
   │
   ├─ Step 4: Build Prompt
   │  └─ Combine: context + conversation history + query
   │
   ├─ Step 5: Generate Answer
   │  └─ GeminiLLMService.generate(prompt)
   │
   ├─ Step 6: Build Source References
   │  └─ Extract chunk metadata for citations
   │
   └─ Step 7: Save & Return
      └─ Save messages to database
      └─ Return ChatResponse
```

---

## Technology Stack

### Backend

- **Java 25**: Programming language
- **Spring Boot 4.0.0**: Application framework
- **Spring Data JPA**: Data persistence
- **Hibernate**: ORM framework
- **Maven**: Build tool and dependency management

### Database

- **PostgreSQL 15**: Relational database
- **pgvector 0.1.6**: Vector extension for embeddings
- **HNSW Index**: Approximate nearest neighbor search

### AI/ML

- **LangChain4j 0.36.2**: Java framework for LLM applications
- **Google Gemini API**: 
  - `gemini-2.5-flash-lite`: Chat model
  - `gemini-embedding-001`: Embedding model (768 dimensions via Matryoshka scaling)

### Document Processing

- **Apache PDFBox 3.0.3**: PDF parsing
- **JSoup 1.18.1**: HTML parsing
- **CommonMark 0.24.0**: Markdown parsing

### CLI

- **Picocli**: Command-line interface framework
- **Jackson**: JSON processing
- **SLF4J + Logback**: Logging

### Infrastructure

- **Docker**: Containerization
- **Docker Compose**: Multi-container orchestration

---

## Database Schema

### Entity Relationship Diagram

```
┌──────────────┐
│  documents   │
├──────────────┤
│ id (PK)      │
│ filename     │
│ content_type │
│ file_size    │
│ status       │
│ metadata     │
│ created_at   │
│ updated_at   │
└──────┬───────┘
       │
       │ 1:N
       ↓
┌──────────────┐
│   chunks     │
├──────────────┤
│ id (PK)      │
│ document_id  │──┐
│ content      │  │ FK
│ chunk_index  │  │
│ embedding    │  │ (vector[768])
│ metadata     │  │
│ created_at   │  │
└──────────────┘  │
                  │
┌──────────────┐  │
│conversations │  │
├──────────────┤  │
│ id (PK)      │  │
│ session_id   │  │
│ title        │  │
│ metadata     │  │
│ created_at   │  │
│ updated_at   │  │
└──────┬───────┘  │
       │          │
       │ 1:N      │
       ↓          │
┌──────────────┐  │
│   messages   │  │
├──────────────┤  │
│ id (PK)      │  │
│ conversation │  │
│ _id          │──┘
│ role         │
│ content      │
│ sources      │ (JSONB)
│ metadata     │
│ created_at   │
└──────────────┘
```

### Tables

#### `documents`
Stores uploaded document metadata.

| Column | Type | Description |
|--------|------|-------------|
| `id`   | UUID | Primary key |
| `filename` | VARCHAR(255) | Original filename |
| `content_type` | VARCHAR(100) | MIME type |
| `file_size` | BIGINT | Size in bytes |
| `status` | VARCHAR(20) | PROCESSING, COMPLETED, FAILED |
| `metadata` | JSONB | Additional metadata |
| `created_at` | TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |

**Indexes:**
- `idx_documents_status`: Status filtering
- `idx_documents_created_at`: Chronological ordering

#### `chunks`
Stores text chunks with vector embeddings.

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `document_id` | UUID | Foreign key → documents |
| `content` | TEXT | Chunk text content |
| `chunk_index` | INTEGER | Position in document |
| `embedding` | vector(768) | Gemini embedding vector |
| `metadata` | JSONB | Additional metadata |
| `created_at` | TIMESTAMP | Creation timestamp |

**Indexes:**
- `idx_chunks_document_id`: Document lookup
- `idx_chunks_document_chunk`: Ordering by document and index
- `idx_chunks_embedding`: **HNSW index** for vector similarity search

**HNSW Index Configuration:**
- `m = 16`: Number of connections per layer
- `ef_construction = 64`: Size of candidate list during construction
- Distance metric: **cosine distance** (`<=>` operator)

#### `conversations`
Stores chat session metadata.

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `session_id` | VARCHAR(255) | Unique session identifier |
| `title` | VARCHAR(500) | Conversation title |
| `metadata` | JSONB | Additional metadata |
| `created_at` | TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | Last update timestamp |

**Indexes:**
- `idx_conversations_session_id`: Session lookup (UNIQUE)
- `idx_conversations_created_at`: Chronological ordering

#### `messages`
Stores conversation messages.

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID | Primary key |
| `conversation_id` | UUID | Foreign key → conversations |
| `role` | VARCHAR(20) | USER or ASSISTANT |
| `content` | TEXT | Message text |
| `sources` | JSONB | Source references array |
| `metadata` | JSONB | Additional metadata |
| `created_at` | TIMESTAMP | Creation timestamp |

**Indexes:**
- `idx_messages_conversation_id`: Conversation lookup
- `idx_messages_conversation_created`: Chronological ordering

**Cascade Deletes:**
- Deleting a document → deletes all associated chunks
- Deleting a conversation → deletes all associated messages

---

## API Endpoints

### Document Management

#### `POST /api/documents/upload`
Uploads a document for processing.

**Request:**
- Content-Type: `multipart/form-data`
- Body: `file` (MultipartFile)

**Response:** `201 Created`
```json
{
  "id": "uuid",
  "filename": "document.md",
  "contentType": "text/markdown",
  "fileSize": 1024,
  "status": "PROCESSING",
  "createdAt": "2025-01-01T00:00:00Z"
}
```

#### `GET /api/documents`
Lists all documents.

**Response:** `200 OK`
```json
{
  "documents": [...],
  "total": 10
}
```

#### `GET /api/documents/{id}`
Retrieves document details.

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "filename": "document.md",
  "contentType": "text/markdown",
  "fileSize": 1024,
  "status": "COMPLETED",
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": "2025-01-01T00:00:00Z",
  "metadata": {...}
}
```

#### `DELETE /api/documents/{id}`
Deletes a document and all associated chunks.

**Response:** `204 No Content`

#### `GET /api/documents/{id}/chunks`
Lists all chunks for a document.

**Response:** `200 OK`
```json
[
  {
    "id": "uuid",
    "documentId": "uuid",
    "content": "chunk text...",
    "chunkIndex": 0,
    "createdAt": "2025-01-01T00:00:00Z"
  }
]
```

### Chat Operations

#### `POST /api/chat/query`
Processes a chat query through the RAG pipeline.

**Request:**
```json
{
  "query": "What is the main topic?",
  "sessionId": "session-123"
}
```

**Response:** `200 OK`
```json
{
  "answer": "The main topic is...",
  "sources": [
    {
      "documentName": "document.md",
      "chunkContent": "relevant text...",
      "similarityScore": 0.85,
      "chunkIndex": 0
    }
  ]
}
```

#### `GET /api/chat/history/{sessionId}`
Retrieves conversation history.

**Response:** `200 OK`
```json
[
  {
    "content": "What is the main topic?",
    "role": "USER",
    "createdAt": "2025-01-01T00:00:00Z",
    "sources": null
  },
  {
    "content": "The main topic is...",
    "role": "ASSISTANT",
    "createdAt": "2025-01-01T00:01:00Z",
    "sources": [...]
  }
]
```

#### `DELETE /api/chat/{sessionId}`
Deletes a conversation and all messages.

**Response:** `204 No Content`

### Health Check

#### `GET /api/health`
Checks system health (database and Gemini API).

**Response:** `200 OK`
```json
{
  "status": "UP",
  "database": {
    "status": "UP",
    "details": {...}
  },
  "gemini": {
    "status": "UP",
    "details": {...}
  }
}
```

---

## Service Layer

### Core Services

#### `DocumentService`
Handles document upload and processing.

**Responsibilities:**
- File validation (type, size)
- Document entity creation
- Asynchronous processing orchestration
- Status management (PROCESSING → COMPLETED/FAILED)

**Key Methods:**
- `uploadDocument(MultipartFile)`: Uploads and initiates processing
- `processDocumentAsync(UUID)`: Async processing pipeline
- `getDocumentById(UUID)`: Retrieves document
- `deleteDocument(UUID)`: Deletes document and chunks

#### `RAGService`
Orchestrates the complete RAG pipeline.

**Responsibilities:**
- Query embedding generation
- Vector similarity search
- Context building from chunks
- Prompt construction with conversation history
- LLM response generation
- Conversation and message persistence

**Key Methods:**
- `processQuery(ChatRequest)`: Complete RAG pipeline execution

**RAG Pipeline Steps:**
1. Get/Create conversation
2. Generate query embedding
3. Find similar chunks (vector search)
4. Build context from chunks
5. Build prompt (context + history + query)
6. Generate answer (LLM)
7. Build source references
8. Save and return response

#### `ChunkingService`
Splits documents into chunks.

**Responsibilities:**
- Text chunking with configurable size and overlap
- Token-based splitting (respects word boundaries)
- Metadata preservation

**Configuration:**
- Chunk size: 300 tokens (default)
- Overlap: 30 tokens (default)

#### `VectorSearchService`
Performs semantic similarity search.

**Responsibilities:**
- Vector similarity search using pgvector
- Cosine distance calculation
- Similarity threshold filtering
- Top-K result limiting

**Key Methods:**
- `findSimilarChunks(float[])`: Finds similar chunks
- `findSimilarChunks(float[], int)`: With custom top-K

**Query Strategy:**
- Uses `<=>` operator (cosine distance)
- Filters by similarity threshold
- Orders by similarity (most similar first)
- Limits to top-K results

#### `GeminiEmbeddingService`
Generates embeddings using Gemini API.

**Responsibilities:**
- Batch embedding generation (max 100 items)
- Embedding normalization
- Error handling and retries

**Configuration:**
- Model: `gemini-embedding-001`
- Dimension: 768 (via Matryoshka scaling)
- Batch size: 100

#### `GeminiLLMService`
Generates text responses using Gemini API.

**Responsibilities:**
- Prompt construction
- LLM API calls
- Response generation
- Token usage tracking

**Configuration:**
- Model: `gemini-2.5-flash-lite`
- Temperature: 0.7
- Max tokens: 512

### Integration Services

#### `ParserFactory`
Creates appropriate parser for document type.

**Supported Parsers:**
- `MarkdownParser`: CommonMark-based
- `HtmlParser`: JSoup-based
- `PdfParser`: PDFBox-based

#### `GeminiConfig`
Configures Gemini API clients.

**Configuration:**
- API key from environment variable
- Rate limiting
- Retry logic

---

## Integration Points

### Google Gemini API

#### Embedding API
- **Endpoint**: `https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-001:embedContent`
- **Model**: `gemini-embedding-001`
- **Dimension**: 768
- **Rate Limits**: 
  - 2M tokens/day (free tier)
  - 32K tokens/minute

#### Chat API
- **Endpoint**: `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent`
- **Model**: `gemini-2.5-flash-lite`
- **Temperature**: 0.7
- **Max Tokens**: 512

**Authentication:**
- API key via `GEMINI_API_KEY` environment variable
- Passed in request headers

**Error Handling:**
- Exponential backoff retry
- Rate limit handling
- Token usage logging

### PostgreSQL + pgvector

#### Connection
- **Host**: localhost (development)
- **Port**: 5432
- **Database**: wikichat
- **User**: wikichat_user

#### Vector Operations
- **Extension**: `vector`
- **Distance Metric**: Cosine distance (`<=>`)
- **Index Type**: HNSW (Hierarchical Navigable Small World)
- **Embedding Dimension**: 768

**Vector Query Example:**
```sql
SELECT * FROM chunks
WHERE embedding <=> '[0.1,0.2,...]'::vector < 0.5
ORDER BY embedding <=> '[0.1,0.2,...]'::vector
LIMIT 3;
```

---

## Configuration

### Application Configuration (`application.yml`)

#### Database
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/wikichat
    username: wikichat_user
    password: wikichat_password
  jpa:
    hibernate:
      ddl-auto: validate
```

#### Gemini API
```yaml
gemini:
  api:
    key: ${GEMINI_API_KEY}
  chat:
    model: gemini-2.5-flash-lite
    temperature: 0.7
    max-tokens: 512
  embedding:
    model: gemini-embedding-001
    dimension: 768
```

#### RAG Configuration
```yaml
rag:
  chunk:
    size: 300        # Tokens per chunk
    overlap: 30      # Overlap tokens
  search:
    top-k: 3         # Number of chunks to retrieve
    similarity-threshold: 0.5  # Minimum similarity (0.0-1.0)
  conversation:
    max-history-messages: 5    # Max previous messages in context
    include-history: true       # Enable conversation history
```

#### Upload Configuration
```yaml
upload:
  allowed-types: application/pdf,text/markdown,text/html,text/plain
  storage-directory: ./uploads
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB
```

#### Async Processing
```yaml
spring:
  task:
    execution:
      pool:
        core-size: 4
        max-size: 8
        queue-capacity: 100
      thread-name-prefix: doc-processor-
```

### Environment Variables

- `GEMINI_API_KEY`: Google Gemini API key (required)

---

## Deployment Architecture

### Development Setup

```
┌─────────────────┐
│   Development   │
│     Machine     │
│                 │
│  ┌───────────┐  │
│  │  Backend  │  │
│  │  (Spring  │  │
│  │   Boot)   │  │
│  └─────┬─────┘  │
│        │        │
│  ┌─────┴─────┐  │
│  │    CLI    │  │
│  │  (Picocli)│  │
│  └───────────┘  │
└────────┬────────┘
         │
         ↓
┌─────────────────┐
│   Docker        │
│   Container     │
│                 │
│  ┌───────────-┐ │
│  │ PostgreSQL │ │
│  │ + pgvector │ │
│  └──────────-─┘ │
└─────────────────┘
```

### Container Architecture

**Docker Compose Services:**
- `wikichat-postgres`: PostgreSQL 15 with pgvector extension

**Backend:**
- Runs as standalone Spring Boot application
- Connects to PostgreSQL container
- Listens on port 8080

**CLI:**
- Standalone Java application
- Connects to backend via HTTP
- No containerization required

### Production Considerations

1. **Database**: Use managed PostgreSQL service (AWS RDS, Google Cloud SQL)
2. **Backend**: Deploy to container orchestration (Kubernetes, Docker Swarm)
3. **Scaling**: Horizontal scaling for backend services
4. **Monitoring**: Application metrics, database performance, API usage
5. **Security**: API authentication, encrypted connections, secrets management
6. **Backup**: Regular database backups, document storage backup

---

## Design Patterns

### Used Patterns

1. **Layered Architecture**: Separation of concerns across layers
2. **Repository Pattern**: Data access abstraction
3. **Factory Pattern**: ParserFactory for document parsers
4. **Strategy Pattern**: Different parsers for different document types
5. **Dependency Injection**: Constructor injection throughout
6. **Async Processing**: Non-blocking document processing
7. **DTO Pattern**: Data transfer objects for API responses

### Best Practices

- **Immutability**: Use `record` for DTOs, `final` fields where possible
- **Transaction Management**: `@Transactional` for write operations
- **Error Handling**: Domain-specific exceptions, global exception handler
- **Logging**: Structured logging with SLF4J
- **Validation**: Input validation at controller level
- **Configuration**: Externalized configuration via `application.yml`

---

## Performance Considerations

### Vector Search Optimization

- **HNSW Index**: Fast approximate nearest neighbor search
- **Index Parameters**: Tuned for balance between speed and accuracy
- **Similarity Threshold**: Filters irrelevant results early
- **Top-K Limiting**: Reduces result set size

### Embedding Generation

- **Batch Processing**: Processes up to 100 chunks per batch
- **Async Processing**: Non-blocking document processing
- **Rate Limiting**: Respects Gemini API limits

### Database Optimization

- **Indexes**: Strategic indexes on frequently queried columns
- **Connection Pooling**: HikariCP connection pool
- **Query Optimization**: Efficient vector queries with pgvector

### Caching Opportunities

- **Embedding Cache**: Cache embeddings for repeated queries (not implemented)
- **Document Cache**: Cache parsed documents (not implemented)
- **Query Result Cache**: Cache frequent query results (not implemented)

---

## Security Considerations

### Current Implementation

- **Input Validation**: File type, size, and content validation
- **SQL Injection**: Protected by JPA parameterized queries
- **Error Messages**: Generic error messages (no sensitive data exposure)

### Production Recommendations

1. **Authentication**: API key or OAuth2 authentication
2. **Authorization**: Role-based access control
3. **HTTPS**: Encrypted connections
4. **Secrets Management**: Secure storage of API keys
5. **Rate Limiting**: Prevent abuse
6. **Input Sanitization**: Additional validation for user inputs
7. **Audit Logging**: Track document access and queries

---

## Future Enhancements

### Potential Improvements

1. **Multi-tenancy**: Support for multiple organizations/users
2. **Advanced Chunking**: Semantic chunking, sentence-aware splitting
3. **Hybrid Search**: Combine vector search with keyword search
4. **Embedding Cache**: Cache embeddings to reduce API calls
5. **Streaming Responses**: Stream LLM responses for better UX
6. **Web UI**: Browser-based interface
7. **Document Versioning**: Track document versions
8. **Analytics**: Query analytics and usage metrics
9. **Custom Models**: Support for custom embedding models
10. **Export/Import**: Export conversations, import documents in bulk

---

## References

- [README.md](../README.md) - Project overview and quick start
- [AGENTS.md](../AGENTS.md) - Development guidelines
- [CLI_README.md](../ao-wiki-chat-cli/CLI_README.md) - CLI documentation
- [CHUNKING_LOGIC.md](./CHUNKING_LOGIC.md) - Detailed chunking algorithm

---

**Last Updated**: January 2025  
**Version**: 0.0.1-SNAPSHOT
