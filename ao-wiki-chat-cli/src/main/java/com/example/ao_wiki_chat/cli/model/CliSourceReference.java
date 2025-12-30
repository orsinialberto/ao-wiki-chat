package com.example.ao_wiki_chat.cli.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Source reference DTO for CLI.
 * Mirrors the backend SourceReference structure.
 */
public record CliSourceReference(
        @JsonProperty("documentName")
        String documentName,

        @JsonProperty("chunkContent")
        String chunkContent,

        @JsonProperty("similarityScore")
        Double similarityScore,

        @JsonProperty("chunkIndex")
        Integer chunkIndex
) {}
