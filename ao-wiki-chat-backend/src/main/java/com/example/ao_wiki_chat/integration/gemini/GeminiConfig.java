package com.example.ao_wiki_chat.integration.gemini;

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
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;

/**
 * Configuration class for Google Gemini API integration.
 * Creates LangChain4j beans for chat and embedding models.
 * These beans are used by the service layer implementations to interact with Gemini API.
 */
@Configuration
public class GeminiConfig {
    
    private static final Logger log = LoggerFactory.getLogger(GeminiConfig.class);
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.chat.model}")
    private String chatModel;
    
    @Value("${gemini.chat.temperature}")
    private double temperature;
    
    @Value("${gemini.chat.max-tokens}")
    private int maxTokens;
    
    @Value("${gemini.embedding.model}")
    private String embeddingModel;
    
    @Value("${gemini.embedding.dimension}")
    private int embeddingDimension;
    
    /**
     * Creates a ChatLanguageModel bean for Gemini chat completions.
     * Active only when {@code app.chat.provider=gemini}.
     *
     * @return configured GoogleAiGeminiChatModel instance
     */
    @Bean("geminiChatModel")
    @ConditionalOnProperty(name = "app.chat.provider", havingValue = "gemini")
    public ChatLanguageModel geminiChatModel() {
        log.info("Initializing Gemini Chat Model: {} with temperature: {}, max tokens: {}",
                chatModel, temperature, maxTokens);

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModel)
                .temperature(temperature)
                .maxOutputTokens(maxTokens)
                .timeout(Duration.ofSeconds(60))
                .logRequestsAndResponses(false)
                .build();
    }

    /**
     * Creates a StreamingChatLanguageModel bean for Gemini streaming chat completions.
     * Registered as "chatStreamingModel" so RAGService can inject the active provider.
     * Active only when {@code app.chat.provider=gemini}.
     *
     * @return configured GoogleAiGeminiStreamingChatModel instance
     */
    @Bean("chatStreamingModel")
    @ConditionalOnProperty(name = "app.chat.provider", havingValue = "gemini")
    public StreamingChatLanguageModel geminiStreamingChatModel() {
        log.info("Initializing Gemini Streaming Chat Model: {} with temperature: {}, max tokens: {}",
                chatModel, temperature, maxTokens);

        return GoogleAiGeminiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModel)
                .temperature(temperature)
                .maxOutputTokens(maxTokens)
                .timeout(Duration.ofSeconds(120))
                .logRequestsAndResponses(false)
                .build();
    }

    /**
     * Creates an EmbeddingModel bean for Gemini text embeddings.
     * This bean is used by EmbeddingService implementations to generate vector embeddings.
     * Uses outputDimensionality to request 768-dim vectors via Matryoshka scaling.
     *
     * @return configured GoogleAiEmbeddingModel instance
     */
    @Bean("geminiEmbeddingModel")
    @ConditionalOnProperty(name = "app.embedding.provider", havingValue = "gemini")
    public EmbeddingModel geminiEmbeddingModel() {
        log.info("Initializing Gemini Embedding Model: {} with dimension: {}",
                embeddingModel, embeddingDimension);
        
        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModel)
                .outputDimensionality(embeddingDimension)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(1)
                .logRequestsAndResponses(false)
                .build();
    }
}

