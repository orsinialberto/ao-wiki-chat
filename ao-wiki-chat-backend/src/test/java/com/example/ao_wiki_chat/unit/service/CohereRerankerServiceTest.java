package com.example.ao_wiki_chat.unit.service;

import com.example.ao_wiki_chat.exception.RerankerException;
import com.example.ao_wiki_chat.model.entity.Chunk;
import com.example.ao_wiki_chat.model.entity.Document;
import com.example.ao_wiki_chat.model.enums.DocumentStatus;
import com.example.ao_wiki_chat.service.CohereRerankerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CohereRerankerService.
 */
class CohereRerankerServiceTest {

    private RestTemplate restTemplate;
    private ObjectMapper objectMapper;
    private CohereRerankerService service;
    private Document doc;
    private Chunk chunk1;
    private Chunk chunk2;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        objectMapper = new ObjectMapper();
        service = new CohereRerankerService(restTemplate, objectMapper, "test-api-key", "rerank-v3.5");
        doc = Document.builder()
                .id(UUID.randomUUID())
                .filename("test.md")
                .contentType("text/markdown")
                .fileSize(100L)
                .status(DocumentStatus.COMPLETED)
                .metadata(null)
                .build();
        chunk1 = Chunk.builder().id(UUID.randomUUID()).document(doc).content("First content").chunkIndex(0).embedding(new float[1]).metadata(null).build();
        chunk2 = Chunk.builder().id(UUID.randomUUID()).document(doc).content("Second content").chunkIndex(1).embedding(new float[1]).metadata(null).build();
    }

    @Test
    void rerankParsesResponseAndReturnsChunksInRerankedOrder() {
        String responseBody = "{\"results\":[{\"index\":1,\"relevance_score\":0.95},{\"index\":0,\"relevance_score\":0.8}]}";
        when(restTemplate.postForObject(eq("https://api.cohere.com/v2/rerank"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseBody);

        List<Chunk> input = Arrays.asList(chunk1, chunk2);
        List<Chunk> result = service.rerank("query", input, 2);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isSameAs(chunk2);
        assertThat(result.get(1)).isSameAs(chunk1);
    }

    @Test
    void rerankWithEmptyListReturnsEmpty() {
        List<Chunk> result = service.rerank("query", Collections.emptyList(), 5);
        assertThat(result).isEmpty();
    }

    @Test
    void isActiveReturnsTrue() {
        assertThat(service.isActive()).isTrue();
    }

    @Test
    void rerankWithNullQueryThrows() {
        assertThatThrownBy(() -> service.rerank(null, Arrays.asList(chunk1), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Query cannot be null");
    }

    @Test
    void rerankWithNullChunksThrows() {
        assertThatThrownBy(() -> service.rerank("q", null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chunks cannot be null");
    }

    @Test
    void rerankWithInvalidTopKThrows() {
        assertThatThrownBy(() -> service.rerank("q", Arrays.asList(chunk1), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Top-k must be positive");
    }

    @Test
    void rerankWhenRestTemplateThrowsWrapsInRerankerException() {
        when(restTemplate.postForObject(eq("https://api.cohere.com/v2/rerank"), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new RestClientException("Network error"));

        List<Chunk> input = Arrays.asList(chunk1);
        assertThatThrownBy(() -> service.rerank("query", input, 1))
                .isInstanceOf(RerankerException.class)
                .hasMessageContaining("Rerank request failed")
                .hasCauseInstanceOf(RestClientException.class);
    }

    @Test
    void rerankWhenResponseMissingResultsThrows() {
        when(restTemplate.postForObject(eq("https://api.cohere.com/v2/rerank"), any(HttpEntity.class), eq(String.class)))
                .thenReturn("{}");

        List<Chunk> input = Arrays.asList(chunk1);
        assertThatThrownBy(() -> service.rerank("query", input, 1))
                .isInstanceOf(RerankerException.class)
                .hasMessageContaining("missing 'results'");
    }

    @Test
    void rerankWhenResponseEmptyThrows() {
        when(restTemplate.postForObject(eq("https://api.cohere.com/v2/rerank"), any(HttpEntity.class), eq(String.class)))
                .thenReturn("");

        List<Chunk> input = Arrays.asList(chunk1);
        assertThatThrownBy(() -> service.rerank("query", input, 1))
                .isInstanceOf(RerankerException.class)
                .hasMessageContaining("empty response");
    }
}
