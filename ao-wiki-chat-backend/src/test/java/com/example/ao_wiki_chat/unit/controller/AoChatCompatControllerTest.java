package com.example.ao_wiki_chat.unit.controller;

import com.example.ao_wiki_chat.controller.ChatStreamController;
import com.example.ao_wiki_chat.model.dto.ChatRequest;
import com.example.ao_wiki_chat.model.dto.aochat.AoChatApiResponse;
import com.example.ao_wiki_chat.model.dto.aochat.AoChatChatDto;
import com.example.ao_wiki_chat.model.dto.aochat.AoChatCreateChatRequest;
import com.example.ao_wiki_chat.model.dto.aochat.AoChatCreateMessageRequest;
import com.example.ao_wiki_chat.model.dto.aochat.AoChatMessageDto;
import com.example.ao_wiki_chat.model.entity.Conversation;
import com.example.ao_wiki_chat.model.entity.Message;
import com.example.ao_wiki_chat.model.enums.MessageRole;
import com.example.ao_wiki_chat.repository.ConversationRepository;
import com.example.ao_wiki_chat.repository.MessageRepository;
import com.example.ao_wiki_chat.service.RAGService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AoChatCompatController (ao-chat API compatibility).
 */
@ExtendWith(MockitoExtension.class)
class AoChatCompatControllerTest {

    @Mock
    private RAGService ragService;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ChatStreamController controller;

    private UUID conversationId;
    private Conversation conversation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @BeforeEach
    void setUp() {
        controller = new ChatStreamController(
                ragService,
                conversationRepository,
                messageRepository,
                objectMapper
        );

        conversationId = UUID.randomUUID();
        createdAt = LocalDateTime.now().minusHours(1);
        updatedAt = LocalDateTime.now().minusMinutes(30);

        conversation = Conversation.builder()
                .id(conversationId)
                .sessionId("aochat-" + conversationId)
                .title("Test Chat")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    // ---------- createAnonymousChat / createChat ----------

    @Test
    void createAnonymousChatWhenValidRequestReturnsCreatedWithChatDto() {
        AoChatCreateChatRequest request = new AoChatCreateChatRequest("My Title", null, null);
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            return Conversation.builder()
                    .id(conversationId)
                    .sessionId(c.getSessionId())
                    .title(c.getTitle())
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build();
        });

