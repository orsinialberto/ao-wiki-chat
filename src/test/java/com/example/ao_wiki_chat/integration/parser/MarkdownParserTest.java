package com.example.ao_wiki_chat.integration.parser;

import com.example.ao_wiki_chat.exception.DocumentParsingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownParserTest {

    private MarkdownParser parser;

    @BeforeEach
    void setUp() {
        parser = new MarkdownParser();
    }

    @Test
    void parseWhenValidMarkdownReturnsPlainText() {
        // Given
        final String markdown = """
                # Title
                
                This is a **bold** text with *italic*.
                
                ## Subtitle
                
                - Item 1
                - Item 2
                """;
        final InputStream inputStream = toInputStream(markdown);

        // When
        final String result = parser.parse(inputStream, "text/markdown");

        // Then
        assertThat(result)
                .isNotBlank()
                .contains("Title")
                .contains("bold")
                .contains("italic")
                .contains("Subtitle")
                .contains("Item 1")
                .contains("Item 2");
    }

    @Test
    void parseWhenEmptyMarkdownReturnsEmptyString() {
        // Given
        final InputStream inputStream = toInputStream("");

        // When
        final String result = parser.parse(inputStream, "text/markdown");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void parseWhenOnlyWhitespaceReturnsEmptyString() {
        // Given
        final InputStream inputStream = toInputStream("   \n\n\t  ");

        // When
        final String result = parser.parse(inputStream, "text/markdown");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void parseWhenNullInputStreamThrowsException() {
        // Given
        final InputStream inputStream = null;

        // When / Then
        assertThatThrownBy(() -> parser.parse(inputStream, "text/markdown"))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void supportsWhenTextMarkdownReturnsTrue() {
        // When
        final boolean result = parser.supports("text/markdown");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void supportsWhenTextXMarkdownReturnsTrue() {
        // When
        final boolean result = parser.supports("text/x-markdown");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void supportsWhenTextPlainReturnsTrue() {
        // When
        final boolean result = parser.supports("text/plain");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void supportsWhenApplicationPdfReturnsFalse() {
        // When
        final boolean result = parser.supports("application/pdf");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void supportsWhenNullReturnsFalse() {
        // When
        final boolean result = parser.supports(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void parseWhenComplexMarkdownExtractsAllText() {
        // Given
        final String markdown = """
                # Main Title
                
                Paragraph with [link](http://example.com) and `code`.
                
                ```java
                public void test() {
                    System.out.println("code block");
                }
                ```
                
                > Quote text
                
                1. Ordered item
                2. Another item
                """;
        final InputStream inputStream = toInputStream(markdown);

        // When
        final String result = parser.parse(inputStream, "text/markdown");

        // Then
        assertThat(result)
                .contains("Main Title")
                .contains("Paragraph with")
                .contains("link")
                .contains("code");
    }

    private InputStream toInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}


