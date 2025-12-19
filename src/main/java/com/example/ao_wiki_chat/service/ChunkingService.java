package com.example.ao_wiki_chat.service;

import java.util.List;

/**
 * Service interface for text chunking operations.
 * Splits documents into smaller, semantically coherent chunks suitable for embedding.
 * Uses configurable chunk size with overlap to preserve context at boundaries.
 */
public interface ChunkingService {
    
    /**
     * Splits text into chunks using semantic boundaries.
     * Preserves sentence and paragraph integrity while respecting target chunk size.
     *
     * @param text the input text to chunk
     * @param chunkSize target chunk size in characters (approximate)
     * @param overlap overlap size in characters between consecutive chunks
     * @return list of text chunks
     * @throws IllegalArgumentException if text is null or parameters are invalid
     */
    List<String> splitIntoChunks(String text, int chunkSize, int overlap);
    
    /**
     * Splits text into chunks using default configuration from properties.
     * Uses rag.chunk.size and rag.chunk.overlap from application.yml.
     *
     * @param text the input text to chunk
     * @return list of text chunks
     * @throws IllegalArgumentException if text is null
     */
    List<String> splitIntoChunks(String text);
    
    /**
     * Validates if chunk size and overlap parameters are valid.
     *
     * @param chunkSize target chunk size
     * @param overlap overlap size
     * @return true if parameters are valid, false otherwise
     */
    boolean areValidParameters(int chunkSize, int overlap);
}

