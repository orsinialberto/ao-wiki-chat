package com.example.ao_wiki_chat.cli.config;

import com.example.ao_wiki_chat.cli.exception.ApiClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

/**
 * Manages CLI configuration persistence.
 * Handles loading and saving configuration from/to ~/.wikichat/config.properties.
 */
public final class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    private static final String CONFIG_DIR_NAME = ".wikichat";
    private static final String CONFIG_FILE_NAME = "config.properties";

    // Property keys
    private static final String KEY_API_URL = "api.url";
    private static final String KEY_API_CONNECT_TIMEOUT = "api.timeout.connect";
    private static final String KEY_API_READ_TIMEOUT = "api.timeout.read";
    private static final String KEY_API_WRITE_TIMEOUT = "api.timeout.write";
    private static final String KEY_OUTPUT_FORMAT = "output.format";
    private static final String KEY_OUTPUT_COLORS = "output.colors";

    private final Path configFile;
    private CliConfig currentConfig;

    /**
     * Creates a new ConfigManager instance.
     * Initializes the config file path and loads existing configuration.
     */
    public ConfigManager() {
        this.configFile = initializeConfigFilePath();
        this.currentConfig = loadConfig();
    }

    /**
     * Initializes the path to the configuration file.
     *
     * @return the path to ~/.wikichat/config.properties
     */
    private Path initializeConfigFilePath() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            throw new ApiClientException("Cannot determine user home directory");
        }
        return Paths.get(userHome, CONFIG_DIR_NAME, CONFIG_FILE_NAME);
    }

    /**
     * Loads configuration from the config file.
     * If the file doesn't exist, returns default configuration.
     *
     * @return the loaded or default configuration
     */
    public CliConfig loadConfig() {
        if (!Files.exists(configFile)) {
            log.debug("Config file does not exist, using defaults: {}", configFile);
            return CliConfig.defaults();
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configFile)) {
            properties.load(input);
            log.debug("Loaded configuration from: {}", configFile);
        } catch (IOException e) {
            log.warn("Failed to load config file, using defaults: {}", e.getMessage());
            return CliConfig.defaults();
        }

        return buildConfigFromProperties(properties);
    }

    /**
     * Builds a CliConfig from Properties.
     *
     * @param properties the properties to read from
     * @return a new CliConfig instance
     */
    private CliConfig buildConfigFromProperties(Properties properties) {
        CliConfig.Builder builder = CliConfig.builder();

        String apiUrl = properties.getProperty(KEY_API_URL);
        if (apiUrl != null && !apiUrl.isBlank()) {
            validateApiUrl(apiUrl);
            builder.apiUrl(apiUrl.trim());
        }

        String connectTimeout = properties.getProperty(KEY_API_CONNECT_TIMEOUT);
        if (connectTimeout != null && !connectTimeout.isBlank()) {
            int seconds = parseTimeout(connectTimeout, KEY_API_CONNECT_TIMEOUT);
            builder.apiConnectTimeoutSeconds(seconds);
        }

        String readTimeout = properties.getProperty(KEY_API_READ_TIMEOUT);
        if (readTimeout != null && !readTimeout.isBlank()) {
            int seconds = parseTimeout(readTimeout, KEY_API_READ_TIMEOUT);
            builder.apiReadTimeoutSeconds(seconds);
        }

        String writeTimeout = properties.getProperty(KEY_API_WRITE_TIMEOUT);
        if (writeTimeout != null && !writeTimeout.isBlank()) {
            int seconds = parseTimeout(writeTimeout, KEY_API_WRITE_TIMEOUT);
            builder.apiWriteTimeoutSeconds(seconds);
        }

        String outputFormat = properties.getProperty(KEY_OUTPUT_FORMAT);
        if (outputFormat != null && !outputFormat.isBlank()) {
            builder.outputFormat(outputFormat.trim());
        }

        String outputColors = properties.getProperty(KEY_OUTPUT_COLORS);
        if (outputColors != null && !outputColors.isBlank()) {
            builder.outputColors(Boolean.parseBoolean(outputColors.trim()));
        }

        CliConfig config = builder.build();
        this.currentConfig = config;
        return config;
    }

    /**
     * Saves the current configuration to the config file.
     * Creates the directory and file if they don't exist.
     *
     * @param config the configuration to save
     */
    public void saveConfig(CliConfig config) {
        try {
            Path configDir = configFile.getParent();
            if (configDir != null && !Files.exists(configDir)) {
                Files.createDirectories(configDir);
                log.debug("Created config directory: {}", configDir);
            }

            Properties properties = new Properties();
            properties.setProperty(KEY_API_URL, config.getApiUrl());
            properties.setProperty(KEY_API_CONNECT_TIMEOUT, String.valueOf(config.getApiConnectTimeout().getSeconds()));
            properties.setProperty(KEY_API_READ_TIMEOUT, String.valueOf(config.getApiReadTimeout().getSeconds()));
            properties.setProperty(KEY_API_WRITE_TIMEOUT, String.valueOf(config.getApiWriteTimeout().getSeconds()));
            properties.setProperty(KEY_OUTPUT_FORMAT, config.getOutputFormat());
            properties.setProperty(KEY_OUTPUT_COLORS, String.valueOf(config.isOutputColors()));

            try (OutputStream output = Files.newOutputStream(configFile)) {
                properties.store(output, "WikiChat CLI Configuration");
                log.debug("Saved configuration to: {}", configFile);
            }

            this.currentConfig = config;
        } catch (IOException e) {
            throw new ApiClientException("Failed to save config file: " + configFile, e);
        }
    }

    /**
     * Gets the current configuration.
     *
     * @return the current configuration
     */
    public CliConfig get() {
        return currentConfig;
    }

    /**
     * Sets a configuration value and saves it to the file.
     *
     * @param key   the configuration key
     * @param value the value to set
     */
    public void set(String key, String value) {
        CliConfig.Builder builder = currentConfig.toBuilder();

        switch (key) {
            case KEY_API_URL:
                validateApiUrl(value);
                builder.apiUrl(value);
                break;
            case KEY_API_CONNECT_TIMEOUT:
                int connectTimeout = parseTimeout(value, KEY_API_CONNECT_TIMEOUT);
                builder.apiConnectTimeoutSeconds(connectTimeout);
                break;
            case KEY_API_READ_TIMEOUT:
                int readTimeout = parseTimeout(value, KEY_API_READ_TIMEOUT);
                builder.apiReadTimeoutSeconds(readTimeout);
                break;
            case KEY_API_WRITE_TIMEOUT:
                int writeTimeout = parseTimeout(value, KEY_API_WRITE_TIMEOUT);
                builder.apiWriteTimeoutSeconds(writeTimeout);
                break;
            case KEY_OUTPUT_FORMAT:
                builder.outputFormat(value);
                break;
            case KEY_OUTPUT_COLORS:
                builder.outputColors(Boolean.parseBoolean(value));
                break;
            default:
                throw new ApiClientException("Unknown configuration key: " + key);
        }

        CliConfig newConfig = builder.build();
        saveConfig(newConfig);
    }

    /**
     * Resets configuration to defaults and saves it.
     */
    public void reset() {
        CliConfig defaults = CliConfig.defaults();
        saveConfig(defaults);
        log.info("Configuration reset to defaults");
    }

    /**
     * Validates an API URL.
     *
     * @param url the URL to validate
     * @throws ApiClientException if the URL is invalid
     */
    private void validateApiUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new ApiClientException("API URL cannot be empty");
        }

        try {
            URI uri = new URI(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
                throw new ApiClientException("API URL must use http or https scheme");
            }
        } catch (URISyntaxException e) {
            throw new ApiClientException("Invalid API URL format: " + url, e);
        }
    }

    /**
     * Parses a timeout value from a string.
     *
     * @param value the string value
     * @param key   the property key (for error messages)
     * @return the parsed timeout in seconds
     * @throws ApiClientException if the value is invalid
     */
    private int parseTimeout(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new ApiClientException("Timeout value cannot be empty for key: " + key);
        }

        try {
            int seconds = Integer.parseInt(value.trim());
            if (seconds <= 0) {
                throw new ApiClientException("Timeout must be greater than 0 for key: " + key);
            }
            return seconds;
        } catch (NumberFormatException e) {
            throw new ApiClientException("Invalid timeout value for key " + key + ": " + value, e);
        }
    }

    /**
     * Gets the path to the configuration file.
     *
     * @return the config file path
     */
    public Path getConfigFilePath() {
        return configFile;
    }
}
