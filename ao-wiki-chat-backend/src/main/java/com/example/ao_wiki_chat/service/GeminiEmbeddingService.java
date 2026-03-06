package com.example.ao_wiki_chat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * and respect rate limits. Includes retry with exponential backoff for 429 errors.
 */
@Service
public class GeminiEmbeddingService {
    
    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingService.class);
    private static final long MAX_RETRY_DELAY_MS = 90_000L;
    private static final Pattern RETRY_DELAY_PATTERN = Pattern.compile(
            "Please retry in ([\\d.]+)s"
    );
    
    private final EmbeddingModel embeddingModel;
    private final int embeddingDimension;
    private final int batchSize;
    private final long batchDelayMs;
    private final int maxRetries;
    private final long retryBaseDelayMs;
    
    /**
     * Constructs a GeminiEmbeddingService with rate-limiting and retry configuration.
     *
     * @param embeddingModel    the configured Gemini embedding model bean
     * @param embeddingDimension the dimensionality of the embeddings (default: 768)
     * @param batchSize         number of texts per API call (default: 25)
     * @param batchDelayMs      milliseconds to wait between batch calls (default: 15000)
     * @param maxRetries        max retry attempts per batch on transient errors (default: 5)
     * @param retryBaseDelayMs  base delay for exponential backoff in ms (default: 10000)
     */
    public GeminiEmbeddingService(
            @Qualifier("geminiEmbeddingModel") EmbeddingModel embeddingModel,
            @Value("${gemini.embedding.dimension:768}") int embeddingDimension,
            @Value("${gemini.embedding.batch-size:25}") int batchSize,
            @Value("${gemini.embedding.batch-delay-ms:15000}") long batchDelayMs,
            @Value("${gemini.embedding.max-retries:5}") int maxRetries,
            @Value("${gemini.embedding.retry-base-delay-ms:10000}") long retryBaseDelayMs
    ) {
        this.embeddingModel = embeddingModel;
        this.embeddingDimension = embeddingDimension;
        this.batchSize = batchSize;
        this.batchDelayMs = batchDelayMs;
        this.maxRetries = maxRetries;
        this.retryBaseDelayMs = retryBaseDelayMs;
        log.info("GeminiEmbeddingService initialized - dimension: {}, batchSize: {}, "
                + "batchDelayMs: {}, maxRetries: {}, retryBaseDelayMs: {}",
                embeddingDimension, batchSize, batchDelayMs, maxRetries, retryBaseDelayMs);
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
     * Applies inter-batch rate limiting and retry with exponential backoff
     * to handle API quota limits (free tier: 100 requests/minute).
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
        
        for (int i = 0; i < texts.size(); i++) {
            if (texts.get(i) == null || texts.get(i).trim().isEmpty()) {
                throw new EmbeddingException("Text at index " + i + " is null or empty");
            }
        }
        
        int totalBatches = (int) Math.ceil((double) texts.size() / batchSize);
        log.info("Generating embeddings for {} texts in {} batches (batch size: {})",
                texts.size(), totalBatches, batchSize);
        
        try {
            List<float[]> allEmbeddings = new ArrayList<>();
            
            for (int i = 0; i < texts.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, texts.size());
                int currentBatch = (i / batchSize) + 1;
                List<String> batch = texts.subList(i, endIndex);
                
                if (i > 0 && batchDelayMs > 0) {
                    log.debug("Rate limit delay: waiting {}ms before batch {}/{}",
                            batchDelayMs, currentBatch, totalBatches);
                    sleep(batchDelayMs);
                }
                
                List<TextSegment> segments = batch.stream()
                        .map(TextSegment::from)
                        .collect(Collectors.toList());
                
                long startTime = System.currentTimeMillis();
                Response<List<Embedding>> response = embedBatchWithRetry(
                        segments, i, endIndex);
                long duration = System.currentTimeMillis() - startTime;
                
                if (response.content() == null || response.content().isEmpty()) {
                    throw new EmbeddingException("Received empty response from Gemini API for batch");
                }
                
                List<float[]> batchEmbeddings = response.content().stream()
                        .map(Embedding::vector)
                        .collect(Collectors.toList());
                
                allEmbeddings.addAll(batchEmbeddings);
                
                log.debug("Batch {}/{} processed in {}ms ({} embeddings)",
                        currentBatch, totalBatches, duration, batchEmbeddings.size());
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
     * Calls embedAll with retry and exponential backoff.
     * On 429 (rate limit) errors, parses the suggested retry delay from the response
     * when available; otherwise falls back to exponential backoff.
     */
    private Response<List<Embedding>> embedBatchWithRetry(
            List<TextSegment> segments, int batchStart, int batchEnd) {
        
        for (int attempt = 1; ; attempt++) {
            try {
                return embeddingModel.embedAll(segments);
            } catch (Exception e) {
                if (attempt >= maxRetries) {
                    log.error("Batch {}-{} failed after {} attempts: {}",
                            batchStart + 1, batchEnd, maxRetries, e.getMessage());
                    throw e;
                }
                
                long delay = calculateRetryDelay(e, attempt);
                log.warn("Batch {}-{} failed (attempt {}/{}), retrying in {}ms: {}",
                        batchStart + 1, batchEnd, attempt, maxRetries, delay,
                        summarizeError(e));
                
                sleep(delay);
            }
        }
    }
    
    /**
     * Determines retry delay: uses the server-suggested delay from 429 responses
     * when parseable, otherwise applies exponential backoff (base * 2^(attempt-1)).
     */
    long calculateRetryDelay(Exception e, int attempt) {
        long parsedDelay = parseRetryDelayFromError(e);
        if (parsedDelay > 0) {
            return Math.min(parsedDelay, MAX_RETRY_DELAY_MS);
        }
        long delay = retryBaseDelayMs * (1L << (attempt - 1));
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }
    
    /**
     * Attempts to extract the retry delay from Google API 429 error messages.
     * Looks for the pattern "Please retry in Xs" in the exception message.
     *
     * @return parsed delay in milliseconds with 1s buffer, or -1 if not parseable
     */
    long parseRetryDelayFromError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return -1;
        }
        
        Matcher matcher = RETRY_DELAY_PATTERN.matcher(message);
        if (matcher.find()) {
            try {
                double seconds = Double.parseDouble(matcher.group(1));
                return (long) (seconds * 1000) + 1000;
            } catch (NumberFormatException nfe) {
                log.debug("Could not parse retry delay number: {}", matcher.group(1));
            }
        }
        return -1;
    }
    
    /**
     * Extracts a short error summary for log messages (first line only).
     */
    private String summarizeError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "unknown error";
        int newline = msg.indexOf('\n');
        return newline > 0 ? msg.substring(0, newline) : msg;
    }
    
    /**
     * Sleeps for the specified duration. Package-private for test overriding.
     */
    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingException("Embedding processing interrupted during rate limit delay", e);
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

