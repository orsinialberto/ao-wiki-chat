package com.example.ao_wiki_chat.controller;

import com.example.ao_wiki_chat.model.dto.ChunkResponse;
import com.example.ao_wiki_chat.model.dto.DocumentListResponse;
import com.example.ao_wiki_chat.model.dto.DocumentResponse;
import com.example.ao_wiki_chat.model.dto.DocumentUploadResponse;
import com.example.ao_wiki_chat.model.entity.Chunk;
import com.example.ao_wiki_chat.model.entity.Document;
import com.example.ao_wiki_chat.repository.ChunkRepository;
import com.example.ao_wiki_chat.service.DocumentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for document management operations.
 * Provides endpoints for uploading, listing, retrieving, and deleting documents.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;
    private final ChunkRepository chunkRepository;
    private final ObjectMapper objectMapper;

    public DocumentController(
            DocumentService documentService,
            ChunkRepository chunkRepository,
            ObjectMapper objectMapper
    ) {
        this.documentService = documentService;
        this.chunkRepository = chunkRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Uploads a document and initiates asynchronous processing.
     * Validates file at controller level before processing.
     *
     * @param file the uploaded file
     * @return document upload response with 201 Created status
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file
    ) {
        validateFileUpload(file);
        
        log.info("Received document upload request: {} ({} bytes, type: {})",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        UUID documentId = documentService.uploadDocument(file);
        Document document = documentService.getDocumentById(documentId);

        DocumentUploadResponse response = toDocumentUploadResponse(document);
        log.info("Document uploaded successfully: {}", documentId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns list of all documents.
     *
     * @return list of documents with 200 OK status
     */
    @GetMapping
    public ResponseEntity<DocumentListResponse> getAllDocuments() {
        log.debug("Retrieving all documents");

        List<Document> documents = documentService.getAllDocuments();
        List<DocumentResponse> documentResponses = documents.stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());

        DocumentListResponse response = new DocumentListResponse(documentResponses, documentResponses.size());
        log.debug("Retrieved {} documents", documentResponses.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Returns single document details.
     *
     * @param id the document ID
     * @return document details with 200 OK status, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable UUID id) {
        validateDocumentId(id);
        log.debug("Retrieving document: {}", id);

        try {
            Document document = documentService.getDocumentById(id);
            DocumentResponse response = toDocumentResponse(document);
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            log.warn("Document not found: {}", id);
            throw e;
        }
    }

    /**
     * Deletes a document and all associated chunks.
     *
     * @param id the document ID
     * @return 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        validateDocumentId(id);
        log.info("Deleting document: {}", id);

        try {
            documentService.deleteDocument(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            log.warn("Document not found for deletion: {}", id);
            throw e;
        }
    }

    /**
     * Returns all chunks for a document.
     *
     * @param id the document ID
     * @return list of chunks with 200 OK status, or 404 if document not found
     */
    @GetMapping("/{id}/chunks")
    public ResponseEntity<List<ChunkResponse>> getDocumentChunks(@PathVariable UUID id) {
        validateDocumentId(id);
        log.debug("Retrieving chunks for document: {}", id);

        // Verify document exists
        documentService.getDocumentById(id);

        List<Chunk> chunks = chunkRepository.findByDocument_IdOrderByChunkIndexAsc(id);
        List<ChunkResponse> chunkResponses = chunks.stream()
                .map(this::toChunkResponse)
                .collect(Collectors.toList());

        log.debug("Retrieved {} chunks for document: {}", chunkResponses.size(), id);
        return ResponseEntity.ok(chunkResponses);
    }

    /**
     * Converts Document entity to DocumentUploadResponse DTO.
     */
    private DocumentUploadResponse toDocumentUploadResponse(Document document) {
        return new DocumentUploadResponse(
                document.getId(),
                document.getFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getStatus(),
                document.getCreatedAt()
        );
    }

    /**
     * Converts Document entity to DocumentResponse DTO.
     * Parses JSON metadata string to Map if present.
     */
    private DocumentResponse toDocumentResponse(Document document) {
        Map<String, Object> metadata = parseMetadata(document.getMetadata());
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getContentType(),
                document.getFileSize(),
                document.getStatus(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                metadata
        );
    }

    /**
     * Converts Chunk entity to ChunkResponse DTO.
     */
    private ChunkResponse toChunkResponse(Chunk chunk) {
        return new ChunkResponse(
                chunk.getId(),
                chunk.getDocument().getId(),
                chunk.getContent(),
                chunk.getChunkIndex(),
                chunk.getCreatedAt()
        );
    }

    /**
     * Parses JSON metadata string to Map.
     * Returns null if metadata is null or empty.
     */
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse metadata JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validates file upload at controller level.
     * Checks for null, empty file, and basic file properties.
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFileUpload(MultipartFile file) {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() == 0) {
            throw new IllegalArgumentException("File size must be greater than 0");
        }

        if (file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or blank");
        }

        if (file.getContentType() == null || file.getContentType().isBlank()) {
            throw new IllegalArgumentException("File content type cannot be null or blank");
        }
    }

    /**
     * Validates document ID (UUID).
     *
     * @param id the document ID to validate
     * @throws IllegalArgumentException if document ID is null
     */
    private void validateDocumentId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Document ID cannot be null");
        }
    }
}
