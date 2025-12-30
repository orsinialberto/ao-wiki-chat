package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.model.CliChunk;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for DocumentCommandUtils.
 */
class DocumentCommandUtilsTest {

    @Test
    void validateFileWhenFileExistsDoesNotThrow() throws Exception {
        // Given
        Path tempFile = Files.createTempFile("test", ".txt");
        Files.write(tempFile, "test".getBytes());

        // When/Then
        DocumentCommandUtils.validateFile(tempFile);

        Files.deleteIfExists(tempFile);
    }

    @Test
    void validateFileWhenFileNotFoundThrowsException() {
        // Given
        Path nonExistentFile = Path.of("/nonexistent/file.txt");

        // When/Then
        assertThatThrownBy(() -> DocumentCommandUtils.validateFile(nonExistentFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void validateFileWhenPathIsNullThrowsException() {
        // When/Then
        assertThatThrownBy(() -> DocumentCommandUtils.validateFile(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void validateUuidWhenValidUuidReturnsUuid() {
        // Given
        String validUuid = "550e8400-e29b-41d4-a716-446655440000";

        // When
        UUID result = DocumentCommandUtils.validateUuid(validUuid);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.toString()).isEqualTo(validUuid);
    }

    @Test
    void validateUuidWhenInvalidUuidThrowsException() {
        // Given
        String invalidUuid = "not-a-uuid";

        // When/Then
        assertThatThrownBy(() -> DocumentCommandUtils.validateUuid(invalidUuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid UUID format");
    }

    @Test
    void validateUuidWhenNullThrowsException() {
        // When/Then
        assertThatThrownBy(() -> DocumentCommandUtils.validateUuid(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    void validateStatusWhenValidStatusReturnsUppercaseStatus() {
        // Given
        String status = "processing";

        // When
        String result = DocumentCommandUtils.validateStatus(status);

        // Then
        assertThat(result).isEqualTo("PROCESSING");
    }

    @Test
    void validateStatusWhenInvalidStatusThrowsException() {
        // Given
        String invalidStatus = "INVALID";

        // When/Then
        assertThatThrownBy(() -> DocumentCommandUtils.validateStatus(invalidStatus))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid status");
    }

    @Test
    void formatFileSizeWhenBytesFormatsCorrectly() {
        // When/Then
        assertThat(DocumentCommandUtils.formatFileSize(500L)).isEqualTo("500 B");
        assertThat(DocumentCommandUtils.formatFileSize(1024L)).contains("KB");
        assertThat(DocumentCommandUtils.formatFileSize(1024L * 1024L)).contains("MB");
        assertThat(DocumentCommandUtils.formatFileSize(1024L * 1024L * 1024L)).contains("GB");
        assertThat(DocumentCommandUtils.formatFileSize(null)).isEqualTo("N/A");
    }

    @Test
    void filterByStatusWhenStatusProvidedFiltersCorrectly() {
        // Given
        CliDocument doc1 = new CliDocument(
                UUID.randomUUID(), "doc1.pdf", "application/pdf", 100L,
                "PROCESSING", LocalDateTime.now(), LocalDateTime.now(), null
        );
        CliDocument doc2 = new CliDocument(
                UUID.randomUUID(), "doc2.pdf", "application/pdf", 200L,
                "COMPLETED", LocalDateTime.now(), LocalDateTime.now(), null
        );
        List<CliDocument> documents = List.of(doc1, doc2);

        // When
        List<CliDocument> filtered = DocumentCommandUtils.filterByStatus(documents, "COMPLETED");

        // Then
        assertThat(filtered).hasSize(1);
        assertThat(filtered.get(0).status()).isEqualTo("COMPLETED");
    }

    @Test
    void filterByStatusWhenStatusIsNullReturnsAll() {
        // Given
        CliDocument doc1 = new CliDocument(
                UUID.randomUUID(), "doc1.pdf", "application/pdf", 100L,
                "PROCESSING", LocalDateTime.now(), LocalDateTime.now(), null
        );
        List<CliDocument> documents = List.of(doc1);

        // When
        List<CliDocument> filtered = DocumentCommandUtils.filterByStatus(documents, null);

        // Then
        assertThat(filtered).hasSize(1);
    }

    @Test
    void formatDocumentsTableWhenEmptyReturnsMessage() {
        // When
        String result = DocumentCommandUtils.formatDocumentsTable(List.of());

        // Then
        assertThat(result).contains("No documents found");
    }

    @Test
    void formatChunksTableWhenEmptyReturnsMessage() {
        // When
        String result = DocumentCommandUtils.formatChunksTable(List.of(), null);

        // Then
        assertThat(result).contains("No chunks found");
    }

    @Test
    void formatChunksTableWhenLimitProvidedLimitsOutput() {
        // Given
        CliChunk chunk1 = new CliChunk(
                UUID.randomUUID(), UUID.randomUUID(), "content1", 0, LocalDateTime.now()
        );
        CliChunk chunk2 = new CliChunk(
                UUID.randomUUID(), UUID.randomUUID(), "content2", 1, LocalDateTime.now()
        );
        List<CliChunk> chunks = List.of(chunk1, chunk2);

        // When
        String result = DocumentCommandUtils.formatChunksTable(chunks, 1);

        // Then
        assertThat(result).contains("... and 1 more chunks");
    }

    @Test
    void formatMetadataWhenEmptyReturnsMessage() {
        // When
        String result = DocumentCommandUtils.formatMetadata(Map.of());

        // Then
        assertThat(result).contains("No metadata available");
    }
}
