package com.example.ao_wiki_chat.unit.service;

import com.example.ao_wiki_chat.model.entity.Chunk;
import com.example.ao_wiki_chat.model.entity.Document;
import com.example.ao_wiki_chat.model.enums.DocumentStatus;
import com.example.ao_wiki_chat.service.NoOpRerankerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for NoOpRerankerService.
 */
class NoOpRerankerServiceTest {

    private NoOpRerankerService service;
    private Document doc;
    private Chunk chunk1;
    private Chunk chunk2;
    private Chunk chunk3;

    @BeforeEach
    void setUp() {
        service = new NoOpRerankerService();
        doc = Document.builder()
                .id(UUID.randomUUID())
                .filename("test.md")
                .contentType("text/markdown")
                .fileSize(100L)
                .status(DocumentStatus.COMPLETED)
                .metadata(null)
                .build();
        chunk1 = Chunk.builder().id(UUID.randomUUID()).document(doc).content("One").chunkIndex(0).embedding(new float[1]).metadata(null).build();
        chunk2 = Chunk.builder().id(UUID.randomUUID()).document(doc).content("Two").chunkIndex(1).embedding(new float[1]).metadata(null).build();
        chunk3 = Chunk.builder().id(UUID.randomUUID()).document(doc).content("Three").chunkIndex(2).embedding(new float[1]).metadata(null).build();
    }

    @Test
    void rerankReturnsSameOrderWhenChunksFewerThanTopK() {
        List<Chunk> input = Arrays.asList(chunk1, chunk2);
        List<Chunk> result = service.rerank("query", input, 5);
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(chunk1, chunk2);
    }

    @Test
    void rerankTruncatesToTopK() {
        List<Chunk> input = Arrays.asList(chunk1, chunk2, chunk3);
        List<Chunk> result = service.rerank("query", input, 2);
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(chunk1, chunk2);
    }

    @Test
    void rerankWithEmptyListReturnsEmpty() {
        List<Chunk> result = service.rerank("query", Collections.emptyList(), 3);
        assertThat(result).isEmpty();
    }

    @Test
    void rerankWithExactTopKReturnsAll() {
        List<Chunk> input = Arrays.asList(chunk1, chunk2, chunk3);
        List<Chunk> result = service.rerank("query", input, 3);
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(chunk1, chunk2, chunk3);
    }

    @Test
    void isActiveReturnsFalse() {
        assertThat(service.isActive()).isFalse();
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
        assertThatThrownBy(() -> service.rerank("q", Arrays.asList(chunk1), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Top-k must be positive");
    }
}
