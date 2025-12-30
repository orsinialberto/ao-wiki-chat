package com.example.ao_wiki_chat.cli.command.chat;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import picocli.CommandLine;

import java.util.Scanner;

/**
 * Command for clearing conversation history from the WikiChat backend.
 * Requires confirmation before deletion.
 */
@CommandLine.Command(
        name = "clear",
        description = "Delete conversation history for a session"
)
public class ClearCommand implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            description = "Session ID of the conversation to delete"
    )
    String sessionId;

    @CommandLine.Option(
            names = {"--confirm", "-y"},
            description = "Skip confirmation prompt",
            defaultValue = "false"
    )
    boolean confirm;

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
     * Creates a Scanner for user input. Can be overridden in tests.
     *
     * @return Scanner instance
     */
    Scanner createScanner() {
        return new Scanner(System.in);
    }

    @Override
    public void run() {
        try {
            // Validate session ID
            ChatCommandUtils.validateSessionId(sessionId);

            // Request confirmation if not already confirmed
            if (!confirm) {
                System.out.print("⚠️  Are you sure you want to delete conversation history for session " + sessionId + "? (yes/no): ");
                Scanner scanner = createScanner();
                String response = scanner.nextLine().trim().toLowerCase();

                if (!"yes".equals(response) && !"y".equals(response)) {
                    System.out.println("Operation cancelled.");
                    return;
                }
            }

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Delete conversation
            apiClient.clearHistory(sessionId);

            System.out.println("✅ Conversation history deleted successfully for session: " + sessionId);

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
