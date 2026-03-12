package com.example.ao_wiki_chat.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.ao_wiki_chat.model.entity.Chunk;

/**
 * No-op implementation of {@link RerankerService}.
 * Returns chunks unchanged (or truncated to topK) without calling any external API.
 * Used when reranking is disabled.
 */
public class NoOpRerankerService implements RerankerService {

    private static final Logger log = LoggerFactory.getLogger(NoOpRerankerService.class);

    @Override
    public List<Chunk> rerank(String query, List<Chunk> chunks, int topK) {
        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }
        if (chunks == null) {
            throw new IllegalArgumentException("Chunks cannot be null");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("Top-k must be positive, got: " + topK);
        }
        if (chunks.isEmpty()) {
            return List.of();
        }
        int limit = Math.min(chunks.size(), topK);
        if (limit < chunks.size()) {
            log.debug("NoOpReranker: truncating {} chunks to top {}", chunks.size(), topK);
            return new ArrayList<>(chunks.subList(0, limit));
        }
        return new ArrayList<>(chunks);
    }

    @Override
    public boolean isActive() {
        return false;
    }
}
