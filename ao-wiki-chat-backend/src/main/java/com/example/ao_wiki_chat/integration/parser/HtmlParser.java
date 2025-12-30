package com.example.ao_wiki_chat.integration.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.ao_wiki_chat.exception.DocumentParsingException;

/**
 * Parser implementation for HTML documents.
 * Uses Jsoup to parse HTML and extract clean text content.
 */
@Component
public final class HtmlParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(HtmlParser.class);

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "text/html",
            "application/xhtml+xml"
    );

    @Override
    public String parse(InputStream inputStream, String contentType) {
        log.debug("Parsing HTML document with content-type: {}", contentType);

        if (inputStream == null) {
            throw new DocumentParsingException("Input stream cannot be null", contentType);
        }

        try {
            final Document document = Jsoup.parse(
                    inputStream, 
                    StandardCharsets.UTF_8.name(), 
                    ""
            );

            // Extract text from body, removing scripts and styles
            final String rawText = document.body().text();

            if (rawText.isBlank()) {
                log.warn("HTML document has no text content");
                return "";
            }

            // Clean and normalize whitespace
            final String cleanText = normalizeWhitespace(rawText);

            log.debug("Successfully parsed HTML document, extracted {} characters", cleanText.length());

            return cleanText;

        } catch (IOException e) {
            log.error("Failed to read HTML document: {}", e.getMessage());
            throw new DocumentParsingException(
                    "Failed to read HTML content: " + e.getMessage(), 
                    contentType, 
                    e
            );
        } catch (Exception e) {
            log.error("Failed to parse HTML document: {}", e.getMessage());
            throw new DocumentParsingException(
                    "Failed to parse HTML content: " + e.getMessage(), 
                    contentType, 
                    e
            );
        }
    }

    @Override
    public boolean supports(String contentType) {
        return contentType != null && SUPPORTED_TYPES.contains(contentType.toLowerCase());
    }

    /**
     * Normalizes whitespace in text: removes extra spaces, tabs, newlines.
     */
    private String normalizeWhitespace(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }
}

