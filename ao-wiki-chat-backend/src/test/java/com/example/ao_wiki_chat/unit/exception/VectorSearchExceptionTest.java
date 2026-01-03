package com.example.ao_wiki_chat.unit.exception;

import com.example.ao_wiki_chat.exception.VectorSearchException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for VectorSearchException.
 */
class VectorSearchExceptionTest {
    
    @Test
    void constructorWithMessageCreatesExceptionWithMessage() {
        // Given
        String message = "Vector search error message";
        
        // When
        VectorSearchException exception = new VectorSearchException(message);
        
        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }
    
    @Test
    void constructorWithMessageAndCauseCreatesExceptionWithBoth() {
        // Given
        String message = "Vector search error message";
        Throwable cause = new RuntimeException("Root cause");
        
        // When
        VectorSearchException exception = new VectorSearchException(message, cause);
        
        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
    
    @Test
    void exceptionIsRuntimeException() {
        // Given
        VectorSearchException exception = new VectorSearchException("Test");
        
        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}

