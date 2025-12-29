package com.example.ao_wiki_chat.controller;

import com.example.ao_wiki_chat.model.dto.ChatRequest;
import com.example.ao_wiki_chat.model.dto.ChatResponse;
import com.example.ao_wiki_chat.model.dto.MessageResponse;
import com.example.ao_wiki_chat.model.entity.Conversation;
import com.example.ao_wiki_chat.model.entity.Message;
import com.example.ao_wiki_chat.repository.ConversationRepository;
import com.example.ao_wiki_chat.repository.MessageRepository;
import com.example.ao_wiki_chat.service.RAGService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for chat operations.
 * Handles user queries and conversation history management.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final RAGService ragService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    /**
     * Constructs a ChatController with required dependencies.
     *
     * @param ragService service for processing RAG queries
     * @param conversationRepository repository for conversation operations
     * @param messageRepository repository for message operations
     */
    public ChatController(
            RAGService ragService,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository
    ) {
        this.ragService = ragService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Processes a chat query through the RAG pipeline.
     * Accepts a chat request, validates it, and returns a response with answer and source references.
     *
     * @param request the chat request containing query and session ID
     * @return chat response with answer and sources, 200 OK status
     */
    @PostMapping("/query")
    public ResponseEntity<ChatResponse> processQuery(
            @Valid @RequestBody ChatRequest request
    ) {
        log.info("Received chat query request for session: {}, query length: {}",
                request.sessionId(), request.query().length());

        ChatResponse response = ragService.processQuery(request);

        log.info("Chat query processed successfully for session: {}", request.sessionId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves conversation history for a session.
     * Returns all messages in chronological order.
     *
     * @param sessionId the session identifier
     * @return list of messages with 200 OK status, or 404 if session not found
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<MessageResponse>> getConversationHistory(
            @PathVariable String sessionId
    ) {
        validateSessionId(sessionId);
        log.debug("Retrieving conversation history for session: {}", sessionId);

        Conversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.warn("Conversation not found for session: {}", sessionId);
                    return new EntityNotFoundException("Conversation not found for session: " + sessionId);
                });

        List<Message> messages = messageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId());
        List<MessageResponse> messageResponses = messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());

        log.debug("Retrieved {} messages for session: {}", messageResponses.size(), sessionId);
        return ResponseEntity.ok(messageResponses);
    }

    /**
     * Deletes a conversation and all associated messages.
     * Uses cascade delete to remove all messages automatically.
     *
     * @param sessionId the session identifier
     * @return 204 No Content on success, or 404 if session not found
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable String sessionId
    ) {
        validateSessionId(sessionId);
        log.info("Deleting conversation for session: {}", sessionId);

        Conversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> {
                    log.warn("Conversation not found for deletion: {}", sessionId);
                    return new EntityNotFoundException("Conversation not found for session: " + sessionId);
                });

        conversationRepository.delete(conversation);
        log.info("Conversation deleted successfully for session: {}", sessionId);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Converts Message entity to MessageResponse DTO.
     *
     * @param message the message entity
     * @return message response DTO
     */
    private MessageResponse toMessageResponse(Message message) {
        return new MessageResponse(
                message.getContent(),
                message.getRole(),
                message.getCreatedAt(),
                message.getSources()
        );
    }

    /**
     * Validates session ID format.
     *
     * @param sessionId the session ID to validate
     * @throws IllegalArgumentException if session ID is invalid
     */
    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID cannot be null or blank");
        }
        if (sessionId.length() > 255) {
            throw new IllegalArgumentException("Session ID must not exceed 255 characters");
        }
    }
}
