package com.example.ao_wiki_chat.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.ao_wiki_chat.exception.RerankerException;
import com.example.ao_wiki_chat.model.entity.Chunk;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Cohere Rerank API implementation of {@link RerankerService}.
 * Calls Cohere v2 Rerank API to reorder chunks by relevance to the query.
 */
public class CohereRerankerService implements RerankerService {

    private static final Logger log = LoggerFactory.getLogger(CohereRerankerService.class);
    private static final String RERANK_URL = "https://api.cohere.com/v2/rerank";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public CohereRerankerService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${cohere.api.key:}") String apiKey,
            @Value("${cohere.rerank.model:rerank-v3.5}") String model
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "rerank-v3.5" : model;
        if (this.apiKey.isEmpty()) {
            throw new IllegalArgumentException("cohere.api.key is required when rag.reranker.enabled=true");
        }
        log.info("CohereRerankerService initialized with model: {}", this.model);
    }

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
        List<String> documents = new ArrayList<>();
        for (Chunk c : chunks) {
            documents.add(c.getContent());
        }
        int requestTopN = Math.min(topK, chunks.size());
        CohereRerankRequest request = new CohereRerankRequest(model, query, documents, requestTopN);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<CohereRerankRequest> entity = new HttpEntity<>(request, headers);
        try {
            String responseBody = restTemplate.postForObject(RERANK_URL, entity, String.class);
            if (responseBody == null || responseBody.isBlank()) {
                throw new RerankerException("Cohere rerank returned empty response");
            }
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) {
                throw new RerankerException("Cohere rerank response missing 'results' array");
            }
            List<Chunk> reranked = new ArrayList<>(results.size());
            for (JsonNode item : results) {
                int index = item.get("index").asInt();
                if (index >= 0 && index < chunks.size()) {
                    reranked.add(chunks.get(index));
                }
            }
            log.debug("Cohere rerank returned {} chunks for query (top_n={})", reranked.size(), requestTopN);
            return reranked;
        } catch (RerankerException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Cohere rerank API request failed: {}", e.getMessage());
            throw new RerankerException("Rerank request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Cohere rerank parse/response error: {}", e.getMessage());
            throw new RerankerException("Rerank failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

    private static class CohereRerankRequest {
        @JsonProperty("model")
        final String model;
        @JsonProperty("query")
        final String query;
        @JsonProperty("documents")
        final List<String> documents;
        @JsonProperty("top_n")
        final int topN;

        CohereRerankRequest(String model, String query, List<String> documents, int topN) {
            this.model = model;
            this.query = query;
            this.documents = documents;
            this.topN = topN;
        }
    }
}
