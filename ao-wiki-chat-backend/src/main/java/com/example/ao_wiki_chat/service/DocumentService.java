package com.example.ao_wiki_chat.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.ao_wiki_chat.exception.DocumentParsingException;
import com.example.ao_wiki_chat.exception.EmbeddingException;
import com.example.ao_wiki_chat.integration.parser.DocumentParser;
import com.example.ao_wiki_chat.integration.parser.ParserFactory;
import com.example.ao_wiki_chat.model.entity.Chunk;
import com.example.ao_wiki_chat.model.entity.Document;
import com.example.ao_wiki_chat.model.enums.DocumentStatus;
import com.example.ao_wiki_chat.repository.ChunkRepository;
import com.example.ao_wiki_chat.repository.DocumentRepository;

/**
 * Service for document upload and processing operations.
 * Handles document validation, parsing, chunking, embedding generation, and storage.
 * Processing is performed asynchronously to avoid blocking the upload endpoint.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final ChunkRepository chunkRepository;
    private final ParserFactory parserFactory;
    private final ChunkingService chunkingService;
    private final GeminiEmbeddingService embeddingService;
    private final List<String> allowedContentTypes;
    private final long maxFileSizeBytes;
    private final Path storageDirectory;

    public DocumentService(
            DocumentRepository documentRepository,
            ChunkRepository chunkRepository,
            ParserFactory parserFactory,
            ChunkingService chunkingService,
            GeminiEmbeddingService embeddingService,
            @Value("${upload.allowed-types}") String allowedTypes,
            @Value("${spring.servlet.multipart.max-file-size:52428800}") String maxFileSize,
            @Value("${upload.storage-directory:./uploads}") String storageDir
    ) throws IOException {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.parserFactory = parserFactory;
        this.chunkingService = chunkingService;
        this.embeddingService = embeddingService;
        this.allowedContentTypes = parseAllowedTypes(allowedTypes);
        this.maxFileSizeBytes = parseFileSize(maxFileSize);
        this.storageDirectory = initializeStorageDirectory(storageDir);
        log.info(
            "DocumentService initialized with allowed types: {}, max size: {} bytes, storage: {}", 
            allowedContentTypes, 
            maxFileSizeBytes, 
            storageDirectory
        );
    }

    /**
     * Uploads a document and initiates asynchronous processing.
     * Validates file type and size, saves metadata, and triggers processing pipeline.
     *
     * @param file the uploaded file
     * @return the document ID
     * @throws IllegalArgumentException if file is invalid
     */
    @Transactional
    public UUID uploadDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        log.info(
            "Uploading document: {} ({} bytes, type: {})",
                file.getOriginalFilename(), 
                file.getSize(), 
                file.getContentType()
            );

        validateFile(file);

        Document document = Document.builder()
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .status(DocumentStatus.PROCESSING)
                .metadata(null)
                .build();

        Document saved = documentRepository.save(document);
        log.info("Document saved with ID: {}, status: PROCESSING", saved.getId());

        saveFileToStorage(saved.getId(), file);

        processDocumentAsync(saved.getId());

        return saved.getId();
    }

    /**
     * Processes a document asynchronously: parsing → chunking → embedding → storage.
     * Updates document status to COMPLETED or FAILED based on processing result.
     *
     * @param documentId the document ID to process
     */
    @Async("documentProcessingExecutor")
    public void processDocumentAsync(UUID documentId) {
        log.info("Starting async processing for document: {}", documentId);

        documentRepository.findById(documentId)
                .orElseThrow(() -> {
                    log.error("Document not found: {}", documentId);
                    return new IllegalArgumentException("Document not found: " + documentId);
                });

        try {
            processDocument(documentId);
            updateDocumentStatus(documentId, DocumentStatus.COMPLETED);
            log.info("Document processing completed successfully: {}", documentId);
        } catch (Exception e) {
            log.error("Document processing failed for {}: {}", documentId, e.getMessage(), e);
            updateDocumentStatus(documentId, DocumentStatus.FAILED);
        } finally {
            deleteFileFromStorage(documentId);
        }
    }

    /**
     * Processes a document: parsing, chunking, embedding generation, and chunk storage.
     * This method performs the actual processing work.
     *
     * @param documentId the document ID to process
     * @throws DocumentParsingException if parsing fails
     * @throws EmbeddingException if embedding generation fails
     */
    @Transactional
    public void processDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        log.debug("Processing document: {} (type: {})", documentId, document.getContentType());

        String textContent = parseDocument(document);
        log.debug("Document parsed, text length: {} characters", textContent.length());

        List<String> chunks = chunkingService.splitIntoChunks(textContent);
        log.info("Document split into {} chunks", chunks.size());

        if (chunks.isEmpty()) {
            log.warn("No chunks generated for document: {}", documentId);
            return;
        }

        List<float[]> embeddings = embeddingService.generateEmbeddings(chunks);
        log.debug("Generated {} embeddings", embeddings.size());

        if (embeddings.size() != chunks.size()) {
            throw new EmbeddingException(
                    String.format("Embedding count mismatch: expected %d, got %d", chunks.size(), embeddings.size())
            );
        }

        saveChunks(document, chunks, embeddings);
        log.info("Saved {} chunks for document: {}", chunks.size(), documentId);
    }

    /**
     * Validates file type and size.
     *
     * @param file the file to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !isContentTypeAllowed(contentType)) {
            throw new IllegalArgumentException(
                    String.format("Content type not allowed: %s. Allowed types: %s", contentType, allowedContentTypes)
            );
        }

        long fileSize = file.getSize();
        if (fileSize > maxFileSizeBytes) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum: %d bytes (max: %d bytes)", fileSize, maxFileSizeBytes)
            );
        }

        if (fileSize == 0) {
            throw new IllegalArgumentException("File is empty");
        }
    }

    /**
     * Checks if content type is in the allowed list.
     */
    private boolean isContentTypeAllowed(String contentType) {
        if (contentType == null) {
            return false;
        }

        String normalizedType = contentType.toLowerCase().trim();
        int semicolonIndex = normalizedType.indexOf(';');
        if (semicolonIndex > 0) {
            normalizedType = normalizedType.substring(0, semicolonIndex).trim();
        }

        return allowedContentTypes.contains(normalizedType);
    }

    /**
     * Parses document content using appropriate parser.
     */
    private String parseDocument(Document document) {
        try (InputStream inputStream = getDocumentInputStream(document)) {
            DocumentParser parser = parserFactory.getParser(document.getContentType());
            return parser.parse(inputStream, document.getContentType());
        } catch (IOException e) {
            throw new DocumentParsingException(
                    "Failed to read document: " + e.getMessage(),
                    document.getContentType(),
                    e
            );
        } catch (DocumentParsingException e) {
            throw e;
        } catch (Exception e) {
            throw new DocumentParsingException(
                    "Unexpected error during parsing: " + e.getMessage(),
                    document.getContentType(),
                    e
            );
        }
    }

    /**
     * Saves uploaded file to temporary storage for processing.
     */
    private void saveFileToStorage(UUID documentId, MultipartFile file) {
        try {
            Path filePath = storageDirectory.resolve(documentId.toString());
            Files.createDirectories(filePath.getParent());
            file.transferTo(filePath.toFile());
            log.debug("File saved to storage: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to save file to storage for document {}: {}", documentId, e.getMessage(), e);
            throw new IllegalArgumentException("Failed to save file: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves document input stream from storage.
     */
    private InputStream getDocumentInputStream(Document document) throws IOException {
        Path filePath = storageDirectory.resolve(document.getId().toString());
        File file = filePath.toFile();
        
        if (!file.exists()) {
            throw new IOException("File not found in storage: " + filePath);
        }
        
        return new FileInputStream(file);
    }

    /**
     * Deletes file from storage after processing.
     */
    private void deleteFileFromStorage(UUID documentId) {
        try {
            Path filePath = storageDirectory.resolve(documentId.toString());
            Files.deleteIfExists(filePath);
            log.debug("File deleted from storage: {}", filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file from storage for document {}: {}", documentId, e.getMessage());
        }
    }

    /**
     * Initializes storage directory, creating it if it doesn't exist.
     */
    private Path initializeStorageDirectory(String storageDir) throws IOException {
        Path dir = Paths.get(storageDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        log.info("Document storage directory initialized: {}", dir);
        return dir;
    }

    /**
     * Saves chunks with embeddings to database.
     */
    private void saveChunks(Document document, List<String> chunks, List<float[]> embeddings) {
        List<Chunk> chunkEntities = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = Chunk.builder()
                    .document(document)
                    .content(chunks.get(i))
                    .chunkIndex(i)
                    .embedding(embeddings.get(i))
                    .metadata(null)
                    .build();
            chunkEntities.add(chunk);
        }

        chunkRepository.saveAll(chunkEntities);
    }

    /**
     * Updates document status in database.
     */
    @Transactional
    public void updateDocumentStatus(UUID documentId, DocumentStatus status) {
        documentRepository.updateStatus(documentId, status);
        log.debug("Updated document {} status to: {}", documentId, status);
    }

    /**
     * Parses allowed content types from comma-separated string.
     */
    private List<String> parseAllowedTypes(String allowedTypes) {
        List<String> types = new ArrayList<>();
        if (allowedTypes != null && !allowedTypes.isBlank()) {
            String[] parts = allowedTypes.split(",");
            for (String part : parts) {
                String trimmed = part.trim().toLowerCase();
                if (!trimmed.isEmpty()) {
                    types.add(trimmed);
                }
            }
        }
        return types;
    }

    /**
     * Parses file size string (e.g., "50MB") to bytes.
     */
    private long parseFileSize(String sizeString) {
        if (sizeString == null || sizeString.isBlank()) {
            return 50 * 1024 * 1024; // Default 50MB
        }

        sizeString = sizeString.trim().toUpperCase();
        try {
            if (sizeString.endsWith("KB")) {
                long value = Long.parseLong(sizeString.substring(0, sizeString.length() - 2));
                return value * 1024;
            } else if (sizeString.endsWith("MB")) {
                long value = Long.parseLong(sizeString.substring(0, sizeString.length() - 2));
                return value * 1024 * 1024;
            } else if (sizeString.endsWith("GB")) {
                long value = Long.parseLong(sizeString.substring(0, sizeString.length() - 2));
                return value * 1024 * 1024 * 1024;
            } else {
                return Long.parseLong(sizeString);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid file size format: {}, using default 50MB", sizeString);
            return 50 * 1024 * 1024;
        }
    }

    /**
     * Retrieves all documents ordered by creation date (newest first).
     *
     * @return list of all documents
     */
    @Transactional(readOnly = true)
    public List<Document> getAllDocuments() {
        log.debug("Retrieving all documents");
        return documentRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Retrieves a document by ID.
     *
     * @param documentId the document ID
     * @return the document
     * @throws jakarta.persistence.EntityNotFoundException if document not found
     */
    @Transactional(readOnly = true)
    public Document getDocumentById(UUID documentId) {
        log.debug("Retrieving document: {}", documentId);
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Document not found: " + documentId));
    }

    /**
     * Deletes a document and all associated chunks.
     *
     * @param documentId the document ID to delete
     * @throws jakarta.persistence.EntityNotFoundException if document not found
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        log.info("Deleting document: {}", documentId);
        
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Document not found: " + documentId));
        
        chunkRepository.deleteByDocument_Id(documentId);
        documentRepository.delete(document);
        
        log.info("Document and associated chunks deleted: {}", documentId);
    }
}

