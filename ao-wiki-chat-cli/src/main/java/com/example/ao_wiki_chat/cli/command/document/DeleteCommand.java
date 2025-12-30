package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import picocli.CommandLine;

import java.util.Scanner;
import java.util.UUID;

/**
 * Command for deleting documents from the WikiChat backend.
 * Supports confirmation prompt to prevent accidental deletions.
 */
@CommandLine.Command(
        name = "delete",
        description = "Delete a document"
)
public class DeleteCommand implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            description = "Document ID (UUID)"
    )
    String documentId;

    @CommandLine.Option(
            names = {"--confirm"},
            description = "Skip confirmation prompt"
    )
    boolean skipConfirmation;

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

            // Confirm deletion unless --confirm is used
            if (!skipConfirmation) {
                System.out.print("Are you sure you want to delete document " + id + "? (yes/no): ");
                Scanner scanner = new Scanner(System.in);
                String confirmation = scanner.nextLine().trim().toLowerCase();

                if (!"yes".equals(confirmation) && !"y".equals(confirmation)) {
                    System.out.println("Deletion cancelled.");
                    return;
                }
            }

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Delete document
            apiClient.deleteDocument(id);
            System.out.println("Document deleted successfully: " + id);

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
