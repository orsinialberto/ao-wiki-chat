package com.example.ao_wiki_chat.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ao_wiki_chat.exception.VectorSearchException;
import com.example.ao_wiki_chat.model.entity.Chunk;
import com.example.ao_wiki_chat.model.entity.Document;
import com.example.ao_wiki_chat.model.enums.DocumentStatus;
import com.example.ao_wiki_chat.repository.ChunkRepository;

/**
 * Unit tests for VectorSearchService.
 * Tests vector similarity search functionality, threshold filtering, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
class VectorSearchServiceTest {
    
    @Mock
    private ChunkRepository chunkRepository;
    
    private VectorSearchService vectorSearchService;
    
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.7;
    private static final int DEFAULT_TOP_K = 5;
    private static final int EMBEDDING_DIMENSION = 768;
    
    private float[] testQueryEmbedding;
    private Document testDocument;
    private Chunk testChunk1;
    private Chunk testChunk2;
    
    @BeforeEach
    void setUp() {
        vectorSearchService = new VectorSearchService(
            chunkRepository, 
            DEFAULT_SIMILARITY_THRESHOLD, 
            DEFAULT_TOP_K
        );
        
        // Create test embedding vector
        testQueryEmbedding = createTestVector(EMBEDDING_DIMENSION, 0.5f);
        
        // Create test document
        UUID documentId = UUID.randomUUID();
        testDocument = Document.builder()
                .id(documentId)
                .filename("test.md")
                .contentType("text/markdown")
                .fileSize(1024L)
                .status(DocumentStatus.COMPLETED)
                .metadata(null)
                .build();
        
        // Create test chunks
        testChunk1 = Chunk.builder()
                .id(UUID.randomUUID())
                .document(testDocument)
                .content("First chunk content about machine learning")
                .chunkIndex(0)
                .embedding(createTestVector(EMBEDDING_DIMENSION, 0.3f))
                .metadata(null)
                .build();
        
        testChunk2 = Chunk.builder()
                .id(UUID.randomUUID())
                .document(testDocument)
                .content("Second chunk content about neural networks")
                .chunkIndex(1)
                .embedding(createTestVector(EMBEDDING_DIMENSION, 0.4f))
                .metadata(null)
                .build();
    }
    
    // ==================== Basic Functionality Tests ====================
    
    @Test
    void findSimilarChunksWithDefaultTopKReturnsResults() {
        // Given
        String expectedVectorString = convertEmbeddingToString(testQueryEmbedding);
        double expectedDistanceThreshold = 1.0 - DEFAULT_SIMILARITY_THRESHOLD; // 0.3
        List<Chunk> expectedResults = Arrays.asList(testChunk1, testChunk2);
        
        when(chunkRepository.findSimilarChunks(
            eq(expectedVectorString),
            eq(expectedDistanceThreshold),
            eq(DEFAULT_TOP_K)
        )).thenReturn(expectedResults);
        
        // When
        List<Chunk> results = vectorSearchService.findSimilarChunks(testQueryEmbedding);
        
        // Then
        assertThat(results).isEqualTo(expectedResults);
        assertThat(results).hasSize(2);
        verify(chunkRepository).findSimilarChunks(
            eq(expectedVectorString),
            eq(expectedDistanceThreshold),
            eq(DEFAULT_TOP_K)
        );
    }
    
    @Test
    void findSimilarChunksWithCustomTopKReturnsResults() {
        // Given
        int customTopK = 10;
        String expectedVectorString = convertEmbeddingToString(testQueryEmbedding);
        double expectedDistanceThreshold = 1.0 - DEFAULT_SIMILARITY_THRESHOLD;
        List<Chunk> expectedResults = Arrays.asList(testChunk1, testChunk2);
        
        when(chunkRepository.findSimilarChunks(
            eq(expectedVectorString),
            eq(expectedDistanceThreshold),
            eq(customTopK)
        )).thenReturn(expectedResults);
        
        // When
        List<Chunk> results = vectorSearchService.findSimilarChunks(testQueryEmbedding, customTopK);
        
        // Then
        assertThat(results).isEqualTo(expectedResults);
        verify(chunkRepository).findSimilarChunks(
            eq(expectedVectorString),
            eq(expectedDistanceThreshold),
            eq(customTopK)
        );
    }
    
    @Test
    void findSimilarChunksWithEmptyResultsReturnsEmptyList() {
        // Given
        String expectedVectorString = convertEmbeddingToString(testQueryEmbedding);
        double expectedDistanceThreshold = 1.0 - DEFAULT_SIMILARITY_THRESHOLD;
        
        when(chunkRepository.findSimilarChunks(
            anyString(),
            anyDouble(),
            anyInt()
        )).thenReturn(Collections.emptyList());
        
        // When
        List<Chunk> results = vectorSearchService.findSimilarChunks(testQueryEmbedding);
        
        // Then
        assertThat(results).isEmpty();
        verify(chunkRepository).findSimilarChunks(
            eq(expectedVectorString),
            eq(expectedDistanceThreshold),
            eq(DEFAULT_TOP_K)
        );
    }
    
    // ==================== Edge Cases and Validation Tests ====================
    
    @Test
    void findSimilarChunksWithNullEmbeddingThrowsException() {
        // When / Then
        assertThatThrownBy(() -> vectorSearchService.findSimilarChunks(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Query embedding cannot be null or empty");
    }
    
    @Test
    void findSimilarChunksWithEmptyEmbeddingThrowsException() {
        // Given
        float[] emptyEmbedding = new float[0];
        
        // When / Then
        assertThatThrownBy(() -> vectorSearchService.findSimilarChunks(emptyEmbedding))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Query embedding cannot be null or empty");
    }
    
    @Test
    void findSimilarChunksWithInvalidTopKThrowsException() {
        // Given
        int invalidTopK = 0;
        
        // When / Then
        assertThatThrownBy(() -> vectorSearchService.findSimilarChunks(testQueryEmbedding, invalidTopK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Top-k must be positive");
    }
    
    @Test
    void findSimilarChunksWithNegativeTopKThrowsException() {
        // Given
        int negativeTopK = -1;
        
        // When / Then
        assertThatThrownBy(() -> vectorSearchService.findSimilarChunks(testQueryEmbedding, negativeTopK))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Top-k must be positive");
    }
    
    // ==================== Threshold Conversion Tests ====================
    
    @Test
    void findSimilarChunksConvertsSimilarityThresholdToDistanceThreshold() {
        // Given
        double similarityThreshold = 0.8;
        VectorSearchService customService = new VectorSearchService(
            chunkRepository,
            similarityThreshold,
            DEFAULT_TOP_K
        );
        
        double expectedDistanceThreshold = 1.0 - similarityThreshold; // 0.2
        String expectedVectorString = convertEmbeddingToString(testQueryEmbedding);
        
        when(chunkRepository.findSimilarChunks(
            anyString(),
            anyDouble(),
            anyInt()
        )).thenReturn(Collections.emptyList());
        
        // When
        customService.findSimilarChunks(testQueryEmbedding);
        
        // Then
        verify(chunkRepository).findSimilarChunks(
            eq(expectedVectorString),
            eq(expectedDistanceThreshold),
            eq(DEFAULT_TOP_K)
        );
    }
    
    // ==================== Constructor Validation Tests ====================
    
    @Test
    void constructorWithInvalidSimilarityThresholdThrowsException() {
        // When / Then
        assertThatThrownBy(() -> new VectorSearchService(
            chunkRepository,
            1.5, // Invalid: > 1.0
            DEFAULT_TOP_K
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Similarity threshold must be between 0.0 and 1.0");
    }
    
    @Test
    void constructorWithNegativeSimilarityThresholdThrowsException() {
        // When / Then
        assertThatThrownBy(() -> new VectorSearchService(
            chunkRepository,
            -0.1, // Invalid: < 0.0
            DEFAULT_TOP_K
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Similarity threshold must be between 0.0 and 1.0");
    }
    
    @Test
    void constructorWithInvalidTopKThrowsException() {
        // When / Then
        assertThatThrownBy(() -> new VectorSearchService(
            chunkRepository,
            DEFAULT_SIMILARITY_THRESHOLD,
            0 // Invalid: <= 0
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Top-k must be positive");
    }
    
    // ==================== Embedding Conversion Tests ====================
    
    @Test
    void findSimilarChunksConvertsEmbeddingToCorrectStringFormat() {
        // Given
        float[] simpleEmbedding = new float[]{1.0f, 2.5f, 3.75f};
        String expectedVectorString = "[1.0,2.5,3.75]";
        double expectedDistanceThreshold = 1.0 - DEFAULT_SIMILARITY_THRESHOLD;
        
        when(chunkRepository.findSimilarChunks(
            anyString(),
            anyDouble(),
            anyInt()
        )).thenReturn(Collections.emptyList());
        
        // When
        vectorSearchService.findSimilarChunks(simpleEmbedding);
        
        // Then
        verify(chunkRepository).findSimilarChunks(
            eq(expectedVectorString),
            eq(expectedDistanceThreshold),
            eq(DEFAULT_TOP_K)
        );
    }
    
    @Test
    void findSimilarChunksWithSingleElementEmbedding() {
        // Given
        float[] singleElementEmbedding = new float[]{0.5f};
        String expectedVectorString = "[0.5]";
        double expectedDistanceThreshold = 1.0 - DEFAULT_SIMILARITY_THRESHOLD;
        
        when(chunkRepository.findSimilarChunks(
            anyString(),
            anyDouble(),
            anyInt()
        )).thenReturn(Collections.emptyList());
        
        // When
        vectorSearchService.findSimilarChunks(singleElementEmbedding);
        
        // Then
        verify(chunkRepository).findSimilarChunks(
            eq(expectedVectorString),
            eq(expectedDistanceThreshold),
            eq(DEFAULT_TOP_K)
        );
    }
    
    @Test
    void findSimilarChunksWhenRepositoryThrowsExceptionThrowsVectorSearchException() {
        // Given
        RuntimeException repositoryException = new RuntimeException("Database connection error");
        
        when(chunkRepository.findSimilarChunks(
            anyString(),
            anyDouble(),
            anyInt()
        )).thenThrow(repositoryException);
        
        // When / Then
        assertThatThrownBy(() -> vectorSearchService.findSimilarChunks(testQueryEmbedding))
                .isInstanceOf(VectorSearchException.class)
                .hasMessageContaining("Vector search failed")
                .hasCause(repositoryException);
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Creates a test embedding vector of specified dimension with a fill value.
     */
    private static float[] createTestVector(int dimension, float fillValue) {
        float[] vector = new float[dimension];
        Arrays.fill(vector, fillValue);
        return vector;
    }
    
    /**
     * Converts an embedding vector to pgvector string format.
     * Uses the same logic as VectorSearchService for consistency.
     */
    private static String convertEmbeddingToString(float[] embedding) {
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
}

