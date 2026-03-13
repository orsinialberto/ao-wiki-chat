package com.example.ao_wiki_chat.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ao_wiki_chat.exception.EmbeddingException;
import com.example.ao_wiki_chat.service.OllamaEmbeddingService;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

/**
 * Unit tests for OllamaEmbeddingService.
 */
@ExtendWith(MockitoExtension.class)
class OllamaEmbeddingServiceTest {

    private static final int EMBEDDING_DIMENSION = 768;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private Response<Embedding> embeddingResponse;

    @Mock
    private Response<List<Embedding>> embeddingListResponse;

    @Mock
    private Embedding embedding;

    private OllamaEmbeddingService ollamaEmbeddingService;

    @BeforeEach
    void setUp() {
        ollamaEmbeddingService = new OllamaEmbeddingService(embeddingModel, EMBEDDING_DIMENSION);
    }

    @Test
    void generateEmbeddingReturnsVectorWithCorrectDimension() {
        float[] modelVector = new float[EMBEDDING_DIMENSION];
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            modelVector[i] = (float) i * 0.1f;
        }
        when(embeddingModel.embed("Test text")).thenReturn(embeddingResponse);
        when(embeddingResponse.content()).thenReturn(embedding);
        when(embedding.vector()).thenReturn(modelVector);

        float[] result = ollamaEmbeddingService.generateEmbedding("Test text");

