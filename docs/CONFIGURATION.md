# Configuration guide

This document describes the main configuration parameters for the backend, as defined in `application.yml`. Use it as a reference when tuning the application.

---

## App: providers

Under `app` you choose which provider to use for embeddings and chat (they can be set independently if multiple providers are supported).

- **`app.embedding.provider`**  
  Provider used to generate **embeddings** (vector representations of text for semantic search).  
  Supported value: `ollama`. The embedding model and dimension are configured under the provider’s section (e.g. `ollama.embedding`).

- **`app.chat.provider`**  
  Provider used for **chat/LLM** (generation of answers).  
  Supported value: `ollama`. The chat model and parameters (temperature, max tokens, etc.) are configured under the provider’s section (e.g. `ollama.chat`).

---

## Chat: temperature

**Temperature** controls how “creative” or “deterministic” the model is when choosing the next token.

| Values | Effect |
|--------|--------|
| **Low (0.0 – 0.3)** | More predictable, stable responses. Suitable for technical answers, data, code, or questions with a single “correct” answer. |
| **High (0.7 – 1.0+)** | More varied and creative responses. Suitable for brainstorming or more natural, less repetitive answers. |

In practice: the higher the temperature, the more the model varies; the lower it is, the more it tends to pick the most likely token.

**In this project** the value is set in the provider’s section (e.g. `ollama.chat.temperature`). A value around 0.5–0.7 is often a good balance for RAG: natural responses without being overly random.

- If responses are too creative or unstable → lower it (e.g. 0.3–0.5).
- If responses are too repetitive or rigid → raise it slightly (e.g. 0.7–0.9).

---

## Ollama

When `app.embedding.provider` and/or `app.chat.provider` are set to `ollama`, the following section applies.

### `ollama.base-url`

Base URL of the Ollama server (default: `http://localhost:11434`). Change it if Ollama runs on another host or port.

### `ollama.embedding`

- **`model`**  
  Model used to generate embeddings (e.g. `nomic-embed-text`). Must be pulled in Ollama (`ollama pull nomic-embed-text`).

- **`dimension`**  
  Dimensionality of the embedding vector (e.g. 768). It must match the model output and `rag.vector.dimension`. If you change it, the vector store and index configuration must be updated accordingly.

### `ollama.chat`

- **`model`**  
  Chat model name (e.g. `llama3.2:3b`). Must be available in Ollama.

- **`temperature`**  
  Sampling temperature (see [Chat: temperature](#chat-temperature) above).

- **`num-predict`**  
  Maximum number of tokens to generate per response.

- **`timeout-seconds`**  
  Request timeout for chat calls.

---

## RAG

Parameters for retrieval-augmented generation (chunking, search, vector store).

### `rag.chunk`

- **`size`** – Target size (in characters or tokens, depending on implementation) for each text chunk.
- **`overlap`** – Overlap between consecutive chunks to preserve context at boundaries.

### `rag.search`

- **`top-k`** – Number of most similar chunks to retrieve for each query.
- **`similarity-threshold`** – Minimum similarity score for a chunk to be included (depends on `rag.vector.distance-metric`).

### `rag.vector`

- **`dimension`** – Must match the embedding dimension of the configured provider (e.g. `ollama.embedding.dimension`).
- **`distance-metric`** – Metric used for similarity (e.g. `cosine`). Must be consistent with how embeddings are produced and indexed.

### `rag.reranker` (optional)

- **`enabled`** – If `true`, a second-stage reranker is used (e.g. Cohere) to improve context quality.
- **`candidate-factor`** – When enabled, the system fetches `top-k * candidate-factor` candidates from vector search, then reranks them to `top-k`.

### `rag.conversation`

- **`max-history-messages`** – Maximum number of previous messages included in the conversation context.
- **`include-history`** – Enable or disable conversation context in RAG prompts.

---

## Upload

- **`upload.allowed-types`** – Comma-separated MIME types allowed for document uploads (e.g. PDF, Markdown, HTML, plain text).
- **`upload.storage-directory`** – Directory where uploaded files are stored (relative or absolute path).

---

## Other sections

- **`spring.datasource`** – Database connection (URL, username, password, Hikari pool settings).
- **`spring.jpa`** – JPA/Hibernate options (e.g. `ddl-auto`, `show-sql`).
- **`server.port`** – HTTP port of the application.
- **`management.endpoints`** – Which actuator endpoints are exposed (e.g. health, info).
- **`logging.level`** – Log levels per package.
- **`cohere`** – Only required when `rag.reranker.enabled=true`; API key and rerank model configuration.

For full details, see `ao-wiki-chat-backend/src/main/resources/application.yml`.
