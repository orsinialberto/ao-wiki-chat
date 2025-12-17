package com.example.ao_wiki_chat.integration.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Set;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.ao_wiki_chat.exception.DocumentParsingException;

/**
 * Parser implementation for Markdown documents.
 * Uses CommonMark library to parse MD files and extract plain text.
 */
@Component
public final class MarkdownParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(MarkdownParser.class);

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "text/markdown",
            "text/x-markdown",
            "text/plain"
    );

    private final Parser parser;
    private final TextContentRenderer renderer;

    public MarkdownParser() {
        this.parser = Parser.builder().build();
        this.renderer = TextContentRenderer.builder().build();
    }

    @Override
    public String parse(InputStream inputStream, String contentType) {
        log.debug("Parsing markdown document with content-type: {}", contentType);

        if (inputStream == null) {
            throw new DocumentParsingException("Input stream cannot be null", contentType);
        }

        try {
            final String markdownContent = readInputStream(inputStream);

            if (markdownContent.isBlank()) {
                log.warn("Markdown document is empty");
                return "";
            }

            final Node document = parser.parse(markdownContent);
            final String plainText = renderer.render(document).trim();

            log.debug("Successfully parsed markdown document, extracted {} characters", plainText.length());
            
            return plainText;

        } catch (IOException e) {
            log.error("Failed to read markdown document: {}", e.getMessage());
            throw new DocumentParsingException(
                    "Failed to read markdown content: " + e.getMessage(), 
                    contentType, 
                    e
            );
        } catch (Exception e) {
            log.error("Failed to parse markdown document: {}", e.getMessage());
            throw new DocumentParsingException(
                    "Failed to parse markdown content: " + e.getMessage(), 
                    contentType, 
                    e
            );
        }
    }

    @Override
    public boolean supports(String contentType) {
        return contentType != null && SUPPORTED_TYPES.contains(contentType.toLowerCase());
    }

    private String readInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {
            final StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }
}


