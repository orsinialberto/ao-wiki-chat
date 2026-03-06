package com.example.ao_wiki_chat.model.dto.aochat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Request DTO for sending a message, matching ao-chat frontend format.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AoChatCreateMessageRequest(
        String chatId,
        String content,
        String role,
        String model
) {}
