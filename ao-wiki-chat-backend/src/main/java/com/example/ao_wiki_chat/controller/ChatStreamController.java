package com.example.ao_wiki_chat.controller;

import java.io.IOException;
import java.io.PrintWriter;
import static java.util.Collections.emptyList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

/**
 * Compatibility controller that exposes the ao-chat frontend API contract.
 * Implements the anonymous chat endpoints so ao-chat can use ao-wiki-chat as a backend.
 * Delegates to the existing RAG pipeline for query processing.
 */
@RestController
@RequestMapping("/api")
public class ChatStreamController {

    private static final Logger log = LoggerFactory.getLogger(ChatStreamController.class);

    private final RAGService ragService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public ChatStreamController(
            RAGService ragService,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            ObjectMapper objectMapper
    ) {
        this.ragService = ragService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    // =====================================================
    // Anonymous chat endpoints (no auth required)
    // =====================================================

    @PostMapping("/anonymous/chats")
    public ResponseEntity<AoChatApiResponse<AoChatChatDto>> createAnonymousChat(
            @RequestBody AoChatCreateChatRequest request
    ) {
        log.info("creating anonymous chat, title={}", request.title());

        String sessionId = "aochat-" + UUID.randomUUID();
        String title = (request.title() != null && !request.title().isBlank()) ? request.title() : "New Chat";
        Conversation conversation = Conversation.builder()
                .sessionId(sessionId)
                .title(title)
                .build();

        conversation = conversationRepository.save(conversation);

        AoChatChatDto dto = toChatDto(conversation, emptyList());
        return ResponseEntity.status(CREATED).body(AoChatApiResponse.success(dto));
    }

    @PostMapping("/anonymous/chats/{chatId}/messages/stream")
    public void streamAnonymousMessage(
            @PathVariable String chatId,
            @RequestBody AoChatCreateMessageRequest request,
            HttpServletResponse response
    ) throws IOException {
        log.info("streaming message for chat {}", chatId);

        response.setContentType("text/event-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");
        response.flushBuffer();

        PrintWriter writer = response.getWriter();

        UUID conversationId;
        try {
            conversationId = UUID.fromString(chatId);
        } catch (IllegalArgumentException e) {
            writeSseError(writer, "Invalid chat ID format");
            return;
        }

        String content = request.content();
        if (content == null || content.isBlank()) {
            writeSseError(writer, "Message content is required");
            return;
        }

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) {
            writeSseError(writer, "Chat not found");
            return;
        }

        // Save user message
        Message userMsg = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(content.trim())
                .build();

        messageRepository.save(userMsg);

        // Stream RAG response
        ragService.processQueryStreaming(
                conversationId,
                content.trim(),
                // onChunk
                chunk -> {
                    try {
                        String json = objectMapper.writeValueAsString(Map.of("type", "chunk", "content", chunk));
                        writer.write("data: " + json + "\n\n");
                        writer.flush();
                    } catch (Exception e) {
                        log.error("Error writing SSE chunk: {}", e.getMessage());
                    }
                },
                // onComplete
                assistantMessage -> {
                    try {
                        AoChatMessageDto msgDto = toMessageDto(assistantMessage);
                        String json = objectMapper.writeValueAsString(Map.of("type", "done", "message", msgDto));
                        writer.write("data: " + json + "\n\n");
                        writer.flush();
                        writer.close();
                    } catch (Exception e) {
                        log.error("Error writing SSE done event: {}", e.getMessage());
                    }
                },
                // onError
                error -> {
                    log.error("Streaming RAG error: {}", error.getMessage(), error);
                    writeSseError(writer, error.getMessage());
                }
        );
    }

