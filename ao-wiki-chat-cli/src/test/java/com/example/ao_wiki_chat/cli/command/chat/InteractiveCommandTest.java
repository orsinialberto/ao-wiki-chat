package com.example.ao_wiki_chat.cli.command.chat;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.CliConfig;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import com.example.ao_wiki_chat.cli.model.CliMessage;
import com.example.ao_wiki_chat.cli.output.OutputFormatter;
import com.example.ao_wiki_chat.cli.output.OutputFormatterFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InteractiveCommand.
 */
class InteractiveCommandTest {

    private ApiClient apiClient;
    private OutputFormatter formatter;
    private ConfigManager configManager;
    private StringWriter outputWriter;
    private PrintWriter printWriter;

    @BeforeEach
    void setUp() {
        apiClient = mock(ApiClient.class);
        formatter = mock(OutputFormatter.class);
        configManager = mock(ConfigManager.class);
        
        CliConfig config = CliConfig.builder()
                .apiUrl("http://localhost:8080")
                .outputColors(false)
                .build();
        when(configManager.get()).thenReturn(config);
        
        outputWriter = new StringWriter();
        printWriter = new PrintWriter(outputWriter, true);
    }

    @Test
    void runWhenSessionIdNotProvidedGeneratesSessionId() {
        // Given
        String input = "/exit\n";
        
        // Ensure printWriter is initialized
        assertThat(printWriter).isNotNull();
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };

        // When
        command.run();

