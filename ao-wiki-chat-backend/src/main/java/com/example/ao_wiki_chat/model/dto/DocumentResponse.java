package com.example.ao_wiki_chat.model.dto;

import com.example.ao_wiki_chat.model.enums.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Full document details response DTO.
 * Contains all document information including metadata and timestamps.
 */
public record DocumentResponse(
        /**
         * Unique identifier of the document.
         */
        @NotNull
        UUID documentId,

        /**
         * Original filename of the document.
         */
        @NotNull
        String filename,

        /**
         * MIME type of the document (e.g., "application/pdf", "text/markdown").
         */
        @NotNull
        String contentType,

        /**
         * Size of the file in bytes.
         */
        @NotNull
        @Positive
        Long fileSize,

        /**
         * Current processing status of the document.
         */
        @NotNull
        DocumentStatus status,

        /**
         * Timestamp when the document was uploaded.
         */
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,

        /**
         * Timestamp when the document was last updated.
         */
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt,

        /**
         * Optional metadata associated with the document.
         * Contains additional information stored as key-value pairs.
         * May be null if no metadata is present.
         */
        Map<String, Object> metadata
) {}
