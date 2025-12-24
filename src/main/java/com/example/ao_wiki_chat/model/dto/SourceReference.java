package com.example.ao_wiki_chat.model.dto;

/**
 * Reference to a source document chunk used in the RAG response.
 * Contains information about the document and chunk that contributed to the answer.
 */
public record SourceReference(
        /**
         * Name of the source document.
         */
        String documentName,
        
        /**
         * Content of the relevant chunk.
         */
        String chunkContent,
        
        /**
         * Similarity score (0.0-1.0) indicating how relevant this chunk is to the query.
         */
        Double similarityScore,
        
        /**
         * Index of the chunk within the document.
         */
        Integer chunkIndex
) {}