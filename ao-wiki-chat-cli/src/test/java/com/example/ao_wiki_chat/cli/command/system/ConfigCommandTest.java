package com.example.ao_wiki_chat.cli.command.system;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.ao_wiki_chat.cli.config.ConfigManager;

import picocli.CommandLine;

/**
 * Unit tests for ConfigCommand and its subcommands.
 */
class ConfigCommandTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private Path tempConfigDir;
    private String originalUserHome;

    @BeforeEach
    void setUp() throws IOException {
        // Create temporary directory for config file
        tempConfigDir = Files.createTempDirectory("wikichat-test-");

        // Save original user.home and set it to our temp directory
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempConfigDir.toString());

        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    void tearDown() {
        // Restore original user.home
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }

        System.setOut(originalOut);
        System.setErr(originalErr);

        // Clean up temp directory
        try {
            if (Files.exists(tempConfigDir)) {
                deleteRecursively(tempConfigDir);
            }
        } catch (IOException e) {
            // Ignore cleanup errors
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.list(path).forEach(p -> {
                try {
                    deleteRecursively(p);
                } catch (IOException e) {
                    // Ignore
                }
            });
        }
        Files.deleteIfExists(path);
    }

    @Test
    void setCommandWhenKeyAndValueAreValidUpdatesConfig() {
        // Given
        ConfigCommand.SetCommand command = new ConfigCommand.SetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("api.url", "http://example.com");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("Configuration updated");
        assertThat(output).contains("api.url = http://example.com");

        ConfigManager configManager = new ConfigManager();
        assertThat(configManager.get().getApiUrl()).isEqualTo("http://example.com");
    }

    @Test
    void setCommandWhenKeyIsEmptyThrowsException() {
        // Given
        ConfigCommand.SetCommand command = new ConfigCommand.SetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("", "value");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Configuration key cannot be empty");
    }

    @Test
    void setCommandWhenValueIsEmptyThrowsException() {
        // Given
        ConfigCommand.SetCommand command = new ConfigCommand.SetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("api.url", "");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Configuration value cannot be empty");
    }

    @Test
    void setCommandWhenUnknownKeyThrowsException() {
        // Given
        ConfigCommand.SetCommand command = new ConfigCommand.SetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("unknown.key", "value");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Unknown configuration key");
    }

    @Test
    void setCommandWhenTimeoutIsValidUpdatesConfig() {
        // Given
        ConfigCommand.SetCommand command = new ConfigCommand.SetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("api.timeout.connect", "45");

        // Then
        assertThat(exitCode).isEqualTo(0);
        ConfigManager configManager = new ConfigManager();
        assertThat(configManager.get().getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(45));
    }

    @Test
    void setCommandWhenOutputFormatIsValidUpdatesConfig() {
        // Given
        ConfigCommand.SetCommand command = new ConfigCommand.SetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("output.format", "json");

        // Then
        assertThat(exitCode).isEqualTo(0);
        ConfigManager configManager = new ConfigManager();
        assertThat(configManager.get().getOutputFormat()).isEqualTo("json");
    }

    @Test
    void getCommandWhenKeyIsValidReturnsValue() {
        // Given
        ConfigManager configManager = new ConfigManager();
        configManager.set("api.url", "http://test.com");

        ConfigCommand.GetCommand command = new ConfigCommand.GetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("api.url");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString().trim();
        assertThat(output).isEqualTo("http://test.com");
    }

    @Test
    void getCommandWhenKeyIsEmptyThrowsException() {
        // Given
        ConfigCommand.GetCommand command = new ConfigCommand.GetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Configuration key cannot be empty");
    }

    @Test
    void getCommandWhenUnknownKeyThrowsException() {
        // Given
        ConfigCommand.GetCommand command = new ConfigCommand.GetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("unknown.key");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Unknown configuration key");
    }

    @Test
    void getCommandWhenTimeoutKeyReturnsSeconds() {
        // Given
        ConfigManager configManager = new ConfigManager();
        configManager.set("api.timeout.connect", "45");

        ConfigCommand.GetCommand command = new ConfigCommand.GetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("api.timeout.connect");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString().trim();
        assertThat(output).isEqualTo("45");
    }

    @Test
    void getCommandWhenOutputColorsKeyReturnsBoolean() {
        // Given
        ConfigManager configManager = new ConfigManager();
        configManager.set("output.colors", "false");

        ConfigCommand.GetCommand command = new ConfigCommand.GetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("output.colors");

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString().trim();
        assertThat(output).isEqualTo("false");
    }

    @Test
    void listCommandWhenCalledDisplaysAllConfiguration() {
        // Given
        ConfigManager configManager = new ConfigManager();
        configManager.set("api.url", "http://example.com");
        configManager.set("output.format", "json");

        ConfigCommand.ListCommand command = new ConfigCommand.ListCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute();

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("Configuration:");
        assertThat(output).contains("api.url = http://example.com");
        assertThat(output).contains("api.timeout.connect");
        assertThat(output).contains("api.timeout.read");
        assertThat(output).contains("api.timeout.write");
        assertThat(output).contains("output.format = json");
        assertThat(output).contains("output.colors");
    }

    @Test
    void resetCommandWhenCalledResetsToDefaults() {
        // Given
        ConfigManager configManager = new ConfigManager();
        configManager.set("api.url", "http://custom.com");
        configManager.set("api.timeout.connect", "100");
        configManager.set("output.format", "json");

        ConfigCommand.ResetCommand command = new ConfigCommand.ResetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute();

        // Then
        assertThat(exitCode).isEqualTo(0);
        String output = outputStream.toString();
        assertThat(output).contains("Configuration reset to defaults");

        ConfigManager newConfigManager = new ConfigManager();
        assertThat(newConfigManager.get().getApiUrl()).isEqualTo("http://localhost:8080");
        assertThat(newConfigManager.get().getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(newConfigManager.get().getApiReadTimeout()).isEqualTo(Duration.ofSeconds(300));
        assertThat(newConfigManager.get().getApiWriteTimeout()).isEqualTo(Duration.ofSeconds(300));
        assertThat(newConfigManager.get().getOutputFormat()).isEqualTo("text");
    }

    @Test
    void setCommandWhenInvalidApiUrlThrowsException() {
        // Given
        ConfigCommand.SetCommand command = new ConfigCommand.SetCommand();
        CommandLine cmd = new CommandLine(command);

        // When - using a URL that fails URI parsing
        int exitCode = cmd.execute("api.url", "http://example.com:invalid port");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Invalid API URL format");
    }

    @Test
    void setCommandWhenApiUrlHasInvalidSchemeThrowsException() {
        // Given
        ConfigCommand.SetCommand command = new ConfigCommand.SetCommand();
        CommandLine cmd = new CommandLine(command);

        // When - using a URL with invalid scheme
        int exitCode = cmd.execute("api.url", "ftp://example.com");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("API URL must use http or https scheme");
    }

    @Test
    void setCommandWhenInvalidTimeoutThrowsException() {
        // Given
        ConfigCommand.SetCommand command = new ConfigCommand.SetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("api.timeout.connect", "0");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Timeout must be greater than 0");
    }

    @Test
    void setCommandWhenTimeoutIsNotANumberThrowsException() {
        // Given
        ConfigCommand.SetCommand command = new ConfigCommand.SetCommand();
        CommandLine cmd = new CommandLine(command);

        // When
        int exitCode = cmd.execute("api.timeout.connect", "not-a-number");

        // Then
        assertThat(exitCode).isNotEqualTo(0);
        assertThat(errorStream.toString()).contains("Invalid timeout value");
    }
}