    @PostMapping("/anonymous/chats/{chatId}/messages")
    public ResponseEntity<AoChatApiResponse<AoChatMessageDto>> sendAnonymousMessage(
            @PathVariable String chatId,
            @RequestBody AoChatCreateMessageRequest request
    ) {
        log.info("sending message for chat {}", chatId);

        UUID conversationId;
        try {
            conversationId = UUID.fromString(chatId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AoChatApiResponse.error("BAD_REQUEST", "Invalid chat ID format"));
        }

        String content = request.content();
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(AoChatApiResponse.error("BAD_REQUEST", "Message content is required"));
        }

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) {
            return ResponseEntity.status(NOT_FOUND).body(AoChatApiResponse.error("NOT_FOUND", "Chat not found"));
        }

        // Use existing RAGService (synchronous)
        ChatRequest chatReq = new ChatRequest(content.trim(), conversation.getSessionId());
        ragService.processQuery(chatReq);

        // Retrieve the last assistant message saved by RAGService
        List<Message> messages = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);
        Message lastAssistant = messages.stream()
                .filter(m -> m.getRole() == MessageRole.ASSISTANT)
                .reduce((first, second) -> second)
                .orElse(null);

        if (lastAssistant == null) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR)
                    .body(AoChatApiResponse.error("INTERNAL_ERROR", "Failed to generate response"));
        }

        return ResponseEntity.ok(AoChatApiResponse.success(toMessageDto(lastAssistant)));
    }

    // =====================================================
    // Authenticated-style endpoints (used without auth for simplicity)
    // ao-chat sidebar calls these when listing/managing chats
    // =====================================================

    @GetMapping("/chats")
    public ResponseEntity<AoChatApiResponse<List<AoChatChatDto>>> listChats() {
        List<Conversation> conversations = conversationRepository.findAllByOrderByUpdatedAtDesc();
        
        List<AoChatChatDto> dtos = conversations.stream()
                .map(conv -> {
                    List<Message> msgs = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conv.getId());
                    Message lastMsg = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
                    List<AoChatMessageDto> lastMsgList = lastMsg != null ? List.of(toMessageDto(lastMsg)) : emptyList();
                    return toChatDto(conv, lastMsgList);
                })
                .toList();

        return ResponseEntity.ok(AoChatApiResponse.success(dtos));
    }

    @GetMapping("/chats/{chatId}")
    public ResponseEntity<AoChatApiResponse<AoChatChatDto>> getChat(@PathVariable String chatId) {
        UUID conversationId;
        try {
            conversationId = UUID.fromString(chatId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AoChatApiResponse.error("BAD_REQUEST", "Invalid chat ID format"));
        }

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) {
            return ResponseEntity.status(NOT_FOUND).body(AoChatApiResponse.error("NOT_FOUND", "Chat not found"));
        }

        List<Message> messages = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);
        List<AoChatMessageDto> msgDtos = messages.stream().map(this::toMessageDto).toList();

        return ResponseEntity.ok(AoChatApiResponse.success(toChatDto(conversation, msgDtos)));
    }

    @PutMapping("/chats/{chatId}")
    public ResponseEntity<AoChatApiResponse<AoChatChatDto>> updateChat(
            @PathVariable String chatId,
            @RequestBody Map<String, String> body
    ) {
        UUID conversationId;
        try {
            conversationId = UUID.fromString(chatId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AoChatApiResponse.error("BAD_REQUEST", "Invalid chat ID format"));
        }

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) {
            return ResponseEntity.status(NOT_FOUND).body(AoChatApiResponse.error("NOT_FOUND", "Chat not found"));
        }

        String newTitle = body.get("title");
        if (newTitle == null || newTitle.isBlank()) {
            return ResponseEntity.badRequest().body(AoChatApiResponse.error("BAD_REQUEST", "Title is required"));
        }

        // Conversation is immutable (no setters), so we delete and re-create with new title
        // preserving messages via sessionId lookup
        // Alternative: add a native query or a @Modifying update
        conversationRepository.updateTitle(conversationId, newTitle.trim());
        conversation = conversationRepository.findById(conversationId).orElseThrow();

        List<Message> messages = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);
        List<AoChatMessageDto> msgDtos = messages.stream().map(this::toMessageDto).toList();

        return ResponseEntity.ok(AoChatApiResponse.success(toChatDto(conversation, msgDtos)));
    }

    @DeleteMapping("/chats/{chatId}")
    public ResponseEntity<AoChatApiResponse<Map<String, String>>> deleteChat(@PathVariable String chatId) {
        UUID conversationId;
        try {
            conversationId = UUID.fromString(chatId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AoChatApiResponse.error("BAD_REQUEST", "Invalid chat ID format"));
        }

        Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
        if (conversation == null) {
            return ResponseEntity.status(NOT_FOUND).body(AoChatApiResponse.error("NOT_FOUND", "Chat not found"));
        }

        conversationRepository.delete(conversation);
        return ResponseEntity.ok(AoChatApiResponse.success(Map.of("message", "Chat deleted successfully")));
    }

    // =====================================================
    // Authenticated streaming endpoint (same logic, different path)
    // =====================================================

    @PostMapping("/chats/{chatId}/messages/stream")
    public void streamMessage(
            @PathVariable String chatId,
            @RequestBody AoChatCreateMessageRequest request,
            HttpServletResponse response
    ) throws IOException {
        streamAnonymousMessage(chatId, request, response);
    }

    @PostMapping("/chats/{chatId}/messages")
    public ResponseEntity<AoChatApiResponse<AoChatMessageDto>> sendMessage(
            @PathVariable String chatId,
            @RequestBody AoChatCreateMessageRequest request
    ) {
        return sendAnonymousMessage(chatId, request);
    }

    @PostMapping("/chats")
    public ResponseEntity<AoChatApiResponse<AoChatChatDto>> createChat(
            @RequestBody AoChatCreateChatRequest request
    ) {
        return createAnonymousChat(request);
    }

    // =====================================================
    // DTO mapping helpers
    // =====================================================

    private AoChatChatDto toChatDto(Conversation conversation, List<AoChatMessageDto> messages) {
        return new AoChatChatDto(
                conversation.getId().toString(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt() != null ? conversation.getUpdatedAt() : conversation.getCreatedAt(),
                messages
        );
    }

    private AoChatMessageDto toMessageDto(Message message) {
        return new AoChatMessageDto(
                message.getId().toString(),
                message.getConversation().getId().toString(),
                message.getRole().name().toLowerCase(),
                message.getContent(),
                null,
                message.getCreatedAt()
        );
    }

    private void writeSseError(PrintWriter writer, String errorMessage) {
        try (writer) {
            String json = objectMapper.writeValueAsString(Map.of("type", "error", "error", errorMessage));
            writer.write("data: " + json + "\n\n");
            writer.flush();
        } catch (Exception e) {
            log.error("Error writing SSE error event: {}", e.getMessage());
        }
    }
}
