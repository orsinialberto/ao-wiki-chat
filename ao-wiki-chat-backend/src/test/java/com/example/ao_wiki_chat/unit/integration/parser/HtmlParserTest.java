package com.example.ao_wiki_chat.unit.integration.parser;

import com.example.ao_wiki_chat.exception.DocumentParsingException;
import com.example.ao_wiki_chat.integration.parser.HtmlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HtmlParserTest {

    private HtmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new HtmlParser();
    }

    @Test
    void parseWhenValidHtmlReturnsPlainText() {
        // Given
        final String html = """
                <html>
                <head><title>Test Page</title></head>
                <body>
                    <h1>Main Title</h1>
                    <p>This is a <strong>bold</strong> text.</p>
                    <div>
                        <span>Nested content</span>
                    </div>
                </body>
                </html>
                """;
        final InputStream inputStream = toInputStream(html);

        // When
        final String result = parser.parse(inputStream, "text/html");

        // Then
        assertThat(result)
                .isNotBlank()
                .contains("Main Title")
                .contains("bold")
                .contains("Nested content");
    }

    @Test
    void parseWhenHtmlWithScriptAndStyleIgnoresThem() {
        // Given
        final String html = """
                <html>
                <head>
                    <style>body { color: red; }</style>
                    <script>console.log('test');</script>
                </head>
                <body>
                    <p>Visible text</p>
                    <script>alert('popup');</script>
                </body>
                </html>
                """;
        final InputStream inputStream = toInputStream(html);

        // When
        final String result = parser.parse(inputStream, "text/html");

        // Then
        assertThat(result)
                .contains("Visible text")
                .doesNotContain("console.log")
                .doesNotContain("alert")
                .doesNotContain("color: red");
    }

    @Test
    void parseWhenEmptyBodyReturnsEmptyString() {
        // Given
        final String html = "<html><body></body></html>";
        final InputStream inputStream = toInputStream(html);

        // When
        final String result = parser.parse(inputStream, "text/html");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void parseWhenOnlyWhitespaceReturnsEmptyString() {
        // Given
        final String html = "<html><body>   \n\n\t  </body></html>";
        final InputStream inputStream = toInputStream(html);

        // When
        final String result = parser.parse(inputStream, "text/html");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void parseWhenNullInputStreamThrowsException() {
        // Given
        final InputStream inputStream = null;

        // When / Then
        assertThatThrownBy(() -> parser.parse(inputStream, "text/html"))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void parseWhenMalformedHtmlStillExtractsText() {
        // Given
        final String html = """
                <html>
                <body>
                    <p>Unclosed paragraph
                    <div>Some text
                    <span>More text</span>
                """;
        final InputStream inputStream = toInputStream(html);

        // When
        final String result = parser.parse(inputStream, "text/html");

        // Then
        assertThat(result)
                .contains("Unclosed paragraph")
                .contains("Some text")
                .contains("More text");
    }

    @Test
    void parseWhenMultipleWhitespaceNormalizesToSingleSpace() {
        // Given
        final String html = """
                <html><body>
                    <p>Text    with     multiple     spaces</p>
                    <p>Text
                    with
                    newlines</p>
                </body></html>
                """;
        final InputStream inputStream = toInputStream(html);

        // When
        final String result = parser.parse(inputStream, "text/html");

        // Then
        assertThat(result)
                .doesNotContain("    ")
                .contains("Text with multiple spaces")
                .contains("Text with newlines");
    }

    @Test
    void supportsWhenTextHtmlReturnsTrue() {
        // When
        final boolean result = parser.supports("text/html");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void supportsWhenApplicationXhtmlXmlReturnsTrue() {
        // When
        final boolean result = parser.supports("application/xhtml+xml");

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
    void supportsWhenUppercaseTextHtmlReturnsTrue() {
        // When
        final boolean result = parser.supports("TEXT/HTML");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void supportsWhenMixedCaseTextHtmlReturnsTrue() {
        // When
        final boolean result = parser.supports("Text/Html");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void supportsWhenUppercaseApplicationXhtmlXmlReturnsTrue() {
        // When
        final boolean result = parser.supports("APPLICATION/XHTML+XML");

        // Then
        assertThat(result).isTrue();
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
        final String contentType = "text/html";

        // When / Then
        assertThatThrownBy(() -> parser.parse(inputStream, contentType))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Failed to read HTML content")
                .hasMessageContaining("Stream closed unexpectedly")
                .satisfies(exception -> {
                    final DocumentParsingException parsingException = (DocumentParsingException) exception;
                    assertThat(parsingException.getContentType()).isEqualTo(contentType);
                    assertThat(parsingException.getCause()).isInstanceOf(IOException.class);
                });
    }

    @Test
    void parseWhenGenericExceptionDuringParsingThrowsDocumentParsingException() {
        // Given
        final InputStream inputStream = new InputStream() {
            private boolean firstRead = true;

            @Override
            public int read() {
                if (firstRead) {
                    firstRead = false;
                    return '<';
                }
                throw new RuntimeException("Unexpected parsing error");
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (firstRead) {
                    firstRead = false;
                    b[off] = '<';
                    return 1;
                }
                throw new RuntimeException("Unexpected parsing error");
            }
        };
        final String contentType = "text/html";

        // When / Then
        assertThatThrownBy(() -> parser.parse(inputStream, contentType))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Failed to parse HTML content")
                .hasMessageContaining("Unexpected parsing error")
                .satisfies(exception -> {
                    final DocumentParsingException docException = (DocumentParsingException) exception;
                    assertThat(docException.getContentType()).isEqualTo(contentType);
                    assertThat(docException.getCause()).isInstanceOf(RuntimeException.class);
                });
    }

    private InputStream toInputStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}


