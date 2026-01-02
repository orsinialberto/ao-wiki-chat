package com.example.ao_wiki_chat.cli.util;

import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.exception.ConfigException;
import com.example.ao_wiki_chat.cli.exception.CliException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorMessageFormatterTest {

    @Test
    void formatErrorWhenApiExceptionWith404ReturnsUserFriendlyMessage() {
        ApiException exception = new ApiException("Document not found", 404);
        String message = ErrorMessageFormatter.formatError(exception);
        assertThat(message).contains("Resource not found");
    }

    @Test
    void formatErrorWhenApiExceptionWith400ReturnsUserFriendlyMessage() {
        ApiException exception = new ApiException("Invalid input", 400);
        String message = ErrorMessageFormatter.formatError(exception);
        assertThat(message).contains("Invalid request");
    }

    @Test
    void formatErrorWhenApiExceptionWith500ReturnsUserFriendlyMessage() {
        ApiException exception = new ApiException("Internal server error", 500);
        String message = ErrorMessageFormatter.formatError(exception);
        assertThat(message).contains("Server error");
    }

    @Test
    void formatErrorWhenConfigExceptionReturnsUserFriendlyMessage() {
        ConfigException exception = new ConfigException("Cannot determine user home directory");
        String message = ErrorMessageFormatter.formatError(exception);
        assertThat(message).contains("home directory");
    }

    @Test
    void formatErrorWhenCliExceptionReturnsUserFriendlyMessage() {
        CliException exception = new CliException("File not found: test.txt");
        String message = ErrorMessageFormatter.formatError(exception);
        assertThat(message).contains("File not found");
    }

    @Test
    void formatErrorWhenIllegalArgumentExceptionReturnsUserFriendlyMessage() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid format");
        String message = ErrorMessageFormatter.formatError(exception);
        assertThat(message).contains("Invalid format");
    }

    @Test
    void formatErrorWhenIOExceptionReturnsUserFriendlyMessage() {
        IOException exception = new IOException("Permission denied");
        String message = ErrorMessageFormatter.formatError(exception);
        assertThat(message).contains("Permission denied");
    }

    @Test
    void formatErrorWhenSocketTimeoutExceptionInCauseReturnsTimeoutMessage() {
        Exception exception = new Exception("Request failed", new SocketTimeoutException("Timeout"));
        String message = ErrorMessageFormatter.formatError(exception);
        assertThat(message).contains("timed out");
    }

    @Test
    void formatErrorWhenConnectExceptionInCauseReturnsConnectionMessage() {
        Exception exception = new Exception("Connection failed", new ConnectException("Connection refused"));
        String message = ErrorMessageFormatter.formatError(exception);
        assertThat(message).contains("connect");
    }

    @Test
    void formatErrorWhenNullReturnsDefaultMessage() {
        String message = ErrorMessageFormatter.formatError(null);
        assertThat(message).isEqualTo("An unknown error occurred");
    }
}
