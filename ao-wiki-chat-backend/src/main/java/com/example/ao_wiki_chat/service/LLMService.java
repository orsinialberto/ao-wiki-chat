package com.example.ao_wiki_chat.service;

/**
 * Service interface for Large Language Model operations.
 * Abstracts the underlying LLM provider (e.g. Ollama)
 * to allow easy switching between different implementations.
 */
public interface LLMService {

    /**
     * Generates a text response based on the provided prompt.
     * Uses the temperature configured in application.yml (e.g. ollama.chat.temperature).
     *
     * @param prompt the input prompt for text generation
     * @return the generated text response
     * @throws com.example.ao_wiki_chat.exception.LLMException if generation fails
     */
    String generate(String prompt);
    
    /**
     * Checks if the LLM service is available and responsive.
     *
     * @return true if the service is healthy, false otherwise
     */
    boolean isHealthy();
}

