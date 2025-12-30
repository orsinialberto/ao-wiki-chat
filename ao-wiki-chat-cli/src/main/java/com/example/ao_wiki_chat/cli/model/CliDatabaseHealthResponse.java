package com.example.ao_wiki_chat.cli.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Database health response DTO for CLI.
 * Mirrors the backend DatabaseHealthResponse structure.
 */
public record CliDatabaseHealthResponse(
        @JsonProperty("status")
        String status,

        @JsonProperty("database")
        String database
) {}
