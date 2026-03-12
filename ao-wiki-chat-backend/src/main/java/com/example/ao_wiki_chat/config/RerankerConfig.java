package com.example.ao_wiki_chat.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.example.ao_wiki_chat.service.CohereRerankerService;
import com.example.ao_wiki_chat.service.NoOpRerankerService;
import com.example.ao_wiki_chat.service.RerankerService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Configuration for the RAG reranker.
 * Registers a single {@link RerankerService} bean: Cohere when enabled and API key set, otherwise no-op.
 */
@Configuration
public class RerankerConfig {

    @Bean
    public RestTemplate rerankerRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnProperty(name = "rag.reranker.enabled", havingValue = "true")
    public RerankerService cohereRerankerService(
            @Qualifier("rerankerRestTemplate") RestTemplate rerankerRestTemplate,
            ObjectMapper objectMapper,
            @Value("${cohere.api.key:}") String cohereApiKey,
            @Value("${cohere.rerank.model:rerank-v3.5}") String cohereRerankModel
    ) {
        return new CohereRerankerService(rerankerRestTemplate, objectMapper, cohereApiKey, cohereRerankModel);
    }

    @Bean
    @ConditionalOnProperty(name = "rag.reranker.enabled", havingValue = "false", matchIfMissing = true)
    public RerankerService noOpRerankerService() {
        return new NoOpRerankerService();
    }
}
