package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.exception.CliException;
import com.example.ao_wiki_chat.cli.util.ColorPrinter;
import com.example.ao_wiki_chat.cli.util.InputValidator;
import picocli.CommandLine;

import java.util.Scanner;
import java.util.UUID;

/**
 * Command for deleting documents from the WikiChat backend.
 * Supports single document by ID or all documents with --all.
 * Supports confirmation prompt to prevent accidental deletions.
 */
@CommandLine.Command(
        name = "delete",
        description = "Delete a document or all documents (--all)"
)
public class DeleteCommand implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            arity = "0..1",
            description = "Document ID (UUID). Omit when using --all"
    )
    String documentId;

    @CommandLine.Option(
            names = {"--all"},
            description = "Delete all documents"
    )
    boolean deleteAll;

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
            if (deleteAll) {
                runDeleteAll(colorPrinter);
            } else {
                runDeleteOne(colorPrinter);
            }
        } catch (CliException | IllegalArgumentException e) {
            colorPrinter.error("Error: " + e.getMessage());
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
        } catch (ApiException e) {
            colorPrinter.error("API Error: " + e.getMessage());
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
    }

    private void runDeleteAll(ColorPrinter colorPrinter) {
        if (documentId != null && !documentId.isBlank()) {
            colorPrinter.error("Do not specify a document ID when using --all.");
            throw new CommandLine.ParameterException(spec.commandLine(), "Use either document ID or --all, not both.");
        }

        if (!skipConfirmation) {
            System.out.print("Are you sure you want to delete ALL documents? (yes/no): ");
            String confirmation;
            try (Scanner scanner = new Scanner(System.in)) {
                confirmation = scanner.nextLine().trim().toLowerCase();
            }
            if (!"yes".equals(confirmation) && !"y".equals(confirmation)) {
                colorPrinter.warning("Deletion cancelled.");
                return;
            }
        }

        ApiClient apiClient = createApiClient();
        int deleted = apiClient.deleteAllDocuments();
        colorPrinter.success("Deleted " + deleted + " document(s).");
    }

    private void runDeleteOne(ColorPrinter colorPrinter) {
        if (documentId == null || documentId.isBlank()) {
            colorPrinter.error("Document ID is required (or use --all to delete all documents).");
            throw new CommandLine.ParameterException(spec.commandLine(), "Specify document ID or --all.");
        }

        UUID id = InputValidator.validateUuid(documentId);

        if (!skipConfirmation) {
            System.out.print("Are you sure you want to delete document " + id + "? (yes/no): ");
            String confirmation;
            try (Scanner scanner = new Scanner(System.in)) {
                confirmation = scanner.nextLine().trim().toLowerCase();
            }
            if (!"yes".equals(confirmation) && !"y".equals(confirmation)) {
                colorPrinter.warning("Deletion cancelled.");
                return;
            }
        }

        ApiClient apiClient = createApiClient();
        apiClient.deleteDocument(id);
        colorPrinter.success("Document deleted successfully: " + id);
    }
}
