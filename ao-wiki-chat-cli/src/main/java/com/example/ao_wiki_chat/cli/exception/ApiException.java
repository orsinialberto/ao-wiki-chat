package com.example.ao_wiki_chat.cli.exception;

/**
 * Base exception for API-related errors in the CLI.
 * All API exceptions extend this class.
 */
public class ApiException extends RuntimeException {

    private final int statusCode;

    /**
     * Constructs an ApiException with a message and status code.
     *
     * @param message the error message
     * @param statusCode the HTTP status code
     */
    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs an ApiException with a message, status code, and cause.
     *
     * @param message the error message
     * @param statusCode the HTTP status code
     * @param cause the cause of the exception
     */
    public ApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Returns the HTTP status code associated with this exception.
     *
     * @return the status code
     */
    public int getStatusCode() {
        return statusCode;
    }
}
