package com.example.ao_wiki_chat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.ao_wiki_chat.exception.EmbeddingException;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

/**
 * Gemini-specific implementation of EmbeddingService.
 * Uses LangChain4j's EmbeddingModel for Gemini gemini-embedding-001 integration.
 * Supports batch processing with configurable batch size to optimize API usage
 * and respect rate limits.
 */
@Service
public class GeminiEmbeddingService {
    
    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingService.class);
    private static final int MAX_BATCH_SIZE = 100;
    
    private final EmbeddingModel embeddingModel;
    private final int embeddingDimension;
    
    /**
     * Constructs a GeminiEmbeddingService with the specified embedding model and dimension.
     *
     * @param embeddingModel the configured Gemini embedding model bean
     * @param embeddingDimension the dimensionality of the embeddings (default: 768)
     */
    public GeminiEmbeddingService(
            @Qualifier("geminiEmbeddingModel") EmbeddingModel embeddingModel,
            @Value("${gemini.embedding.dimension:768}") int embeddingDimension
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingDimension = embeddingDimension;
        log.info("GeminiEmbeddingService initialized with dimension: {}", embeddingDimension);
    }
    
    /**
     * Generates a vector embedding for a single text.
     *
     * @param text the input text to embed
     * @return the embedding as a float array
     * @throws com.example.ao_wiki_chat.exception.EmbeddingException if embedding generation fails
     */
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new EmbeddingException("Text cannot be null or empty");
        }
        
        log.debug("Generating embedding for text of length: {}", text.length());
        
        try {
            long startTime = System.currentTimeMillis();
            Response<Embedding> response = embeddingModel.embed(text);
            Embedding embedding = response.content();
            long duration = System.currentTimeMillis() - startTime;
            
            if (embedding == null || embedding.vector() == null) {
                log.warn("Received null embedding from Gemini");
                throw new EmbeddingException("Received null embedding from Gemini API");
            }
            
            float[] vector = embedding.vector();
            
            if (vector.length != embeddingDimension) {
                log.warn("Expected embedding dimension: {}, but got: {}", embeddingDimension, vector.length);
            }
            
            log.debug("Embedding generated in {}ms, dimension: {}", duration, vector.length);
            
            return vector;
            
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate embedding from Gemini: {}", e.getMessage(), e);
            throw new EmbeddingException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generates vector embeddings for multiple texts in batch.
     * Batch processing is more efficient for large collections.
     *
     * @param texts the list of texts to embed
     * @return list of embeddings (same order as input)
     * @throws com.example.ao_wiki_chat.exception.EmbeddingException if embedding generation fails
     */
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.debug("Empty text list provided, returning empty result");
            return List.of();
        }
        
        // Validate all texts are non-empty
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i) == null || texts.get(i).trim().isEmpty()) {
                throw new EmbeddingException("Text at index " + i + " is null or empty");
            }
        }
        
        log.info("Generating embeddings for {} texts", texts.size());
        
        try {
            List<float[]> allEmbeddings = new ArrayList<>();
            
            // Process in batches to respect API limits
            for (int i = 0; i < texts.size(); i += MAX_BATCH_SIZE) {
                int endIndex = Math.min(i + MAX_BATCH_SIZE, texts.size());
                List<String> batch = texts.subList(i, endIndex);
                
                log.debug("Processing batch {}-{} of {}", i + 1, endIndex, texts.size());
                
                List<TextSegment> segments = batch.stream()
                        .map(TextSegment::from)
                        .collect(Collectors.toList());
                
                long startTime = System.currentTimeMillis();
                Response<List<Embedding>> response = embeddingModel.embedAll(segments);
                long duration = System.currentTimeMillis() - startTime;
                
                if (response.content() == null || response.content().isEmpty()) {
                    throw new EmbeddingException("Received empty response from Gemini API for batch");
                }
                
                List<float[]> batchEmbeddings = response.content().stream()
                        .map(Embedding::vector)
                        .collect(Collectors.toList());
                
                allEmbeddings.addAll(batchEmbeddings);
                
                log.debug("Batch processed in {}ms, {} embeddings generated", duration, batchEmbeddings.size());
            }
            
            if (allEmbeddings.size() != texts.size()) {
                throw new EmbeddingException(
                    String.format("Expected %d embeddings but got %d", texts.size(), allEmbeddings.size())
                );
            }
            
            log.info("Successfully generated {} embeddings", allEmbeddings.size());
            return allEmbeddings;
            
        } catch (EmbeddingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate batch embeddings from Gemini: {}", e.getMessage(), e);
            throw new EmbeddingException("Failed to generate batch embeddings: " + e.getMessage(), e);
        }
    }
    
    /**
     * Returns the dimensionality of the embeddings produced by this service.
     *
     * @return the embedding dimension (e.g., 768 for Gemini gemini-embedding-001)
     */
    public int getEmbeddingDimension() {
        return embeddingDimension;
    }
    
    /**
     * Checks if the embedding service is available and responsive.
     *
     * @return true if the service is healthy, false otherwise
     */
    public boolean isHealthy() {
        try {
            log.debug("Performing health check on Gemini embedding model");
            float[] testEmbedding = generateEmbedding("test");
            boolean healthy = testEmbedding != null && testEmbedding.length == embeddingDimension;
            log.debug("Gemini embedding health check result: {}", healthy);
            return healthy;
        } catch (Exception e) {
            log.warn("Gemini embedding health check failed: {}", e.getMessage());
            return false;
        }
    }
}

