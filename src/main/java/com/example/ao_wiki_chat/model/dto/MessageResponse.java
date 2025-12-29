package com.example.ao_wiki_chat.model.dto;

import com.example.ao_wiki_chat.model.enums.MessageRole;
import java.time.LocalDateTime;

/**
 * Response DTO for individual messages in conversation history.
 * Contains message content, role, and metadata.
 */
public record MessageResponse(
        /**
         * The message content (query or answer).
         */
        String content,
        
        /**
         * The role of the message (USER or ASSISTANT).
         */
        MessageRole role,
        
        /**
         * Timestamp when the message was created.
         */
        LocalDateTime createdAt,
        
        /**
         * Source references for ASSISTANT messages (JSON string).
         * Null for USER messages.
         */
        String sources
) {}
