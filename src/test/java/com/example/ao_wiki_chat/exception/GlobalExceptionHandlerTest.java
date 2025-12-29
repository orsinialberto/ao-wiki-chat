package com.example.ao_wiki_chat.exception;

import com.example.ao_wiki_chat.model.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleIllegalArgumentExceptionReturnsBadRequest() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Invalid argument");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void handleDocumentParsingExceptionReturnsUnprocessableEntity() {
        // Given
        DocumentParsingException ex = new DocumentParsingException("Parsing failed", "application/pdf");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDocumentParsingException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(422);
        assertThat(response.getBody().error()).isEqualTo("Unprocessable Entity");
        assertThat(response.getBody().message()).isEqualTo("Parsing failed");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void handleEmbeddingExceptionReturnsInternalServerError() {
        // Given
        EmbeddingException ex = new EmbeddingException("Embedding failed");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleEmbeddingException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("Failed to generate embeddings. Please try again later.");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void handleVectorSearchExceptionReturnsInternalServerError() {
        // Given
        VectorSearchException ex = new VectorSearchException("Vector search failed");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleVectorSearchException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("Vector search operation failed. Please try again later.");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void handleLLMExceptionReturnsInternalServerError() {
        // Given
        LLMException ex = new LLMException("LLM operation failed");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleLLMException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("LLM operation failed. Please try again later.");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void handleEntityNotFoundExceptionReturnsNotFound() {
        // Given
        EntityNotFoundException ex = new EntityNotFoundException("Entity not found");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFoundException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Entity not found");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void handleEntityNotFoundExceptionWithNullMessageReturnsDefaultMessage() {
        // Given
        EntityNotFoundException ex = new EntityNotFoundException();

        // When
        ResponseEntity<ErrorResponse> response = handler.handleEntityNotFoundException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).isEqualTo("Requested resource not found");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void handleMethodArgumentNotValidExceptionReturnsBadRequestWithFieldErrors() {
        // Given
        MethodArgumentNotValidException ex = createMethodArgumentNotValidException(
                List.of(
                        new FieldError("chatRequest", "query", "Query cannot be blank"),
                        new FieldError("chatRequest", "sessionId", "Session ID must be between 1 and 255 characters")
                )
        );

        // When
        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("Validation failed");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().fieldErrors()).isNotNull();
        assertThat(response.getBody().fieldErrors()).hasSize(2);
        assertThat(response.getBody().fieldErrors().get("query")).isEqualTo("Query cannot be blank");
        assertThat(response.getBody().fieldErrors().get("sessionId")).isEqualTo("Session ID must be between 1 and 255 characters");
    }

    @Test
    void handleMethodArgumentNotValidExceptionWithEmptyErrorsReturnsBadRequest() {
        // Given
        MethodArgumentNotValidException ex = createMethodArgumentNotValidException(Collections.emptyList());

        // When
        ResponseEntity<ErrorResponse> response = handler.handleMethodArgumentNotValidException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().fieldErrors()).isNotNull();
        assertThat(response.getBody().fieldErrors()).isEmpty();
    }

    @Test
    void handleMultipartExceptionReturnsBadRequest() {
        // Given
        MultipartException ex = new MultipartException("File upload failed");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleMultipartException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("File upload failed. Please check the file format and size.");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void handleMultipartExceptionWithSizeExceededReturnsSizeErrorMessage() {
        // Given
        MultipartException ex = new MaxUploadSizeExceededException(50_000_000L, new RuntimeException("exceed"));

        // When
        ResponseEntity<ErrorResponse> response = handler.handleMultipartException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).isEqualTo("File size exceeds maximum allowed limit");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void handleGenericExceptionReturnsInternalServerError() {
        // Given
        RuntimeException ex = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred. Please try again later.");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
    }

    @Test
    void errorResponseIncludesTimestamp() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Test error");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgumentException(ex, request);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().timestamp()).isNotNull();
        assertThat(response.getBody().timestamp()).isNotEmpty();
    }

    private MethodArgumentNotValidException createMethodArgumentNotValidException(List<FieldError> fieldErrors) {
        BindingResult bindingResult = org.mockito.Mockito.mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(fieldErrors.stream()
                .map(error -> (org.springframework.validation.ObjectError) error)
                .toList());
        
        return new MethodArgumentNotValidException(null, bindingResult);
    }
}
