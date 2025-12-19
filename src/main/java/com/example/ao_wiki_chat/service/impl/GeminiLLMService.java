package com.example.ao_wiki_chat.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.example.ao_wiki_chat.exception.LLMException;
import com.example.ao_wiki_chat.service.LLMService;

import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Gemini-specific implementation of LLMService.
 * Uses LangChain4j's ChatLanguageModel for Gemini API integration.
 * This implementation handles text generation requests and delegates to the Gemini API
 * through the configured ChatLanguageModel bean.
 */
@Service
public class GeminiLLMService implements LLMService {
    
    private static final Logger log = LoggerFactory.getLogger(GeminiLLMService.class);
    
    private final ChatLanguageModel chatModel;
    
    /**
     * Constructs a GeminiLLMService with the specified chat model.
     * Temperature is configured in the chat model bean (gemini.chat.temperature in application.yml).
     *
     * @param chatModel the configured Gemini chat model bean
     */
    public GeminiLLMService(@Qualifier("geminiChatModel") ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
        log.info("GeminiLLMService initialized");
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
                log.warn("Received empty response from Gemini");
                throw new LLMException("Received empty response from Gemini API");
            }
            
            log.debug("Response generated in {}ms, length: {} characters", duration, response.length());
            
            return response;
            
        } catch (LLMException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate response from Gemini: {}", e.getMessage(), e);
            throw new LLMException("Failed to generate text with Gemini: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            log.debug("Performing health check on Gemini chat model");
            String testResponse = chatModel.generate("ping");
            boolean healthy = testResponse != null && !testResponse.isEmpty();
            log.debug("Gemini health check result: {}", healthy);
            return healthy;
        } catch (Exception e) {
            log.warn("Gemini health check failed: {}", e.getMessage());
            return false;
        }
    }
}

