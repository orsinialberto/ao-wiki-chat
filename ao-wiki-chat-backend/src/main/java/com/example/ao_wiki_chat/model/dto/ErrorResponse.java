package com.example.ao_wiki_chat.model.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized error response DTO for all API errors.
 * Provides consistent error structure across the application.
 */
public record ErrorResponse(
        /**
         * Timestamp when the error occurred.
         */
        String timestamp,
        
        /**
         * HTTP status code.
         */
        Integer status,
        
        /**
         * Error type/category (e.g., "Bad Request", "Internal Server Error").
         */
        String error,
        
        /**
         * Human-readable error message.
         */
        String message,
        
        /**
         * Request path where the error occurred (optional).
         */
        String path,
        
        /**
         * Field-level validation errors (optional, only for validation errors).
         * Key: field name, Value: error message.
         */
        Map<String, String> fieldErrors
) {
    /**
     * Creates an ErrorResponse without field errors.
     */
    public ErrorResponse(Integer status, String error, String message, String path) {
        this(Instant.now().toString(), status, error, message, path, null);
    }
    
    /**
     * Creates an ErrorResponse with field errors (for validation errors).
     */
    public ErrorResponse(Integer status, String error, String message, String path, Map<String, String> fieldErrors) {
        this(Instant.now().toString(), status, error, message, path, fieldErrors);
    }
}
