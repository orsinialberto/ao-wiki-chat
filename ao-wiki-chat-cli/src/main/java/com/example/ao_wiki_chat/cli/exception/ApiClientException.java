package com.example.ao_wiki_chat.cli.exception;

/**
 * Exception for API client errors (network issues, timeouts, etc.).
 * Used when the request cannot be completed due to client-side issues.
 */
public class ApiClientException extends ApiException {

    /**
     * Constructs an ApiClientException with a message.
     *
     * @param message the error message
     */
    public ApiClientException(String message) {
        super(message, 0);
    }

    /**
     * Constructs an ApiClientException with a message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public ApiClientException(String message, Throwable cause) {
        super(message, 0, cause);
    }
}
