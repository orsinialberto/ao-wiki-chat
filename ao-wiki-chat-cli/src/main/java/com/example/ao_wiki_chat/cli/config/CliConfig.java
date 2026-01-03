package com.example.ao_wiki_chat.cli.config;

import java.time.Duration;

/**
 * Configuration class for WikiChat CLI.
 * Contains all configurable properties with default values.
 * Uses builder pattern for flexible configuration.
 */
public final class CliConfig {

    // API Configuration
    private final String apiUrl;
    private final Duration apiConnectTimeout;
    private final Duration apiReadTimeout;
    private final Duration apiWriteTimeout;

    // Output Configuration
    private final String outputFormat;
    private final boolean outputColors;

    // File Validation Configuration
    private final long maxFileSizeBytes;
    private final String supportedFileTypes;

    // Default values
    public static final String DEFAULT_API_URL = "http://localhost:8080";
    public static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 60;
    public static final int DEFAULT_READ_TIMEOUT_SECONDS = 300;
    public static final int DEFAULT_WRITE_TIMEOUT_SECONDS = 300;
    public static final String DEFAULT_OUTPUT_FORMAT = "text";
    public static final boolean DEFAULT_OUTPUT_COLORS = true;
    public static final long DEFAULT_MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024; // 100 MB
    public static final String DEFAULT_SUPPORTED_FILE_TYPES = "pdf,txt,md,doc,docx";

    private CliConfig(Builder builder) {
        this.apiUrl = builder.apiUrl;
        this.apiConnectTimeout = builder.apiConnectTimeout;
        this.apiReadTimeout = builder.apiReadTimeout;
        this.apiWriteTimeout = builder.apiWriteTimeout;
        this.outputFormat = builder.outputFormat;
        this.outputColors = builder.outputColors;
        this.maxFileSizeBytes = builder.maxFileSizeBytes;
        this.supportedFileTypes = builder.supportedFileTypes;
    }

    /**
     * Creates a new builder with default values.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new builder with values from this config.
     *
     * @return a new builder instance with current values
     */
    public Builder toBuilder() {
        return new Builder()
                .apiUrl(this.apiUrl)
                .apiConnectTimeout(this.apiConnectTimeout)
                .apiReadTimeout(this.apiReadTimeout)
                .apiWriteTimeout(this.apiWriteTimeout)
                .outputFormat(this.outputFormat)
                .outputColors(this.outputColors)
                .maxFileSizeBytes(this.maxFileSizeBytes)
                .supportedFileTypes(this.supportedFileTypes);
    }

    /**
     * Creates a default configuration instance.
     *
     * @return a new CliConfig with default values
     */
    public static CliConfig defaults() {
        return builder().build();
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public Duration getApiConnectTimeout() {
        return apiConnectTimeout;
    }

    public Duration getApiReadTimeout() {
        return apiReadTimeout;
    }

    public Duration getApiWriteTimeout() {
        return apiWriteTimeout;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public boolean isOutputColors() {
        return outputColors;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public String getSupportedFileTypes() {
        return supportedFileTypes;
    }

    /**
     * Builder class for CliConfig.
     */
    public static final class Builder {
        private String apiUrl = DEFAULT_API_URL;
        private Duration apiConnectTimeout = Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
        private Duration apiReadTimeout = Duration.ofSeconds(DEFAULT_READ_TIMEOUT_SECONDS);
        private Duration apiWriteTimeout = Duration.ofSeconds(DEFAULT_WRITE_TIMEOUT_SECONDS);
        private String outputFormat = DEFAULT_OUTPUT_FORMAT;
        private boolean outputColors = DEFAULT_OUTPUT_COLORS;
        private long maxFileSizeBytes = DEFAULT_MAX_FILE_SIZE_BYTES;
        private String supportedFileTypes = DEFAULT_SUPPORTED_FILE_TYPES;

        private Builder() {
        }

        public Builder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public Builder apiConnectTimeout(Duration apiConnectTimeout) {
            this.apiConnectTimeout = apiConnectTimeout;
            return this;
        }

        public Builder apiConnectTimeoutSeconds(int seconds) {
            this.apiConnectTimeout = Duration.ofSeconds(seconds);
            return this;
        }

        public Builder apiReadTimeout(Duration apiReadTimeout) {
            this.apiReadTimeout = apiReadTimeout;
            return this;
        }

        public Builder apiReadTimeoutSeconds(int seconds) {
            this.apiReadTimeout = Duration.ofSeconds(seconds);
            return this;
        }

        public Builder apiWriteTimeout(Duration apiWriteTimeout) {
            this.apiWriteTimeout = apiWriteTimeout;
            return this;
        }

        public Builder apiWriteTimeoutSeconds(int seconds) {
            this.apiWriteTimeout = Duration.ofSeconds(seconds);
            return this;
        }

        public Builder outputFormat(String outputFormat) {
            this.outputFormat = outputFormat;
            return this;
        }

        public Builder outputColors(boolean outputColors) {
            this.outputColors = outputColors;
            return this;
        }

        public Builder maxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
            return this;
        }

        public Builder supportedFileTypes(String supportedFileTypes) {
            this.supportedFileTypes = supportedFileTypes;
            return this;
        }

        public CliConfig build() {
            return new CliConfig(this);
        }
    }
}
