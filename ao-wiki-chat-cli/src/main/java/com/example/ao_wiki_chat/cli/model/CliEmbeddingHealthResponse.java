package com.example.ao_wiki_chat.cli.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Embedding service health response DTO for CLI.
 * Mirrors the backend EmbeddingHealthResponse structure.
 */
public record CliEmbeddingHealthResponse(
        @JsonProperty("status")
        String status,

        @JsonProperty("embedding")
        String embedding
) {}
