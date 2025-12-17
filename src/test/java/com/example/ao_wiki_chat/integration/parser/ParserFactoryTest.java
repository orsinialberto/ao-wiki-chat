package com.example.ao_wiki_chat.integration.parser;

import com.example.ao_wiki_chat.exception.DocumentParsingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserFactoryTest {

    private ParserFactory factory;

    @BeforeEach
    void setUp() {
        // Create factory with all actual parser implementations
        final List<DocumentParser> parsers = List.of(
                new MarkdownParser(),
                new HtmlParser(),
                new PdfParser()
        );
        factory = new ParserFactory(parsers);
    }

    @Test
    void getParserWhenMarkdownReturnsMarkdownParser() {
        // When
        final DocumentParser parser = factory.getParser("text/markdown");

        // Then
        assertThat(parser).isInstanceOf(MarkdownParser.class);
    }

    @Test
    void getParserWhenHtmlReturnsHtmlParser() {
        // When
        final DocumentParser parser = factory.getParser("text/html");

        // Then
        assertThat(parser).isInstanceOf(HtmlParser.class);
    }

    @Test
    void getParserWhenPdfReturnsPdfParser() {
        // When
        final DocumentParser parser = factory.getParser("application/pdf");

        // Then
        assertThat(parser).isInstanceOf(PdfParser.class);
    }

    @Test
    void getParserWhenContentTypeWithCharsetNormalizesAndReturnsParser() {
        // When
        final DocumentParser parser = factory.getParser("text/html; charset=UTF-8");

        // Then
        assertThat(parser).isInstanceOf(HtmlParser.class);
    }

    @Test
    void getParserWhenContentTypeWithMultipleParametersNormalizesAndReturnsParser() {
        // When
        final DocumentParser parser = 
                factory.getParser("application/pdf; name=document.pdf; encoding=binary");

        // Then
        assertThat(parser).isInstanceOf(PdfParser.class);
    }

    @Test
    void getParserWhenUnsupportedContentTypeThrowsException() {
        // When / Then
        assertThatThrownBy(() -> factory.getParser("application/json"))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Unsupported content type");
    }

    @Test
    void getParserWhenNullContentTypeThrowsException() {
        // When / Then
        assertThatThrownBy(() -> factory.getParser(null))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("cannot be null or blank");
    }

    @Test
    void getParserWhenBlankContentTypeThrowsException() {
        // When / Then
        assertThatThrownBy(() -> factory.getParser("   "))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("cannot be null or blank");
    }

    @Test
    void isSupportedWhenMarkdownReturnsTrue() {
        // When
        final boolean result = factory.isSupported("text/markdown");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupportedWhenHtmlReturnsTrue() {
        // When
        final boolean result = factory.isSupported("text/html");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupportedWhenPdfReturnsTrue() {
        // When
        final boolean result = factory.isSupported("application/pdf");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupportedWhenPlainTextReturnsTrue() {
        // When
        final boolean result = factory.isSupported("text/plain");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSupportedWhenUnsupportedTypeReturnsFalse() {
        // When
        final boolean result = factory.isSupported("application/json");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupportedWhenNullReturnsFalse() {
        // When
        final boolean result = factory.isSupported(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupportedWhenBlankReturnsFalse() {
        // When
        final boolean result = factory.isSupported("   ");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void isSupportedWhenContentTypeWithCharsetReturnsTrue() {
        // When
        final boolean result = factory.isSupported("text/html; charset=UTF-8");

        // Then
        assertThat(result).isTrue();
    }
}


