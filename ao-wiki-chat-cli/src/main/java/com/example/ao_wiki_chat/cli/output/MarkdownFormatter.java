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
 * Formatter that outputs data as Markdown.
 * Provides clean, readable Markdown formatting for all output types.
 */
public final class MarkdownFormatter implements OutputFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String formatDocuments(List<CliDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return "No documents found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Documents\n\n");
        sb.append("**Total:** ").append(documents.size()).append("\n\n");
        sb.append("| ID | Filename | Status | Size | Created |\n");
        sb.append("|----|----------|--------|------|----------|\n");

        for (CliDocument doc : documents) {
            String size = formatFileSize(doc.fileSize());
            String created = doc.createdAt() != null ? doc.createdAt().format(DATE_FORMATTER) : "N/A";
            sb.append(String.format("| `%s...` | %s | %s | %s | %s |\n",
                    doc.documentId().toString().substring(0, 8),
                    escapeMarkdown(doc.filename()),
                    doc.status(),
                    size,
                    created
            ));
        }

        return sb.toString();
    }

    @Override
    public String formatDocument(CliDocument document) {
        if (document == null) {
            return "No document provided.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Document Details\n\n");
        sb.append("| Field | Value |\n");
        sb.append("|-------|-------|\n");
        sb.append("| ID | `").append(document.documentId()).append("` |\n");
        sb.append("| Filename | ").append(escapeMarkdown(document.filename())).append(" |\n");
        sb.append("| Content Type | ").append(document.contentType() != null ? document.contentType() : "N/A").append(" |\n");
        sb.append("| File Size | ").append(formatFileSize(document.fileSize())).append(" |\n");
        sb.append("| Status | **").append(document.status()).append("** |\n");
        sb.append("| Created | ").append(document.createdAt() != null ? document.createdAt().format(DATE_FORMATTER) : "N/A").append(" |\n");
        sb.append("| Updated | ").append(document.updatedAt() != null ? document.updatedAt().format(DATE_FORMATTER) : "N/A").append(" |\n");

        return sb.toString();
    }

    @Override
    public String formatMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "No metadata available.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Metadata\n\n");
        sb.append("| Key | Value |\n");
        sb.append("|-----|-------|\n");

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String value = entry.getValue() != null ? escapeMarkdown(entry.getValue().toString()) : "null";
            sb.append("| ").append(escapeMarkdown(entry.getKey())).append(" | ").append(value).append(" |\n");
        }

        return sb.toString();
    }

    @Override
    public String formatChatResponse(CliChatResponse response, boolean showSources) {
        if (response == null) {
            return "No response received.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## Answer\n\n");
        sb.append(response.answer()).append("\n");

        if (showSources && response.sources() != null && !response.sources().isEmpty()) {
            sb.append("\n## Sources\n\n");
            for (int i = 0; i < response.sources().size(); i++) {
                CliSourceReference source = response.sources().get(i);
                sb.append("### ").append(i + 1).append(". ").append(escapeMarkdown(source.documentName())).append("\n\n");
                if (source.similarityScore() != null) {
                    sb.append("**Similarity:** ").append(String.format("%.2f", source.similarityScore())).append("\n\n");
                }
                if (source.chunkContent() != null && !source.chunkContent().isBlank()) {
                    sb.append("```\n").append(source.chunkContent()).append("\n```\n\n");
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
        sb.append("# Conversation History\n\n");
        sb.append("**Total messages:** ").append(messages.size()).append("\n\n");

        for (int i = 0; i < messages.size(); i++) {
            CliMessage message = messages.get(i);
            String role = "USER".equalsIgnoreCase(message.role()) ? "User" : "Assistant";
            String timestamp = message.createdAt() != null
                    ? message.createdAt().format(DATE_FORMATTER)
                    : "N/A";

            sb.append("## Message ").append(i + 1).append(": ").append(role).append(" (").append(timestamp).append(")\n\n");
            sb.append(message.content()).append("\n\n");

            if (showSources && message.sources() != null && !message.sources().isBlank()
                    && !"USER".equalsIgnoreCase(message.role())) {
                sb.append("**Sources:** ").append(escapeMarkdown(message.sources())).append("\n\n");
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
        sb.append("## Chunks\n\n");
        sb.append("**Total:** ").append(chunks.size());
        if (limit != null && limit > 0 && chunks.size() > limit) {
            sb.append(" (showing ").append(limit).append(")");
        }
        sb.append("\n\n");
        sb.append("| Index | ID | Content Preview | Created |\n");
        sb.append("|-------|----|-----------------|----------|\n");

        for (CliChunk chunk : displayChunks) {
            String contentPreview = chunk.content() != null && chunk.content().length() > 50
                    ? chunk.content().substring(0, 47) + "..."
                    : (chunk.content() != null ? chunk.content() : "N/A");
            String created = chunk.createdAt() != null ? chunk.createdAt().format(DATE_FORMATTER) : "N/A";
            sb.append(String.format("| %s | `%s...` | %s | %s |\n",
                    chunk.chunkIndex() != null ? chunk.chunkIndex().toString() : "N/A",
                    chunk.id().toString().substring(0, 8),
                    escapeMarkdown(contentPreview),
                    created
            ));
        }

        if (limit != null && limit > 0 && chunks.size() > limit) {
            sb.append("\n*... and ").append(chunks.size() - limit).append(" more chunks*\n");
        }

        return sb.toString();
    }

    /**
     * Escapes special Markdown characters in text.
     *
     * @param text the text to escape
     * @return escaped text
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("|", "\\|")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("`", "\\`")
                .replace("#", "\\#");
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
