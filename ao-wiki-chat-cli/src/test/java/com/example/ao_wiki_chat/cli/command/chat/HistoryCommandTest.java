package com.example.ao_wiki_chat.cli.command.chat;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HistoryCommand.
 */
class HistoryCommandTest {

    private ApiClient apiClient;
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        apiClient = mock(ApiClient.class);
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void runWhenHistoryExistsDisplaysMessages() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        CliMessage message1 = new CliMessage(
                "User question", "USER", LocalDateTime.now(), null
        );
        CliMessage message2 = new CliMessage(
                "Assistant answer", "ASSISTANT", LocalDateTime.now(), null
        );
        when(apiClient.getHistory(sessionId)).thenReturn(List.of(message1, message2));

        HistoryCommand command = new HistoryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(sessionId);

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("Conversation History");
        assertThat(output).contains("User question");
        assertThat(output).contains("Assistant answer");
        verify(apiClient, times(1)).getHistory(sessionId);
    }

    @Test
    void runWhenHistoryIsEmptyDisplaysMessage() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        when(apiClient.getHistory(sessionId)).thenReturn(List.of());

        HistoryCommand command = new HistoryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(sessionId);

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("No messages");
    }

    @Test
    void runWhenSessionIdIsInvalidThrowsException() {
        // Given
        HistoryCommand command = new HistoryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("invalid-uuid");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("must be a valid UUID format");
    }

    @Test
    void runWhenInvalidFormatThrowsException() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        HistoryCommand command = new HistoryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(sessionId, "--format", "invalid");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Invalid format");
    }

    @Test
    void runWhenFormatIsJsonOutputsJson() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        CliMessage message = new CliMessage(
                "User question", "USER", LocalDateTime.now(), null
        );
        when(apiClient.getHistory(sessionId)).thenReturn(List.of(message));

        HistoryCommand command = new HistoryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(sessionId, "--format", "json");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("\"content\"");
        assertThat(output).contains("User question");
    }

    @Test
    void runWhenFormatIsMarkdownOutputsMarkdown() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        CliMessage message = new CliMessage(
                "User question", "USER", LocalDateTime.now(), null
        );
        when(apiClient.getHistory(sessionId)).thenReturn(List.of(message));

        HistoryCommand command = new HistoryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(sessionId, "--format", "markdown");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("# Conversation History");
        assertThat(output).contains("User question");
    }

    @Test
    void runWhenShowSourcesTrueDisplaysSources() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        CliMessage message = new CliMessage(
                "Assistant answer", "ASSISTANT", LocalDateTime.now(), "source info"
        );
        when(apiClient.getHistory(sessionId)).thenReturn(List.of(message));

        HistoryCommand command = new HistoryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(sessionId, "--sources");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("Sources");
        assertThat(output).contains("source info");
    }

    @Test
    void runWhenApiExceptionOccursExitsWithError() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        when(apiClient.getHistory(sessionId)).thenThrow(new ApiException("API Error", 500));

        HistoryCommand command = new HistoryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(sessionId);

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("API Error");
    }
}
