# WikiChat - RAG System with Java and Gemini

RAG (Retrieval-Augmented Generation) system that allows uploading documents (MD, HTML, PDF) and asking questions about their content through a chat interface. Uses vector embeddings to retrieve relevant information and Google Gemini to generate contextualized responses.

## 🛠️ Tech Stack

- **Backend**: Java 25, Spring Boot 4.x
- **Database**: PostgreSQL 15 + pgvector
- **AI/ML**: LangChain4j, Google Gemini API
- **Containerization**: Docker, Docker Compose

---

## 🚀 Quick Start

### Prerequisites

- Docker and Docker Compose installed
- Java 25 installed
- Maven installed

### 1. Database Startup

```bash
# Start the PostgreSQL container with pgvector
docker-compose up -d

# Verify the container is running
docker-compose ps

# Check database logs
docker-compose logs postgres

# (Optional) Follow logs in real-time
docker-compose logs -f postgres
```

### 2. Database Connection Verification

```bash
# Connect to the PostgreSQL database
docker exec -it wikichat-postgres psql -U wikichat_user -d wikichat
```

Inside the `psql` shell, run:

```sql
-- Verify installed extensions (should include 'vector')
\dx

-- List all tables
\dt

-- Show chunks table structure with vector embedding
\d chunks

-- Verify PostgreSQL version
SELECT version();

-- Exit psql
\q
```

### 3. Shutdown and Cleanup

```bash
# Stop containers
docker-compose down

# Stop containers and remove volumes (WARNING: deletes all data)
docker-compose down -v
```

---

## 📊 Database Structure

The `wikichat` database contains 4 main tables:

### `documents`
Stores documents uploaded by users.
- `id` (UUID): Primary key
- `filename`: Original file name
- `content_type`: MIME type (text/markdown, text/html, application/pdf)
- `file_size`: Size in bytes
- `status`: Processing status (PROCESSING, COMPLETED, FAILED)
- `metadata` (JSONB): Additional metadata
- `created_at`, `updated_at`: Timestamps

### `chunks`
Text fragments extracted from documents with vector embeddings.
- `id` (UUID): Primary key
- `document_id` (UUID): Foreign key → documents
- `content`: Fragment text (~500 tokens)
- `chunk_index`: Position in original document
- `embedding` (vector[768]): Gemini vector embedding
- `metadata` (JSONB): Additional metadata
- **HNSW index** for fast vector search (cosine distance)

### `conversations`
User chat sessions.
- `id` (UUID): Primary key
- `session_id`: Unique session identifier
- `title`: Conversation title
- `metadata` (JSONB): Additional metadata
- `created_at`, `updated_at`: Timestamps

### `messages`
Messages within conversations.
- `id` (UUID): Primary key
- `conversation_id` (UUID): Foreign key → conversations
- `role`: USER or ASSISTANT
- `content`: Message text
- `sources` (JSONB): References to source chunks
- `metadata` (JSONB): Additional metadata
- `created_at`: Timestamp

---

## 🔧 Configuration

### Database Credentials (Development)

- **Host**: localhost
- **Port**: 5432
- **Database**: wikichat
- **User**: wikichat_user
- **Password**: wikichat_password

⚠️ **IMPORTANT**: These credentials are for local development only. Do not use them in production!

### Environment Variables

Create a `.env` file in the project root with:

```env
# Gemini API Key
GEMINI_API_KEY=your_api_key_here

# Database (optional, already configured in docker-compose)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=wikichat
DB_USER=wikichat_user
DB_PASSWORD=wikichat_password
```

---

## 📚 Documentation

- [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md) - Complete development plan
- [ARCHITECTURE.md](ARCHITECTURE.md) - System architecture
- [AGENTS.md](AGENTS.md) - Guidelines for developers and AI agents
- [SETUP.md](SETUP.md) - Detailed setup guide

---

## 🧪 Database Testing

After starting PostgreSQL, you can test CRUD operations:

```sql
-- Insert a test document
INSERT INTO documents (filename, content_type, file_size, status)
VALUES ('test.md', 'text/markdown', 1024, 'PROCESSING');

-- Verify insertion
SELECT * FROM documents;

-- Insert a chunk with test embedding
INSERT INTO chunks (document_id, content, chunk_index, embedding)
VALUES (
    (SELECT id FROM documents LIMIT 1),
    'This is a test chunk',
    0,
    '[0.1, 0.2, ...]'::vector  -- 768 dimensions required
);

-- Test vector search (replace with real embedding)
SELECT id, content, embedding <=> '[0.1, 0.2, ...]'::vector AS distance
FROM chunks
ORDER BY distance
LIMIT 5;
```

---

## 🐛 Troubleshooting

### Container won't start

```bash
# Check logs for errors
docker-compose logs postgres

# Verify port 5432 is not already in use
lsof -i :5432

# Restart from scratch
docker-compose down -v
docker-compose up -d
```

### "relation does not exist" error

The `init.sql` script runs only on first volume creation. If you modify the schema:

```bash
# Remove volume and recreate
docker-compose down -v
docker-compose up -d
```

### pgvector extension not available

Verify you're using the correct Docker image:
```bash
docker-compose ps
# Should show: pgvector/pgvector:pg15
```

---

## 📝 Implementation Status

- [x] **Phase 1.1**: Docker and PostgreSQL Configuration ✅
- [x] **Phase 1.2**: Maven and Dependencies Configuration ✅
- [x] **Phase 1.3**: Application Configuration (application.yml) ✅
- [x] **Phase 1.4**: Gemini API Setup ✅
- [ ] Phase 2: Model and Database Layer
- [ ] Phase 3: Integration Layer
- [ ] Phase 4: Business Logic
- [ ] Phase 5: REST API
- [ ] Phase 6: Advanced Configuration
- [ ] Phase 7: Testing
- [ ] Phase 8: Deployment

---

## 🤝 Contributing

To contribute to the project, check the guidelines in [AGENTS.md](AGENTS.md).

## 📄 License

[Specify license]

---

**Version**: 0.2.0 (Phase 1 completed)  
**Last Updated**: December 2025

