# Reranker

This document describes the **reranker** feature in the RAG (Retrieval-Augmented Generation) pipeline: what it is, how it works, and how to configure it.

## What is a Reranker?

A **reranker** is a second-stage component that reorders the results of an initial retrieval (vector search) by relevance to the user query. It does not perform retrieval itself; it takes a list of candidate chunks and returns the same or a subset of them in a better order (and optionally trims to a smaller top-k).

- **First stage (retrieval):** Vector search returns many candidates quickly (e.g. by cosine similarity between query and chunk embeddings).
- **Second stage (rerank):** A more accurate model (e.g. a cross-encoder or dedicated rerank API) scores each candidate against the query and returns the top-k by relevance.

Reranking often improves answer quality because embedding similarity does not always match “does this chunk actually answer the question?”. A reranker can boost truly relevant chunks and demote superficially similar ones.

## Role in the RAG Pipeline

When the reranker is **enabled**:

1. **Embedding:** The user query is embedded.
2. **Retrieval:** Vector search returns **more** candidates than needed (e.g. `top-k × candidate-factor`).
3. **Rerank:** The reranker scores and reorders these candidates, then returns the top-k.
4. **Context:** The (reranked) top-k chunks are used to build the context for the LLM.
5. **LLM:** The model generates an answer using that context.

When the reranker is **disabled** (default):

- Vector search returns exactly `rag.search.top-k` chunks.
- The no-op reranker simply passes them through (or truncates to top-k if needed).
- No external API is called.

## Configuration

### Application properties

| Property | Default | Description |
|----------|---------|-------------|
| `rag.reranker.enabled` | `false` | Set to `true` to use the Cohere Rerank API. When `false`, a no-op reranker is used. |
| `rag.reranker.candidate-factor` | `3` | When reranker is active: retrieval fetches `rag.search.top-k × candidate-factor` chunks; the reranker then returns the top `rag.search.top-k`. Ignored when reranker is disabled. |
| `rag.search.top-k` | `6` | Number of chunks finally used for context (after reranking, when enabled). |

### Cohere (when `rag.reranker.enabled=true`)

| Property | Default | Description |
|----------|---------|-------------|
| `cohere.api.key` | (env: `COHERE_API_KEY`) | Cohere API key. **Required** when reranker is enabled. |
| `cohere.rerank.model` | `rerank-v3.5` | Cohere rerank model name. |

Example in `application.yml`:

```yaml
rag:
  search:
    top-k: 6
  reranker:
    enabled: true
    candidate-factor: 3

cohere:
  api:
    key: ${COHERE_API_KEY:}
  rerank:
    model: rerank-v3.5
```

If `rag.reranker.enabled` is `true` and `cohere.api.key` is empty, the application fails at startup with a clear error.

## Implementations

- **NoOpRerankerService**  
  Used when `rag.reranker.enabled` is `false`. Returns the input list unchanged (or truncated to top-k). No external calls.

- **CohereRerankerService**  
  Used when `rag.reranker.enabled` is `true`. Calls the [Cohere Rerank API (v2)](https://docs.cohere.com/reference/rerank) with the query and chunk texts, then maps the API response (ordered by relevance) back to chunk entities.

## Errors

If the reranker step fails (e.g. Cohere API error, network, or invalid response), a `RerankerException` is thrown. It is handled by `GlobalExceptionHandler` and results in an HTTP 500 response with a generic “Reranking failed” message.

## Summary

- **Reranker** = second-stage component that reorders (and optionally trims) retrieval results by relevance.
- **Default:** Disabled; vector search top-k is used as-is.
- **Optional:** Enable with `rag.reranker.enabled: true` and set `COHERE_API_KEY` (or `cohere.api.key`); retrieval size is increased by `candidate-factor`, then Cohere Rerank returns the final top-k for context.
