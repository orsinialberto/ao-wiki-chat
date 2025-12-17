package com.example.ao_wiki_chat.integration.parser;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.ao_wiki_chat.exception.DocumentParsingException;

/**
 * Factory for selecting appropriate document parser based on content type.
 * Uses Spring's dependency injection to discover all available parsers.
 */
@Component
public final class ParserFactory {

    private static final Logger log = LoggerFactory.getLogger(ParserFactory.class);

    private final List<DocumentParser> parsers;

    public ParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * Returns the appropriate parser for the given content type.
     *
     * @param contentType the MIME type of the document
     * @return the matching DocumentParser
     * @throws DocumentParsingException if no parser supports the content type
     */
    public DocumentParser getParser(String contentType) {
        log.debug("Selecting parser for content-type: {}", contentType);

        if (contentType == null || contentType.isBlank()) {
            throw new DocumentParsingException(
                    "Content type cannot be null or blank", 
                    contentType
            );
        }

        // Normalize content-type (remove charset if present)
        final String normalizedType = normalizeContentType(contentType);

        return parsers.stream()
                .filter(parser -> parser.supports(normalizedType))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("No parser available for content-type: {}", normalizedType);
                    return new DocumentParsingException(
                            "Unsupported content type: " + normalizedType, 
                            normalizedType
                    );
                });
    }

    /**
     * Checks if any parser supports the given content type.
     *
     * @param contentType the MIME type to check
     * @return true if a parser is available
     */
    public boolean isSupported(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return false;
        }

        final String normalizedType = normalizeContentType(contentType);
        return parsers.stream().anyMatch(parser -> parser.supports(normalizedType));
    }

    /**
     * Normalizes content type by removing charset and other parameters.
     * Example: "text/html; charset=UTF-8" -> "text/html"
     */
    private String normalizeContentType(String contentType) {
        final int semicolonIndex = contentType.indexOf(';');
        if (semicolonIndex > 0) {
            return contentType.substring(0, semicolonIndex).trim().toLowerCase();
        }
        return contentType.trim().toLowerCase();
    }
}


