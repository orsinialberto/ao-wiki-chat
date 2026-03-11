# Gemini Configuration

This document describes the configuration parameters for the Google Gemini integration in the backend (chat and embedding), as defined in `application.yml`.

---

## Chat: temperature

**Temperature** controls how “creative” or “deterministic” the model is when choosing the next token while generating a response.

| Values | Effect |
|--------|--------|
| **Low (0.0 – 0.3)** | More predictable, stable responses with less variation. Suitable for technical answers, data, code, or questions with a single “correct” answer. |
| **High (0.7 – 1.0+)** | More varied and creative responses, with greater lexical and structural diversity. Suitable for brainstorming, copywriting, or more natural, less repetitive answers. |

In practice: the higher the temperature, the more the model “shuffles” token probabilities; the lower it is, the more it tends to pick the most likely token almost every time.

**In this project** the value is set in `gemini.chat.temperature` (e.g. `0.7`) and used by `GeminiConfig` for both the chat and streaming models. A value around 0.7 is a good balance for a chat/RAG use case: natural responses without being overly random.

- If responses are too creative or unstable → lower it (e.g. 0.3–0.5).
- If responses are too repetitive or rigid → raise it slightly (e.g. 0.8–0.9).

---

## Embedding

The `gemini.embedding` section in `application.yml` configures the embedding model and service behaviour (text vectorisation for semantic search / RAG).

### `model: gemini-embedding-001`

Gemini model used to generate **embeddings** (numeric vectors of text). It is separate from the chat model: here we do not generate text, but convert strings into vectors (e.g. 768 dimensions) for semantic search. In `GeminiConfig` it is passed to `GoogleAiEmbeddingModel.builder().modelName(embeddingModel)`.

### `dimension: 768`

**Dimensionality** of the embedding vector. Each text chunk is represented as a vector of 768 floats. It must match the vector database configuration and `rag.vector.dimension` in `application.yml`. If you change this value, the vector store and index configuration must be updated accordingly.

### `batch-size: 25`

Number of **texts per API call** when generating embeddings in batch (e.g. when indexing many chunks). Instead of one request per text, the service sends one request per 25 texts. This reduces the number of calls and helps stay within API limits (e.g. free tier ~100 requests/minute). In `GeminiEmbeddingService`, texts are split into groups of `batchSize` and each group is sent via `embeddingModel.embedAll(segments)`.

### `batch-delay-ms: 15000`

**Delay in milliseconds between one batch and the next** (15 seconds). After processing a batch of `batch-size` texts, the service waits this long before sending the next batch. This helps respect API rate limits. In the code: if it is not the first batch and `batchDelayMs > 0`, a `sleep(batchDelayMs)` is executed before processing the next batch.

### `max-retries: 5`

**Maximum number of retry attempts** per batch on failure (e.g. 429 rate limit or transient errors). If an `embedAll` call fails, the service retries up to 5 times (with backoff) before throwing. Used in `embedBatchWithRetry`: the loop continues until the call succeeds or `maxRetries` is reached.

### `retry-base-delay-ms: 10000`

**Base delay for exponential backoff** in milliseconds (10 seconds). On the first retry the wait is about 10 s, on the second about 20 s, on the third about 40 s, etc. (`retryBaseDelayMs * 2^(attempt-1)`), capped at 90 seconds. If the API 429 response includes a message like “Please retry in Xs”, that value (with a small buffer) is used instead of the backoff, still capped at 90 s. Together with `max-retries`, this defines how long to wait before retrying after an error.

---

## Code references

- **Chat (temperature, model, max-tokens):** `GeminiConfig` → `geminiChatModel()`, `geminiStreamingChatModel()`.
- **Embedding (model, dimension):** `GeminiConfig` → `geminiEmbeddingModel()`.
- **Embedding (batch and retry):** `GeminiEmbeddingService` (constructor and `embedBatchWithRetry`).
