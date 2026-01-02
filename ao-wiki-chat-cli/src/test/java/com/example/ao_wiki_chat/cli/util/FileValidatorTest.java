package com.example.ao_wiki_chat.cli.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for FileValidator.
 * Tests file validation including existence, size, type, and readability.
 */
class FileValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void validateWhenFileIsNullThrowsException() {
        // Given
        FileValidator validator = new FileValidator(1024, "txt");

        // When/Then
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File path cannot be null");
    }

    @Test
    void validateWhenFileDoesNotExistThrowsException() {
        // Given
        FileValidator validator = new FileValidator(1024, "txt");
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");

        // When/Then
        assertThatThrownBy(() -> validator.validate(nonExistentFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void validateWhenPathIsDirectoryThrowsException() throws IOException {
        // Given
        FileValidator validator = new FileValidator(1024, "txt");
        Path directory = tempDir.resolve("dir");
        Files.createDirectory(directory);

        // When/Then
        assertThatThrownBy(() -> validator.validate(directory))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path is not a file");
    }

    @Test
    void validateWhenFileIsEmptyThrowsException() throws IOException {
        // Given
        FileValidator validator = new FileValidator(1024, "txt");
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.createFile(emptyFile);

        // When/Then
        assertThatThrownBy(() -> validator.validate(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is empty");
    }

    @Test
    void validateWhenFileExceedsMaxSizeThrowsException() throws IOException {
        // Given
        FileValidator validator = new FileValidator(100, "txt"); // 100 bytes max
        Path largeFile = tempDir.resolve("large.txt");
        Files.write(largeFile, new byte[200]); // 200 bytes

        // When/Then
        assertThatThrownBy(() -> validator.validate(largeFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum allowed size");
    }

    @Test
    void validateWhenFileHasNoExtensionThrowsException() throws IOException {
        // Given
        FileValidator validator = new FileValidator(1024, "txt,pdf");
        Path fileWithoutExt = tempDir.resolve("noextension");
        Files.write(fileWithoutExt, "content".getBytes());

        // When/Then
        assertThatThrownBy(() -> validator.validate(fileWithoutExt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File has no extension");
    }

    @Test
    void validateWhenFileTypeIsNotSupportedThrowsException() throws IOException {
        // Given
        FileValidator validator = new FileValidator(1024, "txt,pdf");
        Path unsupportedFile = tempDir.resolve("file.exe");
        Files.write(unsupportedFile, "content".getBytes());

        // When/Then
        assertThatThrownBy(() -> validator.validate(unsupportedFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not supported");
    }

    @Test
    void validateWhenFileIsValidDoesNotThrowException() throws IOException {
        // Given
        FileValidator validator = new FileValidator(1024, "txt,pdf");
        Path validFile = tempDir.resolve("valid.txt");
        Files.write(validFile, "content".getBytes());

        // When/Then
        validator.validate(validFile);
        // No exception should be thrown
    }

    @Test
    void validateWhenFileTypeIsCaseInsensitive() throws IOException {
        // Given
        FileValidator validator = new FileValidator(1024, "txt,pdf");
        Path upperCaseFile = tempDir.resolve("FILE.TXT");
        Files.write(upperCaseFile, "content".getBytes());

        // When/Then
        validator.validate(upperCaseFile);
        // No exception should be thrown
    }

    @Test
    void validateWhenSupportedTypesHaveDotsHandlesCorrectly() throws IOException {
        // Given
        FileValidator validator = new FileValidator(1024, ".txt,.pdf");
        Path file = tempDir.resolve("test.txt");
        Files.write(file, "content".getBytes());

        // When/Then
        validator.validate(file);
        // No exception should be thrown
    }

    @Test
    void validateWhenSupportedTypesAreMixedCase() throws IOException {
        // Given
        FileValidator validator = new FileValidator(1024, "TXT,PDF,md");
        Path file = tempDir.resolve("test.txt");
        Files.write(file, "content".getBytes());

        // When/Then
        validator.validate(file);
        // No exception should be thrown
    }

    @Test
    void validateWhenFileSizeIsExactlyMaxSizeDoesNotThrowException() throws IOException {
        // Given
        FileValidator validator = new FileValidator(100, "txt");
        Path file = tempDir.resolve("exact.txt");
        Files.write(file, new byte[100]);

        // When/Then
        validator.validate(file);
        // No exception should be thrown
    }

    @Test
    void validateWhenSupportedTypesStringIsEmptyThrowsException() throws IOException {
        // Given
        FileValidator validator = new FileValidator(1024, "");
        Path file = tempDir.resolve("test.txt");
        Files.write(file, "content".getBytes());

        // When/Then
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not supported");
    }

    @Test
    void validateWhenSupportedTypesStringIsNullThrowsException() throws IOException {
        // Given
        FileValidator validator = new FileValidator(1024, null);
        Path file = tempDir.resolve("test.txt");
        Files.write(file, "content".getBytes());

        // When/Then
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is not supported");
    }
}
