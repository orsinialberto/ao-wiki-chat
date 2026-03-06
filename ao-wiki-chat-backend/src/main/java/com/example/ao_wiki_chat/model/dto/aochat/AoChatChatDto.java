package com.example.ao_wiki_chat.model.dto.aochat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat DTO matching ao-chat frontend expectations.
 * Maps from Conversation entity.
 */
public record AoChatChatDto(
        String id,
        String title,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AoChatMessageDto> messages
) {}
