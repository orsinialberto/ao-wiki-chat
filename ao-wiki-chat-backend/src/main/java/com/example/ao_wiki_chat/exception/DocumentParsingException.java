package com.example.ao_wiki_chat.exception;

/**
 * Exception thrown when document parsing fails.
 * Domain-specific exception for all parsing-related errors.
 */
public class DocumentParsingException extends RuntimeException {

    private final String contentType;

    public DocumentParsingException(String message, String contentType) {
        super(message);
        this.contentType = contentType;
    }

    public DocumentParsingException(String message, String contentType, Throwable cause) {
        super(message, cause);
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}


