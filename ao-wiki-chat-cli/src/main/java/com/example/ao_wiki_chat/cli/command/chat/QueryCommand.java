package com.example.ao_wiki_chat.cli.command.chat;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.exception.CliException;
import com.example.ao_wiki_chat.cli.util.InputValidator;
import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import com.example.ao_wiki_chat.cli.output.OutputFormatter;
import com.example.ao_wiki_chat.cli.output.OutputFormatterFactory;
import com.example.ao_wiki_chat.cli.util.ColorPrinter;
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
            description = "Output format: table, json, markdown, or plain (default: table)",
            defaultValue = "table"
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

    /**
     * Creates an OutputFormatter instance. Can be overridden in tests.
     *
     * @return OutputFormatter instance
     */
    OutputFormatter createFormatter() {
        ConfigManager configManager = new ConfigManager();
        return OutputFormatterFactory.create(format, configManager.get());
    }

    /**
     * Creates a ColorPrinter instance. Can be overridden in tests.
     *
     * @param colorsEnabled whether colors are enabled
     * @return ColorPrinter instance
     */
    ColorPrinter createColorPrinter(boolean colorsEnabled) {
        return new ColorPrinter(colorsEnabled);
    }

    @Override
    public void run() {
        ConfigManager configManager = new ConfigManager();
        ColorPrinter colorPrinter = createColorPrinter(configManager.get().isOutputColors());

        try {
            // Validate query
            ChatCommandUtils.validateQuery(query);

            // Validate format
            if (!isValidFormat(format)) {
                throw new IllegalArgumentException(
                        "Invalid format: " + format + ". Must be one of: table, json, markdown, plain"
                );
            }

            // Generate session ID if not provided
            String effectiveSessionId = sessionId;
            if (effectiveSessionId == null || effectiveSessionId.isBlank()) {
                effectiveSessionId = ChatCommandUtils.generateSessionId();
                colorPrinter.info("Session ID: " + effectiveSessionId);
            } else {
                InputValidator.validateSessionId(effectiveSessionId);
            }

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Send query
            CliChatResponse response = apiClient.query(query, effectiveSessionId);

            // Format and display output using formatter
            OutputFormatter formatter = createFormatter();
            System.out.println(formatter.formatChatResponse(response, showSources));

        } catch (CliException | IllegalArgumentException e) {
            colorPrinter.error("Error: " + e.getMessage());
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
        } catch (ApiException e) {
            colorPrinter.error("API Error: " + e.getMessage());
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
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
        return "table".equals(lower) || "text".equals(lower) || "json".equals(lower)
                || "markdown".equals(lower) || "md".equals(lower) || "plain".equals(lower);
    }
}
