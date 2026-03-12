package com.example.ao_wiki_chat.service;

import java.util.List;

import com.example.ao_wiki_chat.model.entity.Chunk;

/**
 * Contract for services that rerank retrieved chunks by relevance to a query.
 * Used as a second-stage ranker after vector search to improve context quality for RAG.
 */
public interface RerankerService {

    /**
     * Reranks the given chunks by relevance to the query and returns the top-k.
     * Order of the returned list is from most to least relevant.
     *
     * @param query the user query
     * @param chunks the chunks from initial retrieval (e.g. vector search)
     * @param topK maximum number of chunks to return
     * @return list of chunks ordered by relevance, size at most topK; never null
     * @throws IllegalArgumentException if query is null or topK is invalid
     * @throws com.example.ao_wiki_chat.exception.RerankerException if reranking fails
     */
    List<Chunk> rerank(String query, List<Chunk> chunks, int topK);

    /**
     * Whether this implementation performs actual reranking (e.g. API call).
     * When false, rerank may simply return the input list or a sublist.
     *
     * @return true if reranking is active, false for no-op implementation
     */
    boolean isActive();
}
