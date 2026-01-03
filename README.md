# WikiChat - RAG System with Java and Gemini

RAG (Retrieval-Augmented Generation) system that allows uploading documents (MD, HTML, PDF) and asking questions about their content through a chat interface. Uses vector embeddings to retrieve relevant information and Google Gemini to generate contextualized responses.

## 🛠️ Tech Stack

- **Backend**: Java 25, Spring Boot 4.x
- **CLI**: Java 25, Picocli
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

Spring Boot reads environment variables from your system environment:

**Export in your shell session**
```bash
export GEMINI_API_KEY=your_api_key_here

source ~/.bashrc  # oppure ~/.zshrc
```

---

## Build and Start Application

### Download Dependences
```bash
./mvnw clean install
```

### Run from project root, specifying the backend module
```bash
./mvnw spring-boot:run -pl ao-wiki-chat-backend
```

---

## 💻 Command-Line Interface (CLI)

WikiChat includes a powerful CLI for interacting with the system from the command line.

### Quick Start CLI

```bash
# Build the CLI
./mvnw clean package -pl ao-wiki-chat-cli -am

# Install wrapper script (Unix/macOS)
chmod +x scripts/wikichat
export PATH="$PATH:$(pwd)/scripts"

# Or use directly
java -jar ao-wiki-chat-cli/target/ao-wiki-chat-cli-0.0.1-SNAPSHOT.jar --help
```

### Basic Usage

```bash
# Configure backend URL
wikichat config set api.url http://localhost:8080

# Check system health
wikichat health

# Upload a document
wikichat upload document.md --wait

# Query the system
wikichat query "What is the main topic?"

# Start interactive mode
wikichat interactive
```

### Available Commands

- **Document Management**: `upload`, `list`, `show`, `delete`, `chunks`
- **Chat**: `query`, `interactive`, `history`, `clear`
- **System**: `health`, `config`

### Documentation

For complete CLI documentation, see [CLI_README.md](ao-wiki-chat-cli/CLI_README.md).

---

## 📚 Documentation

- [AGENTS.md](AGENTS.md) - Guidelines for developers and AI agents
- [CLI_README.md](ao-wiki-chat-cli/CLI_README.md) - CLI user guide and reference
- [docs/CHUNKING_LOGIC.md](docs/CHUNKING_LOGIC.md) - Chunking algorithm explained

---