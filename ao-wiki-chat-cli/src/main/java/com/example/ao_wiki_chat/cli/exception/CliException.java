package com.example.ao_wiki_chat.cli.exception;

/**
 * Base exception for CLI-specific errors.
 * Used for general CLI errors that are not API or configuration related.
 */
public class CliException extends RuntimeException {

    /**
     * Constructs a CliException with a message.
     *
     * @param message the error message
     */
    public CliException(String message) {
        super(message);
    }

    /**
     * Constructs a CliException with a message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public CliException(String message, Throwable cause) {
        super(message, cause);
    }
}
