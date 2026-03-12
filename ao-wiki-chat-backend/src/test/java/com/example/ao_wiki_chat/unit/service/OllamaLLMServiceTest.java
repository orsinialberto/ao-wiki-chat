package com.example.ao_wiki_chat.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ao_wiki_chat.exception.LLMException;
import com.example.ao_wiki_chat.service.OllamaLLMService;

import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Unit tests for OllamaLLMService.
 */
@ExtendWith(MockitoExtension.class)
class OllamaLLMServiceTest {

    @Mock
    private ChatLanguageModel chatModel;

    private OllamaLLMService ollamaLLMService;

    @BeforeEach
    void setUp() {
        ollamaLLMService = new OllamaLLMService(chatModel);
    }

    @Test
    void generateWithPromptReturnsResponse() {
        String prompt = "Test prompt";
        String expectedResponse = "Test response";
        when(chatModel.generate(prompt)).thenReturn(expectedResponse);

        String response = ollamaLLMService.generate(prompt);

        assertThat(response).isEqualTo(expectedResponse);
        verify(chatModel).generate(prompt);
    }

    @Test
    void generateWithNullPromptThrowsException() {
        assertThatThrownBy(() -> ollamaLLMService.generate(null))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
    }

    @Test
    void generateWithEmptyPromptThrowsException() {
        assertThatThrownBy(() -> ollamaLLMService.generate("   "))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("Prompt cannot be null or empty");
    }

    @Test
    void generateWithNullResponseThrowsException() {
        String prompt = "Test prompt";
        when(chatModel.generate(prompt)).thenReturn(null);

        assertThatThrownBy(() -> ollamaLLMService.generate(prompt))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void generateWithEmptyResponseThrowsException() {
        String prompt = "Test prompt";
        when(chatModel.generate(prompt)).thenReturn("");

        assertThatThrownBy(() -> ollamaLLMService.generate(prompt))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void generateWhenApiFailsThrowsException() {
        String prompt = "Test prompt";
        when(chatModel.generate(prompt)).thenThrow(new RuntimeException("API error"));

        assertThatThrownBy(() -> ollamaLLMService.generate(prompt))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("Failed to generate text with Ollama");
    }

    @Test
    void isHealthyWhenApiRespondsReturnsTrue() {
        when(chatModel.generate(anyString())).thenReturn("pong");

        boolean healthy = ollamaLLMService.isHealthy();

        assertThat(healthy).isTrue();
    }

    @Test
    void isHealthyWhenApiFailsReturnsFalse() {
        when(chatModel.generate(anyString())).thenThrow(new RuntimeException("API error"));

        boolean healthy = ollamaLLMService.isHealthy();

        assertThat(healthy).isFalse();
    }

    @Test
    void isHealthyWhenApiReturnsNullReturnsFalse() {
        when(chatModel.generate(anyString())).thenReturn(null);

        boolean healthy = ollamaLLMService.isHealthy();

        assertThat(healthy).isFalse();
    }

    @Test
    void isHealthyWhenApiReturnsEmptyReturnsFalse() {
        when(chatModel.generate(anyString())).thenReturn("");

        boolean healthy = ollamaLLMService.isHealthy();

        assertThat(healthy).isFalse();
    }
}
