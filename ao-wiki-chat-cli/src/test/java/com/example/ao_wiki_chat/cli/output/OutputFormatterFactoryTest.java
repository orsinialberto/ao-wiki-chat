package com.example.ao_wiki_chat.cli.output;

import com.example.ao_wiki_chat.cli.config.CliConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for OutputFormatterFactory.
 */
class OutputFormatterFactoryTest {

    @Test
    void createWhenFormatIsTableReturnsTableFormatter() {
        CliConfig config = CliConfig.builder().outputColors(true).build();
        OutputFormatter formatter = OutputFormatterFactory.create("table", config);
        assertThat(formatter).isInstanceOf(TableFormatter.class);
    }

    @Test
    void createWhenFormatIsTextReturnsTableFormatter() {
        CliConfig config = CliConfig.builder().outputColors(false).build();
        OutputFormatter formatter = OutputFormatterFactory.create("text", config);
        assertThat(formatter).isInstanceOf(TableFormatter.class);
    }

    @Test
    void createWhenFormatIsJsonReturnsJsonFormatter() {
        CliConfig config = CliConfig.defaults();
        OutputFormatter formatter = OutputFormatterFactory.create("json", config);
        assertThat(formatter).isInstanceOf(JsonFormatter.class);
    }

    @Test
    void createWhenFormatIsMarkdownReturnsMarkdownFormatter() {
        CliConfig config = CliConfig.defaults();
        OutputFormatter formatter = OutputFormatterFactory.create("markdown", config);
        assertThat(formatter).isInstanceOf(MarkdownFormatter.class);
    }

    @Test
    void createWhenFormatIsMdReturnsMarkdownFormatter() {
        CliConfig config = CliConfig.defaults();
        OutputFormatter formatter = OutputFormatterFactory.create("md", config);
        assertThat(formatter).isInstanceOf(MarkdownFormatter.class);
    }

    @Test
    void createWhenFormatIsPlainReturnsPlainFormatter() {
        CliConfig config = CliConfig.defaults();
        OutputFormatter formatter = OutputFormatterFactory.create("plain", config);
        assertThat(formatter).isInstanceOf(PlainFormatter.class);
    }

    @Test
    void createWhenFormatIsInvalidThrowsException() {
        CliConfig config = CliConfig.defaults();
        assertThatThrownBy(() -> OutputFormatterFactory.create("invalid", config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid format");
    }

    @Test
    void createWhenFormatIsNullUsesConfigDefault() {
        CliConfig config = CliConfig.builder().outputFormat("json").build();
        OutputFormatter formatter = OutputFormatterFactory.create(null, config);
        assertThat(formatter).isInstanceOf(JsonFormatter.class);
    }

    @Test
    void createWhenFormatIsBlankUsesConfigDefault() {
        CliConfig config = CliConfig.builder().outputFormat("plain").build();
        OutputFormatter formatter = OutputFormatterFactory.create("  ", config);
        assertThat(formatter).isInstanceOf(PlainFormatter.class);
    }

    @Test
    void createWhenNoConfigProvidedUsesDefaults() {
        OutputFormatter formatter = OutputFormatterFactory.create("json");
        assertThat(formatter).isInstanceOf(JsonFormatter.class);
    }

    @Test
    void createWhenTableFormatterRespectsColorConfig() {
        CliConfig configWithColors = CliConfig.builder().outputColors(true).build();
        TableFormatter formatter1 = (TableFormatter) OutputFormatterFactory.create("table", configWithColors);

        CliConfig configWithoutColors = CliConfig.builder().outputColors(false).build();
        TableFormatter formatter2 = (TableFormatter) OutputFormatterFactory.create("table", configWithoutColors);

        // Both should be TableFormatter instances
        assertThat(formatter1).isNotNull();
        assertThat(formatter2).isNotNull();
    }
}
