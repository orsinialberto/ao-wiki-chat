package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import com.example.ao_wiki_chat.cli.model.CliDocumentList;
import picocli.CommandLine;

import java.util.List;

/**
 * Command for listing documents from the WikiChat backend.
 * Supports filtering by status and different output formats.
 */
@CommandLine.Command(
        name = "list",
        description = "List all documents"
)
public class ListCommand implements Runnable {

    @CommandLine.Option(
            names = {"--status"},
            description = "Filter by status: PROCESSING, COMPLETED, or FAILED"
    )
    String status;

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
            // Validate status if provided
            if (status != null && !status.isBlank()) {
                DocumentCommandUtils.validateStatus(status);
            }

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Fetch documents
            CliDocumentList documentList = apiClient.listDocuments();
            List<CliDocument> documents = documentList.documents();

            // Filter by status if specified
            if (status != null && !status.isBlank()) {
                documents = DocumentCommandUtils.filterByStatus(documents, status);
            }

            // Format output
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(DocumentCommandUtils.formatAsJson(documents));
            } else {
                System.out.println("Total documents: " + documents.size());
                if (!documents.isEmpty()) {
                    System.out.println();
                    System.out.println(DocumentCommandUtils.formatDocumentsTable(documents));
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
