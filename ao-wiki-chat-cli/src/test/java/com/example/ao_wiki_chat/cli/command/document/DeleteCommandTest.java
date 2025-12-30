package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeleteCommand.
 */
class DeleteCommandTest {

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
    void runWhenConfirmOptionEnabledDeletesWithoutPrompt() {
        // Given
        UUID documentId = UUID.randomUUID();
        doNothing().when(apiClient).deleteDocument(documentId);

        DeleteCommand command = new DeleteCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(documentId.toString(), "--confirm");

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("Document deleted successfully");
        verify(apiClient, times(1)).deleteDocument(documentId);
    }

    @Test
    void runWhenInvalidUuidThrowsException() {
        // Given
        DeleteCommand command = new DeleteCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute("not-a-uuid", "--confirm");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Invalid UUID format");
    }

    @Test
    void runWhenApiExceptionOccursExitsWithError() {
        // Given
        UUID documentId = UUID.randomUUID();
        doThrow(new ApiException("API Error", 404)).when(apiClient).deleteDocument(documentId);

        DeleteCommand command = new DeleteCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute(documentId.toString(), "--confirm");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("API Error");
    }
}
