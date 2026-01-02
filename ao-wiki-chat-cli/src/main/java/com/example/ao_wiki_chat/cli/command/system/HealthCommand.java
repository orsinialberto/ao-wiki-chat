package com.example.ao_wiki_chat.cli.command.system;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliDatabaseHealthResponse;
import com.example.ao_wiki_chat.cli.model.CliGeminiHealthResponse;
import com.example.ao_wiki_chat.cli.model.CliHealthResponse;
import com.example.ao_wiki_chat.cli.util.ColorPrinter;
import picocli.CommandLine;

/**
 * Command for checking system health status.
 * Supports general health check, database check, and Gemini API check.
 */
@CommandLine.Command(
        name = "health",
        description = "Check system health status"
)
public class HealthCommand implements Runnable, CommandLine.IExitCodeGenerator {

    @CommandLine.Option(
            names = {"--db"},
            description = "Check database health only",
            defaultValue = "false"
    )
    boolean dbOnly;

    @CommandLine.Option(
            names = {"--gemini"},
            description = "Check Gemini API health only",
            defaultValue = "false"
    )
    boolean geminiOnly;

    @CommandLine.Option(
            names = {"--format", "-f"},
            description = "Output format: text or json (default: text)",
            defaultValue = "text"
    )
    String format;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    private int exitCode = 0;

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
            // Validate format
            if (!isValidFormat(format)) {
                throw new IllegalArgumentException(
                        "Invalid format: " + format + ". Must be one of: text, json"
                );
            }

            // Validate that only one specific check is requested at a time
            if (dbOnly && geminiOnly) {
                throw new IllegalArgumentException("Cannot specify both --db and --gemini options");
            }

            ApiClient apiClient = createApiClient();

            if (dbOnly) {
                CliDatabaseHealthResponse response = apiClient.healthDb();
                String output = formatDatabaseHealth(response, format, colorPrinter);
                System.out.println(output);
                exitCode = "UP".equalsIgnoreCase(response.status()) ? 0 : 1;
            } else if (geminiOnly) {
                CliGeminiHealthResponse response = apiClient.healthGemini();
                String output = formatGeminiHealth(response, format, colorPrinter);
                System.out.println(output);
                exitCode = "UP".equalsIgnoreCase(response.status()) ? 0 : 1;
            } else {
                CliHealthResponse response = apiClient.health();
                String output = formatGeneralHealth(response, format, colorPrinter);
                System.out.println(output);
                exitCode = "UP".equalsIgnoreCase(response.status()) ? 0 : 1;
            }

        } catch (IllegalArgumentException e) {
            colorPrinter.error("Error: " + e.getMessage());
            exitCode = 1;
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
        } catch (ApiException e) {
            colorPrinter.error("API Error: " + e.getMessage());
            exitCode = 1;
            throw new CommandLine.ExecutionException(spec.commandLine(), "API Error: " + e.getMessage(), e);
        } catch (Exception e) {
            colorPrinter.error("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            exitCode = 1;
            throw new CommandLine.ExecutionException(spec.commandLine(), "Unexpected error: " + e.getMessage(), e);
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Formats general health response.
     *
     * @param response     the health response
     * @param format       the output format
     * @param colorPrinter the color printer instance
     * @return formatted output string
     */
    private String formatGeneralHealth(CliHealthResponse response, String format, ColorPrinter colorPrinter) {
        if ("json".equalsIgnoreCase(format)) {
            return formatHealthJson(response.status());
        }
        return formatHealthText("System", response.status(), colorPrinter);
    }

    /**
     * Formats database health response.
     *
     * @param response     the database health response
     * @param format       the output format
     * @param colorPrinter the color printer instance
     * @return formatted output string
     */
    private String formatDatabaseHealth(CliDatabaseHealthResponse response, String format, ColorPrinter colorPrinter) {
        if ("json".equalsIgnoreCase(format)) {
            return formatDatabaseHealthJson(response.status(), response.database());
        }
        return formatHealthText("Database", response.status(), colorPrinter);
    }

    /**
     * Formats Gemini health response.
     *
     * @param response     the Gemini health response
     * @param format       the output format
     * @param colorPrinter the color printer instance
     * @return formatted output string
     */
    private String formatGeminiHealth(CliGeminiHealthResponse response, String format, ColorPrinter colorPrinter) {
        if ("json".equalsIgnoreCase(format)) {
            return formatGeminiHealthJson(response.status(), response.gemini());
        }
        return formatHealthText("Gemini", response.status(), colorPrinter);
    }

    /**
     * Formats health status as text with colored output.
     *
     * @param component    the component name
     * @param status       the status value
     * @param colorPrinter the color printer instance
     * @return formatted text
     */
    private String formatHealthText(String component, String status, ColorPrinter colorPrinter) {
        String message = component + ": " + status;
        if ("UP".equalsIgnoreCase(status)) {
            return colorPrinter.formatColored(message, org.fusesource.jansi.Ansi.Color.GREEN, "✓");
        } else {
            return colorPrinter.formatColored(message, org.fusesource.jansi.Ansi.Color.RED, "✗");
        }
    }

    /**
     * Formats general health as JSON.
     *
     * @param status the status value
     * @return JSON string
     */
    private String formatHealthJson(String status) {
        return String.format("{\"status\":\"%s\"}", status);
    }

    /**
     * Formats database health as JSON.
     *
     * @param status   the status value
     * @param database the database value
     * @return JSON string
     */
    private String formatDatabaseHealthJson(String status, String database) {
        return String.format("{\"status\":\"%s\",\"database\":\"%s\"}", status, database);
    }

    /**
     * Formats Gemini health as JSON.
     *
     * @param status the status value
     * @param gemini the gemini value
     * @return JSON string
     */
    private String formatGeminiHealthJson(String status, String gemini) {
        return String.format("{\"status\":\"%s\",\"gemini\":\"%s\"}", status, gemini);
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
        return "text".equals(lower) || "json".equals(lower);
    }
}
