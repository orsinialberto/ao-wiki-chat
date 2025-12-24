package com.example.ao_wiki_chat.exception;

/**
 * Exception thrown when vector similarity search operations fail.
 * This includes failures in vector search queries, database communication errors,
 * or issues with embedding vector processing.
 */
public class VectorSearchException extends RuntimeException {
    
    /**
     * Constructs a new VectorSearchException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public VectorSearchException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new VectorSearchException with the specified detail message and cause.
     *
     * @param message the detail message explaining the cause of the exception
     * @param cause the underlying cause of the exception
     */
    public VectorSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}

