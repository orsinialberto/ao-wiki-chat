package com.example.ao_wiki_chat.cli.util;

import com.example.ao_wiki_chat.cli.exception.CliException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InputValidatorTest {

    @Test
    void validateUuidWhenValidUuidReturnsParsedUuid() {
        String validUuid = "550e8400-e29b-41d4-a716-446655440000";
        UUID result = InputValidator.validateUuid(validUuid);
        assertThat(result).isNotNull();
        assertThat(result.toString()).isEqualTo(validUuid);
    }

    @Test
    void validateUuidWhenNullThrowsCliException() {
        assertThatThrownBy(() -> InputValidator.validateUuid(null))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("UUID cannot be empty");
    }

    @Test
    void validateUuidWhenEmptyThrowsCliException() {
        assertThatThrownBy(() -> InputValidator.validateUuid(""))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("UUID cannot be empty");
    }

    @Test
    void validateUuidWhenInvalidFormatThrowsCliException() {
        assertThatThrownBy(() -> InputValidator.validateUuid("not-a-uuid"))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("Invalid UUID format");
    }

    @Test
    void validateFilePathWhenFileExistsReturnsPath(@TempDir Path tempDir) throws Exception {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");

        Path result = InputValidator.validateFilePath(testFile.toString());
        assertThat(result).isEqualTo(testFile);
    }

    @Test
    void validateFilePathWhenFileDoesNotExistThrowsCliException(@TempDir Path tempDir) {
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");

        assertThatThrownBy(() -> InputValidator.validateFilePath(nonExistentFile.toString()))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void validateFilePathWhenPathIsDirectoryThrowsCliException(@TempDir Path tempDir) {
        assertThatThrownBy(() -> InputValidator.validateFilePath(tempDir.toString()))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("Path is not a file");
    }

    @Test
    void validateFilePathWhenNullThrowsCliException() {
        assertThatThrownBy(() -> InputValidator.validateFilePath(null))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("File path cannot be empty");
    }

    @Test
    void validateSessionIdWhenValidSessionIdDoesNotThrow() {
        InputValidator.validateSessionId("session-123");
        InputValidator.validateSessionId("session_123");
        InputValidator.validateSessionId("abc123");
        InputValidator.validateSessionId("a".repeat(64));
    }

    @Test
    void validateSessionIdWhenNullThrowsCliException() {
        assertThatThrownBy(() -> InputValidator.validateSessionId(null))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("Session ID cannot be empty");
    }

    @Test
    void validateSessionIdWhenEmptyThrowsCliException() {
        assertThatThrownBy(() -> InputValidator.validateSessionId(""))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("Session ID cannot be empty");
    }

    @Test
    void validateSessionIdWhenTooLongThrowsCliException() {
        String tooLong = "a".repeat(65);
        assertThatThrownBy(() -> InputValidator.validateSessionId(tooLong))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("Invalid session ID format");
    }

    @Test
    void validateSessionIdWhenInvalidCharactersThrowsCliException() {
        assertThatThrownBy(() -> InputValidator.validateSessionId("session@123"))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("Invalid session ID format");
    }

    @Test
    void validateNotEmptyWhenValueIsNullThrowsCliException() {
        assertThatThrownBy(() -> InputValidator.validateNotEmpty(null, "field"))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("field cannot be empty");
    }

    @Test
    void validateNotEmptyWhenValueIsBlankThrowsCliException() {
        assertThatThrownBy(() -> InputValidator.validateNotEmpty("   ", "field"))
                .isInstanceOf(CliException.class)
                .hasMessageContaining("field cannot be empty");
    }

    @Test
    void validateNotEmptyWhenValueIsValidDoesNotThrow() {
        InputValidator.validateNotEmpty("valid", "field");
    }
}
