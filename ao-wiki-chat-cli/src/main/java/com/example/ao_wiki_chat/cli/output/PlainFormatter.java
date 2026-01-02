package com.example.ao_wiki_chat.cli.output;

import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import com.example.ao_wiki_chat.cli.model.CliChunk;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import com.example.ao_wiki_chat.cli.model.CliMessage;
import com.example.ao_wiki_chat.cli.model.CliSourceReference;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Formatter that outputs data as simple plain text.
 * No tables, colors, or special formatting - just readable text.
 */
public final class PlainFormatter implements OutputFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String formatDocuments(List<CliDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "No documents found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Total documents: ").append(documents.size()).append("\n\n");

        for (int i = 0; i < documents.size(); i++) {
            CliDocument doc = documents.get(i);
            sb.append("Document ").append(i + 1).append(":\n");
            sb.append("  ID: ").append(doc.documentId()).append("\n");
            sb.append("  Filename: ").append(doc.filename()).append("\n");
            sb.append("  Status: ").append(doc.status()).append("\n");
            sb.append("  Size: ").append(formatFileSize(doc.fileSize())).append("\n");
            sb.append("  Created: ").append(doc.createdAt() != null ? doc.createdAt().format(DATE_FORMATTER) : "N/A").append("\n");
            if (i < documents.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String formatDocument(CliDocument document) {
        if (document == null) {
            return "No document provided.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Document Details:\n");
        sb.append("  ID: ").append(document.documentId()).append("\n");
        sb.append("  Filename: ").append(document.filename()).append("\n");
        sb.append("  Content Type: ").append(document.contentType() != null ? document.contentType() : "N/A").append("\n");
        sb.append("  File Size: ").append(formatFileSize(document.fileSize())).append("\n");
        sb.append("  Status: ").append(document.status()).append("\n");
        sb.append("  Created: ").append(document.createdAt() != null ? document.createdAt().format(DATE_FORMATTER) : "N/A").append("\n");
        sb.append("  Updated: ").append(document.updatedAt() != null ? document.updatedAt().format(DATE_FORMATTER) : "N/A").append("\n");

        return sb.toString();
    }

    @Override
    public String formatMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "No metadata available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Metadata:\n");

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String value = entry.getValue() != null ? entry.getValue().toString() : "null";
            sb.append("  ").append(entry.getKey()).append(": ").append(value).append("\n");
        }

        return sb.toString();
    }

    @Override
    public String formatChatResponse(CliChatResponse response, boolean showSources) {
        if (response == null) {
            return "No response received.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Answer:\n");
        sb.append(response.answer()).append("\n");

        if (showSources && response.sources() != null && !response.sources().isEmpty()) {
            sb.append("\nSources:\n");
            for (int i = 0; i < response.sources().size(); i++) {
                CliSourceReference source = response.sources().get(i);
                sb.append("  [").append(i + 1).append("] ").append(source.documentName());
                if (source.similarityScore() != null) {
                    sb.append(" (similarity: ").append(String.format("%.2f", source.similarityScore())).append(")");
                }
                sb.append("\n");
                if (source.chunkContent() != null && !source.chunkContent().isBlank()) {
                    String preview = source.chunkContent().length() > 200
                            ? source.chunkContent().substring(0, 197) + "..."
                            : source.chunkContent();
                    sb.append("    ").append(preview).append("\n");
                }
            }
        }

        return sb.toString();
    }

    @Override
    public String formatHistory(List<CliMessage> messages, boolean showSources) {
        if (messages == null || messages.isEmpty()) {
            return "No messages in conversation history.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Conversation History (").append(messages.size()).append(" messages)\n\n");

        for (int i = 0; i < messages.size(); i++) {
            CliMessage message = messages.get(i);
            String role = "USER".equalsIgnoreCase(message.role()) ? "User" : "Assistant";
            String timestamp = message.createdAt() != null
                    ? message.createdAt().format(DATE_FORMATTER)
                    : "N/A";

            sb.append("Message ").append(i + 1).append(": ").append(role).append(" - ").append(timestamp).append("\n");
            sb.append(message.content()).append("\n");

            if (showSources && message.sources() != null && !message.sources().isBlank()
                    && !"USER".equalsIgnoreCase(message.role())) {
                sb.append("Sources: ").append(message.sources()).append("\n");
            }

            if (i < messages.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String formatChunks(List<CliChunk> chunks, Integer limit) {
        if (chunks == null || chunks.isEmpty()) {
            return "No chunks found.";
        }

        List<CliChunk> displayChunks = limit != null && limit > 0 && limit < chunks.size()
                ? chunks.subList(0, limit)
                : chunks;

        StringBuilder sb = new StringBuilder();
        sb.append("Total chunks: ").append(chunks.size());
        if (limit != null && limit > 0 && chunks.size() > limit) {
            sb.append(" (showing ").append(limit).append(")");
        }
        sb.append("\n\n");

        for (int i = 0; i < displayChunks.size(); i++) {
            CliChunk chunk = displayChunks.get(i);
            sb.append("Chunk ").append(i + 1).append(":\n");
            sb.append("  Index: ").append(chunk.chunkIndex() != null ? chunk.chunkIndex().toString() : "N/A").append("\n");
            sb.append("  ID: ").append(chunk.id()).append("\n");
            sb.append("  Content: ");
            if (chunk.content() != null) {
                String content = chunk.content().length() > 200
                        ? chunk.content().substring(0, 197) + "..."
                        : chunk.content();
                sb.append(content);
            } else {
                sb.append("N/A");
            }
            sb.append("\n");
            sb.append("  Created: ").append(chunk.createdAt() != null ? chunk.createdAt().format(DATE_FORMATTER) : "N/A").append("\n");
            if (i < displayChunks.size() - 1) {
                sb.append("\n");
            }
        }

        if (limit != null && limit > 0 && chunks.size() > limit) {
            sb.append("\n... and ").append(chunks.size() - limit).append(" more chunks\n");
        }

        return sb.toString();
    }

    /**
     * Formats file size in human-readable format.
     *
     * @param bytes the file size in bytes
     * @return formatted size string
     */
    private String formatFileSize(Long bytes) {
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
}