        assertThat(result).hasSize(EMBEDDING_DIMENSION).isEqualTo(modelVector);
        verify(embeddingModel).embed("Test text");
    }

    @Test
    void generateEmbeddingWhenModelReturnsFewerDimensionsPadsToConfiguredDimension() {
        float[] modelVector768 = new float[768];
        for (int i = 0; i < 768; i++) {
            modelVector768[i] = (float) i * 0.1f;
        }
        when(embeddingModel.embed("test")).thenReturn(embeddingResponse);
        when(embeddingResponse.content()).thenReturn(embedding);
        when(embedding.vector()).thenReturn(modelVector768);

        float[] result = ollamaEmbeddingService.generateEmbedding("test");

        assertThat(result).hasSize(EMBEDDING_DIMENSION);
        for (int i = 0; i < 768; i++) {
            assertThat(result[i]).isEqualTo(modelVector768[i]);
        }
        for (int i = 768; i < EMBEDDING_DIMENSION; i++) {
            assertThat(result[i]).isEqualTo(0.0f);
        }
    }

    @Test
    void generateEmbeddingWhenModelReturnsMoreDimensionsTruncatesToConfiguredDimension() {
        float[] modelVector1500 = new float[1500];
        for (int i = 0; i < 1500; i++) {
            modelVector1500[i] = (float) i;
        }
        when(embeddingModel.embed("test")).thenReturn(embeddingResponse);
        when(embeddingResponse.content()).thenReturn(embedding);
        when(embedding.vector()).thenReturn(modelVector1500);

        float[] result = ollamaEmbeddingService.generateEmbedding("test");

        assertThat(result).hasSize(EMBEDDING_DIMENSION);
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            assertThat(result[i]).isEqualTo((float) i);
        }
    }

    @Test
    void generateEmbeddingWithNullTextThrowsException() {
        assertThatThrownBy(() -> ollamaEmbeddingService.generateEmbedding(null))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Text cannot be null or empty");
    }

    @Test
    void generateEmbeddingWithEmptyTextThrowsException() {
        assertThatThrownBy(() -> ollamaEmbeddingService.generateEmbedding("   "))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Text cannot be null or empty");
    }

    @Test
    void generateEmbeddingWhenApiReturnsNullEmbeddingThrowsException() {
        when(embeddingModel.embed("test")).thenReturn(embeddingResponse);
        when(embeddingResponse.content()).thenReturn(null);

        assertThatThrownBy(() -> ollamaEmbeddingService.generateEmbedding("test"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("null embedding");
    }

    @Test
    void generateEmbeddingWhenApiFailsThrowsException() {
        when(embeddingModel.embed("test")).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> ollamaEmbeddingService.generateEmbedding("test"))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Failed to generate embedding");
    }

    @Test
    void generateEmbeddingsReturnsVectorListWithCorrectDimensions() {
        List<String> texts = Arrays.asList("A", "B", "C");
        float[] vec = new float[EMBEDDING_DIMENSION];
        Embedding e1 = mockEmbedding(vec);
        Embedding e2 = mockEmbedding(vec);
        Embedding e3 = mockEmbedding(vec);
        when(embeddingModel.embedAll(anyList())).thenReturn(embeddingListResponse);
        when(embeddingListResponse.content()).thenReturn(Arrays.asList(e1, e2, e3));

        List<float[]> results = ollamaEmbeddingService.generateEmbeddings(texts);

        assertThat(results).hasSize(3);
        assertThat(results.get(0)).hasSize(EMBEDDING_DIMENSION);
        verify(embeddingModel).embedAll(anyList());
    }

    @Test
    void generateEmbeddingsWhenModelReturns768ReturnsAsIs() {
        List<String> texts = Arrays.asList("A", "B");
        float[] vec768 = new float[768];
        vec768[0] = 1.0f;
        Embedding e1 = mockEmbedding(vec768);
        Embedding e2 = mockEmbedding(vec768);
        when(embeddingModel.embedAll(anyList())).thenReturn(embeddingListResponse);
        when(embeddingListResponse.content()).thenReturn(Arrays.asList(e1, e2));

        List<float[]> results = ollamaEmbeddingService.generateEmbeddings(texts);

        assertThat(results).hasSize(2);
        assertThat(results.get(0)).hasSize(EMBEDDING_DIMENSION);
        assertThat(results.get(0)[0]).isEqualTo(1.0f);
        assertThat(results.get(0)[767]).isEqualTo(0.0f);
    }

    @Test
    void generateEmbeddingsWithEmptyListReturnsEmptyList() {
        List<float[]> results = ollamaEmbeddingService.generateEmbeddings(List.of());
        assertThat(results).isEmpty();
    }

    @Test
    void generateEmbeddingsWithNullListReturnsEmptyList() {
        List<float[]> results = ollamaEmbeddingService.generateEmbeddings(null);
        assertThat(results).isEmpty();
    }

    @Test
    void generateEmbeddingsWithNullTextInListThrowsException() {
        List<String> texts = Arrays.asList("A", null, "C");
        assertThatThrownBy(() -> ollamaEmbeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Text at index 1 is null or empty");
    }

    @Test
    void generateEmbeddingsWithEmptyTextInListThrowsException() {
        List<String> texts = Arrays.asList("A", "   ", "C");
        assertThatThrownBy(() -> ollamaEmbeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Text at index 1 is null or empty");
    }

    @Test
    void generateEmbeddingsWhenApiReturnsNullResponseThrowsException() {
        List<String> texts = Arrays.asList("A", "B");
        when(embeddingModel.embedAll(anyList())).thenReturn(embeddingListResponse);
        when(embeddingListResponse.content()).thenReturn(null);

        assertThatThrownBy(() -> ollamaEmbeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void generateEmbeddingsWhenApiReturnsWrongCountThrowsException() {
        List<String> texts = Arrays.asList("A", "B", "C");
        float[] vec = new float[EMBEDDING_DIMENSION];
        Embedding e1 = mockEmbedding(vec);
        Embedding e2 = mockEmbedding(vec);
        when(embeddingModel.embedAll(anyList())).thenReturn(embeddingListResponse);
        when(embeddingListResponse.content()).thenReturn(Arrays.asList(e1, e2));

        assertThatThrownBy(() -> ollamaEmbeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Expected 3 embeddings but got 2");
    }

    @Test
    void generateEmbeddingsWhenApiFailsThrowsException() {
        List<String> texts = Arrays.asList("A", "B");
        when(embeddingModel.embedAll(anyList())).thenThrow(new RuntimeException("Timeout"));

        assertThatThrownBy(() -> ollamaEmbeddingService.generateEmbeddings(texts))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Failed to generate batch embeddings");
    }

    @Test
    void getEmbeddingDimensionReturnsConfiguredDimension() {
        assertThat(ollamaEmbeddingService.getEmbeddingDimension()).isEqualTo(EMBEDDING_DIMENSION);
    }

    @Test
    void isHealthyWhenApiRespondsReturnsTrue() {
        float[] vec = new float[EMBEDDING_DIMENSION];
        when(embeddingModel.embed(anyString())).thenReturn(embeddingResponse);
        when(embeddingResponse.content()).thenReturn(embedding);
        when(embedding.vector()).thenReturn(vec);

        assertThat(ollamaEmbeddingService.isHealthy()).isTrue();
    }

    @Test
    void isHealthyWhenApiFailsReturnsFalse() {
        when(embeddingModel.embed(anyString())).thenThrow(new RuntimeException("Connection refused"));

        assertThat(ollamaEmbeddingService.isHealthy()).isFalse();
    }

    private static Embedding mockEmbedding(float[] vector) {
        Embedding e = org.mockito.Mockito.mock(Embedding.class);
        when(e.vector()).thenReturn(vector);
        return e;
    }
}
