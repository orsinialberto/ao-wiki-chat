package com.example.ao_wiki_chat.integration.gemini;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;

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
    
    /**
     * Creates a ChatLanguageModel bean for Gemini chat completions.
     * This bean is used by LLMService implementations to generate text responses.
     *
     * @return configured GoogleAiGeminiChatModel instance
     */
    @Bean("geminiChatModel")
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
     * Creates an EmbeddingModel bean for Gemini text embeddings.
     * This bean is used by EmbeddingService implementations to generate vector embeddings.
     *
     * @return configured GoogleAiEmbeddingModel instance
     */
    @Bean("geminiEmbeddingModel")
    public EmbeddingModel geminiEmbeddingModel() {
        log.info("Initializing Gemini Embedding Model: {}", embeddingModel);
        
        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModel)
                .timeout(Duration.ofSeconds(30))
                .logRequestsAndResponses(false)
                .build();
    }
}

