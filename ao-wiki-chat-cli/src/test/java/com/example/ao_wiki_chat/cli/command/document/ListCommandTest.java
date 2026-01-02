package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import com.example.ao_wiki_chat.cli.model.CliDocumentList;
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
 * Unit tests for ListCommand.
 */
class ListCommandTest {

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
    void runWhenDocumentsExistDisplaysTable() {
        // Given
        CliDocument doc1 = new CliDocument(
                UUID.randomUUID(), "doc1.pdf", "application/pdf", 100L,
                "COMPLETED", LocalDateTime.now(), LocalDateTime.now(), null
        );
        CliDocument doc2 = new CliDocument(
                UUID.randomUUID(), "doc2.pdf", "application/pdf", 200L,
                "PROCESSING", LocalDateTime.now(), LocalDateTime.now(), null
        );

        CliDocumentList documentList = new CliDocumentList(List.of(doc1, doc2), 2);
        when(apiClient.listDocuments()).thenReturn(documentList);

        ListCommand command = new ListCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };
        command.format = "text";

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute();

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("ID");
        assertThat(outputStream.toString()).contains("Filename");
        assertThat(outputStream.toString()).contains("doc1.pdf");
        assertThat(outputStream.toString()).contains("doc2.pdf");
        verify(apiClient, times(1)).listDocuments();
    }

    @Test
    void runWhenStatusFilterProvidedFiltersDocuments() {
        // Given
        CliDocument doc1 = new CliDocument(
                UUID.randomUUID(), "doc1.pdf", "application/pdf", 100L,
                "COMPLETED", LocalDateTime.now(), LocalDateTime.now(), null
        );
        CliDocument doc2 = new CliDocument(
                UUID.randomUUID(), "doc2.pdf", "application/pdf", 200L,
                "PROCESSING", LocalDateTime.now(), LocalDateTime.now(), null
        );

        CliDocumentList documentList = new CliDocumentList(List.of(doc1, doc2), 2);
        when(apiClient.listDocuments()).thenReturn(documentList);

        ListCommand command = new ListCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute("--status", "COMPLETED", "--format", "text");

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("doc1.pdf");
        assertThat(outputStream.toString()).doesNotContain("doc2.pdf");
        verify(apiClient, times(1)).listDocuments();
    }

    @Test
    void runWhenInvalidStatusThrowsException() {
        // Given
        ListCommand command = new ListCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };
        command.status = "INVALID";

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute();

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Invalid status");
    }

    @Test
    void runWhenApiExceptionOccursExitsWithError() {
        // Given
        when(apiClient.listDocuments()).thenThrow(new ApiException("API Error", 500));

        ListCommand command = new ListCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute();

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("API Error");
    }

    @Test
    void runWhenNoDocumentsDisplaysMessage() {
        // Given
        CliDocumentList documentList = new CliDocumentList(List.of(), 0);
        when(apiClient.listDocuments()).thenReturn(documentList);

        ListCommand command = new ListCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };
        command.format = "text";

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute();

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("No documents found");
    }
}
