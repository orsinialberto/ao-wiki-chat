package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.exception.CliException;
import com.example.ao_wiki_chat.cli.util.InputValidator;
import com.example.ao_wiki_chat.cli.model.CliChunk;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import com.example.ao_wiki_chat.cli.output.OutputFormatter;
import com.example.ao_wiki_chat.cli.output.OutputFormatterFactory;
import picocli.CommandLine;

import java.util.List;
import java.util.UUID;

/**
 * Command for showing document details.
 * Supports displaying chunks and metadata.
 */
@CommandLine.Command(
        name = "show",
        description = "Show detailed information about a document"
)
public class ShowCommand implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            description = "Document ID (UUID)"
    )
    String documentId;

    @CommandLine.Option(
            names = {"--chunks"},
            description = "Also display document chunks"
    )
    boolean showChunks;

    @CommandLine.Option(
            names = {"--metadata"},
            description = "Also display document metadata"
    )
    boolean showMetadata;

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

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Fetch document
            CliDocument document = apiClient.getDocument(id);

            // Format output using formatter
            OutputFormatter formatter = createFormatter();
            System.out.println(formatter.formatDocument(document));

            // Show metadata if requested
            if (showMetadata && document.metadata() != null) {
                System.out.println();
                System.out.println(formatter.formatMetadata(document.metadata()));
            }

            // Show chunks if requested
            if (showChunks) {
                List<CliChunk> chunks = apiClient.getDocumentChunks(id);
                System.out.println();
                System.out.println(formatter.formatChunks(chunks, null));
            }

        } catch (CliException | IllegalArgumentException e) {
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
        } catch (ApiException e) {
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
    }
}
