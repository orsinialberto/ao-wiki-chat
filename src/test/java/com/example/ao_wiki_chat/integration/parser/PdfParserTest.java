package com.example.ao_wiki_chat.integration.parser;

import com.example.ao_wiki_chat.exception.DocumentParsingException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfParserTest {

    private PdfParser parser;

    @BeforeEach
    void setUp() {
        parser = new PdfParser();
    }

    @Test
    void parseWhenValidPdfReturnsText() throws Exception {
        // Given
        final String expectedText = "Test PDF Content";
        final InputStream inputStream = createSimplePdf(expectedText);

        // When
        final String result = parser.parse(inputStream, "application/pdf");

        // Then
        assertThat(result)
                .isNotBlank()
                .contains("Test PDF Content");
    }

    @Test
    void parseWhenNullInputStreamThrowsException() {
        // Given
        final InputStream inputStream = null;

        // When / Then
        assertThatThrownBy(() -> parser.parse(inputStream, "application/pdf"))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void parseWhenInvalidPdfThrowsException() {
        // Given
        final InputStream inputStream = new ByteArrayInputStream("not a pdf".getBytes());

        // When / Then
        assertThatThrownBy(() -> parser.parse(inputStream, "application/pdf"))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Failed to");
    }

    @Test
    void parseWhenEmptyPdfReturnsEmptyString() throws Exception {
        // Given
        final InputStream inputStream = createEmptyPdf();

        // When
        final String result = parser.parse(inputStream, "application/pdf");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void supportsWhenApplicationPdfReturnsTrue() {
        // When
        final boolean result = parser.supports("application/pdf");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void supportsWhenTextHtmlReturnsFalse() {
        // When
        final boolean result = parser.supports("text/html");

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

    /**
     * Creates a simple PDF with text content for testing.
     */
    private InputStream createSimplePdf(String text) throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = 
                    new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            document.save(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    /**
     * Creates an empty PDF (no pages) for testing.
     */
    private InputStream createEmptyPdf() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            document.save(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }
}


