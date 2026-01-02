package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.exception.CliException;
import com.example.ao_wiki_chat.cli.util.InputValidator;
import com.example.ao_wiki_chat.cli.model.CliChunk;
import com.example.ao_wiki_chat.cli.output.OutputFormatter;
import com.example.ao_wiki_chat.cli.output.OutputFormatterFactory;
import picocli.CommandLine;

import java.util.List;
import java.util.UUID;

/**
 * Command for listing chunks of a document.
 * Supports limiting the number of chunks displayed and different output formats.
 */
@CommandLine.Command(
        name = "chunks",
        description = "List chunks for a document"
)
public class ChunksCommand implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            description = "Document ID (UUID)"
    )
    String documentId;

    @CommandLine.Option(
            names = {"--limit"},
            description = "Maximum number of chunks to display (default: all)"
    )
    Integer limit;

    @CommandLine.Option(
            names = {"--format"},
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

    @Override
    public void run() {
        try {
            // Validate UUID
            UUID id = InputValidator.validateUuid(documentId);

            // Validate limit if provided
            if (limit != null && limit <= 0) {
                throw new IllegalArgumentException("Limit must be greater than 0");
            }

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Fetch chunks
            List<CliChunk> chunks = apiClient.getDocumentChunks(id);

            // Format output using formatter
            OutputFormatter formatter = createFormatter();
            System.out.println(formatter.formatChunks(chunks, limit));

        } catch (CliException | IllegalArgumentException e) {
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
        } catch (ApiException e) {
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
    }
}
