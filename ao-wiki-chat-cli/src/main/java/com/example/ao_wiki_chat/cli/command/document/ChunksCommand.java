package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliChunk;
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
            description = "Output format: text or json (default: text)",
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
            // Validate UUID
            UUID id = DocumentCommandUtils.validateUuid(documentId);

            // Validate limit if provided
            if (limit != null && limit <= 0) {
                throw new IllegalArgumentException("Limit must be greater than 0");
            }

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Fetch chunks
            List<CliChunk> chunks = apiClient.getDocumentChunks(id);

            // Format output
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(DocumentCommandUtils.formatAsJson(chunks));
            } else {
                System.out.println("Chunks for document " + id + ":");
                System.out.println("Total chunks: " + chunks.size());
                if (!chunks.isEmpty()) {
                    System.out.println();
                    System.out.println(DocumentCommandUtils.formatChunksTable(chunks, limit));
                }
            }

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
}
