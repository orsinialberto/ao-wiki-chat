package com.example.ao_wiki_chat.controller;

import com.example.ao_wiki_chat.model.dto.ChunkResponse;
import com.example.ao_wiki_chat.model.dto.DocumentListResponse;
import com.example.ao_wiki_chat.model.dto.DocumentResponse;
import com.example.ao_wiki_chat.model.dto.DocumentUploadResponse;
import com.example.ao_wiki_chat.model.entity.Chunk;
import com.example.ao_wiki_chat.model.entity.Document;
import com.example.ao_wiki_chat.model.enums.DocumentStatus;
import com.example.ao_wiki_chat.repository.ChunkRepository;
import com.example.ao_wiki_chat.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private ChunkRepository chunkRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MultipartFile multipartFile;

    private DocumentController controller;

    private UUID documentId;
    private Document document;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @BeforeEach
    void setUp() {
        controller = new DocumentController(documentService, chunkRepository, objectMapper);

        documentId = UUID.randomUUID();
        createdAt = LocalDateTime.now().minusHours(1);
        updatedAt = LocalDateTime.now();

        document = Document.builder()
                .id(documentId)
                .filename("test.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .status(DocumentStatus.PROCESSING)
                .metadata(null)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    @Test
    void uploadDocumentWhenValidFileReturnsCreatedStatus() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(documentService.uploadDocument(multipartFile)).thenReturn(documentId);
        when(documentService.getDocumentById(documentId)).thenReturn(document);

        // When
        ResponseEntity<DocumentUploadResponse> response = controller.uploadDocument(multipartFile);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().documentId()).isEqualTo(documentId);
        assertThat(response.getBody().filename()).isEqualTo("test.pdf");
        assertThat(response.getBody().contentType()).isEqualTo("application/pdf");
        assertThat(response.getBody().fileSize()).isEqualTo(1024L);
        assertThat(response.getBody().status()).isEqualTo(DocumentStatus.PROCESSING);
        assertThat(response.getBody().createdAt()).isEqualTo(createdAt);
        verify(documentService).uploadDocument(multipartFile);
        verify(documentService).getDocumentById(documentId);
    }

    @Test
    void uploadDocumentWhenServiceThrowsExceptionPropagatesException() {
        // Given
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(multipartFile.getSize()).thenReturn(1024L);
        when(documentService.uploadDocument(multipartFile))
                .thenThrow(new IllegalArgumentException("File size exceeds maximum"));

        // When/Then
        assertThatThrownBy(() -> controller.uploadDocument(multipartFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File size exceeds maximum");
    }

    @Test
    void getAllDocumentsWhenDocumentsExistReturnsOkWithList() {
        // Given
        Document document2 = Document.builder()
                .id(UUID.randomUUID())
                .filename("test2.md")
                .contentType("text/markdown")
                .fileSize(2048L)
                .status(DocumentStatus.COMPLETED)
                .metadata(null)
                .createdAt(createdAt.minusHours(2))
                .updatedAt(updatedAt.minusHours(2))
                .build();

        List<Document> documents = Arrays.asList(document, document2);
        when(documentService.getAllDocuments()).thenReturn(documents);

        // When
        ResponseEntity<DocumentListResponse> response = controller.getAllDocuments();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().documents()).hasSize(2);
        assertThat(response.getBody().totalCount()).isEqualTo(2);
        assertThat(response.getBody().documents().get(0).documentId()).isEqualTo(documentId);
        assertThat(response.getBody().documents().get(1).documentId()).isEqualTo(document2.getId());
        verify(documentService).getAllDocuments();
    }

    @Test
    void getAllDocumentsWhenNoDocumentsExistReturnsOkWithEmptyList() {
        // Given
        when(documentService.getAllDocuments()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<DocumentListResponse> response = controller.getAllDocuments();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().documents()).isEmpty();
        assertThat(response.getBody().totalCount()).isEqualTo(0);
        verify(documentService).getAllDocuments();
    }

    @Test
    void getDocumentByIdWhenDocumentExistsReturnsOk() {
        // Given
        when(documentService.getDocumentById(documentId)).thenReturn(document);

        // When
        ResponseEntity<DocumentResponse> response = controller.getDocumentById(documentId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().documentId()).isEqualTo(documentId);
        assertThat(response.getBody().filename()).isEqualTo("test.pdf");
        assertThat(response.getBody().contentType()).isEqualTo("application/pdf");
        assertThat(response.getBody().fileSize()).isEqualTo(1024L);
        assertThat(response.getBody().status()).isEqualTo(DocumentStatus.PROCESSING);
        assertThat(response.getBody().createdAt()).isEqualTo(createdAt);
        assertThat(response.getBody().updatedAt()).isEqualTo(updatedAt);
        assertThat(response.getBody().metadata()).isNull();
        verify(documentService).getDocumentById(documentId);
    }

    @Test
    void getDocumentByIdWhenDocumentNotFoundThrowsEntityNotFoundException() {
        // Given
        when(documentService.getDocumentById(documentId))
                .thenThrow(new EntityNotFoundException("Document not found: " + documentId));

        // When/Then
        assertThatThrownBy(() -> controller.getDocumentById(documentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Document not found");
        verify(documentService).getDocumentById(documentId);
    }

    @Test
    void deleteDocumentWhenDocumentExistsReturnsNoContent() {
        // Given
        doNothing().when(documentService).deleteDocument(documentId);

        // When
        ResponseEntity<Void> response = controller.deleteDocument(documentId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(documentService).deleteDocument(documentId);
    }

    @Test
    void deleteDocumentWhenDocumentNotFoundThrowsEntityNotFoundException() {
        // Given
        doThrow(new EntityNotFoundException("Document not found: " + documentId))
                .when(documentService).deleteDocument(documentId);

        // When/Then
        assertThatThrownBy(() -> controller.deleteDocument(documentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Document not found");
        verify(documentService).deleteDocument(documentId);
    }

    @Test
    void getDocumentChunksWhenDocumentAndChunksExistReturnsOk() {
        // Given
        Chunk chunk1 = Chunk.builder()
                .id(UUID.randomUUID())
                .document(document)
                .content("Chunk 1 content")
                .chunkIndex(0)
                .embedding(new float[768])
                .createdAt(createdAt)
                .build();

        Chunk chunk2 = Chunk.builder()
                .id(UUID.randomUUID())
                .document(document)
                .content("Chunk 2 content")
                .chunkIndex(1)
                .embedding(new float[768])
                .createdAt(createdAt.plusMinutes(1))
                .build();

        List<Chunk> chunks = Arrays.asList(chunk1, chunk2);
        when(documentService.getDocumentById(documentId)).thenReturn(document);
        when(chunkRepository.findByDocument_IdOrderByChunkIndexAsc(documentId)).thenReturn(chunks);

        // When
        ResponseEntity<List<ChunkResponse>> response = controller.getDocumentChunks(documentId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).id()).isEqualTo(chunk1.getId());
        assertThat(response.getBody().get(0).documentId()).isEqualTo(documentId);
        assertThat(response.getBody().get(0).content()).isEqualTo("Chunk 1 content");
        assertThat(response.getBody().get(0).chunkIndex()).isEqualTo(0);
        assertThat(response.getBody().get(1).id()).isEqualTo(chunk2.getId());
        assertThat(response.getBody().get(1).chunkIndex()).isEqualTo(1);
        verify(documentService).getDocumentById(documentId);
        verify(chunkRepository).findByDocument_IdOrderByChunkIndexAsc(documentId);
    }

    @Test
    void getDocumentChunksWhenDocumentExistsButNoChunksReturnsOkWithEmptyList() {
        // Given
        when(documentService.getDocumentById(documentId)).thenReturn(document);
        when(chunkRepository.findByDocument_IdOrderByChunkIndexAsc(documentId))
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<ChunkResponse>> response = controller.getDocumentChunks(documentId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
        verify(documentService).getDocumentById(documentId);
        verify(chunkRepository).findByDocument_IdOrderByChunkIndexAsc(documentId);
    }

    @Test
    void getDocumentChunksWhenDocumentNotFoundThrowsEntityNotFoundException() {
        // Given
        when(documentService.getDocumentById(documentId))
                .thenThrow(new EntityNotFoundException("Document not found: " + documentId));

        // When/Then
        assertThatThrownBy(() -> controller.getDocumentChunks(documentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Document not found");
        verify(documentService).getDocumentById(documentId);
        verify(chunkRepository, never()).findByDocument_IdOrderByChunkIndexAsc(any());
    }

    @Test
    void toDocumentResponseWhenMetadataIsNullReturnsNullMetadata() {
        // Given
        when(documentService.getDocumentById(documentId)).thenReturn(document);

        // When
        ResponseEntity<DocumentResponse> response = controller.getDocumentById(documentId);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().metadata()).isNull();
    }

    @Test
    void toDocumentResponseWhenMetadataIsEmptyStringReturnsNullMetadata() {
        // Given
        Document documentWithEmptyMetadata = Document.builder()
                .id(documentId)
                .filename("test.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .status(DocumentStatus.PROCESSING)
                .metadata("")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
        when(documentService.getDocumentById(documentId)).thenReturn(documentWithEmptyMetadata);

        // When
        ResponseEntity<DocumentResponse> response = controller.getDocumentById(documentId);

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().metadata()).isNull();
    }
}
