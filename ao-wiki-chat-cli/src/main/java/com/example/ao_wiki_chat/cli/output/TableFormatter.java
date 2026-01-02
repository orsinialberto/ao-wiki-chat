package com.example.ao_wiki_chat.cli.output;

import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import com.example.ao_wiki_chat.cli.model.CliChunk;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import com.example.ao_wiki_chat.cli.model.CliMessage;
import com.example.ao_wiki_chat.cli.model.CliSourceReference;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Formatter that outputs data as ASCII tables with color support.
 * Uses Jansi for ANSI color codes and ASCII Table library for table formatting.
 */
public final class TableFormatter implements OutputFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final boolean colorsEnabled;

    /**
     * Creates a new TableFormatter.
     *
     * @param colorsEnabled whether to enable ANSI color codes
     */
    public TableFormatter(boolean colorsEnabled) {
        this.colorsEnabled = colorsEnabled;
        if (colorsEnabled) {
            AnsiConsole.systemInstall();
        }
    }

    @Override
    public String formatDocuments(List<CliDocument> documents) {
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
            String status = formatStatus(doc.status());
            table.addRow(
                    doc.documentId().toString().substring(0, 8) + "...",
                    doc.filename(),
                    status,
                    size,
                    created
            );
        }

        table.addRule();
        table.getRenderer().setCWC(new CWC_LongestLine());
        return table.render();
    }

    @Override
    public String formatDocument(CliDocument document) {
        if (document == null) {
            return "No document provided.";
        }

        AsciiTable table = new AsciiTable();
        table.addRule();
        table.addRow("Field", "Value");
        table.addRule();
        table.addRow("ID", document.documentId().toString());
        table.addRow("Filename", document.filename());
        table.addRow("Content Type", document.contentType() != null ? document.contentType() : "N/A");
        table.addRow("File Size", formatFileSize(document.fileSize()));
        table.addRow("Status", formatStatus(document.status()));
        table.addRow("Created", document.createdAt() != null ? document.createdAt().format(DATE_FORMATTER) : "N/A");
        table.addRow("Updated", document.updatedAt() != null ? document.updatedAt().format(DATE_FORMATTER) : "N/A");
        table.addRule();
        table.getRenderer().setCWC(new CWC_LongestLine());
        return table.render();
    }

    @Override
    public String formatMetadata(Map<String, Object> metadata) {
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

    @Override
    public String formatChatResponse(CliChatResponse response, boolean showSources) {
        if (response == null) {
            return "No response received.";
        }

        StringBuilder sb = new StringBuilder();
        String answerIcon = colorsEnabled ? "💬 " : "";
        sb.append(answerIcon).append("Answer:\n");
        sb.append(response.answer()).append("\n");

        if (showSources && response.sources() != null && !response.sources().isEmpty()) {
            String sourcesIcon = colorsEnabled ? "📚 " : "";
            sb.append("\n").append(sourcesIcon).append("Sources:\n");
            for (int i = 0; i < response.sources().size(); i++) {
                CliSourceReference source = response.sources().get(i);
                sb.append(String.format("\n[%d] %s", i + 1, source.documentName()));
                if (source.similarityScore() != null) {
                    sb.append(String.format(" (%.2f)", source.similarityScore()));
                }
                if (source.chunkContent() != null && !source.chunkContent().isBlank()) {
                    String preview = source.chunkContent().length() > 200
                            ? source.chunkContent().substring(0, 197) + "..."
                            : source.chunkContent();
                    sb.append("\n   ").append(preview);
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
        String historyIcon = colorsEnabled ? "📜 " : "";
        sb.append(historyIcon).append("Conversation History (").append(messages.size()).append(" messages)\n\n");

        for (int i = 0; i < messages.size(); i++) {
            CliMessage message = messages.get(i);
            String roleIcon = "USER".equalsIgnoreCase(message.role())
                    ? (colorsEnabled ? "👤 " : "") + "User"
                    : (colorsEnabled ? "🤖 " : "") + "Assistant";
            String timestamp = message.createdAt() != null
                    ? message.createdAt().format(DATE_FORMATTER)
                    : "N/A";

            sb.append(String.format("[%d] %s - %s\n", i + 1, roleIcon, timestamp));
            sb.append(message.content()).append("\n");

            if (showSources && message.sources() != null && !message.sources().isBlank()
                    && !"USER".equalsIgnoreCase(message.role())) {
                String sourcesIcon = colorsEnabled ? "📚 " : "";
                sb.append(sourcesIcon).append("Sources: ").append(message.sources()).append("\n");
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
     * Formats a status string with color if colors are enabled.
     *
     * @param status the status string
     * @return formatted status with color codes if enabled
     */
    private String formatStatus(String status) {
        if (!colorsEnabled || status == null) {
            return status != null ? status : "N/A";
        }

        return switch (status.toUpperCase()) {
            case "COMPLETED" -> ansi().fg(Ansi.Color.GREEN).a(status).reset().toString();
            case "PROCESSING" -> ansi().fg(Ansi.Color.YELLOW).a(status).reset().toString();
            case "FAILED" -> ansi().fg(Ansi.Color.RED).a(status).reset().toString();
            default -> status;
        };
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
