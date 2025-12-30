package com.example.ao_wiki_chat.cli.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CliConfig.
 * Tests builder pattern, default values, and immutability.
 */
class CliConfigTest {

    @Test
    void defaultsWhenCalledReturnsConfigWithDefaultValues() {
        // When
        CliConfig config = CliConfig.defaults();

        // Then
        assertThat(config.getApiUrl()).isEqualTo(CliConfig.DEFAULT_API_URL);
        assertThat(config.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(CliConfig.DEFAULT_CONNECT_TIMEOUT_SECONDS));
        assertThat(config.getApiReadTimeout()).isEqualTo(Duration.ofSeconds(CliConfig.DEFAULT_READ_TIMEOUT_SECONDS));
        assertThat(config.getApiWriteTimeout()).isEqualTo(Duration.ofSeconds(CliConfig.DEFAULT_WRITE_TIMEOUT_SECONDS));
        assertThat(config.getOutputFormat()).isEqualTo(CliConfig.DEFAULT_OUTPUT_FORMAT);
        assertThat(config.isOutputColors()).isEqualTo(CliConfig.DEFAULT_OUTPUT_COLORS);
    }

    @Test
    void builderWhenBuiltWithDefaultsReturnsDefaultConfig() {
        // When
        CliConfig config = CliConfig.builder().build();

        // Then
        assertThat(config.getApiUrl()).isEqualTo(CliConfig.DEFAULT_API_URL);
        assertThat(config.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(CliConfig.DEFAULT_CONNECT_TIMEOUT_SECONDS));
        assertThat(config.getOutputFormat()).isEqualTo(CliConfig.DEFAULT_OUTPUT_FORMAT);
    }

    @Test
    void builderWhenApiUrlIsSetReturnsConfigWithCustomUrl() {
        // When
        CliConfig config = CliConfig.builder()
                .apiUrl("http://custom.com")
                .build();

        // Then
        assertThat(config.getApiUrl()).isEqualTo("http://custom.com");
    }

    @Test
    void builderWhenTimeoutsAreSetReturnsConfigWithCustomTimeouts() {
        // When
        CliConfig config = CliConfig.builder()
                .apiConnectTimeoutSeconds(45)
                .apiReadTimeoutSeconds(90)
                .apiWriteTimeoutSeconds(120)
                .build();

        // Then
        assertThat(config.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(45));
        assertThat(config.getApiReadTimeout()).isEqualTo(Duration.ofSeconds(90));
        assertThat(config.getApiWriteTimeout()).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void builderWhenTimeoutsAreSetWithDurationReturnsConfigWithCustomTimeouts() {
        // When
        CliConfig config = CliConfig.builder()
                .apiConnectTimeout(Duration.ofSeconds(30))
                .apiReadTimeout(Duration.ofMinutes(2))
                .apiWriteTimeout(Duration.ofSeconds(150))
                .build();

        // Then
        assertThat(config.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.getApiReadTimeout()).isEqualTo(Duration.ofMinutes(2));
        assertThat(config.getApiWriteTimeout()).isEqualTo(Duration.ofSeconds(150));
    }

    @Test
    void builderWhenOutputFormatIsSetReturnsConfigWithCustomFormat() {
        // When
        CliConfig config = CliConfig.builder()
                .outputFormat("json")
                .build();

        // Then
        assertThat(config.getOutputFormat()).isEqualTo("json");
    }

    @Test
    void builderWhenOutputColorsIsSetReturnsConfigWithCustomColors() {
        // When
        CliConfig config = CliConfig.builder()
                .outputColors(false)
                .build();

        // Then
        assertThat(config.isOutputColors()).isFalse();
    }

    @Test
    void builderWhenAllPropertiesAreSetReturnsConfigWithAllCustomValues() {
        // When
        CliConfig config = CliConfig.builder()
                .apiUrl("https://api.example.com")
                .apiConnectTimeoutSeconds(40)
                .apiReadTimeoutSeconds(80)
                .apiWriteTimeoutSeconds(100)
                .outputFormat("json")
                .outputColors(false)
                .build();

        // Then
        assertThat(config.getApiUrl()).isEqualTo("https://api.example.com");
        assertThat(config.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(40));
        assertThat(config.getApiReadTimeout()).isEqualTo(Duration.ofSeconds(80));
        assertThat(config.getApiWriteTimeout()).isEqualTo(Duration.ofSeconds(100));
        assertThat(config.getOutputFormat()).isEqualTo("json");
        assertThat(config.isOutputColors()).isFalse();
    }

    @Test
    void toBuilderWhenCalledReturnsBuilderWithCurrentValues() {
        // Given
        CliConfig original = CliConfig.builder()
                .apiUrl("http://original.com")
                .apiConnectTimeoutSeconds(50)
                .outputFormat("json")
                .build();

        // When
        CliConfig modified = original.toBuilder()
                .apiUrl("http://modified.com")
                .build();

        // Then
        assertThat(modified.getApiUrl()).isEqualTo("http://modified.com");
        assertThat(modified.getApiConnectTimeout()).isEqualTo(Duration.ofSeconds(50));
        assertThat(modified.getOutputFormat()).isEqualTo("json");
        // Original should remain unchanged
        assertThat(original.getApiUrl()).isEqualTo("http://original.com");
    }

    @Test
    void builderWhenChainedReturnsCorrectConfig() {
        // When
        CliConfig config = CliConfig.builder()
                .apiUrl("http://test.com")
                .apiConnectTimeoutSeconds(30)
                .apiReadTimeoutSeconds(60)
                .apiWriteTimeoutSeconds(90)
                .outputFormat("text")
                .outputColors(true)
                .build();

        // Then
        assertThat(config.getApiUrl()).isEqualTo("http://test.com");
        assertThat(config.getApiConnectTimeout().getSeconds()).isEqualTo(30);
        assertThat(config.getApiReadTimeout().getSeconds()).isEqualTo(60);
        assertThat(config.getApiWriteTimeout().getSeconds()).isEqualTo(90);
        assertThat(config.getOutputFormat()).isEqualTo("text");
        assertThat(config.isOutputColors()).isTrue();
    }
}
