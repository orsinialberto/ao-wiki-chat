package com.example.ao_wiki_chat.exception;

import com.example.ao_wiki_chat.model.dto.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all controllers.
 * Provides centralized exception handling with standardized error responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles IllegalArgumentException (400 Bad Request).
     * Used for invalid method arguments or business logic violations.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles DocumentParsingException (422 Unprocessable Entity).
     * Used when document parsing fails due to format or content issues.
     */
    @ExceptionHandler(DocumentParsingException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<ErrorResponse> handleDocumentParsingException(
            DocumentParsingException ex, HttpServletRequest request) {
        log.warn("Document parsing failed for content type {}: {}", ex.getContentType(), ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Unprocessable Entity",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Handles EmbeddingException (500 Internal Server Error).
     * Used when embedding generation fails.
     */
    @ExceptionHandler(EmbeddingException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleEmbeddingException(
            EmbeddingException ex, HttpServletRequest request) {
        log.error("Embedding generation failed: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Failed to generate embeddings. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles VectorSearchException (500 Internal Server Error).
     * Used when vector search operations fail.
     */
    @ExceptionHandler(VectorSearchException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleVectorSearchException(
            VectorSearchException ex, HttpServletRequest request) {
        log.error("Vector search failed: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Vector search operation failed. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles LLMException (500 Internal Server Error).
     * Used when LLM operations fail.
     */
    @ExceptionHandler(LLMException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleLLMException(
            LLMException ex, HttpServletRequest request) {
        log.error("LLM operation failed: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "LLM operation failed. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles EntityNotFoundException (404 Not Found).
     * Used when JPA entities are not found.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Entity not found: {}", ex.getMessage());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage() != null ? ex.getMessage() : "Requested resource not found",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles MethodArgumentNotValidException (400 Bad Request).
     * Used when request validation fails.
     * Includes detailed field-level error messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError 
                    ? ((FieldError) error).getField() 
                    : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation failed: {}", fieldErrors);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Validation failed",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles MultipartException (400 Bad Request).
     * Used when file upload operations fail.
     */
    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMultipartException(
            MultipartException ex, HttpServletRequest request) {
        log.warn("File upload failed: {}", ex.getMessage());
        String message = ex.getMessage() != null && ex.getMessage().contains("exceed")
                ? "File size exceeds maximum allowed limit"
                : "File upload failed. Please check the file format and size.";
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                message,
                request.getRequestURI()
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles all other unhandled exceptions (500 Internal Server Error).
     * This is a catch-all handler for any exception not explicitly handled above.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
