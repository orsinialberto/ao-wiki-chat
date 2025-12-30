package com.example.ao_wiki_chat.cli.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Message DTO for CLI responses.
 * Mirrors the backend MessageResponse structure.
 */
public record CliMessage(
        @JsonProperty("content")
        String content,

        @JsonProperty("role")
        String role,

        @JsonProperty("createdAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,

        @JsonProperty("sources")
        String sources
) {}
