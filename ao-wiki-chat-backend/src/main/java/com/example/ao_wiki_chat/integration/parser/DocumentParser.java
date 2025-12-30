package com.example.ao_wiki_chat.integration.parser;

import java.io.InputStream;

/**
 * Interface for document parsing operations.
 * Implementations handle different content types (PDF, HTML, Markdown, etc.)
 */
public interface DocumentParser {

    /**
     * Parses document content from an input stream.
     *
     * @param inputStream the input stream containing document data
     * @param contentType the MIME type of the document
     * @return the extracted text content
     * @throws com.example.ao_wiki_chat.exception.DocumentParsingException if parsing fails
     */
    String parse(InputStream inputStream, String contentType);

    /**
     * Checks if this parser supports the given content type.
     *
     * @param contentType the MIME type to check
     * @return true if this parser can handle the content type
     */
    boolean supports(String contentType);
}


