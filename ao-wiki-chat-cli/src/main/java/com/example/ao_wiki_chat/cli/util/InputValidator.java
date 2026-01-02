package com.example.ao_wiki_chat.cli.util;

import com.example.ao_wiki_chat.cli.exception.CliException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility class for validating user input.
 * Provides validation for UUIDs, file paths, session IDs, and other inputs.
 */
public final class InputValidator {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    private static final Pattern SESSION_ID_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_-]{1,64}$"
    );

    private InputValidator() {
        // Utility class
    }

    /**
     * Validates a UUID string format.
     *
     * @param uuidString the UUID string to validate
     * @return the parsed UUID
     * @throws CliException if the UUID format is invalid
     */
    public static UUID validateUuid(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new CliException("UUID cannot be empty");
        }

        String trimmed = uuidString.trim();

        if (!UUID_PATTERN.matcher(trimmed).matches()) {
            throw new CliException(
                    String.format("Invalid UUID format: '%s'. Expected format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx", trimmed)
            );
        }

        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException e) {
            throw new CliException("Invalid UUID: " + trimmed, e);
        }
    }

    /**
     * Validates a file path.
     * Checks if the file exists, is readable, and is a regular file.
     *
     * @param filePathString the file path string to validate
     * @return the validated Path
     * @throws CliException if the file path is invalid, file doesn't exist, or is not readable
     */
    public static Path validateFilePath(String filePathString) {
        if (filePathString == null || filePathString.isBlank()) {
            throw new CliException("File path cannot be empty");
        }

        try {
            Path path = Paths.get(filePathString);

            if (!Files.exists(path)) {
                throw new CliException("File not found: " + path);
            }

            if (!Files.isRegularFile(path)) {
                throw new CliException("Path is not a file: " + path);
            }

            if (!Files.isReadable(path)) {
                throw new CliException("File is not readable: " + path + ". Check file permissions.");
            }

            return path;
        } catch (CliException e) {
            throw e;
        } catch (Exception e) {
            throw new CliException("Invalid file path: " + filePathString, e);
        }
    }

    /**
     * Validates a session ID format.
     * Session IDs must be 1-64 characters, alphanumeric with underscores and hyphens.
     *
     * @param sessionId the session ID to validate
     * @throws CliException if the session ID format is invalid
     */
    public static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new CliException("Session ID cannot be empty");
        }

        if (!SESSION_ID_PATTERN.matcher(sessionId).matches()) {
            throw new CliException(
                    String.format("Invalid session ID format: '%s'. Session IDs must be 1-64 characters, " +
                            "containing only letters, numbers, underscores, and hyphens.", sessionId)
            );
        }
    }

    /**
     * Validates that a string is not null or blank.
     *
     * @param value the value to validate
     * @param fieldName the name of the field (for error messages)
     * @throws CliException if the value is null or blank
     */
    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new CliException(fieldName + " cannot be empty");
        }
    }
}
