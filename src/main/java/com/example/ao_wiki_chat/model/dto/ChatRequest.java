package com.example.ao_wiki_chat.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for chat queries.
 * Contains the user's question and session identifier for conversation tracking.
 */
public record ChatRequest(

        @NotBlank(message = "Query cannot be blank")
        @Size(min = 1, max = 2000, message = "Query must be between 1 and 2000 characters")
        String query,
        
        @NotBlank(message = "Session ID cannot be blank")
        @Size(min = 1, max = 255, message = "Session ID must be between 1 and 255 characters")
        String sessionId
) {}
