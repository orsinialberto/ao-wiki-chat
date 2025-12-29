package com.example.ao_wiki_chat.model.dto;

/**
 * Gemini API health status response DTO.
 * Used for Gemini API availability health checks.
 */
public record GeminiHealthResponse(
        /**
         * Health status: "UP" or "DOWN".
         */
        String status,
        
        /**
         * Gemini API availability: "available" or "unavailable".
         */
        String gemini
) {
}
