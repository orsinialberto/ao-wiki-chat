package com.example.ao_wiki_chat.model.dto;

/**
 * Embedding service health status response DTO.
 * Used for embedding provider availability health checks.
 */
public record EmbeddingHealthResponse(
        /**
         * Health status: "UP" or "DOWN".
         */
        String status,

        /**
         * Embedding provider availability: "available" or "unavailable".
         */
        String embedding
) {
}
