package com.example.ao_wiki_chat.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ao_wiki_chat.exception.VectorSearchException;
import com.example.ao_wiki_chat.model.entity.Chunk;
import com.example.ao_wiki_chat.repository.ChunkRepository;

/**
 * Service for performing vector similarity search on document chunks.
 * Uses pgvector cosine distance operator for efficient semantic similarity search.
 * 
 * Configuration via application.yml:
 * - rag.search.similarity-threshold: minimum similarity score (0.0-1.0, default: 0.7)
 * - rag.search.top-k: maximum number of results to return (default: 5)
 * 
 * Note: pgvector uses cosine distance (1 - cosine similarity), so threshold is converted accordingly.
 */
@Service
public class VectorSearchService {
    
    private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);
    
    private final ChunkRepository chunkRepository;
    private final double similarityThreshold;
    private final int defaultTopK;
    
    /**
     * Constructs a VectorSearchService with the specified repository and configuration.
     *
     * @param chunkRepository the repository for chunk operations
     * @param similarityThreshold minimum similarity threshold (0.0-1.0, converted to distance internally)
     * @param defaultTopK default maximum number of results to return
     */
    public VectorSearchService(
            ChunkRepository chunkRepository,
            @Value("${rag.search.similarity-threshold:0.7}") double similarityThreshold,
            @Value("${rag.search.top-k:5}") int defaultTopK
    ) {
        this.chunkRepository = chunkRepository;
        this.similarityThreshold = similarityThreshold;
        this.defaultTopK = defaultTopK;
        
        if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
            throw new IllegalArgumentException(
                String.format("Similarity threshold must be between 0.0 and 1.0, got: %.2f", similarityThreshold)
            );
        }
        
        if (defaultTopK <= 0) {
            throw new IllegalArgumentException(
                String.format("Top-k must be positive, got: %d", defaultTopK)
            );
        }
        
        log.info("VectorSearchService initialized with similarity threshold: {}, default top-k: {}", 
                similarityThreshold, defaultTopK);
    }
    
    /**
     * Finds semantically similar chunks using vector similarity search.
     * Uses cosine distance to find chunks with embeddings similar to the query embedding.
     * Only returns chunks above the configured similarity threshold.
     * Results are ordered by similarity (most similar first).
     *
     * @param queryEmbedding the embedding vector for the search query
     * @return list of similar chunks ordered by similarity (most similar first), never null
     * @throws IllegalArgumentException if queryEmbedding is null or empty
     */
    @Transactional(readOnly = true)
    public List<Chunk> findSimilarChunks(float[] queryEmbedding) {
        return findSimilarChunks(queryEmbedding, defaultTopK);
    }
    
    /**
     * Finds semantically similar chunks using vector similarity search.
     * Uses cosine distance to find chunks with embeddings similar to the query embedding.
     * Only returns chunks above the configured similarity threshold.
     * Results are ordered by similarity (most similar first).
     *
     * @param queryEmbedding the embedding vector for the search query
     * @param topK maximum number of results to return
     * @return list of similar chunks ordered by similarity (most similar first), never null
     * @throws IllegalArgumentException if queryEmbedding is null or empty, or topK is invalid
     */
    @Transactional(readOnly = true)
    public List<Chunk> findSimilarChunks(float[] queryEmbedding, int topK) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw new IllegalArgumentException("Query embedding cannot be null or empty");
        }
        
        if (topK <= 0) {
            throw new IllegalArgumentException(
                String.format("Top-k must be positive, got: %d", topK)
            );
        }
        
        log.debug("Searching for similar chunks: embedding dimension={}, topK={}", 
                 queryEmbedding.length, topK);
        
        // Convert float[] to pgvector string format: "[1.0,2.0,3.0,...]"
        String queryVectorString = convertEmbeddingToString(queryEmbedding);
        
        // Convert similarity threshold to distance threshold
        // Cosine distance = 1 - cosine similarity
        double distanceThreshold = 1.0 - similarityThreshold;
        
        log.debug("Using distance threshold: {} (similarity: {})", distanceThreshold, similarityThreshold);
        
        try {
            List<Chunk> results = chunkRepository.findSimilarChunks(
                queryVectorString, 
                distanceThreshold, 
                topK
            );
            
            // Log similarity scores for debugging (convert distance back to similarity)
            if (log.isDebugEnabled() && !results.isEmpty()) {
                logSimilarityScores(results, distanceThreshold);
            }
            
            log.info("Found {} similar chunks (threshold: {:.2f}, topK: {})", 
                    results.size(), String.format("%.2f", similarityThreshold), topK);
            
            return results;
            
        } catch (Exception e) {
            log.error("Error during vector similarity search: {}", e.getMessage(), e);
            throw new VectorSearchException("Vector search failed", e);
        }
    }
    
    /**
     * Converts a float array embedding to pgvector string format.
     * Format: "[1.0,2.0,3.0,...]"
     * 
     * Uses the same conversion logic as VectorAttributeConverter.
     *
     * @param embedding the embedding vector
     * @return string representation in pgvector format
     */
    private String convertEmbeddingToString(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding cannot be null or empty");
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Logs similarity scores for debugging purposes.
     * Converts distance back to similarity: similarity = 1 - distance
     * Note: The actual distance values are not available from the query result,
     * so we log the number of results and the threshold used.
     *
     * @param chunks the found chunks
     * @param distanceThreshold the distance threshold used in the query
     */
    private void logSimilarityScores(List<Chunk> chunks, double distanceThreshold) {
        double similarityThreshold = 1.0 - distanceThreshold;
        log.debug("Retrieved {} chunks with similarity >= {}", 
                 chunks.size(), String.format("%.2f", similarityThreshold));
        
        // Log chunk details for first few results
        int maxLogEntries = Math.min(3, chunks.size());
        for (int i = 0; i < maxLogEntries; i++) {
            Chunk chunk = chunks.get(i);
            log.debug("  Chunk[{}]: documentId={}, chunkIndex={}, content length={}", 
                     i, chunk.getDocument().getId(), chunk.getChunkIndex(), chunk.getContent().length());
        }
    }
}

