package com.example.ao_wiki_chat.service;

import java.util.List;

import com.example.ao_wiki_chat.exception.EmbeddingException;

/**
 * Contract for services that generate vector embeddings from text.
 * Implementations may use different providers (e.g. Ollama) and
 * support single or batch embedding generation.
 */
public interface EmbeddingService {

    /**
     * Generates a vector embedding for a single text.
     *
     * @param text the input text to embed
     * @return the embedding as a float array
     * @throws EmbeddingException if embedding generation fails
     */
    float[] generateEmbedding(String text);

    /**
     * Generates vector embeddings for multiple texts (e.g. in batch).
     *
     * @param texts the list of texts to embed
     * @return list of embeddings in the same order as the input texts
     * @throws EmbeddingException if embedding generation fails
     */
    List<float[]> generateEmbeddings(List<String> texts);

    /**
     * Returns the dimensionality of the embeddings produced by this service.
     *
     * @return the embedding dimension (e.g., 768)
     */
    int getEmbeddingDimension();

    /**
     * Checks if the embedding service is available and responsive.
     *
     * @return true if the service is healthy, false otherwise
     */
    boolean isHealthy();
}
