package com.example.ao_wiki_chat.cli.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Health response DTO for CLI.
 * Mirrors the backend HealthResponse structure.
 */
public record CliHealthResponse(
        @JsonProperty("status")
        String status
) {}
