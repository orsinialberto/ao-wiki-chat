package com.example.ao_wiki_chat.service;

/**
 * Service interface for Large Language Model operations.
 * Abstracts the underlying LLM provider (Gemini, OpenAI, Claude, etc.)
 * to allow easy switching between different implementations.
 */
public interface LLMService {
    
    /**
     * Generates a text response based on the provided prompt.
     *
     * @param prompt the input prompt for text generation
     * @return the generated text response
     * @throws com.example.ao_wiki_chat.exception.LLMException if generation fails
     */
    String generate(String prompt);
    
    /**
     * Generates a text response with custom temperature setting.
     *
     * @param prompt the input prompt for text generation
     * @param temperature controls randomness (0.0 = deterministic, 1.0 = creative)
     * @return the generated text response
     * @throws com.example.ao_wiki_chat.exception.LLMException if generation fails
     */
    String generate(String prompt, double temperature);
    
    /**
     * Checks if the LLM service is available and responsive.
     *
     * @return true if the service is healthy, false otherwise
     */
    boolean isHealthy();
}

