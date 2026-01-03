package com.example.ao_wiki_chat.unit.service;

import com.example.ao_wiki_chat.service.GeminiEmbeddingService;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ao_wiki_chat.exception.EmbeddingException;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

/**
 * Unit tests for GeminiEmbeddingService.
 */
@ExtendWith(MockitoExtension.class)
class GeminiEmbeddingServiceTest {
    
    @Mock
    private EmbeddingModel embeddingModel;
    
    @Mock
    private Response<Embedding> embeddingResponse;
    
    @Mock
    private Response<List<Embedding>> embeddingListResponse;
    
    @Mock
    private Embedding embedding;
    
    private GeminiEmbeddingService geminiEmbeddingService;
    
    private static final int EMBEDDING_DIMENSION = 768;
    private static final float[] TEST_VECTOR = createTestVector(EMBEDDING_DIMENSION);
    
    @BeforeEach
    void setUp() {
        geminiEmbeddingService = new GeminiEmbeddingService(embeddingModel, EMBEDDING_DIMENSION);
    }
    
    @Test
    void generateEmbeddingReturnsVector() {
        // Given
        String text = "Test text";
        when(embeddingModel.embed(text)).thenReturn(embeddingResponse);
        when(embeddingResponse.content()).thenReturn(embedding);
        when(embedding.vector()).thenReturn(TEST_VECTOR);
        
        // When
        float[] result = geminiEmbeddingService.generateEmbedding(text);
        
        // Then
        assertThat(result).isEqualTo(TEST_VECTOR);
        assertThat(result.length).isEqualTo(EMBEDDING_DIMENSION);
        verify(embeddingModel).embed(text);
    }
    
    @Test
    void generateEmbeddingWithNullTextThrowsException() {
        // When / Then
        assertThatThrownBy(() -> geminiEmbeddingService.generateEmbedding(null))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Text cannot be null or empty");
    }
    
    @Test
    void generateEmbeddingWithEmptyTextThrowsException() {
        // When / Then
        assertThatThrownBy(() -> geminiEmbeddingService.generateEmbedding("   "))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Text cannot be null or empty");
    }
    
    @Test
    void generateEmbeddingWhenApiReturnsNullEmbeddingThrowsException() {
        // Given
        String text = "Test text";
        when(embeddingModel.embed(text)).thenReturn(embeddingResponse);
        when(embeddingResponse.content()).thenReturn(null);
        
        // When / Then
        assertThatThrownBy(() -> geminiEmbeddingService.generateEmbedding(text))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("null embedding");
    }
    
    @Test
    void generateEmbeddingWhenApiFailsThrowsException() {
        // Given
        String text = "Test text";
        when(embeddingModel.embed(text)).thenThrow(new RuntimeException("API error"));
        
        // When / Then
        assertThatThrownBy(() -> geminiEmbeddingService.generateEmbedding(text))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Failed to generate embedding");
    }
    
