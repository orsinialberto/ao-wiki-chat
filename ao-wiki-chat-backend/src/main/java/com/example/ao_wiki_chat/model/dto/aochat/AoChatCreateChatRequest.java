package com.example.ao_wiki_chat.model.dto.aochat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Request DTO for creating a new chat, matching ao-chat frontend format.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AoChatCreateChatRequest(
        String title,
        String initialMessage,
        String model
) {}
