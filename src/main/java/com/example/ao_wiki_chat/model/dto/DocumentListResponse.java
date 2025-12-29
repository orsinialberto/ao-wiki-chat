package com.example.ao_wiki_chat.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * Response DTO for paginated list of documents.
 * Used by GET /api/documents endpoint to return multiple documents with total count.
 */
public record DocumentListResponse(
        /**
         * List of document details.
         * Ordered by creation date (newest first) or as specified by the request.
         */
        @NotNull
        List<DocumentResponse> documents,

        /**
         * Total number of documents matching the query criteria.
         * Used for pagination calculations.
         */
        @NotNull
        @PositiveOrZero
        Integer totalCount
) {}
