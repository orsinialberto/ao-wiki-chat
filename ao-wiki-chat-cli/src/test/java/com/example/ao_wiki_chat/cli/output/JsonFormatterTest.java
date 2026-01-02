package com.example.ao_wiki_chat.cli.output;

import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import com.example.ao_wiki_chat.cli.model.CliChunk;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import com.example.ao_wiki_chat.cli.model.CliMessage;
import com.example.ao_wiki_chat.cli.model.CliSourceReference;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JsonFormatter.
 */
class JsonFormatterTest {

    private final JsonFormatter formatter = new JsonFormatter();

    @Test
    void formatDocumentsWhenEmptyListReturnsEmptyArray() {
        String result = formatter.formatDocuments(new ArrayList<>());
        assertThat(result).contains("[");
        assertThat(result).contains("]");
    }

    @Test
    void formatDocumentsWhenNullReturnsEmptyObject() {
        String result = formatter.formatDocuments(null);
        assertThat(result).contains("{");
    }

    @Test
    void formatDocumentsWhenListProvidedReturnsValidJson() {
        List<CliDocument> documents = List.of(
                createTestDocument("test.pdf", "COMPLETED", 1024L)
        );

        String result = formatter.formatDocuments(documents);

        assertThat(result).contains("documentId");
        assertThat(result).contains("filename");
        assertThat(result).contains("test.pdf");
    }

    @Test
    void formatDocumentWhenNullReturnsNull() {
        String result = formatter.formatDocument(null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    void formatDocumentWhenProvidedReturnsValidJson() {
        CliDocument document = createTestDocument("test.pdf", "COMPLETED", 1024L);

        String result = formatter.formatDocument(document);

        assertThat(result).contains("documentId");
        assertThat(result).contains("test.pdf");
        assertThat(result).contains("COMPLETED");
    }

    @Test
    void formatMetadataWhenNullReturnsNull() {
        String result = formatter.formatMetadata(null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    void formatMetadataWhenProvidedReturnsValidJson() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 123);

        String result = formatter.formatMetadata(metadata);

        assertThat(result).contains("key1");
        assertThat(result).contains("value1");
        assertThat(result).contains("key2");
        assertThat(result).contains("123");
    }

    @Test
    void formatChatResponseWhenNullReturnsNull() {
        String result = formatter.formatChatResponse(null, false);
        assertThat(result).isEqualTo("null");
    }

    @Test
    void formatChatResponseWhenProvidedReturnsValidJson() {
        CliChatResponse response = new CliChatResponse("Test answer", null);

        String result = formatter.formatChatResponse(response, false);

        assertThat(result).contains("answer");
        assertThat(result).contains("Test answer");
    }

    @Test
    void formatChatResponseWhenShowSourcesFalseExcludesSources() {
        List<CliSourceReference> sources = List.of(
                new CliSourceReference("doc1.pdf", "content", 0.95, 0)
        );
        CliChatResponse response = new CliChatResponse("Test answer", sources);

        String result = formatter.formatChatResponse(response, false);

        assertThat(result).contains("answer");
        assertThat(result).doesNotContain("sources");
    }

    @Test
    void formatChatResponseWhenShowSourcesTrueIncludesSources() {
        List<CliSourceReference> sources = List.of(
                new CliSourceReference("doc1.pdf", "content", 0.95, 0)
        );
        CliChatResponse response = new CliChatResponse("Test answer", sources);

        String result = formatter.formatChatResponse(response, true);

        assertThat(result).contains("sources");
        assertThat(result).contains("doc1.pdf");
    }

    @Test
    void formatHistoryWhenEmptyReturnsEmptyArray() {
        String result = formatter.formatHistory(new ArrayList<>(), false);
        // Jackson pretty printer may add spaces, so we check for array brackets
        assertThat(result.replaceAll("\\s+", "")).isEqualTo("[]");
    }

    @Test
    void formatHistoryWhenNullReturnsEmptyArray() {
        String result = formatter.formatHistory(null, false);
        // Jackson pretty printer adds spaces, so we check for array brackets
        assertThat(result.trim()).isEqualTo("[]");
    }

    @Test
    void formatHistoryWhenProvidedReturnsValidJson() {
        List<CliMessage> messages = List.of(
                new CliMessage("Hello", "USER", LocalDateTime.now(), null)
        );

        String result = formatter.formatHistory(messages, false);

        assertThat(result).contains("content");
        assertThat(result).contains("role");
        assertThat(result).contains("Hello");
    }

    @Test
    void formatHistoryWhenShowSourcesFalseExcludesSources() {
        List<CliMessage> messages = List.of(
                new CliMessage("Hello", "ASSISTANT", LocalDateTime.now(), "sources")
        );

        String result = formatter.formatHistory(messages, false);

        assertThat(result).contains("content");
        assertThat(result).doesNotContain("sources");
    }

    @Test
    void formatChunksWhenEmptyReturnsEmptyArray() {
        String result = formatter.formatChunks(new ArrayList<>(), null);
        // Jackson pretty printer may add spaces, so we check for array brackets
        assertThat(result.replaceAll("\\s+", "")).isEqualTo("[]");
    }

    @Test
    void formatChunksWhenNullReturnsEmptyArray() {
        String result = formatter.formatChunks(null, null);
        // Jackson pretty printer adds spaces, so we check for array brackets
        assertThat(result.trim()).isEqualTo("[]");
    }

    @Test
    void formatChunksWhenProvidedReturnsValidJson() {
        List<CliChunk> chunks = List.of(
                createTestChunk("content", 0)
        );

        String result = formatter.formatChunks(chunks, null);

        assertThat(result).contains("id");
        assertThat(result).contains("content");
    }

    @Test
    void formatChunksWhenLimitProvidedRespectsLimit() {
        List<CliChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            chunks.add(createTestChunk("chunk " + i, i));
        }

        String result = formatter.formatChunks(chunks, 5);

        // Should only contain 5 chunks
        long count = result.chars().filter(ch -> ch == '{').count();
        assertThat(count).isEqualTo(5);
    }

    private CliDocument createTestDocument(String filename, String status, Long fileSize) {
        return new CliDocument(
                UUID.randomUUID(),
                filename,
                "application/pdf",
                fileSize,
                status,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }

    private CliChunk createTestChunk(String content, int index) {
        return new CliChunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                content,
                index,
                LocalDateTime.now()
        );
    }
}
