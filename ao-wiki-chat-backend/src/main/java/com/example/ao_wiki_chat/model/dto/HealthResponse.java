package com.example.ao_wiki_chat.model.dto;

/**
 * Health status response DTO.
 * Used for general application health checks.
 */
public record HealthResponse(
        /**
         * Health status: "UP" or "DOWN".
         */
        String status
) {
}
