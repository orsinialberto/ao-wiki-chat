package com.example.ao_wiki_chat.cli.command.chat;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import com.example.ao_wiki_chat.cli.model.CliMessage;
import com.example.ao_wiki_chat.cli.output.OutputFormatter;
import com.example.ao_wiki_chat.cli.output.OutputFormatterFactory;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Interactive command for continuous chat in REPL (Read-Eval-Print Loop) mode.
 * Supports special commands for session management, history, and configuration.
 */
@CommandLine.Command(
        name = "interactive",
        aliases = {"i", "repl"},
        description = "Start interactive chat mode (REPL)"
)
public class InteractiveCommand implements Runnable {

    @CommandLine.Option(
            names = {"--session", "-s"},
            description = "Session ID for conversation tracking (auto-generated if not provided)"
    )
    String sessionId;

    @CommandLine.Option(
            names = {"--sources"},
            description = "Show source references in responses by default",
            defaultValue = "false"
    )
    boolean showSources;

    @CommandLine.Option(
            names = {"--prompt"},
            description = "Custom prompt string (default: '> ')",
            defaultValue = "> "
    )
    String prompt;

    @CommandLine.Option(
            names = {"--format", "-f"},
            description = "Output format: table, json, markdown, or plain (default: table)",
            defaultValue = "table"
    )
    String format;

    @CommandLine.Option(
            names = {"--save-session"},
            description = "Save session ID to config for future use",
            defaultValue = "false"
    )
    boolean saveSession;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    boolean running = true;
    boolean sourcesEnabled = false;
    String currentSessionId;
    ApiClient apiClient;
    OutputFormatter formatter;
    Scanner scanner;
    PrintWriter printWriter;
    boolean colorsEnabled;

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

    /**
     * Creates a Scanner for user input. Can be overridden in tests.
     *
     * @return Scanner instance
     */
    Scanner createScanner() {
        return new Scanner(System.in);
    }

    /**
     * Creates a PrintWriter for output. Can be overridden in tests.
     *
     * @return PrintWriter instance
     */
    PrintWriter createPrintWriter() {
        return new PrintWriter(System.out, true);
    }

