package com.example.ao_wiki_chat.exception;

/**
 * Exception thrown when Large Language Model operations fail.
 * This includes failures in text generation, API communication errors,
 * rate limiting issues, or configuration problems.
 */
public class LLMException extends RuntimeException {
    
    /**
     * Constructs a new LLMException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public LLMException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new LLMException with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of the exception
     */
    public LLMException(String message, Throwable cause) {
        super(message, cause);
    }
}

