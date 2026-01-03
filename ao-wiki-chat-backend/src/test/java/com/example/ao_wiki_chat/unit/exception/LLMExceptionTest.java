package com.example.ao_wiki_chat.unit.exception;

import com.example.ao_wiki_chat.exception.LLMException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LLMException.
 */
class LLMExceptionTest {
    
    @Test
    void constructorWithMessageCreatesExceptionWithMessage() {
        // Given
        String message = "Test error message";
        
        // When
        LLMException exception = new LLMException(message);
        
        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isNull();
    }
    
    @Test
    void constructorWithMessageAndCauseCreatesExceptionWithBoth() {
        // Given
        String message = "Test error message";
        Throwable cause = new RuntimeException("Root cause");
        
        // When
        LLMException exception = new LLMException(message, cause);
        
        // Then
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
    
    @Test
    void exceptionIsRuntimeException() {
        // Given
        LLMException exception = new LLMException("Test");
        
        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }
}

