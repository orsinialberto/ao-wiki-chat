# WikiChat - RAG System with Java and Gemini

RAG (Retrieval-Augmented Generation) system that allows uploading documents (MD, HTML, PDF) and asking questions about their content through a chat interface. Uses vector embeddings to retrieve relevant information and Google Gemini to generate contextualized responses.

## 🛠️ Tech Stack

- **Backend**: Java 25, Spring Boot 4.x
- **CLI**: Java 25, Picocli
- **Database**: PostgreSQL 15 + pgvector
- **AI/ML**: LangChain4j, Google Gemini API
- **Containerization**: Docker, Docker Compose

---

## Quick Start

### Prerequisites

- Docker and Docker Compose installed
- Java 25 installed
- Maven installed
- **Ollama** (optional): required if you use local embeddings; see [Ollama setup](#ollama-local-embeddings) below.

### Ollama (local embeddings)

To use **local embeddings** instead of Gemini, you need Ollama running and the embedding model installed.

**1. Install Ollama**

- **macOS / Linux**: install from [ollama.com](https://ollama.com) or via shell:
  ```bash
  curl -fsSL https://ollama.com/install.sh | sh
  ```

**2. Start Ollama and pull the embedding model**

Ollama runs as a service (often started automatically after install). The project expects the **nomic-embed-text** model for embeddings:

```bash
# Pull the embedding model (used when app.embedding.provider=ollama)
ollama pull nomic-embed-text
```

**3. Configure the backend**

In `ao-wiki-chat-backend/src/main/resources/application.yml` set:

```yaml
app:
  embedding:
    provider: ollama
```

Optional: adjust `ollama.base-url` if Ollama is not on `http://localhost:11434`.

Without Ollama (and with `provider: gemini`), the backend uses the Google Gemini API for embeddings; set `GEMINI_API_KEY` in that case.

### 1. Database Startup

```bash
# Start the PostgreSQL container with pgvector
docker-compose up -d

# Verify the container is running
docker-compose ps

```

### 2. Environment Variables

Spring Boot reads environment variables from your system environment:

**Export in your shell session**
```bash
export GEMINI_API_KEY=your_api_key_here
```


### 3. Download dependencies
```bash
./mvnw clean install
```

### Run from project root, specifying the backend module
```bash
./mvnw spring-boot:run -pl ao-wiki-chat-backend
```

### Running tests

To run the tests you must have Docker running and the project containers started (e.g. with `docker-compose up -d`). The tests rely on services (PostgreSQL, etc.) provided by the containers.

```bash
# Run the tests
./mvnw test
```

---

## Command-Line Interface (CLI)

WikiChat includes a powerful CLI for interacting with the system from the command line.

### Quick Start CLI

```bash
# Build the CLI
./mvnw clean package -pl ao-wiki-chat-cli -am

# Install wrapper script (Unix/macOS)
chmod +x scripts/wikichat
export PATH="$PATH:$(pwd)/scripts"
```

### Basic Usage

```bash
# Configure backend URL
wikichat config set api.url http://localhost:8080

# Check system health
wikichat health

# Upload a document (or wait for processing to complete)
wikichat upload document.md --wait

# Upload all supported files from a folder (recursive, async; lists uploaded files at the end)
wikichat upload ./my-docs
wikichat upload ./my-docs --format json

# Query the system
wikichat query "What is the main topic?"

# Start interactive mode
wikichat interactive

# List all documents
wikichat list --format plain
```

### Available Commands

- **Document Management**: `upload` (file or folder), `list`, `show`, `delete`, `chunks`
- **Chat**: `query`, `interactive`, `history`, `clear`
- **System**: `health`, `config`

### Checking the database schema

```bash
# Count total chunks
docker exec -it wikichat-postgres psql -U wikichat_user -d wikichat -c "SELECT COUNT(*) FROM chunks;"

# View all chunks with document info
docker exec -it wikichat-postgres psql -U wikichat_user -d wikichat -c "SELECT c.id, c.chunk_index, LEFT(c.content, 50) as content_preview, d.filename, c.created_at FROM chunks c JOIN documents d ON c.document_id = d.id ORDER BY d.created_at DESC, c.chunk_index;"

# Count chunks per document
docker exec -it wikichat-postgres psql -U wikichat_user -d wikichat -c "SELECT d.filename, COUNT(c.id) as chunk_count FROM documents d LEFT JOIN chunks c ON d.id = c.document_id GROUP BY d.id, d.filename ORDER BY chunk_count DESC;"

# Check if embeddings are present
docker exec -it wikichat-postgres psql -U wikichat_user -d wikichat -c "SELECT COUNT(*) as total_chunks, COUNT(embedding) as chunks_with_embeddings, COUNT(*) - COUNT(embedding) as chunks_without_embeddings FROM chunks;"
```

---

## Documentation

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) - System architecture and technical design
- [docs/CHUNKING_LOGIC.md](docs/CHUNKING_LOGIC.md) - Chunking algorithm explained

---