        // Then
        String output = outputWriter.toString();
        assertThat(output).contains("Session ID:");
    }

    @Test
    void runWhenSessionIdProvidedUsesProvidedSessionId() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String input = "/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.sessionId = sessionId;

        // When
        command.run();

        // Then
        String output = outputWriter.toString();
        assertThat(output).contains(sessionId);
    }

    @Test
    void handleSpecialCommandWhenExitCommandExitsLoop() {
        // Given
        String input = "/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };

        // When
        command.run();

        // Then
        String output = outputWriter.toString();
        assertThat(output).contains("Exiting interactive mode");
        assertThat(command.running).isFalse();
    }

    @Test
    void handleSpecialCommandWhenQuitCommandExitsLoop() {
        // Given
        String input = "/quit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };

        // When
        command.run();

        // Then
        String output = outputWriter.toString();
        assertThat(output).contains("Exiting interactive mode");
        assertThat(command.running).isFalse();
    }

    @Test
    void handleSpecialCommandWhenClearCommandClearsHistory() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String input = "/clear\n/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.sessionId = sessionId;

        // When
        command.run();

        // Then
        verify(InteractiveCommandTest.this.apiClient, times(1)).clearHistory(command.currentSessionId);
        String output = outputWriter.toString();
        assertThat(output).contains("Conversation history cleared");
    }

    @Test
    void handleSpecialCommandWhenHistoryCommandShowsHistory() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        CliMessage message = new CliMessage(
                "Test message",
                "USER",
                LocalDateTime.now(),
                null
        );
        List<CliMessage> messages = List.of(message);
        
        String input = "/history\n/exit\n";
        
        when(InteractiveCommandTest.this.apiClient.getHistory(any())).thenReturn(messages);
        when(InteractiveCommandTest.this.formatter.formatHistory(messages, false)).thenReturn("Formatted history");
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.sessionId = sessionId;

        // When
        command.run();

        // Then
        verify(InteractiveCommandTest.this.apiClient, times(1)).getHistory(command.currentSessionId);
        verify(InteractiveCommandTest.this.formatter, times(1)).formatHistory(messages, false);
        String output = outputWriter.toString();
        assertThat(output).contains("Formatted history");
    }

    @Test
    void handleSpecialCommandWhenHelpCommandShowsHelp() {
        // Given
        String input = "/help\n/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };

        // When
        command.run();

        // Then
        String output = outputWriter.toString();
        assertThat(output).contains("Available commands");
        assertThat(output).contains("/exit");
        assertThat(output).contains("/clear");
        assertThat(output).contains("/history");
        assertThat(output).contains("/sources");
        assertThat(output).contains("/session");
    }

    @Test
    void handleSpecialCommandWhenSourcesOnEnablesSources() {
        // Given
        String input = "/sources on\n/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.sourcesEnabled = false;

        // When
        command.run();

        // Then
        assertThat(command.sourcesEnabled).isTrue();
        String output = outputWriter.toString();
        assertThat(output).contains("Sources display enabled");
    }

    @Test
    void handleSpecialCommandWhenSourcesOffDisablesSources() {
        // Given
        String input = "/sources off\n/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.sourcesEnabled = true;

        // When
        command.run();

        // Then
        assertThat(command.sourcesEnabled).isFalse();
        String output = outputWriter.toString();
        assertThat(output).contains("Sources display disabled");
    }

    @Test
    void handleSpecialCommandWhenSourcesWithoutArgShowsStatus() {
        // Given
        String input = "/sources\n/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.showSources = true; // Set showSources so sourcesEnabled is set to true in run()

        // When
        command.run();

        // Then
        String output = outputWriter.toString();
        assertThat(output).contains("Sources display: ON");
    }

    @Test
    void handleSpecialCommandWhenSessionCommandChangesSession() {
        // Given
        String oldSessionId = UUID.randomUUID().toString();
        String newSessionId = UUID.randomUUID().toString();
        String input = "/session " + newSessionId + "\n/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.sessionId = oldSessionId;

        // When
        command.run();

        // Then
        assertThat(command.currentSessionId).isEqualTo(newSessionId);
        String output = outputWriter.toString();
        assertThat(output).contains("Switched to session: " + newSessionId);
    }

    @Test
    void handleSpecialCommandWhenSessionWithoutArgShowsCurrentSession() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String input = "/session\n/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };

        // When
        command.run();

        // Then
        String output = outputWriter.toString();
        assertThat(output).contains("Current session: " + command.currentSessionId);
    }

    @Test
    void handleSpecialCommandWhenInvalidSessionIdShowsError() {
        // Given
        String input = "/session invalid-id\n/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };

        // When
        command.run();

        // Then
        String output = outputWriter.toString();
        assertThat(output).contains("Invalid session ID");
    }

    @Test
    void handleSpecialCommandWhenUnknownCommandShowsError() {
        // Given
        String input = "/unknown\n/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };

        // When
        command.run();

        // Then
        String output = outputWriter.toString();
        assertThat(output).contains("Unknown command");
        assertThat(output).contains("/help");
    }

    @Test
    void handleQueryWhenValidQuerySendsToApi() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String query = "What is AI?";
        CliChatResponse response = new CliChatResponse("AI is artificial intelligence", null);
        
        String input = query + "\n/exit\n";
        
        when(InteractiveCommandTest.this.apiClient.query(eq(query), any())).thenReturn(response);
        when(InteractiveCommandTest.this.formatter.formatChatResponse(response, false)).thenReturn("AI is artificial intelligence");
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.sessionId = sessionId;

        // When
        command.run();

        // Then
        verify(InteractiveCommandTest.this.apiClient, times(1)).query(eq(query), eq(command.currentSessionId));
        verify(InteractiveCommandTest.this.formatter, times(1)).formatChatResponse(response, false);
        String output = outputWriter.toString();
        assertThat(output).contains("AI is artificial intelligence");
    }

    @Test
    void handleQueryWhenSourcesEnabledShowsSources() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String query = "What is AI?";
        CliChatResponse response = new CliChatResponse("AI is artificial intelligence", null);
        
        String input = query + "\n/exit\n";
        
        when(InteractiveCommandTest.this.apiClient.query(eq(query), any())).thenReturn(response);
        when(InteractiveCommandTest.this.formatter.formatChatResponse(response, true)).thenReturn("Formatted with sources");
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.sessionId = sessionId;
        command.showSources = true; // Set showSources so sourcesEnabled is set to true in run()

        // When
        command.run();

        // Then
        verify(InteractiveCommandTest.this.formatter, times(1)).formatChatResponse(response, true);
        String output = outputWriter.toString();
        assertThat(output).contains("Formatted with sources");
    }

    @Test
    void handleQueryWhenEmptyQueryIsIgnored() {
        // Given
        String input = "\n/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };

        // When
        command.run();

        // Then
        verify(InteractiveCommandTest.this.apiClient, never()).query(any(), any());
    }

    @Test
    void handleQueryWhenApiExceptionShowsError() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String query = "What is AI?";
        
        String input = query + "\n/exit\n";
        
        when(InteractiveCommandTest.this.apiClient.query(eq(query), any())).thenThrow(new ApiException("API Error", 500));
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.sessionId = sessionId;

        // When
        command.run();

        // Then
        String output = outputWriter.toString();
        assertThat(output).contains("API Error");
    }

    @Test
    void handleQueryWhenInvalidQueryShowsError() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        String query = "";
        
        String input = query + "\n/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.sessionId = sessionId;

        // When
        command.run();

        // Then
        verify(InteractiveCommandTest.this.apiClient, never()).query(any(), any());
    }

    @Test
    void runWhenShowSourcesOptionIsTrueEnablesSourcesByDefault() {
        // Given
        String input = "/exit\n";
        
        InteractiveCommand command = new InteractiveCommand() {
            @Override
            ApiClient createApiClient() {
                return InteractiveCommandTest.this.apiClient;
            }

            @Override
            OutputFormatter createFormatter() {
                return InteractiveCommandTest.this.formatter;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream(input.getBytes()));
            }

            @Override
            PrintWriter createPrintWriter() {
                // Capture printWriter from outer scope explicitly
                PrintWriter pw = InteractiveCommandTest.this.printWriter;
                if (pw == null) {
                    throw new IllegalStateException("Test printWriter is null");
                }
                return pw;
            }
        };
        command.showSources = true;

        // When
        command.run();

        // Then
        assertThat(command.sourcesEnabled).isTrue();
    }

}
