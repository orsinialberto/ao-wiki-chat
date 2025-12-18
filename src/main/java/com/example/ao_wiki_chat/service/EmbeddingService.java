package com.example.ao_wiki_chat.service;

import java.util.List;

/**
 * Service interface for text embedding operations.
 * Abstracts the underlying embedding provider to allow easy switching
 * between different implementations (Gemini, OpenAI, local models, etc.)
 */
public interface EmbeddingService {
    
    /**
     * Generates a vector embedding for a single text.
     *
     * @param text the input text to embed
     * @return the embedding as a float array
     * @throws com.example.ao_wiki_chat.exception.EmbeddingException if embedding generation fails
     */
    float[] generateEmbedding(String text);
    
    /**
     * Generates vector embeddings for multiple texts in batch.
     * Batch processing is more efficient for large collections.
     *
     * @param texts the list of texts to embed
     * @return list of embeddings (same order as input)
     * @throws com.example.ao_wiki_chat.exception.EmbeddingException if embedding generation fails
     */
    List<float[]> generateEmbeddings(List<String> texts);
    
    /**
     * Returns the dimensionality of the embeddings produced by this service.
     *
     * @return the embedding dimension (e.g., 768 for Gemini text-embedding-004)
     */
    int getEmbeddingDimension();
    
    /**
     * Checks if the embedding service is available and responsive.
     *
     * @return true if the service is healthy, false otherwise
     */
    boolean isHealthy();
}

