package com.example.ao_wiki_chat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.example.ao_wiki_chat.exception.LLMException;

import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Ollama-specific implementation of LLMService.
 * Active only when {@code app.chat.provider=ollama}.
 */
@Service
@ConditionalOnProperty(name = "app.chat.provider", havingValue = "ollama")
public class OllamaLLMService implements LLMService {

    private static final Logger log = LoggerFactory.getLogger(OllamaLLMService.class);

    private final ChatLanguageModel chatModel;

    public OllamaLLMService(@Qualifier("ollamaChatModel") ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
        log.info("OllamaLLMService initialized");
    }

    @Override
    public String generate(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new LLMException("Prompt cannot be null or empty");
        }

        log.debug("Generating response for prompt of length: {}", prompt.length());

        try {
            long startTime = System.currentTimeMillis();
            String response = chatModel.generate(prompt);
            long duration = System.currentTimeMillis() - startTime;

            if (response == null || response.isEmpty()) {
                log.warn("Received empty response from Ollama");
                throw new LLMException("Received empty response from Ollama");
            }

            log.debug("Response generated in {}ms, length: {} characters", duration, response.length());

            return response;

        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate response from Ollama: {}", e.getMessage(), e);
            throw new LLMException("Failed to generate text with Ollama: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            log.debug("Performing health check on Ollama chat model");
            String testResponse = chatModel.generate("ping");
            boolean healthy = testResponse != null && !testResponse.isEmpty();
            log.debug("Ollama health check result: {}", healthy);
            return healthy;
        } catch (Exception e) {
            log.warn("Ollama health check failed: {}", e.getMessage());
            return false;
        }
    }
}