    @Test
    void generateEmbeddingsReturnsVectorList() {
        // Given
        List<String> texts = Arrays.asList("Text 1", "Text 2", "Text 3");
        Embedding embedding1 = createMockEmbedding(TEST_VECTOR);
        Embedding embedding2 = createMockEmbedding(TEST_VECTOR);
        Embedding embedding3 = createMockEmbedding(TEST_VECTOR);
        List<Embedding> embeddings = Arrays.asList(embedding1, embedding2, embedding3);
        
        when(embeddingModel.embedAll(anyList())).thenReturn(embeddingListResponse);
        when(embeddingListResponse.content()).thenReturn(embeddings);
        
        // When
        List<float[]> results = geminiEmbeddingService.generateEmbeddings(texts);
        
        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0)).isEqualTo(TEST_VECTOR);
        verify(embeddingModel).embedAll(anyList());
    }
    
    @Test
    void generateEmbeddingsWithEmptyListReturnsEmptyList() {
        // Given
        List<String> texts = List.of();
        
        // When
        List<float[]> results = geminiEmbeddingService.generateEmbeddings(texts);
        
        // Then
        assertThat(results).isEmpty();
    }
    
    @Test
    void generateEmbeddingsWithNullTextInListThrowsException() {
        // Given
        List<String> texts = Arrays.asList("Text 1", null, "Text 3");
        
        // When / Then
        assertThatThrownBy(() -> geminiEmbeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Text at index 1 is null or empty");
    }
    
    @Test
    void generateEmbeddingsWithEmptyTextInListThrowsException() {
        // Given
        List<String> texts = Arrays.asList("Text 1", "   ", "Text 3");
        
        // When / Then
        assertThatThrownBy(() -> geminiEmbeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Text at index 1 is null or empty");
    }
    
    @Test
    void generateEmbeddingsWhenApiReturnsNullResponseThrowsException() {
        // Given
        List<String> texts = Arrays.asList("Text 1", "Text 2");
        when(embeddingModel.embedAll(anyList())).thenReturn(embeddingListResponse);
        when(embeddingListResponse.content()).thenReturn(null);
        
        // When / Then
        assertThatThrownBy(() -> geminiEmbeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("empty response");
    }
    
    @Test
    void generateEmbeddingsWhenApiReturnsWrongCountThrowsException() {
        // Given
        List<String> texts = Arrays.asList("Text 1", "Text 2", "Text 3");
        Embedding embedding1 = createMockEmbedding(TEST_VECTOR);
        Embedding embedding2 = createMockEmbedding(TEST_VECTOR);
        List<Embedding> embeddings = Arrays.asList(embedding1, embedding2); // Only 2 instead of 3
        
        when(embeddingModel.embedAll(anyList())).thenReturn(embeddingListResponse);
        when(embeddingListResponse.content()).thenReturn(embeddings);
        
        // When / Then
        assertThatThrownBy(() -> geminiEmbeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Expected 3 embeddings but got 2");
    }
    
    @Test
    void generateEmbeddingsWhenApiFailsThrowsException() {
        // Given
        List<String> texts = Arrays.asList("Text 1", "Text 2");
        when(embeddingModel.embedAll(anyList())).thenThrow(new RuntimeException("API error"));
        
        // When / Then
        assertThatThrownBy(() -> geminiEmbeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Failed to generate batch embeddings");
    }
    
    @Test
    void getEmbeddingDimensionReturnsConfiguredDimension() {
        // When
        int dimension = geminiEmbeddingService.getEmbeddingDimension();
        
        // Then
        assertThat(dimension).isEqualTo(EMBEDDING_DIMENSION);
    }
    
    @Test
    void isHealthyWhenApiRespondsReturnsTrue() {
        // Given
        when(embeddingModel.embed(anyString())).thenReturn(embeddingResponse);
        when(embeddingResponse.content()).thenReturn(embedding);
        when(embedding.vector()).thenReturn(TEST_VECTOR);
        
        // When
        boolean healthy = geminiEmbeddingService.isHealthy();
        
        // Then
        assertThat(healthy).isTrue();
    }
    
    @Test
    void isHealthyWhenApiFailsReturnsFalse() {
        // Given
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("API error"));
        
        // When
        boolean healthy = geminiEmbeddingService.isHealthy();
        
        // Then
        assertThat(healthy).isFalse();
    }
    
    @Test
    void isHealthyWhenApiReturnsWrongDimensionReturnsFalse() {
        // Given
        float[] wrongSizeVector = new float[512]; // Wrong dimension
        when(embeddingModel.embed(anyString())).thenReturn(embeddingResponse);
        when(embeddingResponse.content()).thenReturn(embedding);
        when(embedding.vector()).thenReturn(wrongSizeVector);
        
        // When
        boolean healthy = geminiEmbeddingService.isHealthy();
        
        // Then
        assertThat(healthy).isFalse();
    }
    
    // Helper methods
    
    private static float[] createTestVector(int dimension) {
        float[] vector = new float[dimension];
        for (int i = 0; i < dimension; i++) {
            vector[i] = (float) (Math.random() * 2 - 1); // Random values between -1 and 1
        }
        return vector;
    }
    
    private Embedding createMockEmbedding(float[] vector) {
        Embedding mockEmbedding = org.mockito.Mockito.mock(Embedding.class);
        when(mockEmbedding.vector()).thenReturn(vector);
        return mockEmbedding;
    }
}

