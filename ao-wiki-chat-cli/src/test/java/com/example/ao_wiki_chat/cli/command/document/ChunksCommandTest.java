package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChunksCommand.
 */
class ChunksCommandTest {

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
    void runWhenChunksExistDisplaysTable() {
        // Given
        UUID documentId = UUID.randomUUID();
        CliChunk chunk1 = new CliChunk(
                UUID.randomUUID(), documentId, "chunk content 1", 0, LocalDateTime.now()
        );
        CliChunk chunk2 = new CliChunk(
                UUID.randomUUID(), documentId, "chunk content 2", 1, LocalDateTime.now()
        );

        when(apiClient.getDocumentChunks(documentId)).thenReturn(List.of(chunk1, chunk2));

        ChunksCommand command = new ChunksCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(documentId.toString(), "--format", "text");

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("Index");
        assertThat(outputStream.toString()).contains("chunk content");
        verify(apiClient, times(1)).getDocumentChunks(documentId);
    }

    @Test
    void runWhenLimitProvidedLimitsOutput() {
        // Given
        UUID documentId = UUID.randomUUID();
        CliChunk chunk1 = new CliChunk(
                UUID.randomUUID(), documentId, "chunk content 1", 0, LocalDateTime.now()
        );
        CliChunk chunk2 = new CliChunk(
                UUID.randomUUID(), documentId, "chunk content 2", 1, LocalDateTime.now()
        );
        CliChunk chunk3 = new CliChunk(
                UUID.randomUUID(), documentId, "chunk content 3", 2, LocalDateTime.now()
        );

        when(apiClient.getDocumentChunks(documentId)).thenReturn(List.of(chunk1, chunk2, chunk3));

        ChunksCommand command = new ChunksCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute(documentId.toString(), "--limit", "2", "--format", "text");

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("... and 1 more chunks");
        verify(apiClient, times(1)).getDocumentChunks(documentId);
    }

    @Test
    void runWhenInvalidUuidThrowsException() {
        // Given
        ChunksCommand command = new ChunksCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute("not-a-uuid");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Invalid UUID format");
    }

    @Test
    void runWhenInvalidLimitThrowsException() {
        // Given
        UUID documentId = UUID.randomUUID();
        ChunksCommand command = new ChunksCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute(documentId.toString(), "--limit", "-1");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Limit must be greater than 0");
    }

    @Test
    void runWhenApiExceptionOccursExitsWithError() {
        // Given
        UUID documentId = UUID.randomUUID();
        when(apiClient.getDocumentChunks(documentId)).thenThrow(new ApiException("API Error", 404));

        ChunksCommand command = new ChunksCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute(documentId.toString());

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("API Error");
    }

    @Test
    void runWhenNoChunksDisplaysMessage() {
        // Given
        UUID documentId = UUID.randomUUID();
        when(apiClient.getDocumentChunks(documentId)).thenReturn(List.of());

        ChunksCommand command = new ChunksCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(documentId.toString(), "--format", "text");

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("No chunks found");
    }
}
