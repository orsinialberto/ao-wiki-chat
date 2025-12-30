package com.example.ao_wiki_chat.cli.command.chat;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ClearCommand.
 */
class ClearCommandTest {

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
    void runWhenConfirmTrueDeletesConversation() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        doNothing().when(apiClient).clearHistory(sessionId);

        ClearCommand command = new ClearCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(sessionId, "--confirm");

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("deleted successfully");
        verify(apiClient, times(1)).clearHistory(sessionId);
    }

    @Test
    void runWhenConfirmFalseAndUserConfirmsDeletesConversation() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        doNothing().when(apiClient).clearHistory(sessionId);

        ClearCommand command = new ClearCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream("yes\n".getBytes()));
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(sessionId);

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("deleted successfully");
        verify(apiClient, times(1)).clearHistory(sessionId);
    }

    @Test
    void runWhenConfirmFalseAndUserCancelsDoesNotDelete() {
        // Given
        String sessionId = UUID.randomUUID().toString();

        ClearCommand command = new ClearCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }

            @Override
            Scanner createScanner() {
                return new Scanner(new ByteArrayInputStream("no\n".getBytes()));
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(sessionId);

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("Operation cancelled");
        verify(apiClient, never()).clearHistory(any());
    }

    @Test
    void runWhenSessionIdIsInvalidThrowsException() {
        // Given
        ClearCommand command = new ClearCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("invalid-uuid", "--confirm");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("must be a valid UUID format");
    }

    @Test
    void runWhenApiExceptionOccursExitsWithError() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        doThrow(new ApiException("API Error", 500)).when(apiClient).clearHistory(sessionId);

        ClearCommand command = new ClearCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(sessionId, "--confirm");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("API Error");
    }
}
