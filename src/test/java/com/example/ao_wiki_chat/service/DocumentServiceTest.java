package com.example.ao_wiki_chat.service;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private ParserFactory parserFactory;

    @Mock
    private ChunkingService chunkingService;

    @Mock
    private GeminiEmbeddingService embeddingService;

    @Mock
    private DocumentParser documentParser;

    @Mock
    private MultipartFile multipartFile;

    @TempDir
    Path tempDir;

    private DocumentService documentService;

    private UUID documentId;
    private Document document;
    private String testContent;

    @BeforeEach
    void setUp() throws IOException {
        documentId = UUID.randomUUID();
        testContent = "This is a test document content for chunking and embedding.";

        document = Document.builder()
                .id(documentId)
                .filename("test.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .status(DocumentStatus.PROCESSING)
                .metadata(null)
                .build();

        // Initialize service with temp directory
        DocumentService service = new DocumentService(
                documentRepository,
                chunkRepository,
                parserFactory,
                chunkingService,
                embeddingService,
                "application/pdf,text/markdown,text/html,text/plain",
                "50MB",
                tempDir.toString()
        );
        
        // Create spy to avoid async execution in tests
        documentService = spy(service);
    }

    @Test
    void uploadDocumentWhenValidFileReturnsDocumentId() throws IOException {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        doNothing().when(multipartFile).transferTo(any(java.io.File.class));
        doNothing().when(documentService).processDocumentAsync(any(UUID.class));

        // When
        UUID result = documentService.uploadDocument(multipartFile);

        // Then
        assertThat(result).isEqualTo(documentId);
        verify(documentRepository).save(any(Document.class));
        verify(multipartFile).transferTo(any(java.io.File.class));
        verify(documentService).processDocumentAsync(documentId);
    }

    @Test
    void uploadDocumentWhenFileIsNullThrowsException() {
        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File cannot be null or empty");
    }

    @Test
    void uploadDocumentWhenFileIsEmptyThrowsException() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File cannot be null or empty");
    }

    @Test
    void uploadDocumentWhenContentTypeNotAllowedThrowsException() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/json");
        when(multipartFile.getSize()).thenReturn(1024L);

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content type not allowed");
    }

    @Test
    void uploadDocumentWhenFileSizeExceedsMaximumThrowsException() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(100 * 1024 * 1024L + 1); // > 50MB

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds maximum");
    }

    @Test
    void uploadDocumentWhenFileSizeIsZeroThrowsException() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(0L);

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File is empty");
    }

    @Test
    void uploadDocumentWhenContentTypeIsNullThrowsException() {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn(null);
        when(multipartFile.getSize()).thenReturn(1024L);

        // When/Then
        assertThatThrownBy(() -> documentService.uploadDocument(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Content type not allowed");
    }

    @Test
    void uploadDocumentWhenContentTypeHasSemicolonNormalizesCorrectly() throws IOException {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf; charset=utf-8");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        doNothing().when(multipartFile).transferTo(any(java.io.File.class));
        doNothing().when(documentService).processDocumentAsync(any(UUID.class));

        // When
        UUID result = documentService.uploadDocument(multipartFile);

        // Then
        assertThat(result).isEqualTo(documentId);
        verify(documentRepository).save(any(Document.class));
        verify(multipartFile).transferTo(any(java.io.File.class));
        verify(documentService).processDocumentAsync(documentId);
    }

    @Test
    void uploadDocumentWhenContentTypeIsCaseInsensitiveAcceptsUpperCase() throws IOException {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getContentType()).thenReturn("APPLICATION/PDF");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        doNothing().when(multipartFile).transferTo(any(java.io.File.class));
        doNothing().when(documentService).processDocumentAsync(any(UUID.class));

        // When
        UUID result = documentService.uploadDocument(multipartFile);

        // Then
        assertThat(result).isEqualTo(documentId);
        verify(documentRepository).save(any(Document.class));
        verify(multipartFile).transferTo(any(java.io.File.class));
        verify(documentService).processDocumentAsync(documentId);
    }

    @Test
    void uploadDocumentWhenContentTypeIsCaseInsensitiveAcceptsMixedCase() throws IOException {
        // Given
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getContentType()).thenReturn("Application/Pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(documentRepository.save(any(Document.class))).thenReturn(document);
        doNothing().when(multipartFile).transferTo(any(java.io.File.class));
        doNothing().when(documentService).processDocumentAsync(any(UUID.class));

        // When
        UUID result = documentService.uploadDocument(multipartFile);

        // Then
        assertThat(result).isEqualTo(documentId);
        verify(documentRepository).save(any(Document.class));
        verify(multipartFile).transferTo(any(java.io.File.class));
        verify(documentService).processDocumentAsync(documentId);
    }

    @Test
    void processDocumentWhenValidDocumentProcessesSuccessfully() throws IOException {
        // Given
        List<String> chunks = Arrays.asList("Chunk 1", "Chunk 2");
        List<float[]> embeddings = Arrays.asList(
                new float[768], // Mock embeddings with correct dimension
                new float[768]
        );

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(parserFactory.getParser("application/pdf")).thenReturn(documentParser);
        when(documentParser.parse(any(java.io.InputStream.class), eq("application/pdf")))
                .thenReturn(testContent);
        when(chunkingService.splitIntoChunks(testContent)).thenReturn(chunks);
        when(embeddingService.generateEmbeddings(chunks)).thenReturn(embeddings);
        List<Chunk> savedChunks = Arrays.asList(
                Chunk.builder()
                        .document(document)
                        .content("Chunk 1")
                        .chunkIndex(0)
                        .embedding(new float[768])
                        .build(),
                Chunk.builder()
                        .document(document)
                        .content("Chunk 2")
                        .chunkIndex(1)
                        .embedding(new float[768])
                        .build()
        );
        when(chunkRepository.saveAll(any(List.class))).thenReturn(savedChunks);

        // Create a temporary file for the document (simulating file storage)
        Path filePath = tempDir.resolve(documentId.toString());
        Files.write(filePath, testContent.getBytes());

        // When
        documentService.processDocument(documentId);

        // Then
        verify(documentParser).parse(any(java.io.InputStream.class), eq("application/pdf"));
        verify(chunkingService).splitIntoChunks(testContent);
        verify(embeddingService).generateEmbeddings(chunks);
        verify(chunkRepository).saveAll(any(List.class));
    }

    @Test
    void processDocumentWhenDocumentNotFoundThrowsException() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> documentService.processDocument(documentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void processDocumentWhenParsingFailsThrowsException() throws IOException {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(parserFactory.getParser("application/pdf")).thenReturn(documentParser);
        when(documentParser.parse(any(java.io.InputStream.class), eq("application/pdf")))
                .thenThrow(new DocumentParsingException("Parsing failed", "application/pdf"));

        Path filePath = tempDir.resolve(documentId.toString());
        Files.write(filePath, testContent.getBytes());

        // When/Then
        assertThatThrownBy(() -> documentService.processDocument(documentId))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Parsing failed");
    }

    @Test
    void processDocumentWhenEmbeddingGenerationFailsThrowsException() throws IOException {
        // Given
        List<String> chunks = Arrays.asList("Chunk 1", "Chunk 2");

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(parserFactory.getParser("application/pdf")).thenReturn(documentParser);
        when(documentParser.parse(any(java.io.InputStream.class), eq("application/pdf")))
                .thenReturn(testContent);
        when(chunkingService.splitIntoChunks(testContent)).thenReturn(chunks);
        when(embeddingService.generateEmbeddings(chunks))
                .thenThrow(new EmbeddingException("Embedding generation failed"));

        Path filePath = tempDir.resolve(documentId.toString());
        Files.write(filePath, testContent.getBytes());

        // When/Then
        assertThatThrownBy(() -> documentService.processDocument(documentId))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Embedding generation failed");
    }

    @Test
    void processDocumentWhenEmbeddingCountMismatchThrowsException() throws IOException {
        // Given
        List<String> chunks = Arrays.asList("Chunk 1", "Chunk 2");
        float[] singleEmbedding = new float[768];
        List<float[]> embeddings = Arrays.asList(singleEmbedding); // Only 1 embedding

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(parserFactory.getParser("application/pdf")).thenReturn(documentParser);
        when(documentParser.parse(any(java.io.InputStream.class), eq("application/pdf")))
                .thenReturn(testContent);
        when(chunkingService.splitIntoChunks(testContent)).thenReturn(chunks);
        when(embeddingService.generateEmbeddings(chunks)).thenReturn(embeddings);

        Path filePath = tempDir.resolve(documentId.toString());
        Files.write(filePath, testContent.getBytes());

        // When/Then
        assertThatThrownBy(() -> documentService.processDocument(documentId))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Embedding count mismatch");
    }

    @Test
    void processDocumentWhenNoChunksGeneratedSkipsSaving() throws IOException {
        // Given
        List<String> emptyChunks = List.of();

        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(parserFactory.getParser("application/pdf")).thenReturn(documentParser);
        when(documentParser.parse(any(java.io.InputStream.class), eq("application/pdf")))
                .thenReturn(testContent);
        when(chunkingService.splitIntoChunks(testContent)).thenReturn(emptyChunks);

        Path filePath = tempDir.resolve(documentId.toString());
        Files.write(filePath, testContent.getBytes());

        // When
        documentService.processDocument(documentId);

        // Then
        verify(chunkRepository, never()).saveAll(any(List.class));
    }

    @Test
    void updateDocumentStatusWhenValidIdUpdatesStatus() {
        // Given
        doNothing().when(documentRepository).updateStatus(documentId, DocumentStatus.COMPLETED);

        // When
        documentService.updateDocumentStatus(documentId, DocumentStatus.COMPLETED);

        // Then
        verify(documentRepository).updateStatus(documentId, DocumentStatus.COMPLETED);
    }

    @Test
    void parseDocumentWhenFileNotFoundThrowsDocumentParsingException() {
        // Given
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        // File does not exist in storage

        // When/Then
        assertThatThrownBy(() -> documentService.processDocument(documentId))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Failed to read document")
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void parseDocumentWhenParserThrowsGenericExceptionWrapsInDocumentParsingException() throws IOException {
        // Given
        RuntimeException genericException = new RuntimeException("Generic parsing error");
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(parserFactory.getParser("application/pdf")).thenReturn(documentParser);
        when(documentParser.parse(any(java.io.InputStream.class), eq("application/pdf")))
                .thenThrow(genericException);

        Path filePath = tempDir.resolve(documentId.toString());
        Files.write(filePath, testContent.getBytes());

        // When/Then
        assertThatThrownBy(() -> documentService.processDocument(documentId))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessageContaining("Unexpected error during parsing")
                .hasCause(genericException);
    }

    @Test
    void parseDocumentWhenParserThrowsDocumentParsingExceptionReThrowsWithoutWrapping() throws IOException {
        // Given
        DocumentParsingException originalException = new DocumentParsingException(
                "Original parsing error", 
                "application/pdf"
        );
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(parserFactory.getParser("application/pdf")).thenReturn(documentParser);
        when(documentParser.parse(any(java.io.InputStream.class), eq("application/pdf")))
                .thenThrow(originalException);

        Path filePath = tempDir.resolve(documentId.toString());
        Files.write(filePath, testContent.getBytes());

        // When/Then
        assertThatThrownBy(() -> documentService.processDocument(documentId))
                .isInstanceOf(DocumentParsingException.class)
                .hasMessage("Original parsing error")
                .isSameAs(originalException);
    }

    @Test
    void parseFileSizeWhenKbFormatReturnsCorrectBytes() throws Exception {
        // Given
        Method parseFileSizeMethod = DocumentService.class.getDeclaredMethod("parseFileSize", String.class);
        parseFileSizeMethod.setAccessible(true);
        String sizeString = "100KB";

        // When
        long result = (long) parseFileSizeMethod.invoke(documentService, sizeString);

        // Then
        assertThat(result).isEqualTo(100 * 1024L);
    }

    @Test
    void parseFileSizeWhenMbFormatReturnsCorrectBytes() throws Exception {
        // Given
        Method parseFileSizeMethod = DocumentService.class.getDeclaredMethod("parseFileSize", String.class);
        parseFileSizeMethod.setAccessible(true);
        String sizeString = "50MB";

        // When
        long result = (long) parseFileSizeMethod.invoke(documentService, sizeString);

        // Then
        assertThat(result).isEqualTo(50 * 1024 * 1024L);
    }

    @Test
    void parseFileSizeWhenGbFormatReturnsCorrectBytes() throws Exception {
        // Given
        Method parseFileSizeMethod = DocumentService.class.getDeclaredMethod("parseFileSize", String.class);
        parseFileSizeMethod.setAccessible(true);
        String sizeString = "1GB";

        // When
        long result = (long) parseFileSizeMethod.invoke(documentService, sizeString);

        // Then
        assertThat(result).isEqualTo(1024 * 1024 * 1024L);
    }

    @Test
    void parseFileSizeWhenNumericFormatReturnsCorrectBytes() throws Exception {
        // Given
        Method parseFileSizeMethod = DocumentService.class.getDeclaredMethod("parseFileSize", String.class);
        parseFileSizeMethod.setAccessible(true);
        String sizeString = "52428800";

        // When
        long result = (long) parseFileSizeMethod.invoke(documentService, sizeString);

        // Then
        assertThat(result).isEqualTo(52428800L);
    }

    @Test
    void parseFileSizeWhenInvalidFormatReturnsDefault50Mb() throws Exception {
        // Given
        Method parseFileSizeMethod = DocumentService.class.getDeclaredMethod("parseFileSize", String.class);
        parseFileSizeMethod.setAccessible(true);
        String sizeString = "invalid";

        // When
        long result = (long) parseFileSizeMethod.invoke(documentService, sizeString);

        // Then
        assertThat(result).isEqualTo(50 * 1024 * 1024L);
    }

    @Test
    void parseFileSizeWhenNullReturnsDefault50Mb() throws Exception {
        // Given
        Method parseFileSizeMethod = DocumentService.class.getDeclaredMethod("parseFileSize", String.class);
        parseFileSizeMethod.setAccessible(true);

        // When
        long result = (long) parseFileSizeMethod.invoke(documentService, (String) null);

        // Then
        assertThat(result).isEqualTo(50 * 1024 * 1024L);
    }

    @Test
    void parseFileSizeWhenBlankReturnsDefault50Mb() throws Exception {
        // Given
        Method parseFileSizeMethod = DocumentService.class.getDeclaredMethod("parseFileSize", String.class);
        parseFileSizeMethod.setAccessible(true);
        String sizeString = "   ";

        // When
        long result = (long) parseFileSizeMethod.invoke(documentService, sizeString);

        // Then
        assertThat(result).isEqualTo(50 * 1024 * 1024L);
    }

    @Test
    void parseFileSizeWhenEmptyStringReturnsDefault50Mb() throws Exception {
        // Given
        Method parseFileSizeMethod = DocumentService.class.getDeclaredMethod("parseFileSize", String.class);
        parseFileSizeMethod.setAccessible(true);
        String sizeString = "";

        // When
        long result = (long) parseFileSizeMethod.invoke(documentService, sizeString);

        // Then
        assertThat(result).isEqualTo(50 * 1024 * 1024L);
    }

    @Test
    void parseFileSizeWhenCaseInsensitiveReturnsCorrectBytes() throws Exception {
        // Given
        Method parseFileSizeMethod = DocumentService.class.getDeclaredMethod("parseFileSize", String.class);
        parseFileSizeMethod.setAccessible(true);
        String sizeString = "100kb";

        // When
        long result = (long) parseFileSizeMethod.invoke(documentService, sizeString);

        // Then
        assertThat(result).isEqualTo(100 * 1024L);
    }

    @Test
    void parseFileSizeWhenWithWhitespaceReturnsCorrectBytes() throws Exception {
        // Given
        Method parseFileSizeMethod = DocumentService.class.getDeclaredMethod("parseFileSize", String.class);
        parseFileSizeMethod.setAccessible(true);
        String sizeString = "  100MB  ";

        // When
        long result = (long) parseFileSizeMethod.invoke(documentService, sizeString);

        // Then
        assertThat(result).isEqualTo(100 * 1024 * 1024L);
    }
}

