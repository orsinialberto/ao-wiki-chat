package com.example.ao_wiki_chat.integration.ollama;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;

/**
 * Configuration for Ollama embedding model.
 * Active only when {@code app.embedding.provider=ollama}.
 */
@Configuration
public class OllamaConfig {

    private static final Logger log = LoggerFactory.getLogger(OllamaConfig.class);

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.embedding.model:nomic-embed-text}")
    private String embeddingModelName;

    @Bean("ollamaEmbeddingModel")
    @ConditionalOnProperty(name = "app.embedding.provider", havingValue = "ollama")
    public EmbeddingModel ollamaEmbeddingModel() {
        log.info("Initializing Ollama Embedding Model: {} at {}", embeddingModelName, baseUrl);
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .build();
    }
}
