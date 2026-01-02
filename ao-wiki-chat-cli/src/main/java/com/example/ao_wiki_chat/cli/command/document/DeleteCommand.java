package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.util.ColorPrinter;
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
            // Validate UUID
            UUID id = DocumentCommandUtils.validateUuid(documentId);

            // Confirm deletion unless --confirm is used
            if (!skipConfirmation) {
                System.out.print("Are you sure you want to delete document " + id + "? (yes/no): ");
                Scanner scanner = new Scanner(System.in);
                String confirmation = scanner.nextLine().trim().toLowerCase();

                if (!"yes".equals(confirmation) && !"y".equals(confirmation)) {
                    colorPrinter.warning("Deletion cancelled.");
                    return;
                }
            }

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Delete document
            apiClient.deleteDocument(id);
            colorPrinter.success("Document deleted successfully: " + id);

        } catch (IllegalArgumentException e) {
            colorPrinter.error("Error: " + e.getMessage());
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
        } catch (ApiException e) {
            colorPrinter.error("API Error: " + e.getMessage());
            throw new CommandLine.ExecutionException(spec.commandLine(), "API Error: " + e.getMessage(), e);
        } catch (Exception e) {
            colorPrinter.error("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new CommandLine.ExecutionException(spec.commandLine(), "Unexpected error: " + e.getMessage(), e);
        }
    }
}
