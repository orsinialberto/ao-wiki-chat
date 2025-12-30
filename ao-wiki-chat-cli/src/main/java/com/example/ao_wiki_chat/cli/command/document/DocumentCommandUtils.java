package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.model.CliChunk;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Utility class for document command operations.
 * Provides common formatting, validation, and output methods.
 */
public final class DocumentCommandUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DocumentCommandUtils() {
        // Utility class
    }

    /**
     * Validates that a file exists and is readable.
     *
     * @param filePath the file path to validate
     * @throws IllegalArgumentException if the file is invalid
     */
    public static void validateFile(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found: " + filePath);
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException("Path is not a file: " + filePath);
        }

        if (!Files.isReadable(filePath)) {
            throw new IllegalArgumentException("File is not readable: " + filePath);
        }
    }

    /**
     * Validates a UUID string.
     *
     * @param uuidString the UUID string to validate
     * @return the parsed UUID
     * @throws IllegalArgumentException if the UUID is invalid
     */
    public static UUID validateUuid(String uuidString) {
        if (uuidString == null || uuidString.isBlank()) {
            throw new IllegalArgumentException("UUID cannot be null or empty");
        }

        try {
            return UUID.fromString(uuidString.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid UUID format: " + uuidString, e);
        }
    }

    /**
     * Validates a document status value.
     *
     * @param status the status string to validate
     * @return the validated status (uppercase)
     * @throws IllegalArgumentException if the status is invalid
     */
    public static String validateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }

        String upperStatus = status.trim().toUpperCase();
        if (!upperStatus.equals("PROCESSING") && !upperStatus.equals("COMPLETED") && !upperStatus.equals("FAILED")) {
            throw new IllegalArgumentException("Invalid status: " + status + ". Must be one of: PROCESSING, COMPLETED, FAILED");
        }

        return upperStatus;
    }

    /**
     * Formats a list of documents as a table.
     *
     * @param documents the documents to format
     * @return formatted table string
     */
    public static String formatDocumentsTable(List<CliDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "No documents found.";
        }

        AsciiTable table = new AsciiTable();
        table.addRule();
        table.addRow("ID", "Filename", "Status", "Size", "Created");
        table.addRule();

        for (CliDocument doc : documents) {
            String size = formatFileSize(doc.fileSize());
            String created = doc.createdAt() != null ? doc.createdAt().format(DATE_FORMATTER) : "N/A";
            table.addRow(
                    doc.documentId().toString().substring(0, 8) + "...",
                    doc.filename(),
                    doc.status(),
                    size,
                    created
            );
        }

        table.addRule();
        table.getRenderer().setCWC(new CWC_LongestLine());
        return table.render();
    }

    /**
     * Formats a single document as a detailed table.
     *
     * @param document the document to format
     * @return formatted table string
     */
    public static String formatDocumentDetails(CliDocument document) {
        AsciiTable table = new AsciiTable();
        table.addRule();
        table.addRow("Field", "Value");
        table.addRule();
        table.addRow("ID", document.documentId().toString());
        table.addRow("Filename", document.filename());
        table.addRow("Content Type", document.contentType() != null ? document.contentType() : "N/A");
        table.addRow("File Size", formatFileSize(document.fileSize()));
        table.addRow("Status", document.status());
        table.addRow("Created", document.createdAt() != null ? document.createdAt().format(DATE_FORMATTER) : "N/A");
        table.addRow("Updated", document.updatedAt() != null ? document.updatedAt().format(DATE_FORMATTER) : "N/A");
        table.addRule();
        table.getRenderer().setCWC(new CWC_LongestLine());
        return table.render();
    }

    /**
     * Formats document metadata as a table.
     *
     * @param metadata the metadata map
     * @return formatted table string
     */
    public static String formatMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "No metadata available.";
        }

        AsciiTable table = new AsciiTable();
        table.addRule();
        table.addRow("Key", "Value");
        table.addRule();

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue().toString() : "null";
            table.addRow(entry.getKey(), value);
        }

        table.addRule();
        table.getRenderer().setCWC(new CWC_LongestLine());
        return table.render();
    }

    /**
     * Formats a list of chunks as a table.
     *
     * @param chunks the chunks to format
     * @param limit  maximum number of chunks to display
     * @return formatted table string
     */
    public static String formatChunksTable(List<CliChunk> chunks, Integer limit) {
        if (chunks == null || chunks.isEmpty()) {
            return "No chunks found.";
        }

        List<CliChunk> displayChunks = limit != null && limit > 0 && limit < chunks.size()
                ? chunks.subList(0, limit)
                : chunks;

        AsciiTable table = new AsciiTable();
        table.addRule();
        table.addRow("Index", "ID", "Content Preview", "Created");
        table.addRule();

        for (CliChunk chunk : displayChunks) {
            String contentPreview = chunk.content() != null && chunk.content().length() > 50
                    ? chunk.content().substring(0, 47) + "..."
                    : (chunk.content() != null ? chunk.content() : "N/A");
            String created = chunk.createdAt() != null ? chunk.createdAt().format(DATE_FORMATTER) : "N/A";
            table.addRow(
                    chunk.chunkIndex() != null ? chunk.chunkIndex().toString() : "N/A",
                    chunk.id().toString().substring(0, 8) + "...",
                    contentPreview,
                    created
            );
        }

        table.addRule();
        table.getRenderer().setCWC(new CWC_LongestLine());

        String result = table.render();
        if (limit != null && limit > 0 && chunks.size() > limit) {
            result += "\n... and " + (chunks.size() - limit) + " more chunks (use --limit to see more)";
        }

        return result;
    }

    /**
     * Formats file size in human-readable format.
     *
     * @param bytes the file size in bytes
     * @return formatted size string
     */
    public static String formatFileSize(Long bytes) {
        if (bytes == null) {
            return "N/A";
        }

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

    /**
     * Filters documents by status.
     *
     * @param documents the documents to filter
     * @param status    the status to filter by (null to return all)
     * @return filtered list of documents
     */
    public static List<CliDocument> filterByStatus(List<CliDocument> documents, String status) {
        if (status == null || status.isBlank()) {
            return documents;
        }

        String upperStatus = status.trim().toUpperCase();
        return documents.stream()
                .filter(doc -> doc.status() != null && doc.status().equalsIgnoreCase(upperStatus))
                .toList();
    }

    /**
     * Formats output as JSON (simple implementation).
     * For full JSON support, consider using Jackson ObjectMapper.
     *
     * @param object the object to format (simplified)
     * @return JSON-like string representation
     */
    public static String formatAsJson(Object object) {
        // Simple JSON formatting - for production, use Jackson ObjectMapper
        if (object == null) {
            return "null";
        }
        return object.toString();
    }
}
