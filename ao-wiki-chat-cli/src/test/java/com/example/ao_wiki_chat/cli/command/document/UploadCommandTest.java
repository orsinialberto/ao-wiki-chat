package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import com.example.ao_wiki_chat.cli.model.CliDocumentUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UploadCommand.
 */
class UploadCommandTest {

    @TempDir
    Path tempDir;

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
    void runWhenFileExistsUploadsSuccessfully() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.pdf");
        Files.write(testFile, "test content".getBytes());

        CliDocumentUpload uploadResponse = new CliDocumentUpload(
                UUID.randomUUID(), "test.pdf", "application/pdf",
                12L, "PROCESSING", LocalDateTime.now()
        );

        when(apiClient.uploadDocument(any())).thenReturn(uploadResponse);

        UploadCommand command = new UploadCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(testFile.toString(), "--format", "text");

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("Document uploaded successfully");
        assertThat(outputStream.toString()).contains("Document ID");
        verify(apiClient, times(1)).uploadDocument(any());
    }

    @Test
    void runWhenFileNotFoundThrowsException() {
        // Given
        UploadCommand command = new UploadCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute("/nonexistent/file.pdf");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("File not found");
    }

    @Test
    void runWhenApiExceptionOccursExitsWithError() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.pdf");
        Files.write(testFile, "test content".getBytes());

        when(apiClient.uploadDocument(any())).thenThrow(new ApiException("API Error", 500));

        UploadCommand command = new UploadCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute(testFile.toString());

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("API Error");
    }

    @Test
    void runWhenWaitEnabledPollsStatus() throws Exception {
        // Given
        Path testFile = tempDir.resolve("test.pdf");
        Files.write(testFile, "test content".getBytes());

        UUID documentId = UUID.randomUUID();
        CliDocumentUpload uploadResponse = new CliDocumentUpload(
                documentId, "test.pdf", "application/pdf",
                12L, "PROCESSING", LocalDateTime.now()
        );

        CliDocument processingDoc = new CliDocument(
                documentId, "test.pdf", "application/pdf", 12L,
                "PROCESSING", LocalDateTime.now(), LocalDateTime.now(), null
        );

        CliDocument completedDoc = new CliDocument(
                documentId, "test.pdf", "application/pdf", 12L,
                "COMPLETED", LocalDateTime.now(), LocalDateTime.now(), null
        );

        when(apiClient.uploadDocument(any())).thenReturn(uploadResponse);
        when(apiClient.getDocument(documentId))
                .thenReturn(processingDoc)
                .thenReturn(completedDoc);

        UploadCommand command = new UploadCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);
        cmd.setOut(new PrintWriter(new PrintStream(outputStream)));
        cmd.setErr(new PrintWriter(new PrintStream(errorStream)));

        // When
        int exitCode = cmd.execute(testFile.toString(), "--wait", "--timeout", "10");

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("Document processing completed");
        verify(apiClient, atLeastOnce()).getDocument(documentId);
    }
}
