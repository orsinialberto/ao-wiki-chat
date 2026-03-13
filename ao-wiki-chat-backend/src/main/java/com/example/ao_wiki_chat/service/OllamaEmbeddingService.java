package com.example.ao_wiki_chat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.example.ao_wiki_chat.exception.EmbeddingException;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

/**
 * Ollama implementation of EmbeddingService.
 * Uses LangChain4j's OllamaEmbeddingModel (e.g. nomic-embed-text).
 * Vectors are padded to the configured dimension (768) when the model
 * returns fewer dimensions.
 */
@Service("ollamaEmbeddingService")
@ConditionalOnProperty(name = "app.embedding.provider", havingValue = "ollama")
public class OllamaEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(OllamaEmbeddingService.class);

    private final EmbeddingModel embeddingModel;
    private final int embeddingDimension;

    public OllamaEmbeddingService(
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
            @Value("${ollama.embedding.dimension:768}") int embeddingDimension
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingDimension = embeddingDimension;
        log.info("Ollama embedding service configured with dimension: {}", embeddingDimension);
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new EmbeddingException("Text cannot be null or empty");
        }
        log.debug("Generating Ollama embedding for text of length: {}", text.length());
        try {
            Response<Embedding> response = embeddingModel.embed(text);
            Embedding embedding = response.content();
            if (embedding == null || embedding.vector() == null) {
                throw new EmbeddingException("Received null embedding from Ollama");
            }
            return ensureDimension(embedding.vector());
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate embedding from Ollama: {}", e.getMessage(), e);
            throw new EmbeddingException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i) == null || texts.get(i).trim().isEmpty()) {
                throw new EmbeddingException("Text at index " + i + " is null or empty");
            }
        }
        log.info("Generating {} embeddings via Ollama", texts.size());
        try {
            List<TextSegment> segments = texts.stream()
                    .map(TextSegment::from)
                    .collect(Collectors.toList());
            Response<List<Embedding>> response = embeddingModel.embedAll(segments);
            if (response.content() == null || response.content().isEmpty()) {
                throw new EmbeddingException("Received empty response from Ollama for batch");
            }
            List<float[]> result = response.content().stream()
                    .map(Embedding::vector)
                    .map(this::ensureDimension)
                    .collect(Collectors.toList());
            if (result.size() != texts.size()) {
                throw new EmbeddingException(
                        String.format("Expected %d embeddings but got %d", texts.size(), result.size()));
            }
            return result;
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate batch embeddings from Ollama: {}", e.getMessage(), e);
            throw new EmbeddingException("Failed to generate batch embeddings: " + e.getMessage(), e);
        }
    }

    /**
     * Ensures the vector has exactly {@link #embeddingDimension} elements.
     * If the model returns fewer (e.g. 768 for nomic-embed-text), pads with zeros.
     */
    private float[] ensureDimension(float[] vector) {
        if (vector.length == embeddingDimension) {
            return vector;
        }
        if (vector.length > embeddingDimension) {
            float[] truncated = new float[embeddingDimension];
            System.arraycopy(vector, 0, truncated, 0, embeddingDimension);
            log.debug("Truncated embedding from {} to {} dimensions", vector.length, embeddingDimension);
            return truncated;
        }
        float[] padded = new float[embeddingDimension];
        System.arraycopy(vector, 0, padded, 0, vector.length);
        log.debug("Padded embedding from {} to {} dimensions", vector.length, embeddingDimension);
        return padded;
    }

    @Override
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }

    @Override
    public boolean isHealthy() {
        try {
            float[] test = generateEmbedding("test");
            return test != null && test.length == embeddingDimension;
        } catch (Exception e) {
            log.warn("Ollama embedding health check failed: {}", e.getMessage());
            return false;
        }
    }
}