    @Override
    public void run() {
        try {
            // Initialize components
            ConfigManager configManager = new ConfigManager();
            colorsEnabled = configManager.get().isOutputColors();
            
            if (colorsEnabled) {
                AnsiConsole.systemInstall();
            }

            apiClient = createApiClient();
            formatter = createFormatter();
            scanner = createScanner();
            PrintWriter pw = createPrintWriter();
            if (pw == null) {
                // Fallback to System.out if createPrintWriter returns null (should not happen)
                printWriter = new PrintWriter(System.out, true);
            } else {
                printWriter = pw;
            }

            // Initialize session
            if (sessionId == null || sessionId.isBlank()) {
                currentSessionId = ChatCommandUtils.generateSessionId();
                printColored("📝 Session ID: " + currentSessionId + "\n", Ansi.Color.CYAN);
            } else {
                ChatCommandUtils.validateSessionId(sessionId);
                currentSessionId = sessionId;
            }

            // Set initial sources state
            sourcesEnabled = showSources;

            // Save session if requested
            if (saveSession) {
                saveSessionToConfig();
            }

            // Setup shutdown hook for graceful exit
            setupShutdownHook();

            // Print welcome message
            printWelcome();

            // Main REPL loop
            while (running) {
                try {
                    printPrompt();
                    String input = scanner.nextLine();

                    if (input == null || input.trim().isEmpty()) {
                        continue;
                    }

                    String trimmed = input.trim();

                    // Check for special commands
                    if (trimmed.startsWith("/")) {
                        handleSpecialCommand(trimmed);
                    } else {
                        // Regular query
                        handleQuery(trimmed);
                    }
                } catch (java.util.NoSuchElementException e) {
                    // End of input stream
                    break;
                } catch (Exception e) {
                    if (running) {
                        printError("Error: " + e.getMessage());
                    }
                }
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            if (spec != null && spec.commandLine() != null) {
                throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
            } else {
                throw new RuntimeException("Error: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            if (spec != null && spec.commandLine() != null) {
                throw new CommandLine.ExecutionException(spec.commandLine(), "Unexpected error: " + e.getMessage(), e);
            } else {
                throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
            }
        } finally {
            cleanup();
        }
    }

    /**
     * Sets up a shutdown hook for graceful exit on Ctrl+C.
     */
    private void setupShutdownHook() {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            if (colorsEnabled) {
                AnsiConsole.systemUninstall();
            }
            printWriter.println("\n👋 Goodbye!");
        }));
    }

    /**
     * Prints welcome message.
     */
    private void printWelcome() {
        printWriter.println();
        printColored("🤖 WikiChat Interactive Mode\n", Ansi.Color.CYAN);
        printColored("Type your questions or use /help for commands\n", Ansi.Color.YELLOW);
        printColored("Session: " + currentSessionId + "\n", Ansi.Color.CYAN);
        printWriter.println();
    }

    /**
     * Prints the prompt.
     */
    private void printPrompt() {
        printWriter.print(prompt);
        printWriter.flush();
    }

    /**
     * Handles a regular query.
     *
     * @param query the user query
     */
    private void handleQuery(String query) {
        try {
            ChatCommandUtils.validateQuery(query);

            // Print user message
            printUserMessage(query);

            // Send query
            CliChatResponse response = apiClient.query(query, currentSessionId);

            // Print assistant response
            printAssistantResponse(response);

        } catch (IllegalArgumentException e) {
            printError("Invalid query: " + e.getMessage());
        } catch (ApiException e) {
            printError("API Error: " + e.getMessage());
        } catch (Exception e) {
            printError("Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Handles special commands.
     *
     * @param command the command string (including /)
     */
    private void handleSpecialCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String arg = parts.length > 1 ? parts[1].trim() : "";

        switch (cmd) {
            case "/exit", "/quit" -> handleExit();
            case "/clear" -> handleClear();
            case "/history" -> handleHistory();
            case "/help" -> handleHelp();
            case "/sources" -> handleSources(arg);
            case "/session" -> handleSession(arg);
            default -> printError("Unknown command: " + cmd + ". Type /help for available commands.");
        }
    }

    /**
     * Handles /exit command.
     */
    private void handleExit() {
        running = false;
        printColored("👋 Exiting interactive mode...\n", Ansi.Color.YELLOW);
    }

    /**
     * Handles /clear command.
     */
    private void handleClear() {
        try {
            apiClient.clearHistory(currentSessionId);
            printColored("✅ Conversation history cleared\n", Ansi.Color.GREEN);
        } catch (ApiException e) {
            printError("Failed to clear history: " + e.getMessage());
        }
    }

    /**
     * Handles /history command.
     */
    private void handleHistory() {
        try {
            List<CliMessage> messages = apiClient.getHistory(currentSessionId);
            String formatted = formatter.formatHistory(messages, sourcesEnabled);
            printWriter.println(formatted);
        } catch (ApiException e) {
            printError("Failed to retrieve history: " + e.getMessage());
        }
    }

    /**
     * Handles /help command.
     */
    private void handleHelp() {
        printWriter.println();
        printColored("Available commands:\n", Ansi.Color.CYAN);
        printWriter.println("  /exit, /quit          Exit interactive mode");
        printWriter.println("  /clear                Clear conversation history");
        printWriter.println("  /history              Show conversation history");
        printWriter.println("  /sources on|off       Toggle source display");
        printWriter.println("  /session <id>         Change active session");
        printWriter.println("  /help                 Show this help message");
        printWriter.println();
    }

    /**
     * Handles /sources command.
     *
     * @param arg the argument (on/off)
     */
    private void handleSources(String arg) {
        if (arg.isEmpty()) {
            printWriter.println("Sources display: " + (sourcesEnabled ? "ON" : "OFF"));
            return;
        }

        String lower = arg.toLowerCase();
        if ("on".equals(lower) || "true".equals(lower) || "1".equals(lower)) {
            sourcesEnabled = true;
            printColored("✅ Sources display enabled\n", Ansi.Color.GREEN);
        } else if ("off".equals(lower) || "false".equals(lower) || "0".equals(lower)) {
            sourcesEnabled = false;
            printColored("✅ Sources display disabled\n", Ansi.Color.GREEN);
        } else {
            printError("Invalid argument. Use 'on' or 'off'");
        }
    }

    /**
     * Handles /session command.
     *
     * @param arg the session ID
     */
    private void handleSession(String arg) {
        if (arg.isEmpty()) {
            printWriter.println("Current session: " + currentSessionId);
            return;
        }

        try {
            ChatCommandUtils.validateSessionId(arg);
            currentSessionId = arg;
            printColored("✅ Switched to session: " + currentSessionId + "\n", Ansi.Color.GREEN);
        } catch (IllegalArgumentException e) {
            printError("Invalid session ID: " + e.getMessage());
        }
    }

    /**
     * Prints a user message with color.
     *
     * @param message the message
     */
    private void printUserMessage(String message) {
        if (colorsEnabled) {
            printWriter.println(ansi().fg(Ansi.Color.BLUE).a("👤 You: ").reset().a(message).toString());
        } else {
            printWriter.println("👤 You: " + message);
        }
    }

    /**
     * Prints an assistant response with color.
     *
     * @param response the chat response
     */
    private void printAssistantResponse(CliChatResponse response) {
        if (colorsEnabled) {
            printWriter.println(ansi().fg(Ansi.Color.GREEN).a("🤖 Assistant:\n").reset().toString());
        } else {
            printWriter.println("🤖 Assistant:\n");
        }

        String formatted = formatter.formatChatResponse(response, sourcesEnabled);
        printWriter.println(formatted);
        printWriter.println();
    }

    /**
     * Prints colored text.
     *
     * @param text the text to print
     * @param color the color to use
     */
    private void printColored(String text, Ansi.Color color) {
        if (text == null) {
            return;
        }
        if (colorsEnabled) {
            String colored = ansi().fg(color).a(text).reset().toString();
            printWriter.print(colored != null ? colored : text);
        } else {
            printWriter.print(text);
        }
    }

    /**
     * Prints an error message.
     *
     * @param message the error message
     */
    private void printError(String message) {
        if (colorsEnabled) {
            printWriter.println(ansi().fg(Ansi.Color.RED).a("❌ " + message).reset().toString());
        } else {
            printWriter.println("❌ " + message);
        }
    }

    /**
     * Saves the current session ID to config.
     */
    private void saveSessionToConfig() {
        try {
            ConfigManager configManager = new ConfigManager();
            // Note: ConfigManager doesn't have a direct method to save session,
            // so we'll just print a message. In a real implementation, you might
            // want to add a method to save session ID.
            printColored("💾 Session will be saved (feature not fully implemented)\n", Ansi.Color.YELLOW);
        } catch (Exception e) {
            printError("Failed to save session: " + e.getMessage());
        }
    }

    /**
     * Cleans up resources.
     */
    private void cleanup() {
        if (scanner != null) {
            scanner.close();
        }
        if (colorsEnabled) {
            AnsiConsole.systemUninstall();
        }
    }
}
