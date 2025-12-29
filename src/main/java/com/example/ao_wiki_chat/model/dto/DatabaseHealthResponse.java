package com.example.ao_wiki_chat.model.dto;

/**
 * Database health status response DTO.
 * Used for database connectivity health checks.
 */
public record DatabaseHealthResponse(
        /**
         * Health status: "UP" or "DOWN".
         */
        String status,
        
        /**
         * Database connection status: "connected" or "disconnected".
         */
        String database
) {
}
