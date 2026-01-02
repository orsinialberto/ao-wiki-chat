package com.example.ao_wiki_chat.cli.output;

import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import com.example.ao_wiki_chat.cli.model.CliChunk;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import com.example.ao_wiki_chat.cli.model.CliMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Formatter that outputs data as pretty-printed JSON.
 * Uses Jackson ObjectMapper for JSON serialization.
 */
public final class JsonFormatter implements OutputFormatter {

    private static final ObjectMapper objectMapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    @Override
    public String formatDocuments(List<CliDocument> documents) {
        try {
            if (documents == null) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(new HashMap<>());
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(documents);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize documents: " + e.getMessage() + "\"}";
        }
    }

    @Override
    public String formatDocument(CliDocument document) {
        try {
            if (document == null) {
                return "null";
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(document);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize document: " + e.getMessage() + "\"}";
        }
    }

    @Override
    public String formatMetadata(Map<String, Object> metadata) {
        try {
            if (metadata == null) {
                return "null";
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize metadata: " + e.getMessage() + "\"}";
        }
    }

    @Override
    public String formatChatResponse(CliChatResponse response, boolean showSources) {
        try {
            if (response == null) {
                return "null";
            }
            // If showSources is false, create a response without sources
            if (!showSources) {
                CliChatResponse responseWithoutSources = new CliChatResponse(
                        response.answer(),
                        null
                );
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseWithoutSources);
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize response: " + e.getMessage() + "\"}";
        }
    }

    @Override
    public String formatHistory(List<CliMessage> messages, boolean showSources) {
        try {
            if (messages == null) {
                return "[]";
            }
            // If showSources is false, create messages without sources
            if (!showSources) {
                List<CliMessage> messagesWithoutSources = messages.stream()
                        .map(msg -> new CliMessage(
                                msg.content(),
                                msg.role(),
                                msg.createdAt(),
                                null
                        ))
                        .toList();
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(messagesWithoutSources);
            }
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(messages);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize messages: " + e.getMessage() + "\"}";
        }
    }

    @Override
    public String formatChunks(List<CliChunk> chunks, Integer limit) {
        try {
            if (chunks == null) {
                return "[]";
            }
            List<CliChunk> displayChunks = limit != null && limit > 0 && limit < chunks.size()
                    ? chunks.subList(0, limit)
                    : chunks;
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(displayChunks);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize chunks: " + e.getMessage() + "\"}";
        }
    }
}
