package com.example.ao_wiki_chat.integration.ollama;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

/**
 * Configuration for Ollama embedding and chat models.
 * Embedding bean is active when {@code app.embedding.provider=ollama}.
 * Chat beans are active when {@code app.chat.provider=ollama}.
 */
@Configuration
public class OllamaConfig {

    private static final Logger log = LoggerFactory.getLogger(OllamaConfig.class);

    @Value("${ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${ollama.embedding.model:nomic-embed-text}")
    private String embeddingModelName;

    @Value("${ollama.chat.model:llama3.2:1b}")
    private String chatModelName;

    @Value("${ollama.chat.temperature:0.5}")
    private double chatTemperature;

    @Value("${ollama.chat.num-predict:4096}")
    private int chatNumPredict;

    @Value("${ollama.chat.timeout-seconds:120}")
    private int chatTimeoutSeconds;

    @Bean("ollamaEmbeddingModel")
    @ConditionalOnProperty(name = "app.embedding.provider", havingValue = "ollama")
    public EmbeddingModel ollamaEmbeddingModel() {
        log.info("Initializing Ollama Embedding Model: {} at {}", embeddingModelName, baseUrl);
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(embeddingModelName)
                .build();
    }

    /**
     * Ollama chat model for synchronous LLM responses.
     * Active only when {@code app.chat.provider=ollama}.
     */
    @Bean("ollamaChatModel")
    @ConditionalOnProperty(name = "app.chat.provider", havingValue = "ollama")
    public ChatLanguageModel ollamaChatModel() {
        log.info("Initializing Ollama Chat Model: {} at {}, temperature: {}, numPredict: {}",
                chatModelName, baseUrl, chatTemperature, chatNumPredict);
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .temperature(chatTemperature)
                .numPredict(chatNumPredict)
                .timeout(Duration.ofSeconds(chatTimeoutSeconds))
                .build();
    }

    /**
     * Ollama streaming chat model for SSE streaming.
     * Registered as "chatStreamingModel" so RAGService can inject the active provider.
     * Active only when {@code app.chat.provider=ollama}.
     */
    @Bean("chatStreamingModel")
    @ConditionalOnProperty(name = "app.chat.provider", havingValue = "ollama")
    public StreamingChatLanguageModel ollamaStreamingChatModel() {
        log.info("Initializing Ollama Streaming Chat Model: {} at {}, temperature: {}, numPredict: {}",
                chatModelName, baseUrl, chatTemperature, chatNumPredict);
        return OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(chatModelName)
                .temperature(chatTemperature)
                .numPredict(chatNumPredict)
                .timeout(Duration.ofSeconds(chatTimeoutSeconds))
                .build();
    }
}
