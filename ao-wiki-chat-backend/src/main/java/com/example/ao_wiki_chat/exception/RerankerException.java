package com.example.ao_wiki_chat.exception;

/**
 * Exception thrown when reranking of search results fails (e.g. API error, timeout).
 */
public class RerankerException extends RuntimeException {

    public RerankerException(String message) {
        super(message);
    }

    public RerankerException(String message, Throwable cause) {
        super(message, cause);
    }
}
