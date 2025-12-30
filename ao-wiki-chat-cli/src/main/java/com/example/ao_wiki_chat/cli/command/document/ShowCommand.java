package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliChunk;
import com.example.ao_wiki_chat.cli.model.CliDocument;
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

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Fetch document
            CliDocument document = apiClient.getDocument(id);

            // Format output
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(document);
            } else {
                System.out.println("Document Details:");
                System.out.println(DocumentCommandUtils.formatDocumentDetails(document));

                // Show metadata if requested
                if (showMetadata && document.metadata() != null) {
                    System.out.println();
                    System.out.println("Metadata:");
                    System.out.println(DocumentCommandUtils.formatMetadata(document.metadata()));
                }

                // Show chunks if requested
                if (showChunks) {
                    List<CliChunk> chunks = apiClient.getDocumentChunks(id);
                    System.out.println();
                    System.out.println("Chunks (" + chunks.size() + "):");
                    System.out.println(DocumentCommandUtils.formatChunksTable(chunks, null));
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
