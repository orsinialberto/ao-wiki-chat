package com.example.ao_wiki_chat.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for document chunk details.
 * Used by GET /api/documents/{id}/chunks endpoint to return chunks of a document.
 */
public record ChunkResponse(
        /**
         * Unique identifier of the chunk.
         */
        @NotNull
        UUID id,

        /**
         * Unique identifier of the parent document.
         */
        @NotNull
        UUID documentId,

        /**
         * Text content of the chunk.
         */
        @NotNull
        String content,

        /**
         * Zero-based index indicating the position of this chunk within the document.
         * Lower indices appear earlier in the document.
         */
        @NotNull
        @PositiveOrZero
        Integer chunkIndex,

        /**
         * Timestamp when the chunk was created.
         */
        @NotNull
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}
