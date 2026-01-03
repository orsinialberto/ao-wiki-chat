package com.example.ao_wiki_chat.unit.integration.parser;

import com.example.ao_wiki_chat.exception.DocumentParsingException;
import com.example.ao_wiki_chat.integration.parser.MarkdownParser;
import org.commonmark.parser.Parser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void parseWhenIOExceptionDuringReadThrowsDocumentParsingException() {
        // Given
        final InputStream inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Stream closed unexpectedly");
            }
        };
        final String contentType = "text/markdown";

        // When / Then
        assertThatThrownBy(() -> parser.parse(inputStream, contentType))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Failed to read markdown content")
                .hasMessageContaining("Stream closed unexpectedly")
                .satisfies(exception -> {
                    final DocumentParsingException parsingException = (DocumentParsingException) exception;
                    assertThat(parsingException.getContentType()).isEqualTo(contentType);
                    assertThat(parsingException.getCause()).isInstanceOf(IOException.class);
                });
    }

    @Test
    void parseWhenGenericExceptionDuringParsingThrowsDocumentParsingException() throws Exception {
        // Given
        final String markdown = "# Test";
        final InputStream inputStream = toInputStream(markdown);
        final String contentType = "text/markdown";
        
        // Create a spy to inject a mock parser that throws exception
        final MarkdownParser spyParser = new MarkdownParser();
        final Parser mockParser = mock(Parser.class);
        final RuntimeException parsingException = new RuntimeException("Parsing failed");
        
        when(mockParser.parse(anyString())).thenThrow(parsingException);
        
        // Use reflection to replace the parser field
        final Field parserField = MarkdownParser.class.getDeclaredField("parser");
        parserField.setAccessible(true);
        parserField.set(spyParser, mockParser);

        // When / Then
        assertThatThrownBy(() -> spyParser.parse(inputStream, contentType))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Failed to parse markdown content")
                .hasMessageContaining("Parsing failed")
                .satisfies(exception -> {
                    final DocumentParsingException docException = (DocumentParsingException) exception;
                    assertThat(docException.getContentType()).isEqualTo(contentType);
                    assertThat(docException.getCause()).isSameAs(parsingException);
                });
    }

    @Test
    void supportsWhenUppercaseContentTypeReturnsTrue() {
        // When
        final boolean result = parser.supports("TEXT/MARKDOWN");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void supportsWhenMixedCaseContentTypeReturnsTrue() {
        // When
        final boolean result = parser.supports("Text/Markdown");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void supportsWhenUppercaseTextXMarkdownReturnsTrue() {
        // When
        final boolean result = parser.supports("TEXT/X-MARKDOWN");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void supportsWhenUppercaseTextPlainReturnsTrue() {
        // When
        final boolean result = parser.supports("TEXT/PLAIN");

        // Then
        assertThat(result).isTrue();
    }

    private InputStream toInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}


