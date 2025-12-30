package com.example.ao_wiki_chat.cli.command.chat;

import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import com.example.ao_wiki_chat.cli.model.CliMessage;
import com.example.ao_wiki_chat.cli.model.CliSourceReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for chat command operations.
 * Provides common formatting, validation, and output methods.
 */
public final class ChatCommandUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_QUERY_LENGTH = 10000;
    private static final ObjectMapper objectMapper = createObjectMapper();

    private ChatCommandUtils() {
        // Utility class
    }

    /**
     * Creates and configures an ObjectMapper for JSON serialization.
     *
     * @return configured ObjectMapper
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Validates a chat query.
     *
     * @param query the query to validate
     * @throws IllegalArgumentException if the query is invalid
     */
    public static void validateQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }

        if (trimmed.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Query exceeds maximum length of %d characters", MAX_QUERY_LENGTH)
            );
        }
    }

    /**
     * Validates a session ID format.
     *
     * @param sessionId the session ID to validate
     * @throws IllegalArgumentException if the session ID is invalid
     */
    public static void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be null or blank");
        }

        if (sessionId.length() > 255) {
            throw new IllegalArgumentException("Session ID must not exceed 255 characters");
        }

        // Try to parse as UUID to validate format
        try {
            UUID.fromString(sessionId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Session ID must be a valid UUID format", e);
        }
    }

    /**
     * Generates a new session ID.
     *
     * @return a new UUID as string
     */
    public static String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Formats a chat response in text format.
     *
     * @param response the chat response
     * @param showSources whether to show sources
     * @return formatted text string
     */
    public static String formatChatResponseText(CliChatResponse response, boolean showSources) {
        if (response == null) {
            return "No response received.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("💬 Answer:\n");
        sb.append(response.answer()).append("\n");

        if (showSources && response.sources() != null && !response.sources().isEmpty()) {
            sb.append("\n📚 Sources:\n");
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

    /**
     * Formats a chat response in markdown format.
     *
     * @param response the chat response
     * @param showSources whether to show sources
     * @return formatted markdown string
     */
    public static String formatChatResponseMarkdown(CliChatResponse response, boolean showSources) {
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
                sb.append(String.format("### %d. %s\n", i + 1, source.documentName()));
                if (source.similarityScore() != null) {
                    sb.append(String.format("**Similarity:** %.2f\n", source.similarityScore()));
                }
                if (source.chunkContent() != null && !source.chunkContent().isBlank()) {
                    sb.append("\n```\n").append(source.chunkContent()).append("\n```\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Formats a chat response in JSON format.
     *
     * @param response the chat response
     * @return formatted JSON string
     */
    public static String formatChatResponseJson(CliChatResponse response) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize response: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Formats conversation history in text format.
     *
     * @param messages the list of messages
     * @param showSources whether to show sources
     * @return formatted text string
     */
    public static String formatHistoryText(List<CliMessage> messages, boolean showSources) {
        if (messages == null || messages.isEmpty()) {
            return "No messages in conversation history.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📜 Conversation History (").append(messages.size()).append(" messages)\n\n");

        for (int i = 0; i < messages.size(); i++) {
            CliMessage message = messages.get(i);
            String role = "USER".equalsIgnoreCase(message.role()) ? "👤 User" : "🤖 Assistant";
            String timestamp = message.createdAt() != null
                    ? message.createdAt().format(DATE_FORMATTER)
                    : "N/A";

            sb.append(String.format("[%d] %s - %s\n", i + 1, role, timestamp));
            sb.append(message.content()).append("\n");

            if (showSources && message.sources() != null && !message.sources().isBlank()
                    && !"USER".equalsIgnoreCase(message.role())) {
                sb.append("📚 Sources: ").append(message.sources()).append("\n");
            }

            if (i < messages.size() - 1) {
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Formats conversation history in markdown format.
     *
     * @param messages the list of messages
     * @param showSources whether to show sources
     * @return formatted markdown string
     */
    public static String formatHistoryMarkdown(List<CliMessage> messages, boolean showSources) {
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

            sb.append(String.format("## Message %d: %s (%s)\n\n", i + 1, role, timestamp));
            sb.append(message.content()).append("\n\n");

            if (showSources && message.sources() != null && !message.sources().isBlank()
                    && !"USER".equalsIgnoreCase(message.role())) {
                sb.append("**Sources:** ").append(message.sources()).append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * Formats conversation history in JSON format.
     *
     * @param messages the list of messages
     * @return formatted JSON string
     */
    public static String formatHistoryJson(List<CliMessage> messages) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(messages);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize messages: " + e.getMessage() + "\"}";
        }
    }
}
