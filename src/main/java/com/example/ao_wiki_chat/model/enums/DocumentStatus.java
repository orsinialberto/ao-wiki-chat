package com.example.ao_wiki_chat.model.enums;

/**
 * Status of document processing pipeline.
 * Transitions: PROCESSING → COMPLETED or FAILED
 */
public enum DocumentStatus {
    /**
     * Document is being processed (parsing, chunking, embedding generation)
     */
    PROCESSING,
    
    /**
     * Document processing completed successfully, chunks with embeddings saved
     */
    COMPLETED,
    
    /**
     * Document processing failed due to parsing error, API error, or other issue
     */
    FAILED
}