        ResponseEntity<AoChatApiResponse<AoChatChatDto>> response =
                controller.createAnonymousChat(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).isNotNull();
        assertThat(response.getBody().data().title()).isEqualTo("My Title");
        assertThat(response.getBody().data().messages()).isEmpty();
        verify(conversationRepository).save(any(Conversation.class));
    }

    @Test
    void createAnonymousChatWhenTitleBlankUsesDefaultTitle() {
        AoChatCreateChatRequest request = new AoChatCreateChatRequest("   ", null, null);
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            return Conversation.builder()
                    .id(conversationId)
                    .sessionId(c.getSessionId())
                    .title(c.getTitle())
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build();
        });

        ResponseEntity<AoChatApiResponse<AoChatChatDto>> response =
                controller.createAnonymousChat(request);

        assertThat(response.getBody().data().title()).isEqualTo("New Chat");
    }

    @Test
    void createChatDelegatesToCreateAnonymousChat() {
        AoChatCreateChatRequest request = new AoChatCreateChatRequest("Delegated", null, null);
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            return Conversation.builder()
                    .id(conversationId)
                    .sessionId(c.getSessionId())
                    .title(c.getTitle())
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .build();
        });

        ResponseEntity<AoChatApiResponse<AoChatChatDto>> response = controller.createChat(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().data().title()).isEqualTo("Delegated");
    }

    // ---------- sendAnonymousMessage / sendMessage ----------

    @Test
    void sendAnonymousMessageWhenValidReturnsOkWithMessageDto() {
        String content = "What is RAG?";
        AoChatCreateMessageRequest request = new AoChatCreateMessageRequest(null, content, null, null);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        lenient().when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(
                        Message.builder().id(UUID.randomUUID()).conversation(conversation)
                                .role(MessageRole.USER).content(content).createdAt(createdAt).build(),
                        Message.builder().id(UUID.randomUUID()).conversation(conversation)
                                .role(MessageRole.ASSISTANT).content("RAG is retrieval-augmented generation.")
                                .createdAt(updatedAt).build()
                ));

        ResponseEntity<AoChatApiResponse<AoChatMessageDto>> response =
                controller.sendAnonymousMessage(conversationId.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().content()).isEqualTo("RAG is retrieval-augmented generation.");
        assertThat(response.getBody().data().role()).isEqualTo("assistant");
        verify(ragService).processQuery(any(ChatRequest.class));
    }

    @Test
    void sendAnonymousMessageWhenInvalidChatIdReturnsBadRequest() {
        AoChatCreateMessageRequest request = new AoChatCreateMessageRequest(null, "Hi", null, null);

        ResponseEntity<AoChatApiResponse<AoChatMessageDto>> response =
                controller.sendAnonymousMessage("not-a-uuid", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error()).isEqualTo("BAD_REQUEST");
        verify(conversationRepository, never()).findById(any());
    }

    @Test
    void sendAnonymousMessageWhenContentBlankReturnsBadRequest() {
        AoChatCreateMessageRequest request = new AoChatCreateMessageRequest(null, "   ", null, null);

        ResponseEntity<AoChatApiResponse<AoChatMessageDto>> response =
                controller.sendAnonymousMessage(conversationId.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error()).isEqualTo("BAD_REQUEST");
    }

    @Test
    void sendAnonymousMessageWhenChatNotFoundReturnsNotFound() {
        AoChatCreateMessageRequest request = new AoChatCreateMessageRequest(null, "Hi", null, null);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        ResponseEntity<AoChatApiResponse<AoChatMessageDto>> response =
                controller.sendAnonymousMessage(conversationId.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().error()).isEqualTo("NOT_FOUND");
    }

    @Test
    void sendAnonymousMessageWhenNoAssistantMessageReturnsInternalError() {
        AoChatCreateMessageRequest request = new AoChatCreateMessageRequest(null, "Hi", null, null);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        lenient().when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(
                        Message.builder().id(UUID.randomUUID()).conversation(conversation)
                                .role(MessageRole.USER).content("Hi").createdAt(createdAt).build()
                ));

        ResponseEntity<AoChatApiResponse<AoChatMessageDto>> response =
                controller.sendAnonymousMessage(conversationId.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void sendMessageDelegatesToSendAnonymousMessage() {
        AoChatCreateMessageRequest request = new AoChatCreateMessageRequest(null, "Hello", null, null);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        lenient().when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(
                        Message.builder().id(UUID.randomUUID()).conversation(conversation)
                                .role(MessageRole.USER).content("Hello").createdAt(createdAt).build(),
                        Message.builder().id(UUID.randomUUID()).conversation(conversation)
                                .role(MessageRole.ASSISTANT).content("Hi there").createdAt(updatedAt).build()
                ));

        ResponseEntity<AoChatApiResponse<AoChatMessageDto>> response =
                controller.sendMessage(conversationId.toString(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().content()).isEqualTo("Hi there");
    }

    // ---------- listChats ----------

    @Test
    void listChatsReturnsOkWithChatDtos() {
        when(conversationRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(conversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(Collections.emptyList());

        ResponseEntity<AoChatApiResponse<List<AoChatChatDto>>> response = controller.listChats();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).hasSize(1);
        assertThat(response.getBody().data().get(0).id()).isEqualTo(conversationId.toString());
        assertThat(response.getBody().data().get(0).title()).isEqualTo("Test Chat");
    }

    @Test
    void listChatsWhenConversationHasLastMessageIncludesItInDto() {
        Message lastMsg = Message.builder()
                .id(UUID.randomUUID())
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content("Last reply")
                .createdAt(updatedAt)
                .build();
        when(conversationRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(conversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(List.of(lastMsg));

        ResponseEntity<AoChatApiResponse<List<AoChatChatDto>>> response = controller.listChats();

        assertThat(response.getBody().data().get(0).messages()).hasSize(1);
        assertThat(response.getBody().data().get(0).messages().get(0).content()).isEqualTo("Last reply");
    }

    // ---------- getChat ----------

    @Test
    void getChatWhenFoundReturnsOkWithChatDto() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(Collections.emptyList());

        ResponseEntity<AoChatApiResponse<AoChatChatDto>> response =
                controller.getChat(conversationId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().id()).isEqualTo(conversationId.toString());
        assertThat(response.getBody().data().updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void getChatWhenConversationHasNullUpdatedAtUsesCreatedAt() {
        Conversation noUpdated = Conversation.builder()
                .id(conversationId)
                .sessionId("s1")
                .title("T")
                .createdAt(createdAt)
                .updatedAt(null)
                .build();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(noUpdated));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(Collections.emptyList());

        ResponseEntity<AoChatApiResponse<AoChatChatDto>> response =
                controller.getChat(conversationId.toString());

        assertThat(response.getBody().data().updatedAt()).isEqualTo(createdAt);
    }

    @Test
    void getChatWhenInvalidIdReturnsBadRequest() {
        ResponseEntity<AoChatApiResponse<AoChatChatDto>> response = controller.getChat("bad");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getChatWhenNotFoundReturnsNotFound() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        ResponseEntity<AoChatApiResponse<AoChatChatDto>> response =
                controller.getChat(conversationId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- updateChat ----------

    @Test
    void updateChatWhenValidReturnsOkWithUpdatedChatDto() {
        doAnswer(inv -> null).when(conversationRepository).updateTitle(conversationId, "New Title");
        Conversation updated = Conversation.builder()
                .id(conversationId)
                .sessionId(conversation.getSessionId())
                .title("New Title")
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(conversation))
                .thenReturn(Optional.of(updated));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId))
                .thenReturn(Collections.emptyList());

        ResponseEntity<AoChatApiResponse<AoChatChatDto>> response =
                controller.updateChat(conversationId.toString(), Map.of("title", "New Title"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data().title()).isEqualTo("New Title");
        verify(conversationRepository).updateTitle(conversationId, "New Title");
    }

    @Test
    void updateChatWhenTitleBlankReturnsBadRequest() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        ResponseEntity<AoChatApiResponse<AoChatChatDto>> response =
                controller.updateChat(conversationId.toString(), Map.of("title", "   "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateChatWhenNotFoundReturnsNotFound() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        ResponseEntity<AoChatApiResponse<AoChatChatDto>> response =
                controller.updateChat(conversationId.toString(), Map.of("title", "T"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- deleteChat ----------

    @Test
    void deleteChatWhenFoundReturnsOk() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        ResponseEntity<AoChatApiResponse<Map<String, String>>> response =
                controller.deleteChat(conversationId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data().get("message")).isEqualTo("Chat deleted successfully");
        verify(conversationRepository).delete(conversation);
    }

    @Test
    void deleteChatWhenNotFoundReturnsNotFound() {
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        ResponseEntity<AoChatApiResponse<Map<String, String>>> response =
                controller.deleteChat(conversationId.toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(conversationRepository, never()).delete(any());
    }

    // ---------- streamAnonymousMessage (sync callback) ----------

    @Test
    void streamAnonymousMessageWhenValidInvokesStreamingAndWritesSse() throws IOException {
        String content = "Hello";
        AoChatCreateMessageRequest request = new AoChatCreateMessageRequest(null, content, null, null);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));

        doAnswer(inv -> {
            java.util.function.Consumer<String> onChunk = inv.getArgument(2);
            onChunk.accept("Streamed ");
            onChunk.accept("reply");
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<Message> onComplete = inv.getArgument(3);
            Message msg = Message.builder()
                    .id(UUID.randomUUID())
                    .conversation(conversation)
                    .role(MessageRole.ASSISTANT)
                    .content("Streamed reply")
                    .createdAt(updatedAt)
                    .build();
            onComplete.accept(msg);
            return null;
        }).when(ragService).processQueryStreaming(
                eq(conversationId),
                eq(content),
                any(),
                any(),
                any()
        );

        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(writer);

        controller.streamAnonymousMessage(conversationId.toString(), request, response);

        verify(ragService).processQueryStreaming(eq(conversationId), eq(content), any(), any(), any());
        String written = out.toString();
        assertThat(written).contains("data:");
        assertThat(written).contains("chunk");
        assertThat(written).contains("Streamed");
    }

    @Test
    void streamAnonymousMessageWhenInvalidChatIdWritesSseError() throws IOException {
        AoChatCreateMessageRequest request = new AoChatCreateMessageRequest(null, "Hi", null, null);
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(writer);

        controller.streamAnonymousMessage("invalid-uuid", request, response);

        verify(ragService, never()).processQueryStreaming(any(), any(), any(), any(), any());
        assertThat(out.toString()).contains("error");
        assertThat(out.toString()).contains("Invalid chat ID");
    }

    @Test
    void streamMessageDelegatesToStreamAnonymousMessage() throws IOException {
        AoChatCreateMessageRequest request = new AoChatCreateMessageRequest(null, "Hi", null, null);
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        lenient().when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<Message> onComplete = inv.getArgument(3);
            onComplete.accept(
                    Message.builder().id(UUID.randomUUID()).conversation(conversation)
                            .role(MessageRole.ASSISTANT).content("OK").createdAt(updatedAt).build()
            );
            return null;
        }).when(ragService).processQueryStreaming(any(), any(), any(), any(), any());

        StringWriter out = new StringWriter();
        HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(out));

        controller.streamMessage(conversationId.toString(), request, response);

        verify(ragService).processQueryStreaming(any(), any(), any(), any(), any());
    }
}
