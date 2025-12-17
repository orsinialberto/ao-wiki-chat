package com.example.ao_wiki_chat.repository;

import com.example.ao_wiki_chat.model.entity.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Chunk entity operations.
 * Provides vector similarity search capabilities for RAG retrieval pipeline.
 */
@Repository
public interface ChunkRepository extends JpaRepository<Chunk, UUID> {

    /**
     * Find chunks by document ID ordered by chunk index.
     * Used to retrieve all chunks of a specific document in order.
     *
     * @param documentId the document UUID
     * @return list of chunks for the document, ordered by index
     */
    List<Chunk> findByDocument_IdOrderByChunkIndexAsc(UUID documentId);

    /**
     * Count chunks for a specific document.
     *
     * @param documentId the document UUID
     * @return number of chunks for the document
     */
    long countByDocument_Id(UUID documentId);

    /**
     * Find semantically similar chunks using vector cosine similarity.
     * Uses pgvector HNSW index for efficient similarity search.
     * 
     * Only returns chunks with similarity above threshold (distance < 0.3 = similarity > 0.7).
     * Distance is inverse of similarity for cosine: distance = 1 - similarity.
     * 
     * @param queryVector the embedding vector as string "[1.0,2.0,...]"
     * @param similarityThreshold maximum distance threshold (0.3 = 70% similarity)
     * @param topK maximum number of results to return
     * @return list of similar chunks ordered by similarity (closest first)
     */
    @Query(value = """
            SELECT c.*, (c.embedding <=> CAST(:queryVector AS vector)) AS distance
            FROM chunks c
            WHERE c.embedding IS NOT NULL
              AND (c.embedding <=> CAST(:queryVector AS vector)) < :similarityThreshold
            ORDER BY distance
            LIMIT :topK
            """, nativeQuery = true)
    List<Chunk> findSimilarChunks(
            @Param("queryVector") String queryVector,
            @Param("similarityThreshold") double similarityThreshold,
            @Param("topK") int topK
    );

    /**
     * Delete all chunks associated with a document.
     * Note: This is handled automatically by CASCADE DELETE,
     * but can be called explicitly if needed.
     *
     * @param documentId the document UUID
     */
    void deleteByDocument_Id(UUID documentId);
}

