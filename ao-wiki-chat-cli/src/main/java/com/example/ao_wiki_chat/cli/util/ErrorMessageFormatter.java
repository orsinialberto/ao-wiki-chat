package com.example.ao_wiki_chat.cli.util;

import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.exception.CliException;
import com.example.ao_wiki_chat.cli.exception.ConfigException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.regex.Pattern;

/**
 * Utility class for formatting user-friendly error messages.
 * Converts technical exceptions into actionable messages with suggestions.
 */
public final class ErrorMessageFormatter {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );

    private ErrorMessageFormatter() {
        // Utility class
    }

    /**
     * Formats an exception into a user-friendly error message.
     *
     * @param exception the exception to format
     * @return formatted error message
     */
    public static String formatError(Throwable exception) {
        if (exception == null) {
            return "An unknown error occurred";
        }

        if (exception instanceof ApiException apiException) {
            return formatApiError(apiException);
        }

        if (exception instanceof ConfigException) {
            return formatConfigError(exception);
        }

        if (exception instanceof CliException) {
            return formatCliError(exception);
        }

        if (exception instanceof IllegalArgumentException iae) {
            return formatValidationError(iae);
        }

        if (exception instanceof IOException ioe) {
            return formatIoError(ioe);
        }

        // Check for common network/timeout exceptions in cause chain
        Throwable cause = exception.getCause();
        if (cause instanceof SocketTimeoutException) {
            return "Request timed out. The server took too long to respond. " +
                    "Try increasing the timeout in your configuration or check your network connection.";
        }

        if (cause instanceof ConnectException) {
            return "Cannot connect to the API server. " +
                    "Please check that the server is running and the API URL is correct. " +
                    "Use 'wikichat config set api.url <url>' to configure the API URL.";
        }

        // Generic error
        String message = exception.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }

        return "An unexpected error occurred: " + exception.getClass().getSimpleName();
    }

    /**
     * Formats an API exception into a user-friendly message.
     *
     * @param exception the API exception
     * @return formatted error message
     */
    private static String formatApiError(ApiException exception) {
        int statusCode = exception.getStatusCode();
        String message = exception.getMessage();

        return switch (statusCode) {
            case 400 -> "Invalid request: " + extractUserMessage(message) +
                    " Please check your input and try again.";
            case 401 -> "Authentication failed. Please check your API credentials.";
            case 403 -> "Access forbidden. You don't have permission to perform this operation.";
            case 404 -> {
                String suggestion = extractIdSuggestion(message);
                yield "Resource not found. " + extractUserMessage(message) + suggestion;
            }
            case 408 -> "Request timeout. The server took too long to process your request. " +
                    "Try again or increase the timeout in your configuration.";
            case 409 -> "Conflict: " + extractUserMessage(message) +
                    " The resource may already exist or be in use.";
            case 413 -> "File too large. The file exceeds the maximum allowed size. " +
                    "Please use a smaller file or increase the limit in your configuration.";
            case 415 -> "Unsupported file type. Please check that the file format is supported.";
            case 422 -> "Invalid input: " + extractUserMessage(message) +
                    " Please check your data and try again.";
            case 429 -> "Rate limit exceeded. Too many requests. Please wait a moment and try again.";
            case 500, 502, 503, 504 -> "Server error. The API server encountered an issue. " +
                    "Please try again later. If the problem persists, contact support.";
            default -> {
                if (statusCode >= 500) {
                    yield "Server error (HTTP " + statusCode + "): " + extractUserMessage(message) +
                            " Please try again later.";
                } else if (statusCode >= 400) {
                    yield "Client error (HTTP " + statusCode + "): " + extractUserMessage(message);
                } else {
                    yield "API error: " + extractUserMessage(message);
                }
            }
        };
    }

    /**
     * Formats a configuration exception.
     *
     * @param exception the configuration exception
     * @return formatted error message
     */
    private static String formatConfigError(Throwable exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("Cannot determine user home directory")) {
            return "Cannot access your home directory. " +
                    "Please check your system configuration or set the HOME environment variable.";
        }
        if (message != null && message.contains("Failed to save config")) {
            return "Failed to save configuration. " +
                    "Please check that you have write permissions to ~/.wikichat/ directory.";
        }
        if (message != null && message.contains("Failed to load config")) {
            return "Failed to load configuration file. Using default settings. " +
                    "You can reconfigure using 'wikichat config set <key> <value>'.";
        }
        return message != null ? message : "Configuration error occurred";
    }

    /**
     * Formats a CLI exception.
     *
     * @param exception the CLI exception
     * @return formatted error message
     */
    private static String formatCliError(Throwable exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("UUID")) {
            return message + " Did you mean to use a different identifier?";
        }
        if (message != null && message.contains("File not found")) {
            return message + " Please check the file path and try again.";
        }
        if (message != null && message.contains("not readable")) {
            return message + " Please check file permissions.";
        }
        return message != null ? message : "An error occurred";
    }

    /**
     * Formats a validation error.
     *
     * @param exception the validation exception
     * @return formatted error message
     */
    private static String formatValidationError(IllegalArgumentException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("format")) {
            return message + " Please check the format and try again.";
        }
        return message != null ? message : "Invalid input provided";
    }

    /**
     * Formats an I/O error.
     *
     * @param exception the I/O exception
     * @return formatted error message
     */
    private static String formatIoError(IOException exception) {
        String message = exception.getMessage();
        if (message != null && message.contains("Permission denied")) {
            return "Permission denied. Please check file permissions or run with appropriate privileges.";
        }
        if (message != null && message.contains("No such file")) {
            return "File not found. Please check the file path and try again.";
        }
        if (message != null && message.contains("timed out")) {
            return "Operation timed out. The operation took too long to complete. " +
                    "Try increasing the timeout in your configuration.";
        }
        return message != null ? message : "I/O error occurred";
    }

    /**
     * Extracts a user-friendly message from an API error response.
     *
     * @param message the raw error message
     * @return cleaned user message
     */
    private static String extractUserMessage(String message) {
        if (message == null || message.isBlank()) {
            return "An error occurred";
        }

        // Try to extract meaningful error from JSON response
        if (message.contains("{") && message.contains("message")) {
            // Simple extraction - could be improved with proper JSON parsing
            int messageIndex = message.indexOf("\"message\"");
            if (messageIndex > 0) {
                int start = message.indexOf("\"", messageIndex + 9) + 1;
                int end = message.indexOf("\"", start);
                if (end > start) {
                    return message.substring(start, end);
                }
            }
        }

        // Remove technical details
        String cleaned = message;
        if (cleaned.contains("API request failed")) {
            cleaned = cleaned.replaceFirst("API request failed with status \\d+: ", "");
        }
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 197) + "...";
        }

        return cleaned;
    }

    /**
     * Extracts ID suggestion from error message.
     *
     * @param message the error message
     * @return suggestion string
     */
    private static String extractIdSuggestion(String message) {
        if (message == null) {
            return "";
        }

        // Try to find UUID in message
        java.util.regex.Matcher matcher = UUID_PATTERN.matcher(message);
        if (matcher.find()) {
            String foundId = matcher.group();
            return " Did you mean: " + foundId + "?";
        }

        return "";
    }
}
