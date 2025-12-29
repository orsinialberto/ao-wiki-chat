package com.example.ao_wiki_chat.integration.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.ao_wiki_chat.exception.DocumentParsingException;

/**
 * Parser implementation for PDF documents.
 * Uses Apache PDFBox to extract text from PDF files.
 */
@Component
public final class PdfParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(PdfParser.class);

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "application/pdf"
    );

    @Override
    public String parse(InputStream inputStream, String contentType) {
        log.debug("Parsing PDF document with content-type: {}", contentType);

        if (inputStream == null) {
            throw new DocumentParsingException("Input stream cannot be null", contentType);
        }

        try {
            final byte[] pdfBytes = inputStream.readAllBytes();
            
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {

                if (document.isEncrypted()) {
                    log.error("PDF document is encrypted and cannot be parsed");
                    throw new DocumentParsingException(
                            "Cannot parse encrypted PDF without password", 
                            contentType
                    );
                }

            final int pageCount = document.getNumberOfPages();
            log.debug("PDF document has {} pages", pageCount);

            if (pageCount == 0) {
                log.warn("PDF document has no pages");
                return "";
            }

            final PDFTextStripper stripper = new PDFTextStripper();
            final String extractedText = stripper.getText(document).trim();

            if (extractedText.isBlank()) {
                log.warn("PDF document has no extractable text content");
                return "";
            }

            log.debug("Successfully parsed PDF document, extracted {} characters from {} pages", extractedText.length(), pageCount);
            
            return extractedText;
            }

        } catch (IOException e) {
            // Check if the IOException is due to encrypted PDF
            if (e.getMessage() != null && 
                (e.getMessage().contains("Cannot decrypt PDF") || 
                 e.getMessage().contains("password is incorrect") ||
                 e.getMessage().contains("encrypted"))) {
                log.error("PDF document is encrypted and cannot be parsed");
                throw new DocumentParsingException(
                        "Cannot parse encrypted PDF without password", 
                        contentType,
                        e
                );
            }
            log.error("Failed to read PDF document: {}", e.getMessage());
            throw new DocumentParsingException(
                    "Failed to read PDF content: " + e.getMessage(), 
                    contentType, 
                    e
            );
        } catch (Exception e) {
            log.error("Failed to parse PDF document: {}", e.getMessage());
            throw new DocumentParsingException(
                    "Failed to parse PDF content: " + e.getMessage(), 
                    contentType, 
                    e
            );
        }
    }

    @Override
    public boolean supports(String contentType) {
        return contentType != null && SUPPORTED_TYPES.contains(contentType.toLowerCase());
    }
}

