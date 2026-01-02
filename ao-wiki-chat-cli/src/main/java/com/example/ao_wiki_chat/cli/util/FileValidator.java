package com.example.ao_wiki_chat.cli.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for validating file uploads.
 * Checks file existence, size, type, and other constraints.
 */
public final class FileValidator {

    private final long maxFileSizeBytes;
    private final Set<String> supportedExtensions;

    /**
     * Creates a FileValidator instance.
     *
     * @param maxFileSizeBytes    maximum file size in bytes
     * @param supportedFileTypes  comma-separated list of supported file extensions (e.g., "pdf,txt,md")
     */
    public FileValidator(long maxFileSizeBytes, String supportedFileTypes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.supportedExtensions = parseSupportedTypes(supportedFileTypes);
    }

    /**
     * Validates a file for upload.
     *
     * @param filePath the file path to validate
     * @throws IllegalArgumentException if validation fails with a user-friendly message
     */
    public void validate(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        // Check if file exists
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        // Check if it's a regular file
        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("Path is not a file: " + filePath);
        }

        // Check if file is readable
        if (!Files.isReadable(filePath)) {
            throw new IllegalArgumentException("File is not readable: " + filePath);
        }

        // Check if file is not empty
        try {
            long fileSize = Files.size(filePath);
            if (fileSize == 0) {
                throw new IllegalArgumentException("File is empty: " + filePath.getFileName());
            }

            // Check file size
            if (fileSize > maxFileSizeBytes) {
                String maxSize = formatFileSize(maxFileSizeBytes);
                String actualSize = formatFileSize(fileSize);
                throw new IllegalArgumentException(
                        String.format("File size (%s) exceeds maximum allowed size (%s): %s",
                                actualSize, maxSize, filePath.getFileName())
                );
            }
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Cannot read file size: " + filePath, e);
        }

        // Check file type
        String fileName = filePath.getFileName().toString();
        String extension = getFileExtension(fileName);
        if (extension == null || extension.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("File has no extension: %s. Supported types: %s",
                            fileName, String.join(", ", supportedExtensions))
            );
        }

        if (!supportedExtensions.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("File type '%s' is not supported. Supported types: %s",
                            extension, String.join(", ", supportedExtensions))
            );
        }
    }

    /**
     * Parses the supported file types string into a set of extensions.
     *
     * @param supportedFileTypes comma-separated list of extensions
     * @return set of lowercase extensions (without dots)
     */
    private Set<String> parseSupportedTypes(String supportedFileTypes) {
        if (supportedFileTypes == null || supportedFileTypes.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(supportedFileTypes.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .map(ext -> ext.startsWith(".") ? ext.substring(1) : ext)
                .filter(ext -> !ext.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Gets the file extension from a filename.
     *
     * @param fileName the filename
     * @return the extension (without dot) or null if no extension
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return null;
        }

        return fileName.substring(lastDotIndex + 1);
    }

    /**
     * Formats file size in human-readable format.
     *
     * @param bytes the file size in bytes
     * @return formatted size string
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
