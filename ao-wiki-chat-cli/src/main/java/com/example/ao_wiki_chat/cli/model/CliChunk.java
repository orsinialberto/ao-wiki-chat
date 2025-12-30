package com.example.ao_wiki_chat.cli.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Chunk DTO for CLI responses.
 * Mirrors the backend ChunkResponse structure.
 */
public record CliChunk(
        @JsonProperty("id")
        UUID id,

        @JsonProperty("documentId")
        UUID documentId,

        @JsonProperty("content")
        String content,

        @JsonProperty("chunkIndex")
        Integer chunkIndex,

        @JsonProperty("createdAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}
