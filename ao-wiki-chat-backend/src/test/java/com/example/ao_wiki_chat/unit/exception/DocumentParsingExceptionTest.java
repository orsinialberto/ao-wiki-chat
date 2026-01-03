package com.example.ao_wiki_chat.unit.exception;

import com.example.ao_wiki_chat.exception.DocumentParsingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentParsingExceptionTest {

    @Test
    void constructorWithMessageAndContentTypeSetsProperties() {
        // Given
        final String message = "Parsing failed";
        final String contentType = "application/pdf";

        // When
        final DocumentParsingException exception = 
                new DocumentParsingException(message, contentType);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getContentType()).isEqualTo(contentType);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void constructorWithMessageContentTypeAndCauseSetsProperties() {
        // Given
        final String message = "Parsing failed";
        final String contentType = "text/html";
        final Throwable cause = new RuntimeException("Root cause");

        // When
        final DocumentParsingException exception = 
                new DocumentParsingException(message, contentType, cause);

        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getContentType()).isEqualTo(contentType);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}


