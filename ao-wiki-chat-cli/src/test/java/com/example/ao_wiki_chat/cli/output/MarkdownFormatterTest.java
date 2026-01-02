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
 * Unit tests for MarkdownFormatter.
 */
class MarkdownFormatterTest {

    private final MarkdownFormatter formatter = new MarkdownFormatter();

    @Test
    void formatDocumentsWhenEmptyListReturnsNoDocumentsMessage() {
        String result = formatter.formatDocuments(new ArrayList<>());
        assertThat(result).contains("No documents found");
    }

    @Test
    void formatDocumentsWhenNullReturnsNoDocumentsMessage() {
        String result = formatter.formatDocuments(null);
        assertThat(result).contains("No documents found");
    }

    @Test
    void formatDocumentsWhenListProvidedReturnsMarkdownTable() {
        List<CliDocument> documents = List.of(
                createTestDocument("test1.pdf", "COMPLETED", 1024L)
        );

        String result = formatter.formatDocuments(documents);

        assertThat(result).contains("## Documents");
        assertThat(result).contains("| ID |");
        assertThat(result).contains("test1.pdf");
    }

    @Test
    void formatDocumentWhenNullReturnsNoDocumentMessage() {
        String result = formatter.formatDocument(null);
        assertThat(result).contains("No document provided");
    }

    @Test
    void formatDocumentWhenProvidedReturnsMarkdownTable() {
        CliDocument document = createTestDocument("test.pdf", "COMPLETED", 1024L);

        String result = formatter.formatDocument(document);

        assertThat(result).contains("## Document Details");
        assertThat(result).contains("| Field |");
        assertThat(result).contains("test.pdf");
    }

    @Test
    void formatMetadataWhenEmptyReturnsNoMetadataMessage() {
        String result = formatter.formatMetadata(new HashMap<>());
        assertThat(result).contains("No metadata available");
    }

    @Test
    void formatMetadataWhenNullReturnsNoMetadataMessage() {
        String result = formatter.formatMetadata(null);
        assertThat(result).contains("No metadata available");
    }

    @Test
    void formatMetadataWhenProvidedReturnsMarkdownTable() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");

        String result = formatter.formatMetadata(metadata);

        assertThat(result).contains("## Metadata");
        assertThat(result).contains("| Key |");
        assertThat(result).contains("key1");
    }

    @Test
    void formatChatResponseWhenNullReturnsNoResponseMessage() {
        String result = formatter.formatChatResponse(null, false);
        assertThat(result).contains("No response received");
    }

    @Test
    void formatChatResponseWhenProvidedReturnsMarkdown() {
        CliChatResponse response = new CliChatResponse("Test answer", null);

        String result = formatter.formatChatResponse(response, false);

        assertThat(result).contains("## Answer");
        assertThat(result).contains("Test answer");
    }

    @Test
    void formatChatResponseWhenShowSourcesTrueIncludesSources() {
        List<CliSourceReference> sources = List.of(
                new CliSourceReference("doc1.pdf", "chunk content", 0.95, 0)
        );
        CliChatResponse response = new CliChatResponse("Test answer", sources);

        String result = formatter.formatChatResponse(response, true);

        assertThat(result).contains("## Sources");
        assertThat(result).contains("doc1.pdf");
    }

    @Test
    void formatHistoryWhenEmptyReturnsNoMessagesMessage() {
        String result = formatter.formatHistory(new ArrayList<>(), false);
        assertThat(result).contains("No messages");
    }

    @Test
    void formatHistoryWhenNullReturnsNoMessagesMessage() {
        String result = formatter.formatHistory(null, false);
        assertThat(result).contains("No messages");
    }

    @Test
    void formatHistoryWhenProvidedReturnsMarkdown() {
        List<CliMessage> messages = List.of(
                new CliMessage("Hello", "USER", LocalDateTime.now(), null)
        );

        String result = formatter.formatHistory(messages, false);

        assertThat(result).contains("# Conversation History");
        assertThat(result).contains("Hello");
    }

    @Test
    void formatChunksWhenEmptyReturnsNoChunksMessage() {
        String result = formatter.formatChunks(new ArrayList<>(), null);
        assertThat(result).contains("No chunks found");
    }

    @Test
    void formatChunksWhenNullReturnsNoChunksMessage() {
        String result = formatter.formatChunks(null, null);
        assertThat(result).contains("No chunks found");
    }

    @Test
    void formatChunksWhenProvidedReturnsMarkdownTable() {
        List<CliChunk> chunks = List.of(
                createTestChunk("content", 0)
        );

        String result = formatter.formatChunks(chunks, null);

        assertThat(result).contains("## Chunks");
        assertThat(result).contains("| Index |");
    }

    @Test
    void formatChunksWhenLimitProvidedRespectsLimit() {
        List<CliChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            chunks.add(createTestChunk("chunk " + i, i));
        }

        String result = formatter.formatChunks(chunks, 5);

        assertThat(result).contains("more chunks");
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
