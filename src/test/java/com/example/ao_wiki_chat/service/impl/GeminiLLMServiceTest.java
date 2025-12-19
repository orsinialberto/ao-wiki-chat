package com.example.ao_wiki_chat.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ao_wiki_chat.exception.LLMException;

import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Unit tests for GeminiLLMService.
 */
@ExtendWith(MockitoExtension.class)
class GeminiLLMServiceTest {
    
    @Mock
    private ChatLanguageModel chatModel;
    
    private GeminiLLMService geminiLLMService;
    
    @BeforeEach
    void setUp() {
        geminiLLMService = new GeminiLLMService(chatModel);
    }
    
    @Test
    void generateWithPromptReturnsResponse() {
        // Given
        String prompt = "Test prompt";
        String expectedResponse = "Test response";
        when(chatModel.generate(prompt)).thenReturn(expectedResponse);
        
        // When
        String response = geminiLLMService.generate(prompt);
        
        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(chatModel).generate(prompt);
    }
    
    @Test
    void generateWithNullPromptThrowsException() {
        // When / Then
        assertThatThrownBy(() -> geminiLLMService.generate(null))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
    }
    
    @Test
    void generateWithEmptyPromptThrowsException() {
        // When / Then
        assertThatThrownBy(() -> geminiLLMService.generate("   "))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
    }
    
    @Test
    void generateWithNullResponseThrowsException() {
        // Given
        String prompt = "Test prompt";
        when(chatModel.generate(prompt)).thenReturn(null);
        
        // When / Then
        assertThatThrownBy(() -> geminiLLMService.generate(prompt))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("empty response");
    }
    
    @Test
    void generateWithEmptyResponseThrowsException() {
        // Given
        String prompt = "Test prompt";
        when(chatModel.generate(prompt)).thenReturn("");
        
        // When / Then
        assertThatThrownBy(() -> geminiLLMService.generate(prompt))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("empty response");
    }
    
    @Test
    void generateWhenApiFailsThrowsException() {
        // Given
        String prompt = "Test prompt";
        when(chatModel.generate(prompt)).thenThrow(new RuntimeException("API error"));
        
        // When / Then
        assertThatThrownBy(() -> geminiLLMService.generate(prompt))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("Failed to generate text with Gemini");
    }
    
    @Test
    void isHealthyWhenApiRespondsReturnsTrue() {
        // Given
        when(chatModel.generate(anyString())).thenReturn("pong");
        
        // When
        boolean healthy = geminiLLMService.isHealthy();
        
        // Then
        assertThat(healthy).isTrue();
    }
    
    @Test
    void isHealthyWhenApiFailsReturnsFalse() {
        // Given
        when(chatModel.generate(anyString())).thenThrow(new RuntimeException("API error"));
        
        // When
        boolean healthy = geminiLLMService.isHealthy();
        
        // Then
        assertThat(healthy).isFalse();
    }
    
    @Test
    void isHealthyWhenApiReturnsNullReturnsFalse() {
        // Given
        when(chatModel.generate(anyString())).thenReturn(null);
        
        // When
        boolean healthy = geminiLLMService.isHealthy();
        
        // Then
        assertThat(healthy).isFalse();
    }
    
    @Test
    void isHealthyWhenApiReturnsEmptyReturnsFalse() {
        // Given
        when(chatModel.generate(anyString())).thenReturn("");
        
        // When
        boolean healthy = geminiLLMService.isHealthy();
        
        // Then
        assertThat(healthy).isFalse();
    }
}

