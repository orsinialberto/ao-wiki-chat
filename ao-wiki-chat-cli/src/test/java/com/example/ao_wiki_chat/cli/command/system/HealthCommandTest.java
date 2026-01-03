package com.example.ao_wiki_chat.cli.command.system;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliDatabaseHealthResponse;
import com.example.ao_wiki_chat.cli.model.CliGeminiHealthResponse;
import com.example.ao_wiki_chat.cli.model.CliHealthResponse;
import com.example.ao_wiki_chat.cli.util.ColorPrinter;

import picocli.CommandLine;

/**
 * Unit tests for HealthCommand.
 */
class HealthCommandTest {

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
    void runWhenGeneralHealthCheckReturnsUpExitsWithZero() {
        // Given
        CliHealthResponse response = new CliHealthResponse("UP");
        when(apiClient.health()).thenReturn(response);

        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute();

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("✓");
        assertThat(output).contains("System");
        assertThat(output).contains("UP");
        verify(apiClient, times(1)).health();
    }

    @Test
    void runWhenGeneralHealthCheckReturnsDownExitsWithOne() {
        // Given
        CliHealthResponse response = new CliHealthResponse("DOWN");
        when(apiClient.health()).thenReturn(response);

        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute();

        // Then
        assertThat(exitCode).isEqualTo(1);
        String output = outputStream.toString();
        assertThat(output).contains("✗");
        assertThat(output).contains("System");
        assertThat(output).contains("DOWN");
        verify(apiClient, times(1)).health();
    }

    @Test
    void runWhenDatabaseHealthCheckReturnsUpExitsWithZero() {
        // Given
        CliDatabaseHealthResponse response = new CliDatabaseHealthResponse("UP", "PostgreSQL");
        when(apiClient.healthDb()).thenReturn(response);

        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("--db");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("✓");
        assertThat(output).contains("Database");
        assertThat(output).contains("UP");
        verify(apiClient, times(1)).healthDb();
        verify(apiClient, never()).health();
        verify(apiClient, never()).healthGemini();
    }

    @Test
    void runWhenDatabaseHealthCheckReturnsDownExitsWithOne() {
        // Given
        CliDatabaseHealthResponse response = new CliDatabaseHealthResponse("DOWN", "PostgreSQL");
        when(apiClient.healthDb()).thenReturn(response);

        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("--db");

        // Then
        assertThat(exitCode).isEqualTo(1);
        String output = outputStream.toString();
        assertThat(output).contains("✗");
        assertThat(output).contains("Database");
        assertThat(output).contains("DOWN");
    }

    @Test
    void runWhenGeminiHealthCheckReturnsUpExitsWithZero() {
        // Given
        CliGeminiHealthResponse response = new CliGeminiHealthResponse("UP", "OK");
        when(apiClient.healthGemini()).thenReturn(response);

        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("--gemini");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("✓");
        assertThat(output).contains("Gemini");
        assertThat(output).contains("UP");
        verify(apiClient, times(1)).healthGemini();
        verify(apiClient, never()).health();
        verify(apiClient, never()).healthDb();
    }

    @Test
    void runWhenGeminiHealthCheckReturnsDownExitsWithOne() {
        // Given
        CliGeminiHealthResponse response = new CliGeminiHealthResponse("DOWN", "ERROR");
        when(apiClient.healthGemini()).thenReturn(response);

        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("--gemini");

        // Then
        assertThat(exitCode).isEqualTo(1);
        String output = outputStream.toString();
        assertThat(output).contains("✗");
        assertThat(output).contains("Gemini");
        assertThat(output).contains("DOWN");
    }

    @Test
    void runWhenFormatIsJsonOutputsJson() {
        // Given
        CliHealthResponse response = new CliHealthResponse("UP");
        when(apiClient.health()).thenReturn(response);

        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("--format", "json");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString().trim();
        assertThat(output).contains("\"status\"");
        assertThat(output).contains("\"UP\"");
        assertThat(output).startsWith("{");
        assertThat(output).endsWith("}");
    }

    @Test
    void runWhenFormatIsJsonAndDatabaseCheckOutputsJson() {
        // Given
        CliDatabaseHealthResponse response = new CliDatabaseHealthResponse("UP", "PostgreSQL");
        when(apiClient.healthDb()).thenReturn(response);

        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }

            @Override
            ColorPrinter createColorPrinter(boolean colorsEnabled) {
                return new ColorPrinter(false);
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("--db", "--format", "json");

        // Then
        assertThat(exitCode)
                .as("Exit code should be 0. Error stream: " + errorStream.toString())
                .isEqualTo(0);
        String output = outputStream.toString().trim();
        assertThat(output)
                .as("Output should not be empty. Error stream: " + errorStream.toString())
                .isNotEmpty();
        assertThat(output).contains("\"status\"");
        assertThat(output).contains("\"database\"");
        assertThat(output).contains("\"UP\"");
        assertThat(output).contains("\"PostgreSQL\"");
    }

    @Test
    void runWhenFormatIsJsonAndGeminiCheckOutputsJson() {
        // Given
        CliGeminiHealthResponse response = new CliGeminiHealthResponse("UP", "OK");
        when(apiClient.healthGemini()).thenReturn(response);

        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("--gemini", "--format", "json");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString().trim();
        assertThat(output).contains("\"status\"");
        assertThat(output).contains("\"gemini\"");
        assertThat(output).contains("\"UP\"");
        assertThat(output).contains("\"OK\"");
    }

    @Test
    void runWhenInvalidFormatThrowsException() {
        // Given
        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("--format", "invalid");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Invalid format");
    }

    @Test
    void runWhenBothDbAndGeminiOptionsSpecifiedThrowsException() {
        // Given
        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("--db", "--gemini");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Cannot specify both --db and --gemini");
    }

    @Test
    void runWhenApiExceptionOccursExitsWithError() {
        // Given
        when(apiClient.health()).thenThrow(new ApiException("API Error", 500));

        HealthCommand command = new HealthCommand() {
            @Override
            ApiClient createApiClient() {
                return apiClient;
            }
        };

        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute();

        // Then
        assertThat(exitCode).isEqualTo(1);
        assertThat(errorStream.toString()).contains("API Error");
    }
}
