package com.example.ao_wiki_chat.cli.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Chat response DTO for CLI.
 * Mirrors the backend ChatResponse structure.
 */
public record CliChatResponse(
        @JsonProperty("answer")
        String answer,

        @JsonProperty("sources")
        List<CliSourceReference> sources
) {}
