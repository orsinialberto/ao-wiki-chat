package com.example.ao_wiki_chat.controller;

import com.example.ao_wiki_chat.exception.EmbeddingException;
import com.example.ao_wiki_chat.exception.LLMException;
import com.example.ao_wiki_chat.exception.VectorSearchException;
import com.example.ao_wiki_chat.model.dto.ChatRequest;
import com.example.ao_wiki_chat.model.dto.ChatResponse;
import com.example.ao_wiki_chat.model.dto.MessageResponse;
import com.example.ao_wiki_chat.model.dto.SourceReference;
import com.example.ao_wiki_chat.model.entity.Conversation;
import com.example.ao_wiki_chat.model.entity.Message;
import com.example.ao_wiki_chat.model.enums.MessageRole;
import com.example.ao_wiki_chat.repository.ConversationRepository;
import com.example.ao_wiki_chat.repository.MessageRepository;
import com.example.ao_wiki_chat.service.RAGService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ChatController.
 * Tests chat query processing, conversation history retrieval, and conversation deletion.
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private RAGService ragService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    private ChatController controller;

    private String sessionId;
    private Conversation conversation;
    private UUID conversationId;
    private LocalDateTime createdAt;
    private ChatRequest chatRequest;
    private ChatResponse chatResponse;

    @BeforeEach
    void setUp() {
        controller = new ChatController(ragService, conversationRepository, messageRepository);

        sessionId = "test-session-123";
        conversationId = UUID.randomUUID();
        createdAt = LocalDateTime.now().minusHours(1);

        conversation = Conversation.builder()
                .id(conversationId)
                .sessionId(sessionId)
                .title(null)
                .metadata(null)
                .createdAt(createdAt)
                .build();

        chatRequest = new ChatRequest("What is machine learning?", sessionId);

        SourceReference source1 = new SourceReference(
                "test-document.md",
                "Machine learning is a subset of AI.",
                0.85,
                0
        );
        SourceReference source2 = new SourceReference(
                "test-document.md",
                "It enables systems to learn from data.",
                0.82,
                1
        );

        chatResponse = new ChatResponse(
                "Machine learning is a subset of AI that enables systems to learn from data.",
                Arrays.asList(source1, source2)
        );
    }

    @Test
    void processQueryWhenValidRequestReturnsOkWithChatResponse() {
        // Given
        when(ragService.processQuery(chatRequest)).thenReturn(chatResponse);

        // When
        ResponseEntity<ChatResponse> response = controller.processQuery(chatRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().answer()).isEqualTo(chatResponse.answer());
        assertThat(response.getBody().sources()).hasSize(2);
        assertThat(response.getBody().sources().get(0).documentName()).isEqualTo("test-document.md");
        assertThat(response.getBody().sources().get(0).chunkContent()).contains("Machine learning");
        verify(ragService).processQuery(chatRequest);
    }

    @Test
    void processQueryWhenServiceThrowsEmbeddingExceptionPropagatesException() {
        // Given
        when(ragService.processQuery(chatRequest))
                .thenThrow(new EmbeddingException("Embedding generation failed"));

        // When/Then
        assertThatThrownBy(() -> controller.processQuery(chatRequest))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Embedding generation failed");
        verify(ragService).processQuery(chatRequest);
    }

    @Test
    void processQueryWhenServiceThrowsVectorSearchExceptionPropagatesException() {
        // Given
        when(ragService.processQuery(chatRequest))
                .thenThrow(new VectorSearchException("Vector search failed"));

        // When/Then
        assertThatThrownBy(() -> controller.processQuery(chatRequest))
                .isInstanceOf(VectorSearchException.class)
                .hasMessageContaining("Vector search failed");
        verify(ragService).processQuery(chatRequest);
    }

    @Test
    void processQueryWhenServiceThrowsLLMExceptionPropagatesException() {
        // Given
        when(ragService.processQuery(chatRequest))
                .thenThrow(new LLMException("LLM generation failed"));

        // When/Then
        assertThatThrownBy(() -> controller.processQuery(chatRequest))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("LLM generation failed");
        verify(ragService).processQuery(chatRequest);
    }

    @Test
    void getConversationHistoryWhenSessionExistsReturnsOkWithMessages() {
        // Given
        Message userMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .role(MessageRole.USER)
                .content("What is machine learning?")
                .sources(null)
                .createdAt(createdAt)
                .build();

        Message assistantMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content("Machine learning is a subset of AI.")
                .sources("[{\"documentName\":\"test.md\"}]")
                .createdAt(createdAt.plusMinutes(1))
                .build();

        List<Message> messages = Arrays.asList(userMessage, assistantMessage);

        when(conversationRepository.findBySessionId(sessionId))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(messages);

        // When
        ResponseEntity<List<MessageResponse>> response = controller.getConversationHistory(sessionId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).content()).isEqualTo("What is machine learning?");
        assertThat(response.getBody().get(0).role()).isEqualTo(MessageRole.USER);
        assertThat(response.getBody().get(0).sources()).isNull();
        assertThat(response.getBody().get(1).content()).isEqualTo("Machine learning is a subset of AI.");
        assertThat(response.getBody().get(1).role()).isEqualTo(MessageRole.ASSISTANT);
        assertThat(response.getBody().get(1).sources()).isEqualTo("[{\"documentName\":\"test.md\"}]");
        verify(conversationRepository).findBySessionId(sessionId);
        verify(messageRepository).findByConversation_IdOrderByCreatedAtAsc(conversationId);
    }

    @Test
    void getConversationHistoryWhenSessionExistsButNoMessagesReturnsOkWithEmptyList() {
        // Given
        when(conversationRepository.findBySessionId(sessionId))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(Collections.emptyList());

        // When
        ResponseEntity<List<MessageResponse>> response = controller.getConversationHistory(sessionId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
        verify(conversationRepository).findBySessionId(sessionId);
        verify(messageRepository).findByConversation_IdOrderByCreatedAtAsc(conversationId);
    }

    @Test
    void getConversationHistoryWhenSessionNotFoundThrowsEntityNotFoundException() {
        // Given
        when(conversationRepository.findBySessionId(sessionId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> controller.getConversationHistory(sessionId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Conversation not found for session: " + sessionId);
        verify(conversationRepository).findBySessionId(sessionId);
        verify(messageRepository, never()).findByConversation_IdOrderByCreatedAtAsc(any());
    }

    @Test
    void getConversationHistoryWhenMessagesOrderedByCreationTime() {
        // Given
        LocalDateTime time1 = createdAt;
        LocalDateTime time2 = createdAt.plusMinutes(1);
        LocalDateTime time3 = createdAt.plusMinutes(2);

        Message message1 = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .role(MessageRole.USER)
                .content("First message")
                .sources(null)
                .createdAt(time1)
                .build();

        Message message2 = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content("Second message")
                .sources(null)
                .createdAt(time2)
                .build();

        Message message3 = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .role(MessageRole.USER)
                .content("Third message")
                .sources(null)
                .createdAt(time3)
                .build();

        List<Message> messages = Arrays.asList(message1, message2, message3);

        when(conversationRepository.findBySessionId(sessionId))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(messages);

        // When
        ResponseEntity<List<MessageResponse>> response = controller.getConversationHistory(sessionId);

        // Then
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody().get(0).content()).isEqualTo("First message");
        assertThat(response.getBody().get(0).createdAt()).isEqualTo(time1);
        assertThat(response.getBody().get(1).content()).isEqualTo("Second message");
        assertThat(response.getBody().get(1).createdAt()).isEqualTo(time2);
        assertThat(response.getBody().get(2).content()).isEqualTo("Third message");
        assertThat(response.getBody().get(2).createdAt()).isEqualTo(time3);
    }

    @Test
    void deleteConversationWhenSessionExistsReturnsNoContent() {
        // Given
        when(conversationRepository.findBySessionId(sessionId))
                .thenReturn(Optional.of(conversation));
        doNothing().when(conversationRepository).delete(conversation);

        // When
        ResponseEntity<Void> response = controller.deleteConversation(sessionId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(conversationRepository).findBySessionId(sessionId);
        verify(conversationRepository).delete(conversation);
    }

    @Test
    void deleteConversationWhenSessionNotFoundThrowsEntityNotFoundException() {
        // Given
        when(conversationRepository.findBySessionId(sessionId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> controller.deleteConversation(sessionId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Conversation not found for session: " + sessionId);
        verify(conversationRepository).findBySessionId(sessionId);
        verify(conversationRepository, never()).delete(any());
    }

    @Test
    void deleteConversationWhenCascadeDeleteRemovesAllMessages() {
        // Given
        Message message1 = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .role(MessageRole.USER)
                .content("Test message")
                .sources(null)
                .createdAt(createdAt)
                .build();

        Message message2 = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content("Test response")
                .sources(null)
                .createdAt(createdAt.plusMinutes(1))
                .build();

        conversation.getMessages().add(message1);
        conversation.getMessages().add(message2);

        when(conversationRepository.findBySessionId(sessionId))
                .thenReturn(Optional.of(conversation));
        doNothing().when(conversationRepository).delete(conversation);

        // When
        ResponseEntity<Void> response = controller.deleteConversation(sessionId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(conversationRepository).delete(conversation);
        // Cascade delete is handled by JPA, so we verify the conversation deletion
    }

    @Test
    void getConversationHistoryWhenSessionIdIsNullThrowsIllegalArgumentException() {
        // Given
        String nullSessionId = null;

        // When/Then
        assertThatThrownBy(() -> controller.getConversationHistory(nullSessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Session ID cannot be null or blank");
        verify(conversationRepository, never()).findBySessionId(any());
    }

    @Test
    void getConversationHistoryWhenSessionIdIsBlankThrowsIllegalArgumentException() {
        // Given
        String blankSessionId = "   ";

        // When/Then
        assertThatThrownBy(() -> controller.getConversationHistory(blankSessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Session ID cannot be null or blank");
        verify(conversationRepository, never()).findBySessionId(any());
    }

    @Test
    void getConversationHistoryWhenSessionIdExceedsMaxLengthThrowsIllegalArgumentException() {
        // Given
        String longSessionId = "a".repeat(256);

        // When/Then
        assertThatThrownBy(() -> controller.getConversationHistory(longSessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Session ID must not exceed 255 characters");
        verify(conversationRepository, never()).findBySessionId(any());
    }

    @Test
    void deleteConversationWhenSessionIdIsNullThrowsIllegalArgumentException() {
        // Given
        String nullSessionId = null;

        // When/Then
        assertThatThrownBy(() -> controller.deleteConversation(nullSessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Session ID cannot be null or blank");
        verify(conversationRepository, never()).findBySessionId(any());
    }

    @Test
    void deleteConversationWhenSessionIdIsBlankThrowsIllegalArgumentException() {
        // Given
        String blankSessionId = "";

        // When/Then
        assertThatThrownBy(() -> controller.deleteConversation(blankSessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Session ID cannot be null or blank");
        verify(conversationRepository, never()).findBySessionId(any());
    }

    @Test
    void deleteConversationWhenSessionIdExceedsMaxLengthThrowsIllegalArgumentException() {
        // Given
        String longSessionId = "b".repeat(256);

        // When/Then
        assertThatThrownBy(() -> controller.deleteConversation(longSessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Session ID must not exceed 255 characters");
        verify(conversationRepository, never()).findBySessionId(any());
    }
}
