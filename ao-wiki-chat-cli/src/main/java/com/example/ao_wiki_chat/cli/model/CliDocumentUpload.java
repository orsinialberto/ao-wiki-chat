package com.example.ao_wiki_chat.cli.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Document upload response DTO for CLI.
 * Mirrors the backend DocumentUploadResponse structure.
 */
public record CliDocumentUpload(
        @JsonProperty("documentId")
        UUID documentId,

        @JsonProperty("filename")
        String filename,

        @JsonProperty("contentType")
        String contentType,

        @JsonProperty("fileSize")
        Long fileSize,

        @JsonProperty("status")
        String status,

        @JsonProperty("createdAt")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt
) {}
