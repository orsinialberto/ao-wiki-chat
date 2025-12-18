package com.example.ao_wiki_chat.exception;

/**
 * Exception thrown when embedding generation operations fail.
 * This includes failures in vector embedding generation, API communication errors,
 * rate limiting issues, or configuration problems.
 */
public class EmbeddingException extends RuntimeException {
    
    /**
     * Constructs a new EmbeddingException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public EmbeddingException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new EmbeddingException with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of the exception
     */
    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}

