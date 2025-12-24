package com.example.ao_wiki_chat.model.dto;

import java.util.List;

/**
 * Response DTO for chat queries.
 * Contains the generated answer and references to source chunks used.
 */
public record ChatResponse(
        /**
         * The generated answer from the LLM.
         */
        String answer,
        
        /**
         * List of source references (chunks) used to generate the answer.
         * Ordered by relevance (most relevant first).
         */
        List<SourceReference> sources
) {}