package com.example.ao_wiki_chat.cli.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Chat request DTO for CLI.
 * Mirrors the backend ChatRequest structure.
 */
public record CliChatRequest(
        @JsonProperty("query")
        String query,

        @JsonProperty("sessionId")
        String sessionId
) {}
