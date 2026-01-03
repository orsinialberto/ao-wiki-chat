package com.example.ao_wiki_chat.unit.exception;

import com.example.ao_wiki_chat.exception.EmbeddingException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EmbeddingException.
 */
class EmbeddingExceptionTest {
    
    @Test
    void constructorWithMessageCreatesExceptionWithMessage() {
        // Given
        String message = "Embedding error message";
        
        // When
        EmbeddingException exception = new EmbeddingException(message);
        
        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }
    
    @Test
    void constructorWithMessageAndCauseCreatesExceptionWithBoth() {
        // Given
        String message = "Embedding error message";
        Throwable cause = new RuntimeException("Root cause");
        
        // When
        EmbeddingException exception = new EmbeddingException(message, cause);
        
        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
    
    @Test
    void exceptionIsRuntimeException() {
        // Given
        EmbeddingException exception = new EmbeddingException("Test");
        
        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}

