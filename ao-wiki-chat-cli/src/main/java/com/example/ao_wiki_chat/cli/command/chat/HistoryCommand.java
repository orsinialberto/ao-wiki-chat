package com.example.ao_wiki_chat.cli.command.chat;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliMessage;
import picocli.CommandLine;

import java.util.List;

/**
 * Command for retrieving conversation history from the WikiChat backend.
 * Supports different output formats and optional source display.
 */
@CommandLine.Command(
        name = "history",
        description = "Show conversation history for a session"
)
public class HistoryCommand implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            description = "Session ID of the conversation"
    )
    String sessionId;

    @CommandLine.Option(
            names = {"--format", "-f"},
            description = "Output format: text, json, or markdown (default: text)",
            defaultValue = "text"
    )
    String format;

    @CommandLine.Option(
            names = {"--sources"},
            description = "Show source references in messages",
            defaultValue = "false"
    )
    boolean showSources;

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
            // Validate session ID
            ChatCommandUtils.validateSessionId(sessionId);

            // Validate format
            if (!isValidFormat(format)) {
                throw new IllegalArgumentException(
                        "Invalid format: " + format + ". Must be one of: text, json, markdown"
                );
            }

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Fetch history
            List<CliMessage> messages = apiClient.getHistory(sessionId);

            // Format and display output
            String output = formatHistory(messages, format, showSources);
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
     * Formats the conversation history according to the specified format.
     *
     * @param messages the list of messages
     * @param format the output format
     * @param showSources whether to show sources
     * @return formatted output string
     */
    private String formatHistory(List<CliMessage> messages, String format, boolean showSources) {
        return switch (format.toLowerCase()) {
            case "json" -> ChatCommandUtils.formatHistoryJson(messages);
            case "markdown" -> ChatCommandUtils.formatHistoryMarkdown(messages, showSources);
            default -> ChatCommandUtils.formatHistoryText(messages, showSources);
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
