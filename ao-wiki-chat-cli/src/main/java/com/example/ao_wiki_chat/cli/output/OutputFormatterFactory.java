package com.example.ao_wiki_chat.cli.output;

import com.example.ao_wiki_chat.cli.config.CliConfig;

/**
 * Factory for creating OutputFormatter instances based on format string and configuration.
 */
public final class OutputFormatterFactory {

    private OutputFormatterFactory() {
        // Utility class
    }

    /**
     * Creates an OutputFormatter based on the format string and configuration.
     *
     * @param format the format string (table, json, markdown, plain, or text)
     * @param config the CLI configuration (for color settings)
     * @return an OutputFormatter instance
     * @throws IllegalArgumentException if the format is invalid
     */
    public static OutputFormatter create(String format, CliConfig config) {
        if (format == null || format.isBlank()) {
            format = config.getOutputFormat();
        }

        String normalizedFormat = format.toLowerCase().trim();

        return switch (normalizedFormat) {
            case "table", "text" -> new TableFormatter(config.isOutputColors());
            case "json" -> new JsonFormatter();
            case "markdown", "md" -> new MarkdownFormatter();
            case "plain" -> new PlainFormatter();
            default -> throw new IllegalArgumentException(
                    "Invalid format: " + format + ". Must be one of: table, json, markdown, plain, text"
            );
        };
    }

    /**
     * Creates an OutputFormatter with default configuration.
     *
     * @param format the format string
     * @return an OutputFormatter instance
     */
    public static OutputFormatter create(String format) {
        return create(format, CliConfig.defaults());
    }
}
