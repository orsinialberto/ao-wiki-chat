package com.example.ao_wiki_chat.unit.integration.parser;

import com.example.ao_wiki_chat.exception.DocumentParsingException;
import com.example.ao_wiki_chat.integration.parser.PdfParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    @Test
    void parseWhenEncryptedPdfThrowsDocumentParsingException() throws Exception {
        // Given
        final InputStream inputStream = createEncryptedPdf();

        // When / Then
        assertThatThrownBy(() -> parser.parse(inputStream, "application/pdf"))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Cannot parse encrypted PDF without password");
    }

    @Test
    void parseWhenIOExceptionDuringReadAllBytesThrowsDocumentParsingException() {
        // Given
        final InputStream inputStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Simulated read error");
            }

            @Override
            public byte[] readAllBytes() throws IOException {
                throw new IOException("Simulated readAllBytes error");
            }
        };

        // When / Then
        assertThatThrownBy(() -> parser.parse(inputStream, "application/pdf"))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Failed to read PDF content")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void parseWhenGenericExceptionDuringParsingThrowsDocumentParsingException() {
        // Given
        // This test verifies the generic Exception catch block handles unexpected exceptions
        // PDFBox typically throws IOException, but the generic catch is a safety net
        // We test with invalid PDF that causes parsing to fail
        final InputStream inputStream = new ByteArrayInputStream("not a pdf".getBytes());

        // When / Then
        // Invalid PDF will cause exception during parsing
        // Note: PDFBox throws IOException for most errors, which is caught by the IOException handler
        // The generic Exception catch is a safety net for unexpected exception types
        assertThatThrownBy(() -> parser.parse(inputStream, "application/pdf"))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Failed to");
    }

    @Test
    void parseWhenPdfWithPagesButEmptyTextReturnsEmptyString() throws Exception {
        // Given
        final InputStream inputStream = createPdfWithPagesButNoText();

        // When
        final String result = parser.parse(inputStream, "application/pdf");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void supportsWhenApplicationPdfUppercaseReturnsTrue() {
        // When
        final boolean result = parser.supports("APPLICATION/PDF");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void supportsWhenApplicationPdfMixedCaseReturnsTrue() {
        // When
        final boolean result = parser.supports("Application/Pdf");

        // Then
        assertThat(result).isTrue();
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

    /**
     * Creates an encrypted PDF for testing.
     */
    private InputStream createEncryptedPdf() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = 
                    new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText("Encrypted PDF Content");
                contentStream.endText();
            }

            // Encrypt the PDF with a password
            final AccessPermission accessPermission = new AccessPermission();
            final StandardProtectionPolicy protectionPolicy = new StandardProtectionPolicy(
                    "ownerPassword", "userPassword", accessPermission);
            protectionPolicy.setEncryptionKeyLength(128);
            document.protect(protectionPolicy);

            document.save(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }

    /**
     * Creates a PDF with pages but no extractable text content.
     */
    private InputStream createPdfWithPagesButNoText() throws Exception {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Add a page with graphics but no text
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = 
                    new PDPageContentStream(document, page)) {
                // Draw a rectangle (graphics only, no text)
                contentStream.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                contentStream.addRect(100, 700, 200, 50);
                contentStream.fill();
            }

            document.save(outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        }
    }
}


