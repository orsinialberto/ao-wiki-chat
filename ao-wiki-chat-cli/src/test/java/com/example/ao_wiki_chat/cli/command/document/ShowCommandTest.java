package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliChunk;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ShowCommand.
 */
class ShowCommandTest {

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
    void runWhenDocumentExistsDisplaysDetails() {
        // Given
        UUID documentId = UUID.randomUUID();
        CliDocument document = new CliDocument(
                documentId, "test.pdf", "application/pdf", 100L,
                "COMPLETED", LocalDateTime.now(), LocalDateTime.now(), null
        );

        when(apiClient.getDocument(documentId)).thenReturn(document);

        ShowCommand command = new ShowCommand() {
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
        assertThat(outputStream.toString()).contains("Document Details");
        assertThat(outputStream.toString()).contains("test.pdf");
        verify(apiClient, times(1)).getDocument(documentId);
    }

    @Test
    void runWhenChunksOptionEnabledDisplaysChunks() {
        // Given
        UUID documentId = UUID.randomUUID();
        CliDocument document = new CliDocument(
                documentId, "test.pdf", "application/pdf", 100L,
                "COMPLETED", LocalDateTime.now(), LocalDateTime.now(), null
        );

        CliChunk chunk = new CliChunk(
                UUID.randomUUID(), documentId, "chunk content", 0, LocalDateTime.now()
        );

        when(apiClient.getDocument(documentId)).thenReturn(document);
        when(apiClient.getDocumentChunks(documentId)).thenReturn(List.of(chunk));

        ShowCommand command = new ShowCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute(documentId.toString(), "--chunks", "--format", "text");

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("Chunks");
        verify(apiClient, times(1)).getDocumentChunks(documentId);
    }

    @Test
    void runWhenMetadataOptionEnabledDisplaysMetadata() {
        // Given
        UUID documentId = UUID.randomUUID();
        Map<String, Object> metadata = Map.of("key1", "value1", "key2", "value2");
        CliDocument document = new CliDocument(
                documentId, "test.pdf", "application/pdf", 100L,
                "COMPLETED", LocalDateTime.now(), LocalDateTime.now(), metadata
        );

        when(apiClient.getDocument(documentId)).thenReturn(document);

        ShowCommand command = new ShowCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute(documentId.toString(), "--metadata", "--format", "text");

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("Metadata");
        verify(apiClient, times(1)).getDocument(documentId);
    }

    @Test
    void runWhenInvalidUuidThrowsException() {
        // Given
        ShowCommand command = new ShowCommand() {
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
    void runWhenApiExceptionOccursExitsWithError() {
        // Given
        UUID documentId = UUID.randomUUID();
        when(apiClient.getDocument(documentId)).thenThrow(new ApiException("API Error", 404));

        ShowCommand command = new ShowCommand() {
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
}
