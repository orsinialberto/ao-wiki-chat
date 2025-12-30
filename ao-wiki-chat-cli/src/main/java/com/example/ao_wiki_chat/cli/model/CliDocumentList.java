package com.example.ao_wiki_chat.cli.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Document list response DTO for CLI.
 * Mirrors the backend DocumentListResponse structure.
 */
public record CliDocumentList(
        @JsonProperty("documents")
        List<CliDocument> documents,

        @JsonProperty("totalCount")
        Integer totalCount
) {}
