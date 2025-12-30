package com.example.ao_wiki_chat.cli.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Gemini health response DTO for CLI.
 * Mirrors the backend GeminiHealthResponse structure.
 */
public record CliGeminiHealthResponse(
        @JsonProperty("status")
        String status,

        @JsonProperty("gemini")
        String gemini
) {}
