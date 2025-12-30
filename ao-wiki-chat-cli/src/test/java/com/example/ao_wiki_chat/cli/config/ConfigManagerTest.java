package com.example.ao_wiki_chat.cli.config;

import com.example.ao_wiki_chat.cli.exception.ApiClientException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ConfigManager.
 * Tests configuration loading, saving, validation, and error handling.
 */
class ConfigManagerTest {

    private Path tempConfigDir;
    private Path tempConfigFile;
    private String originalUserHome;

    @BeforeEach
    void setUp() throws IOException {
        // Create temporary directory for config file
        tempConfigDir = Files.createTempDirectory("wikichat-test-");
        tempConfigFile = tempConfigDir.resolve(".wikichat").resolve("config.properties");

        // Save original user.home and set it to our temp directory
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempConfigDir.toString());
    }

    @AfterEach
    void tearDown() {
        // Restore original user.home
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }

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
    void loadConfigWhenFileDoesNotExistReturnsDefaults() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When
        CliConfig config = configManager.loadConfig();

        // Then
        assertThat(config).isNotNull();
        assertThat(config.getApiUrl()).isEqualTo(CliConfig.DEFAULT_API_URL);
        assertThat(config.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(CliConfig.DEFAULT_CONNECT_TIMEOUT_SECONDS));
        assertThat(config.getApiReadTimeout()).isEqualTo(Duration.ofSeconds(CliConfig.DEFAULT_READ_TIMEOUT_SECONDS));
        assertThat(config.getApiWriteTimeout()).isEqualTo(Duration.ofSeconds(CliConfig.DEFAULT_WRITE_TIMEOUT_SECONDS));
        assertThat(config.getOutputFormat()).isEqualTo(CliConfig.DEFAULT_OUTPUT_FORMAT);
        assertThat(config.isOutputColors()).isEqualTo(CliConfig.DEFAULT_OUTPUT_COLORS);
    }

    @Test
    void loadConfigWhenFileExistsLoadsProperties() throws IOException {
        // Given
        Files.createDirectories(tempConfigFile.getParent());
        Properties properties = new Properties();
        properties.setProperty("api.url", "http://example.com:9090");
        properties.setProperty("api.timeout.connect", "45");
        properties.setProperty("api.timeout.read", "90");
        properties.setProperty("api.timeout.write", "120");
        properties.setProperty("output.format", "json");
        properties.setProperty("output.colors", "false");

        try (var output = Files.newOutputStream(tempConfigFile)) {
            properties.store(output, "Test config");
        }

        ConfigManager configManager = new ConfigManager();

        // When
        CliConfig config = configManager.loadConfig();

        // Then
        assertThat(config).isNotNull();
        assertThat(config.getApiUrl()).isEqualTo("http://example.com:9090");
        assertThat(config.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(45));
        assertThat(config.getApiReadTimeout()).isEqualTo(Duration.ofSeconds(90));
        assertThat(config.getApiWriteTimeout()).isEqualTo(Duration.ofSeconds(120));
        assertThat(config.getOutputFormat()).isEqualTo("json");
        assertThat(config.isOutputColors()).isFalse();
    }

    @Test
    void loadConfigWhenFileHasPartialPropertiesUsesDefaultsForMissing() throws IOException {
        // Given
        Files.createDirectories(tempConfigFile.getParent());
        Properties properties = new Properties();
        properties.setProperty("api.url", "http://example.com");
        properties.setProperty("output.colors", "false");

        try (var output = Files.newOutputStream(tempConfigFile)) {
            properties.store(output, "Test config");
        }

        ConfigManager configManager = new ConfigManager();

        // When
        CliConfig config = configManager.loadConfig();

        // Then
        assertThat(config.getApiUrl()).isEqualTo("http://example.com");
        assertThat(config.isOutputColors()).isFalse();
        // Other properties should use defaults
        assertThat(config.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(CliConfig.DEFAULT_CONNECT_TIMEOUT_SECONDS));
        assertThat(config.getOutputFormat()).isEqualTo(CliConfig.DEFAULT_OUTPUT_FORMAT);
    }

    @Test
    void saveConfigWhenDirectoryDoesNotExistCreatesDirectory() {
        // Given
        ConfigManager configManager = new ConfigManager();
        CliConfig config = CliConfig.builder()
                .apiUrl("http://test.com")
                .build();

        // When
        configManager.saveConfig(config);

        // Then
        assertThat(Files.exists(tempConfigFile.getParent())).isTrue();
        assertThat(Files.exists(tempConfigFile)).isTrue();
    }

    @Test
    void saveConfigWhenFileExistsOverwritesFile() throws IOException {
        // Given
        Files.createDirectories(tempConfigFile.getParent());
        Files.writeString(tempConfigFile, "old content");

        ConfigManager configManager = new ConfigManager();
        CliConfig config = CliConfig.builder()
                .apiUrl("http://new.com")
                .build();

        // When
        configManager.saveConfig(config);

        // Then
        assertThat(Files.exists(tempConfigFile)).isTrue();
        Properties loaded = new Properties();
        try (var input = Files.newInputStream(tempConfigFile)) {
            loaded.load(input);
        }
        assertThat(loaded.getProperty("api.url")).isEqualTo("http://new.com");
    }

    @Test
    void saveConfigWhenSuccessfulSavesAllProperties() throws IOException {
        // Given
        ConfigManager configManager = new ConfigManager();
        CliConfig config = CliConfig.builder()
                .apiUrl("http://example.com:8080")
                .apiConnectTimeoutSeconds(40)
                .apiReadTimeoutSeconds(80)
                .apiWriteTimeoutSeconds(100)
                .outputFormat("json")
                .outputColors(false)
                .build();

        // When
        configManager.saveConfig(config);

        // Then
        assertThat(Files.exists(tempConfigFile)).isTrue();
        Properties loaded = new Properties();
        try (var input = Files.newInputStream(tempConfigFile)) {
            loaded.load(input);
        }
        assertThat(loaded.getProperty("api.url")).isEqualTo("http://example.com:8080");
        assertThat(loaded.getProperty("api.timeout.connect")).isEqualTo("40");
        assertThat(loaded.getProperty("api.timeout.read")).isEqualTo("80");
        assertThat(loaded.getProperty("api.timeout.write")).isEqualTo("100");
        assertThat(loaded.getProperty("output.format")).isEqualTo("json");
        assertThat(loaded.getProperty("output.colors")).isEqualTo("false");
    }

    @Test
    void getWhenCalledReturnsCurrentConfig() {
        // Given
        ConfigManager configManager = new ConfigManager();
        CliConfig expectedConfig = CliConfig.builder()
                .apiUrl("http://test.com")
                .build();
        configManager.saveConfig(expectedConfig);

        // When
        CliConfig result = configManager.get();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getApiUrl()).isEqualTo("http://test.com");
    }

    @Test
    void setWhenApiUrlIsValidUpdatesConfig() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When
        configManager.set("api.url", "https://api.example.com");

        // Then
        CliConfig config = configManager.get();
        assertThat(config.getApiUrl()).isEqualTo("https://api.example.com");
    }

    @Test
    void setWhenTimeoutIsValidUpdatesConfig() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When
        configManager.set("api.timeout.connect", "50");

        // Then
        CliConfig config = configManager.get();
        assertThat(config.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(50));
    }

    @Test
    void setWhenOutputFormatIsValidUpdatesConfig() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When
        configManager.set("output.format", "json");

        // Then
        CliConfig config = configManager.get();
        assertThat(config.getOutputFormat()).isEqualTo("json");
    }

    @Test
    void setWhenOutputColorsIsValidUpdatesConfig() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When
        configManager.set("output.colors", "false");

        // Then
        CliConfig config = configManager.get();
        assertThat(config.isOutputColors()).isFalse();
    }

    @Test
    void setWhenUnknownKeyThrowsException() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When/Then
        assertThatThrownBy(() -> configManager.set("unknown.key", "value"))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("Unknown configuration key");
    }

    @Test
    void resetWhenCalledResetsToDefaults() {
        // Given
        ConfigManager configManager = new ConfigManager();
        CliConfig customConfig = CliConfig.builder()
                .apiUrl("http://custom.com")
                .apiConnectTimeoutSeconds(100)
                .outputFormat("json")
                .outputColors(false)
                .build();
        configManager.saveConfig(customConfig);

        // When
        configManager.reset();

        // Then
        CliConfig config = configManager.get();
        assertThat(config.getApiUrl()).isEqualTo(CliConfig.DEFAULT_API_URL);
        assertThat(config.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(CliConfig.DEFAULT_CONNECT_TIMEOUT_SECONDS));
        assertThat(config.getOutputFormat()).isEqualTo(CliConfig.DEFAULT_OUTPUT_FORMAT);
        assertThat(config.isOutputColors()).isEqualTo(CliConfig.DEFAULT_OUTPUT_COLORS);
    }

    @Test
    void validateApiUrlWhenUrlIsEmptyThrowsException() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When/Then
        assertThatThrownBy(() -> configManager.set("api.url", ""))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("API URL cannot be empty");
    }

    @Test
    void validateApiUrlWhenUrlIsBlankThrowsException() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When/Then
        assertThatThrownBy(() -> configManager.set("api.url", "   "))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("API URL cannot be empty");
    }

    @Test
    void validateApiUrlWhenUrlHasInvalidSchemeThrowsException() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When/Then
        assertThatThrownBy(() -> configManager.set("api.url", "ftp://example.com"))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("API URL must use http or https scheme");
    }

    @Test
    void validateApiUrlWhenUrlIsInvalidFormatThrowsException() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When/Then
        assertThatThrownBy(() -> configManager.set("api.url", "not a url"))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("Invalid API URL format");
    }

    @Test
    void validateApiUrlWhenUrlIsValidHttpAcceptsIt() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When
        configManager.set("api.url", "http://localhost:8080");

        // Then
        CliConfig config = configManager.get();
        assertThat(config.getApiUrl()).isEqualTo("http://localhost:8080");
    }

    @Test
    void validateApiUrlWhenUrlIsValidHttpsAcceptsIt() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When
        configManager.set("api.url", "https://api.example.com");

        // Then
        CliConfig config = configManager.get();
        assertThat(config.getApiUrl()).isEqualTo("https://api.example.com");
    }

    @Test
    void parseTimeoutWhenValueIsZeroThrowsException() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When/Then
        assertThatThrownBy(() -> configManager.set("api.timeout.connect", "0"))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("Timeout must be greater than 0");
    }

    @Test
    void parseTimeoutWhenValueIsNegativeThrowsException() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When/Then
        assertThatThrownBy(() -> configManager.set("api.timeout.connect", "-5"))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("Timeout must be greater than 0");
    }

    @Test
    void parseTimeoutWhenValueIsNotANumberThrowsException() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When/Then
        assertThatThrownBy(() -> configManager.set("api.timeout.connect", "not-a-number"))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("Invalid timeout value");
    }

    @Test
    void parseTimeoutWhenValueIsEmptyThrowsException() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When/Then
        assertThatThrownBy(() -> configManager.set("api.timeout.connect", ""))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("Timeout value cannot be empty");
    }

    @Test
    void parseTimeoutWhenValueIsValidAcceptsIt() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When
        configManager.set("api.timeout.connect", "120");

        // Then
        CliConfig config = configManager.get();
        assertThat(config.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void getConfigFilePathReturnsCorrectPath() {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When
        Path configPath = configManager.getConfigFilePath();

        // Then
        assertThat(configPath).isNotNull();
        assertThat(configPath.toString()).contains(".wikichat");
        assertThat(configPath.toString()).contains("config.properties");
    }

    @Test
    void loadConfigWhenFileIsCorruptedUsesDefaults() throws IOException {
        // Given
        Files.createDirectories(tempConfigFile.getParent());
        Files.writeString(tempConfigFile, "corrupted content that is not valid properties");

        ConfigManager configManager = new ConfigManager();

        // When
        CliConfig config = configManager.loadConfig();

        // Then - should fall back to defaults
        assertThat(config).isNotNull();
        assertThat(config.getApiUrl()).isEqualTo(CliConfig.DEFAULT_API_URL);
    }

    @Test
    void setWhenCalledSavesToFile() throws IOException {
        // Given
        ConfigManager configManager = new ConfigManager();

        // When
        configManager.set("api.url", "http://saved.com");

        // Then
        assertThat(Files.exists(tempConfigFile)).isTrue();
        Properties loaded = new Properties();
        try (var input = Files.newInputStream(tempConfigFile)) {
            loaded.load(input);
        }
        assertThat(loaded.getProperty("api.url")).isEqualTo("http://saved.com");
    }
}
