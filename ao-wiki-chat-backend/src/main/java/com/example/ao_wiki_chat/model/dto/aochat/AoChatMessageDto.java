package com.example.ao_wiki_chat.model.dto.aochat;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Message DTO matching ao-chat frontend expectations.
 * Maps from Message entity.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AoChatMessageDto(
        String id,
        String chatId,
        String role,
        String content,
        Map<String, Object> metadata,
        LocalDateTime createdAt
) {}
