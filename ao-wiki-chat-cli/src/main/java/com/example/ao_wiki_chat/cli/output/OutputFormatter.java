package com.example.ao_wiki_chat.cli.output;

import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import com.example.ao_wiki_chat.cli.model.CliChunk;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import com.example.ao_wiki_chat.cli.model.CliMessage;

import java.util.List;
import java.util.Map;

/**
 * Interface for formatting CLI output in different formats.
 * Implementations handle formatting of documents, chat responses, history, and chunks.
 */
public interface OutputFormatter {

    /**
     * Formats a list of documents.
     *
     * @param documents the documents to format
     * @return formatted output string
     */
    String formatDocuments(List<CliDocument> documents);

    /**
     * Formats a single document with details.
     *
     * @param document the document to format
     * @return formatted output string
     */
    String formatDocument(CliDocument document);

    /**
     * Formats document metadata.
     *
     * @param metadata the metadata map to format
     * @return formatted output string
     */
    String formatMetadata(Map<String, Object> metadata);

    /**
     * Formats a chat response.
     *
     * @param response the chat response to format
     * @param showSources whether to include source references
     * @return formatted output string
     */
    String formatChatResponse(CliChatResponse response, boolean showSources);

    /**
     * Formats conversation history.
     *
     * @param messages the list of messages to format
     * @param showSources whether to include source references
     * @return formatted output string
     */
    String formatHistory(List<CliMessage> messages, boolean showSources);

    /**
     * Formats a list of chunks.
     *
     * @param chunks the chunks to format
     * @param limit optional limit on number of chunks to display (null for all)
     * @return formatted output string
     */
    String formatChunks(List<CliChunk> chunks, Integer limit);
}
