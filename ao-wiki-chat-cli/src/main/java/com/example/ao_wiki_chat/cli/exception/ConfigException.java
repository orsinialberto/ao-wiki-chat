package com.example.ao_wiki_chat.cli.exception;

/**
 * Exception for configuration-related errors.
 * Used when configuration loading, validation, or saving fails.
 */
public class ConfigException extends CliException {

    /**
     * Constructs a ConfigException with a message.
     *
     * @param message the error message
     */
    public ConfigException(String message) {
        super(message);
    }

    /**
     * Constructs a ConfigException with a message and cause.
     *
     * @param message the error message
     * @param cause the cause of the exception
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
