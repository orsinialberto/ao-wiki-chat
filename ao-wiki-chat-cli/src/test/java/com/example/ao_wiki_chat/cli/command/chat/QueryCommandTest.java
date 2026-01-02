package com.example.ao_wiki_chat.cli.command.chat;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import com.example.ao_wiki_chat.cli.model.CliSourceReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for QueryCommand.
 */
class QueryCommandTest {

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
    void runWhenQueryIsValidAndSessionIdProvidedSendsQuery() {
        // Given
        String sessionId = UUID.randomUUID().toString();
        CliChatResponse response = new CliChatResponse("Test answer", null);
        when(apiClient.query(any(), eq(sessionId))).thenReturn(response);

        QueryCommand command = new QueryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("What is AI?", "--session", sessionId);

        // Then
        assertThat(exitCode).isEqualTo(0);
        assertThat(outputStream.toString()).contains("Test answer");
        verify(apiClient, times(1)).query("What is AI?", sessionId);
    }

    @Test
    void runWhenQueryIsValidAndSessionIdNotProvidedGeneratesSessionId() {
        // Given
        CliChatResponse response = new CliChatResponse("Test answer", null);
        when(apiClient.query(any(), any())).thenReturn(response);

        QueryCommand command = new QueryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("What is AI?");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("Session ID:");
        assertThat(output).contains("Test answer");
        verify(apiClient, times(1)).query(eq("What is AI?"), any());
    }

    @Test
    void runWhenQueryIsEmptyThrowsException() {
        // Given
        QueryCommand command = new QueryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Query cannot be null or empty");
    }

    @Test
    void runWhenQueryExceedsMaxLengthThrowsException() {
        // Given
        String longQuery = "a".repeat(10001);
        QueryCommand command = new QueryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute(longQuery);

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("exceeds maximum length");
    }

    @Test
    void runWhenInvalidSessionIdThrowsException() {
        // Given
        QueryCommand command = new QueryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When - using a session ID with invalid characters (@ is not allowed)
        int exitCode = cmd.execute("What is AI?", "--session", "invalid@session");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Invalid session ID format");
    }

    @Test
    void runWhenInvalidFormatThrowsException() {
        // Given
        QueryCommand command = new QueryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("What is AI?", "--format", "invalid");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Invalid format");
    }

    @Test
    void runWhenShowSourcesTrueDisplaysSources() {
        // Given
        CliSourceReference source = new CliSourceReference(
                "test.pdf", "chunk content", 0.95, 0
        );
        CliChatResponse response = new CliChatResponse("Test answer", List.of(source));
        when(apiClient.query(any(), any())).thenReturn(response);

        QueryCommand command = new QueryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("What is AI?", "--sources");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("Sources");
        assertThat(output).contains("test.pdf");
    }

    @Test
    void runWhenFormatIsJsonOutputsJson() {
        // Given
        CliChatResponse response = new CliChatResponse("Test answer", null);
        when(apiClient.query(any(), any())).thenReturn(response);

        QueryCommand command = new QueryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("What is AI?", "--format", "json");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("\"answer\"");
        assertThat(output).contains("Test answer");
    }

    @Test
    void runWhenFormatIsMarkdownOutputsMarkdown() {
        // Given
        CliChatResponse response = new CliChatResponse("Test answer", null);
        when(apiClient.query(any(), any())).thenReturn(response);

        QueryCommand command = new QueryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("What is AI?", "--format", "markdown");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("## Answer");
        assertThat(output).contains("Test answer");
    }

    @Test
    void runWhenApiExceptionOccursExitsWithError() {
        // Given
        when(apiClient.query(any(), any())).thenThrow(new ApiException("API Error", 500));

        QueryCommand command = new QueryCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("What is AI?");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("API Error");
    }
}
