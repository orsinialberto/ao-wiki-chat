package com.example.ao_wiki_chat.unit.integration.gemini;

import com.example.ao_wiki_chat.integration.gemini.GeminiConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GeminiConfig.
 */
class GeminiConfigTest {
    
    private GeminiConfig geminiConfig;
    
    @BeforeEach
    void setUp() {
        geminiConfig = new GeminiConfig();
        
        // Set required fields using reflection
        ReflectionTestUtils.setField(geminiConfig, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(geminiConfig, "chatModel", "gemini-2.0-flash-exp");
        ReflectionTestUtils.setField(geminiConfig, "temperature", 0.7);
        ReflectionTestUtils.setField(geminiConfig, "maxTokens", 2048);
        ReflectionTestUtils.setField(geminiConfig, "embeddingModel", "text-embedding-004");
    }
    
    @Test
    void geminiChatModelCreatesValidChatModel() {
        // When
        ChatLanguageModel chatModel = geminiConfig.geminiChatModel();
        
        // Then
        assertThat(chatModel).isNotNull();
    }
    
    @Test
    void geminiEmbeddingModelCreatesValidEmbeddingModel() {
        // When
        EmbeddingModel embeddingModel = geminiConfig.geminiEmbeddingModel();
        
        // Then
        assertThat(embeddingModel).isNotNull();
    }
}

