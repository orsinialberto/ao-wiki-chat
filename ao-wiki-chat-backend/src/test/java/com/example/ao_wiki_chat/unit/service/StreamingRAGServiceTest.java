package com.example.ao_wiki_chat.unit.service;

import com.example.ao_wiki_chat.model.entity.Chunk;
import com.example.ao_wiki_chat.model.entity.Conversation;
import com.example.ao_wiki_chat.model.entity.Document;
import com.example.ao_wiki_chat.model.entity.Message;
import com.example.ao_wiki_chat.model.enums.DocumentStatus;
import com.example.ao_wiki_chat.model.enums.MessageRole;
import com.example.ao_wiki_chat.repository.ConversationRepository;
import com.example.ao_wiki_chat.repository.MessageRepository;
import com.example.ao_wiki_chat.service.GeminiEmbeddingService;
import com.example.ao_wiki_chat.service.LLMService;
import com.example.ao_wiki_chat.service.RAGService;
import com.example.ao_wiki_chat.service.VectorSearchService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the streaming path of RAGService (processQueryStreaming).
 */
@ExtendWith(MockitoExtension.class)
class StreamingRAGServiceTest {

    private static final int EMBEDDING_DIMENSION = 768;
    private static final UUID CONVERSATION_ID = UUID.randomUUID();

    @Mock
    private GeminiEmbeddingService embeddingService;

    @Mock
    private VectorSearchService vectorSearchService;

    @Mock
    private LLMService llmService;

    @Mock
    private StreamingChatLanguageModel streamingChatModel;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    private RAGService ragService;

    private float[] queryEmbedding;
    private Conversation conversation;
    private Document document;
    private Chunk chunk1;
    private Chunk chunk2;

    @BeforeEach
    void setUp() {
        ragService = new RAGService(
                embeddingService,
                vectorSearchService,
                llmService,
                streamingChatModel,
                conversationRepository,
                messageRepository,
                10,
                true
        );

        queryEmbedding = createTestVector(EMBEDDING_DIMENSION, 0.5f);
        conversation = Conversation.builder()
                .id(CONVERSATION_ID)
                .sessionId("session-1")
                .title("Test")
                .build();

        document = Document.builder()
                .id(UUID.randomUUID())
                .filename("doc.md")
                .contentType("text/markdown")
                .fileSize(1024L)
                .status(DocumentStatus.COMPLETED)
                .build();

        chunk1 = Chunk.builder()
                .id(UUID.randomUUID())
                .document(document)
                .content("First chunk content.")
                .chunkIndex(0)
                .embedding(createTestVector(EMBEDDING_DIMENSION, 0.3f))
                .build();

        chunk2 = Chunk.builder()
                .id(UUID.randomUUID())
                .document(document)
                .content("Second chunk content.")
                .chunkIndex(1)
                .embedding(createTestVector(EMBEDDING_DIMENSION, 0.4f))
                .build();
    }

    @Test
    void processQueryStreamingWhenChunksFoundStreamsAndCompletes() {
        String query = "What is it?";
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(CONVERSATION_ID))
                .thenReturn(Collections.emptyList());
        when(embeddingService.generateEmbedding(query)).thenReturn(queryEmbedding);
        when(vectorSearchService.findSimilarChunks(queryEmbedding)).thenReturn(Arrays.asList(chunk1, chunk2));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            StreamingResponseHandler<AiMessage> h = inv.getArgument(1);
            h.onNext("Hello ");
            h.onNext("world.");
            h.onComplete(Response.from(AiMessage.from("Hello world.")));
            return null;
        }).when(streamingChatModel).generate(anyList(), any());

        AtomicReference<Message> completedMessage = new AtomicReference<>();
        AtomicReference<String> chunks = new AtomicReference<>("");

        ragService.processQueryStreaming(
                CONVERSATION_ID,
                query,
                token -> chunks.set(chunks.get() + token),
                completedMessage::set,
                error -> { throw new AssertionError("Unexpected error: " + error); }
        );

        assertThat(chunks.get()).isEqualTo("Hello world.");
        assertThat(completedMessage.get()).isNotNull();
        assertThat(completedMessage.get().getContent()).isEqualTo("Hello world.");
        assertThat(completedMessage.get().getRole()).isEqualTo(MessageRole.ASSISTANT);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void processQueryStreamingWhenNoChunksReturnsNoContextMessage() {
        String query = "Anything";
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(CONVERSATION_ID))
                .thenReturn(Collections.emptyList());
        when(embeddingService.generateEmbedding(query)).thenReturn(queryEmbedding);
        when(vectorSearchService.findSimilarChunks(queryEmbedding)).thenReturn(Collections.emptyList());
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        AtomicReference<Message> completedMessage = new AtomicReference<>();
        AtomicReference<String> firstChunk = new AtomicReference<>("");

        ragService.processQueryStreaming(
                CONVERSATION_ID,
                query,
                token -> firstChunk.compareAndSet("", token),
                completedMessage::set,
                error -> { throw new AssertionError("Unexpected error: " + error); }
        );

        assertThat(firstChunk.get()).contains("couldn't find any relevant information");
        assertThat(completedMessage.get().getContent()).contains("couldn't find any relevant information");
        verify(streamingChatModel, never()).generate(anyList(), any());
    }

    @Test
    void processQueryStreamingWhenConversationNotFoundCallsOnError() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.empty());

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        ragService.processQueryStreaming(
                CONVERSATION_ID,
                "query",
                token -> {},
                msg -> { throw new AssertionError("Should not complete"); },
                errorRef::set
        );

        assertThat(errorRef.get()).isNotNull();
        assertThat(errorRef.get()).isInstanceOf(IllegalArgumentException.class);
        assertThat(errorRef.get().getMessage()).contains("Conversation not found");
        verify(embeddingService, never()).generateEmbedding(anyString());
    }

    @Test
    void processQueryStreamingWhenStreamErrorsCallsOnError() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(CONVERSATION_ID))
                .thenReturn(Collections.emptyList());
        when(embeddingService.generateEmbedding(anyString())).thenReturn(queryEmbedding);
        when(vectorSearchService.findSimilarChunks(queryEmbedding)).thenReturn(List.of(chunk1));
        doAnswer(inv -> {
            StreamingResponseHandler<AiMessage> h = inv.getArgument(1);
            h.onError(new RuntimeException("LLM failed"));
            return null;
        }).when(streamingChatModel).generate(anyList(), any());

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        ragService.processQueryStreaming(
                CONVERSATION_ID,
                "query",
                token -> {},
                msg -> { throw new AssertionError("Should not complete"); },
                errorRef::set
        );

        assertThat(errorRef.get()).isNotNull();
        assertThat(errorRef.get().getMessage()).isEqualTo("LLM failed");
    }

    @Test
    void processQueryStreamingWhenEmbeddingFailsCallsOnError() {
        when(conversationRepository.findById(CONVERSATION_ID)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(CONVERSATION_ID))
                .thenReturn(Collections.emptyList());
        when(embeddingService.generateEmbedding(anyString()))
                .thenThrow(new RuntimeException("Embedding failed"));

        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        ragService.processQueryStreaming(
                CONVERSATION_ID,
                "query",
                token -> {},
                msg -> { throw new AssertionError("Should not complete"); },
                errorRef::set
        );

        assertThat(errorRef.get()).isNotNull();
        assertThat(errorRef.get().getMessage()).contains("Embedding failed");
        verify(streamingChatModel, never()).generate(anyList(), any());
    }

    private static float[] createTestVector(int dimension, float fillValue) {
        float[] v = new float[dimension];
        Arrays.fill(v, fillValue);
        return v;
    }
}
