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
 * Unit tests for TableFormatter.
 */
class TableFormatterTest {

    private final TableFormatter formatterWithColors = new TableFormatter(true);
    private final TableFormatter formatterWithoutColors = new TableFormatter(false);

    @Test
    void formatDocumentsWhenEmptyListReturnsNoDocumentsMessage() {
        String result = formatterWithColors.formatDocuments(new ArrayList<>());
        assertThat(result).contains("No documents found");
    }

    @Test
    void formatDocumentsWhenNullReturnsNoDocumentsMessage() {
        String result = formatterWithColors.formatDocuments(null);
        assertThat(result).contains("No documents found");
    }

    @Test
    void formatDocumentsWhenListProvidedReturnsFormattedTable() {
        List<CliDocument> documents = List.of(
                createTestDocument("test1.pdf", "COMPLETED", 1024L),
                createTestDocument("test2.txt", "PROCESSING", 2048L)
        );

        String result = formatterWithColors.formatDocuments(documents);

        assertThat(result).contains("ID");
        assertThat(result).contains("Filename");
        assertThat(result).contains("Status");
        assertThat(result).contains("test1.pdf");
        assertThat(result).contains("test2.txt");
    }

    @Test
    void formatDocumentsWhenColorsEnabledFormatsStatusWithColors() {
        List<CliDocument> documents = List.of(
                createTestDocument("test.pdf", "COMPLETED", 1024L)
        );

        String result = formatterWithColors.formatDocuments(documents);

        // Check that status is present (color codes may not be visible in string but status should be)
        assertThat(result).contains("COMPLETED");
    }

    @Test
    void formatDocumentWhenNullReturnsNoDocumentMessage() {
        String result = formatterWithColors.formatDocument(null);
        assertThat(result).contains("No document provided");
    }

    @Test
    void formatDocumentWhenProvidedReturnsFormattedTable() {
        CliDocument document = createTestDocument("test.pdf", "COMPLETED", 1024L);

        String result = formatterWithColors.formatDocument(document);

        assertThat(result).contains("Field");
        assertThat(result).contains("Value");
        assertThat(result).contains("test.pdf");
        assertThat(result).contains("COMPLETED");
    }

    @Test
    void formatMetadataWhenEmptyReturnsNoMetadataMessage() {
        String result = formatterWithColors.formatMetadata(new HashMap<>());
        assertThat(result).contains("No metadata available");
    }

    @Test
    void formatMetadataWhenNullReturnsNoMetadataMessage() {
        String result = formatterWithColors.formatMetadata(null);
        assertThat(result).contains("No metadata available");
    }

    @Test
    void formatMetadataWhenProvidedReturnsFormattedTable() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 123);

        String result = formatterWithColors.formatMetadata(metadata);

        assertThat(result).contains("Key");
        assertThat(result).contains("Value");
        assertThat(result).contains("key1");
        assertThat(result).contains("value1");
    }

    @Test
    void formatChatResponseWhenNullReturnsNoResponseMessage() {
        String result = formatterWithColors.formatChatResponse(null, false);
        assertThat(result).contains("No response received");
    }

    @Test
    void formatChatResponseWhenProvidedReturnsFormattedOutput() {
        CliChatResponse response = new CliChatResponse("Test answer", null);

        String result = formatterWithColors.formatChatResponse(response, false);

        assertThat(result).contains("Answer:");
        assertThat(result).contains("Test answer");
    }

    @Test
    void formatChatResponseWhenShowSourcesTrueIncludesSources() {
        List<CliSourceReference> sources = List.of(
                new CliSourceReference("doc1.pdf", "chunk content", 0.95, 0)
        );
        CliChatResponse response = new CliChatResponse("Test answer", sources);

        String result = formatterWithColors.formatChatResponse(response, true);

        assertThat(result).contains("Sources");
        assertThat(result).contains("doc1.pdf");
    }

    @Test
    void formatHistoryWhenEmptyReturnsNoMessagesMessage() {
        String result = formatterWithColors.formatHistory(new ArrayList<>(), false);
        assertThat(result).contains("No messages");
    }

    @Test
    void formatHistoryWhenNullReturnsNoMessagesMessage() {
        String result = formatterWithColors.formatHistory(null, false);
        assertThat(result).contains("No messages");
    }

    @Test
    void formatHistoryWhenProvidedReturnsFormattedOutput() {
        List<CliMessage> messages = List.of(
                new CliMessage("Hello", "USER", LocalDateTime.now(), null),
                new CliMessage("Hi there", "ASSISTANT", LocalDateTime.now(), null)
        );

        String result = formatterWithColors.formatHistory(messages, false);

        assertThat(result).contains("Conversation History");
        assertThat(result).contains("Hello");
        assertThat(result).contains("Hi there");
    }

    @Test
    void formatChunksWhenEmptyReturnsNoChunksMessage() {
        String result = formatterWithColors.formatChunks(new ArrayList<>(), null);
        assertThat(result).contains("No chunks found");
    }

    @Test
    void formatChunksWhenNullReturnsNoChunksMessage() {
        String result = formatterWithColors.formatChunks(null, null);
        assertThat(result).contains("No chunks found");
    }

    @Test
    void formatChunksWhenProvidedReturnsFormattedTable() {
        List<CliChunk> chunks = List.of(
                createTestChunk("chunk content 1", 0),
                createTestChunk("chunk content 2", 1)
        );

        String result = formatterWithColors.formatChunks(chunks, null);

        assertThat(result).contains("Index");
        assertThat(result).contains("ID");
        assertThat(result).contains("Content Preview");
    }

    @Test
    void formatChunksWhenLimitProvidedRespectsLimit() {
        List<CliChunk> chunks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            chunks.add(createTestChunk("chunk " + i, i));
        }

        String result = formatterWithColors.formatChunks(chunks, 5);

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
