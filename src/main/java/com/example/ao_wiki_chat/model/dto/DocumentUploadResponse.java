package com.example.ao_wiki_chat.model.dto;

import com.example.ao_wiki_chat.model.enums.DocumentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO returned after document upload.
 * Contains basic document information immediately after upload, before processing completes.
 */
public record DocumentUploadResponse(
        /**
         * Unique identifier of the uploaded document.
         */
        @NotNull
        UUID documentId,

        /**
         * Original filename of the uploaded document.
         */
        @NotNull
        String filename,

        /**
         * MIME type of the uploaded document (e.g., "application/pdf", "text/markdown").
         */
        @NotNull
        String contentType,

        /**
         * Size of the uploaded file in bytes.
         */
        @NotNull
        @Positive
        Long fileSize,

        /**
         * Current processing status of the document.
         * Typically PROCESSING immediately after upload.
         */
        @NotNull
        DocumentStatus status,

        /**
         * Timestamp when the document was uploaded.
         */
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}
