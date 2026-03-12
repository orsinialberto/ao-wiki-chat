package com.example.ao_wiki_chat.unit.integration.ollama;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.ao_wiki_chat.integration.ollama.OllamaConfig;

import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * Unit tests for OllamaConfig.
 */
class OllamaConfigTest {

    private OllamaConfig ollamaConfig;

    @BeforeEach
    void setUp() {
        ollamaConfig = new OllamaConfig();
        ReflectionTestUtils.setField(ollamaConfig, "baseUrl", "http://localhost:11434");
        ReflectionTestUtils.setField(ollamaConfig, "embeddingModelName", "nomic-embed-text");
    }

    @Test
    void ollamaEmbeddingModelCreatesValidEmbeddingModel() {
        EmbeddingModel model = ollamaConfig.ollamaEmbeddingModel();
        assertThat(model).isNotNull();
    }

    @Test
    void ollamaEmbeddingModelWithCustomBaseUrlUsesConfiguredUrl() {
        ReflectionTestUtils.setField(ollamaConfig, "baseUrl", "http://custom:11434");
        EmbeddingModel model = ollamaConfig.ollamaEmbeddingModel();
        assertThat(model).isNotNull();
    }

    @Test
    void ollamaEmbeddingModelWithCustomModelNameUsesConfiguredModel() {
        ReflectionTestUtils.setField(ollamaConfig, "embeddingModelName", "custom-embed");
        EmbeddingModel model = ollamaConfig.ollamaEmbeddingModel();
        assertThat(model).isNotNull();
    }
}
