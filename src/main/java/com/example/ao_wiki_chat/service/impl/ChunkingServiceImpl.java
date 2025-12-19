package com.example.ao_wiki_chat.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.ao_wiki_chat.service.ChunkingService;

/**
 * Implementation of ChunkingService using hybrid semantic + sliding window strategy.
 * 
 * Strategy:
 * 1. Splits text on semantic boundaries (paragraphs, sentences) to preserve meaning
 * 2. Applies sliding window overlap to capture information at chunk boundaries
 * 3. Uses cascading delimiters: paragraphs → sentences → words
 * 
 * Configurable via application.yml:
 * - rag.chunk.size: target chunk size in characters (~500 tokens)
 * - rag.chunk.overlap: overlap size in characters (~50 tokens)
 */
@Service
public class ChunkingServiceImpl implements ChunkingService {
    
    private static final Logger log = LoggerFactory.getLogger(ChunkingServiceImpl.class);
    
    // Minimum chunk size to avoid creating tiny useless chunks
    private static final int MIN_CHUNK_SIZE = 50;
    
    // Sentence delimiters (period, exclamation, question mark followed by space/newline)
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[.!?])\\s+");
    
    @Value("${rag.chunk.size:2000}")
    private int defaultChunkSize;
    
    @Value("${rag.chunk.overlap:200}")
    private int defaultOverlap;
    
    @Override
    public List<String> splitIntoChunks(String text) {
        return splitIntoChunks(text, defaultChunkSize, defaultOverlap);
    }
    
    @Override
    public List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        
        if (!areValidParameters(chunkSize, overlap)) {
            throw new IllegalArgumentException(
                String.format("Invalid parameters: chunkSize=%d, overlap=%d. " +
                    "ChunkSize must be > 0, overlap must be >= 0 and < chunkSize", 
                    chunkSize, overlap)
            );
        }
        
        // Handle empty or very short text
        String normalizedText = preprocessText(text);
        if (normalizedText.isEmpty()) {
            log.debug("Empty text provided, returning empty list");
            return List.of();
        }
        
        if (normalizedText.length() <= chunkSize) {
            log.debug("Text shorter than chunk size, returning single chunk");
            return List.of(normalizedText);
        }
        
        log.debug("Chunking text: {} chars, target size: {}, overlap: {}", 
                  normalizedText.length(), chunkSize, overlap);
        
        // Step 1: Split on semantic boundaries (paragraphs)
        List<String> semanticChunks = splitOnSemanticBoundaries(normalizedText, chunkSize);
        
        // Step 2: Apply overlap between chunks
        List<String> overlappedChunks = applyOverlap(semanticChunks, overlap);
        
        // Step 3: Filter out chunks that are too small
        List<String> finalChunks = filterSmallChunks(overlappedChunks);
        
        log.info("Chunking complete: {} chars → {} chunks (avg: {} chars)", 
                 normalizedText.length(), 
                 finalChunks.size(),
                 finalChunks.isEmpty() ? 0 : normalizedText.length() / finalChunks.size());
        
        return finalChunks;
    }
    
    @Override
    public boolean areValidParameters(int chunkSize, int overlap) {
        return chunkSize > 0 && overlap >= 0 && overlap < chunkSize;
    }
    
    /**
     * Preprocesses text: normalizes whitespace and removes control characters.
     */
    private String preprocessText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Normalize line endings
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        
        // Remove control characters except newlines and tabs
        normalized = normalized.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
        
        // Trim but preserve internal structure
        return normalized.trim();
    }
    
    /**
     * Splits text on semantic boundaries (paragraphs, then sentences if needed).
     * Uses cascading strategy to preserve meaning while respecting chunk size.
     */
    private List<String> splitOnSemanticBoundaries(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        
        // Split on paragraph boundaries (double newline)
        String[] paragraphs = text.split("\n\n+");
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }
            
            // If adding this paragraph exceeds chunk size, save current chunk
            if (currentChunk.length() + paragraph.length() + 2 > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }
                
                // If paragraph itself is too large, split it further
                if (paragraph.length() > chunkSize) {
                    chunks.addAll(splitLargeParagraph(paragraph, chunkSize));
                    currentChunk = new StringBuilder();
                } else {
                    currentChunk = new StringBuilder(paragraph);
                }
            } else {
                // Add paragraph to current chunk
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(paragraph);
            }
        }
        
        // Add final chunk if not empty
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        log.debug("Split into {} semantic chunks", chunks.size());
        return chunks;
    }
    
    /**
     * Splits a large paragraph into smaller chunks on sentence boundaries.
     * Last resort: forces split on word boundaries if sentences are too long.
     */
    private List<String> splitLargeParagraph(String paragraph, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        
        // Try splitting on sentence boundaries
        String[] sentences = SENTENCE_PATTERN.split(paragraph);
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) {
                continue;
            }
            
            // If adding sentence exceeds size, save current chunk
            if (currentChunk.length() + sentence.length() + 1 > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }
                
                // Last resort: force split if single sentence is too large
                if (sentence.length() > chunkSize) {
                    chunks.addAll(forceSplitOnWords(sentence, chunkSize));
                    currentChunk = new StringBuilder();
                } else {
                    currentChunk = new StringBuilder(sentence);
                }
            } else {
                // Add sentence to current chunk
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(sentence);
            }
        }
        
        // Add final chunk if not empty
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    /**
     * Last resort: splits text on word boundaries when semantic splitting fails.
     * This ensures we never create chunks larger than the maximum size.
     */
    private List<String> forceSplitOnWords(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] words = text.split("\\s+");
        
        StringBuilder currentChunk = new StringBuilder();
        
        for (String word : words) {
            // If single word exceeds chunk size, we must split it (rare case)
            if (word.length() > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                // Split the word itself
                for (int i = 0; i < word.length(); i += chunkSize) {
                    chunks.add(word.substring(i, Math.min(i + chunkSize, word.length())));
                }
                continue;
            }
            
            // If adding word exceeds size, save current chunk
            if (currentChunk.length() + word.length() + 1 > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                }
                currentChunk = new StringBuilder(word);
            } else {
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(word);
            }
        }
        
        // Add final chunk if not empty
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }
        
        return chunks;
    }
    
    /**
     * Applies sliding window overlap between consecutive chunks.
     * Takes the last N characters of each chunk and prepends to the next chunk.
     */
    private List<String> applyOverlap(List<String> chunks, int overlap) {
        if (overlap <= 0 || chunks.size() <= 1) {
            return chunks;
        }
        
        List<String> overlappedChunks = new ArrayList<>();
        
        for (int i = 0; i < chunks.size(); i++) {
            String currentChunk = chunks.get(i);
            
            // Add overlap from previous chunk
            if (i > 0) {
                String previousChunk = chunks.get(i - 1);
                String overlapText = getLastNCharacters(previousChunk, overlap);
                
                // Only add overlap if it's meaningful (not just whitespace)
                if (!overlapText.trim().isEmpty()) {
                    currentChunk = overlapText + "\n" + currentChunk;
                }
            }
            
            overlappedChunks.add(currentChunk);
        }
        
        log.debug("Applied overlap of {} chars, {} chunks", overlap, overlappedChunks.size());
        return overlappedChunks;
    }
    
    /**
     * Extracts the last N characters from a string, trying to break at word boundary.
     */
    private String getLastNCharacters(String text, int n) {
        if (text.length() <= n) {
            return text;
        }
        
        String substring = text.substring(text.length() - n);
        
        // Try to find the first space to avoid breaking in the middle of a word
        int firstSpace = substring.indexOf(' ');
        if (firstSpace > 0 && firstSpace < n / 2) {
            // If space is in first half, skip to it to avoid partial word
            return substring.substring(firstSpace + 1);
        }
        
        return substring;
    }
    
    /**
     * Filters out chunks that are too small to be useful.
     */
    private List<String> filterSmallChunks(List<String> chunks) {
        List<String> filtered = chunks.stream()
                .filter(chunk -> chunk.length() >= MIN_CHUNK_SIZE)
                .toList();
        
        if (filtered.size() < chunks.size()) {
            log.debug("Filtered out {} small chunks (< {} chars)", 
                     chunks.size() - filtered.size(), MIN_CHUNK_SIZE);
        }
        
        return filtered;
    }
}

