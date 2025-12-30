package com.example.ao_wiki_chat.cli.command.chat;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import picocli.CommandLine;

/**
 * Command for sending chat queries to the WikiChat backend.
 * Supports session management, source display, and multiple output formats.
 */
@CommandLine.Command(
        name = "query",
        description = "Send a chat query to the WikiChat system"
)
public class QueryCommand implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            description = "The query/question to send"
    )
    String query;

    @CommandLine.Option(
            names = {"--session", "-s"},
            description = "Session ID for conversation tracking (auto-generated if not provided)"
    )
    String sessionId;

    @CommandLine.Option(
            names = {"--sources"},
            description = "Show source references in the response",
            defaultValue = "false"
    )
    boolean showSources;

    @CommandLine.Option(
            names = {"--format", "-f"},
            description = "Output format: text, json, or markdown (default: text)",
            defaultValue = "text"
    )
    String format;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    /**
     * Creates an ApiClient instance. Can be overridden in tests.
     *
     * @return ApiClient instance
     */
    ApiClient createApiClient() {
        ConfigManager configManager = new ConfigManager();
        return new ApiClient(
                configManager.get().getApiUrl(),
                configManager.get().getApiConnectTimeout(),
                configManager.get().getApiReadTimeout(),
                configManager.get().getApiWriteTimeout(),
                false
        );
    }

    @Override
    public void run() {
        try {
            // Validate query
            ChatCommandUtils.validateQuery(query);

            // Validate format
            if (!isValidFormat(format)) {
                throw new IllegalArgumentException(
                        "Invalid format: " + format + ". Must be one of: text, json, markdown"
                );
            }

            // Generate session ID if not provided
            String effectiveSessionId = sessionId;
            if (effectiveSessionId == null || effectiveSessionId.isBlank()) {
                effectiveSessionId = ChatCommandUtils.generateSessionId();
                System.out.println("📝 Session ID: " + effectiveSessionId);
            } else {
                ChatCommandUtils.validateSessionId(effectiveSessionId);
            }

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Send query
            CliChatResponse response = apiClient.query(query, effectiveSessionId);

            // Format and display output
            String output = formatResponse(response, format, showSources);
            System.out.println(output);

        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
        } catch (ApiException e) {
            System.err.println("API Error: " + e.getMessage());
            throw new CommandLine.ExecutionException(spec.commandLine(), "API Error: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new CommandLine.ExecutionException(spec.commandLine(), "Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Formats the response according to the specified format.
     *
     * @param response the chat response
     * @param format the output format
     * @param showSources whether to show sources
     * @return formatted output string
     */
    private String formatResponse(CliChatResponse response, String format, boolean showSources) {
        return switch (format.toLowerCase()) {
            case "json" -> ChatCommandUtils.formatChatResponseJson(response);
            case "markdown" -> ChatCommandUtils.formatChatResponseMarkdown(response, showSources);
            default -> ChatCommandUtils.formatChatResponseText(response, showSources);
        };
    }

    /**
     * Validates the output format.
     *
     * @param format the format to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidFormat(String format) {
        if (format == null || format.isBlank()) {
            return false;
        }
        String lower = format.toLowerCase();
        return "text".equals(lower) || "json".equals(lower) || "markdown".equals(lower);
    }
}
