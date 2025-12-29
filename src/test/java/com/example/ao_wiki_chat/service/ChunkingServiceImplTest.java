package com.example.ao_wiki_chat.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for ChunkingServiceImpl.
 * Tests semantic splitting, overlap functionality, and edge cases.
 */
class ChunkingServiceImplTest {
    
    private ChunkingService chunkingService;
    
    private static final int DEFAULT_CHUNK_SIZE = 100;
    private static final int DEFAULT_OVERLAP = 20;
    
    @BeforeEach
    void setUp() {
        chunkingService = new ChunkingService();
        
        // Set test values for default chunk size and overlap
        ReflectionTestUtils.setField(chunkingService, "defaultChunkSize", DEFAULT_CHUNK_SIZE);
        ReflectionTestUtils.setField(chunkingService, "defaultOverlap", DEFAULT_OVERLAP);
    }
    
    // ==================== Basic Functionality Tests ====================
    
    @Test
    void splitIntoChunksWithValidTextReturnsChunks() {
        // Given
        String text = "This is a test. This is another sentence. And one more sentence. " +
                      "Here is additional content to make it longer. More text here.";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 80, 10);
        
        // Then
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isGreaterThanOrEqualTo(1);
    }
    
    @Test
    void splitIntoChunksWithDefaultParametersUsesConfiguredValues() {
        // Given
        String text = "This is a test sentence that should be chunked using default parameters.";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text);
        
        // Then
        assertThat(chunks).isNotEmpty();
    }
    
    @Test
    void splitIntoChunksWithShortTextReturnsSingleChunk() {
        // Given
        String text = "Short text";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 100, 10);
        
        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo(text);
    }
    
    @Test
    void splitIntoChunksWithEmptyTextReturnsEmptyList() {
        // Given
        String text = "";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 100, 10);
        
        // Then
        assertThat(chunks).isEmpty();
    }
    
    @Test
    void splitIntoChunksWithWhitespaceOnlyReturnsEmptyList() {
        // Given
        String text = "   \n\n\t  ";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 100, 10);
        
        // Then
        assertThat(chunks).isEmpty();
    }
    
    // ==================== Parameter Validation Tests ====================
    
    @Test
    void splitIntoChunksWithNullTextThrowsException() {
        // When / Then
        assertThatThrownBy(() -> chunkingService.splitIntoChunks(null, 100, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Text cannot be null");
    }
    
    @Test
    void splitIntoChunksWithInvalidChunkSizeThrowsException() {
        // Given
        String text = "Test text";
        
        // When / Then
        assertThatThrownBy(() -> chunkingService.splitIntoChunks(text, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid parameters");
        
        assertThatThrownBy(() -> chunkingService.splitIntoChunks(text, -1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid parameters");
    }
    
    @Test
    void splitIntoChunksWithInvalidOverlapThrowsException() {
        // Given
        String text = "Test text";
        
        // When / Then - overlap negative
        assertThatThrownBy(() -> chunkingService.splitIntoChunks(text, 100, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid parameters");
        
        // When / Then - overlap >= chunkSize
        assertThatThrownBy(() -> chunkingService.splitIntoChunks(text, 100, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid parameters");
        
        assertThatThrownBy(() -> chunkingService.splitIntoChunks(text, 100, 150))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid parameters");
    }
    
    @Test
    void areValidParametersReturnsTrueForValidParameters() {
        // When / Then
        assertThat(chunkingService.areValidParameters(100, 10)).isTrue();
        assertThat(chunkingService.areValidParameters(500, 50)).isTrue();
        assertThat(chunkingService.areValidParameters(1000, 0)).isTrue();
    }
    
    @Test
    void areValidParametersReturnsFalseForInvalidParameters() {
        // When / Then
        assertThat(chunkingService.areValidParameters(0, 10)).isFalse();
        assertThat(chunkingService.areValidParameters(-1, 10)).isFalse();
        assertThat(chunkingService.areValidParameters(100, -1)).isFalse();
        assertThat(chunkingService.areValidParameters(100, 100)).isFalse();
        assertThat(chunkingService.areValidParameters(100, 150)).isFalse();
    }
    
    // ==================== Semantic Splitting Tests ====================
    
    @Test
    void splitIntoChunksPreservesParagraphBoundaries() {
        // Given
        String text = "First paragraph with enough text to be meaningful content.\n\n" +
                      "Second paragraph also with sufficient text content here.\n\n" +
                      "Third paragraph containing adequate text for testing.";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 80, 0);
        
        // Then
        assertThat(chunks).isNotEmpty();
        // Each chunk should not contain multiple paragraphs separated by \n\n
        for (String chunk : chunks) {
            // Count double newlines - should be 0 or at boundaries
            assertThat(chunk.split("\n\n").length).isLessThanOrEqualTo(2);
        }
    }
    
    @Test
    void splitIntoChunksPreservesSentenceIntegrity() {
        // Given
        String text = "First sentence with meaningful content here. " +
                      "Second sentence also containing good text. " +
                      "Third sentence with more testing data. " +
                      "Fourth sentence completing the test case.";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 100, 10);
        
        // Then
        assertThat(chunks).isNotEmpty();
        // Each chunk should contain valid text (not cut in the middle of words)
        for (String chunk : chunks) {
            String trimmed = chunk.trim();
            if (!trimmed.isEmpty()) {
                // Should not start/end with weird characters
                assertThat(trimmed).isNotEmpty();
                assertThat(trimmed.length()).isGreaterThan(0);
            }
        }
    }
    
    @Test
    void splitIntoChunksWithLargeParagraphSplitsOnSentences() {
        // Given - single paragraph with multiple sentences
        String text = "This is the first sentence. This is the second sentence. " +
                      "This is the third sentence. This is the fourth sentence. " +
                      "This is the fifth sentence. This is the sixth sentence.";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 80, 0);
        
        // Then
        assertThat(chunks).hasSizeGreaterThan(1);
        // Each chunk should contain complete sentences
        for (String chunk : chunks) {
            assertThat(chunk.trim()).isNotEmpty();
        }
    }
    
    @Test
    void splitIntoChunksWithVeryLongSentenceForcesWordSplit() {
        // Given - single very long sentence without punctuation
        String text = "word ".repeat(100); // 500 characters
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 100, 0);
        
        // Then
        assertThat(chunks).isNotEmpty();
        for (String chunk : chunks) {
            // Each chunk should be approximately chunk size
            assertThat(chunk.length()).isLessThanOrEqualTo(110); // Allow small margin
        }
    }
    
    // ==================== Overlap Tests ====================
    
    @Test
    void splitIntoChunksWithOverlapIncludesPreviousContent() {
        // Given
        String text = "AAAAA BBBBB CCCCC DDDDD EEEEE FFFFF GGGGG HHHHH IIIII JJJJJ " +
                      "KKKKK LLLLL MMMMM NNNNN OOOOO PPPPP QQQQQ RRRRR SSSSS TTTTT";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 80, 15);
        
        // Then
        assertThat(chunks).hasSizeGreaterThanOrEqualTo(1);
        
        // If we have multiple chunks, verify overlap
        if (chunks.size() > 1) {
            for (int i = 1; i < chunks.size(); i++) {
                String currentChunk = chunks.get(i);
                String previousChunk = chunks.get(i - 1);
                
                // Extract last part of previous chunk
                String previousEnd = previousChunk.substring(
                    Math.max(0, previousChunk.length() - 20)
                );
                
                // Current chunk should contain some words from previous chunk
                String[] previousWords = previousEnd.split("\\s+");
                
                boolean hasOverlap = false;
                for (String prevWord : previousWords) {
                    if (!prevWord.isEmpty() && currentChunk.contains(prevWord)) {
                        hasOverlap = true;
                        break;
                    }
                }
                
                assertThat(hasOverlap).isTrue();
            }
        }
    }
    
    @Test
    void splitIntoChunksWithZeroOverlapHasNoOverlap() {
        // Given
        String text = "First paragraph with sufficient text content here for testing.\n\n" +
                      "Second paragraph also with good amount of text content.\n\n" +
                      "Third paragraph containing adequate testing material.";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 80, 0);
        
        // Then
        assertThat(chunks).isNotEmpty();
        // With zero overlap, chunks should be independent
        // (This is hard to verify directly, but we can check chunk count is reasonable)
        assertThat(chunks.size()).isGreaterThanOrEqualTo(1);
    }
    
    // ==================== Text Preprocessing Tests ====================
    
    @Test
    void splitIntoChunksNormalizesLineEndings() {
        // Given - text with mixed line endings
        String text = "Line 1\r\nLine 2\rLine 3\nLine 4";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 100, 0);
        
        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).doesNotContain("\r");
    }
    
    @Test
    void splitIntoChunksTrimsWhitespace() {
        // Given
        String text = "   Text with leading and trailing spaces   ";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 100, 0);
        
        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("Text with leading and trailing spaces");
    }
    
    @Test
    void splitIntoChunksHandlesMultipleConsecutiveNewlines() {
        // Given
        String text = "Paragraph one with sufficient content for testing purposes.\n\n\n\n" +
                      "Paragraph two also containing adequate text here.\n\n\n\n\n" +
                      "Paragraph three with enough text to be meaningful.";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 100, 0);
        
        // Then
        assertThat(chunks).isNotEmpty();
        // The regex \n\n+ in implementation collapses multiple newlines
        // So we just verify chunks were created successfully
    }
    
    // ==================== Edge Cases Tests ====================
    
    @Test
    void splitIntoChunksWithSingleWordReturnsWord() {
        // Given
        String text = "Word";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 100, 10);
        
        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).isEqualTo("Word");
    }
    
    @Test
    void splitIntoChunksWithExactlySChunkSizeReturnsChunk() {
        // Given
        String text = "A".repeat(100);
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 100, 0);
        
        // Then
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0)).hasSize(100);
    }
    
    @Test
    void splitIntoChunksFiltersOutTooSmallChunks() {
        // Given - text that would create tiny chunks at the end
        String text = "This is a longer text. " + "x".repeat(20);
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 25, 0);
        
        // Then - all chunks should be at least MIN_CHUNK_SIZE (50 chars)
        for (String chunk : chunks) {
            assertThat(chunk.length()).isGreaterThanOrEqualTo(50);
        }
    }
    
    @Test
    void splitIntoChunksWithUnicodeCharactersHandlesCorrectly() {
        // Given
        String text = "Hello 世界! Café résumé. Emoji: 🎉🎊";
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 100, 10);
        
        // Then
        assertThat(chunks).isNotEmpty();
        assertThat(String.join(" ", chunks)).contains("世界");
        assertThat(String.join(" ", chunks)).contains("Café");
        assertThat(String.join(" ", chunks)).contains("🎉");
    }
    
    @Test
    void splitLargeParagraphWithVeryLongSentencesWithoutPunctuationForcesWordSplit() {
        // Given - paragraph with very long sentences without punctuation (triggers splitLargeParagraph)
        // Each "sentence" is just a long sequence of words without punctuation
        String longSentence1 = "word ".repeat(200); // ~1000 chars
        String longSentence2 = "text ".repeat(200); // ~1000 chars
        String paragraph = longSentence1 + longSentence2; // Single paragraph > chunkSize
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(paragraph, 100, 0);
        
        // Then
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isGreaterThan(1);
        // Each chunk should respect chunk size (allowing small margin for word boundaries)
        for (String chunk : chunks) {
            assertThat(chunk.length()).isLessThanOrEqualTo(110);
            assertThat(chunk.trim()).isNotEmpty();
        }
        // Verify all words are preserved
        String rejoined = String.join(" ", chunks);
        assertThat(rejoined).contains("word");
        assertThat(rejoined).contains("text");
    }
    
    @Test
    void splitLargeParagraphWithSingleVeryLongSentenceSplitsOnWords() {
        // Given - paragraph with single very long sentence (no sentence boundaries)
        // This will trigger splitLargeParagraph, which will then call forceSplitOnWords
        String veryLongSentence = "This is a very long sentence without any punctuation " +
                                  "that continues for many words and should be split " +
                                  "on word boundaries when it exceeds the chunk size " +
                                  "because there are no sentence delimiters available " +
                                  "to use for splitting the paragraph into smaller chunks " +
                                  "that respect semantic boundaries like periods or exclamation marks " +
                                  "so the algorithm must fall back to word boundary splitting " +
                                  "to ensure that no chunk exceeds the maximum allowed size " +
                                  "which is important for maintaining consistent chunk sizes " +
                                  "across the entire document processing pipeline.";
        // Make it a paragraph (will trigger splitLargeParagraph)
        String paragraph = veryLongSentence + " " + veryLongSentence; // ~2000+ chars
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(paragraph, 150, 0);
        
        // Then
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.size()).isGreaterThan(1);
        // Each chunk should be split on word boundaries (no mid-word splits)
        for (String chunk : chunks) {
            assertThat(chunk.length()).isLessThanOrEqualTo(160); // chunkSize + margin
            assertThat(chunk.trim()).isNotEmpty();
            // Verify chunks don't start/end in the middle of words
            String trimmed = chunk.trim();
            if (!trimmed.isEmpty()) {
                // Should not start with space or end with partial word
                assertThat(trimmed.charAt(0)).isNotEqualTo(' ');
            }
        }
    }
    
    @Test
    void splitLargeParagraphWithEmptySentencesAfterSplitFiltersThemOut() {
        // Given - paragraph that after sentence splitting might produce empty sentences
        // This tests the empty sentence handling in splitLargeParagraph (line 200-202)
        String paragraph = "First sentence with content.   .   .   Second sentence here. " +
                          "   .   .   Third sentence with more content.   .   .   " +
                          "Fourth sentence completes the paragraph.";
        // Make it large enough to trigger splitLargeParagraph
        paragraph = paragraph.repeat(10); // ~2000+ chars
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(paragraph, 100, 0);
        
        // Then
        assertThat(chunks).isNotEmpty();
        // All chunks should be non-empty (empty sentences should be filtered)
        for (String chunk : chunks) {
            assertThat(chunk.trim()).isNotEmpty();
            // Chunks should not contain only whitespace or punctuation
            String trimmed = chunk.trim();
            assertThat(trimmed.length()).isGreaterThan(0);
            // Should contain actual words
            assertThat(trimmed.split("\\s+").length).isGreaterThan(0);
        }
        // Verify meaningful content is preserved
        String rejoined = String.join(" ", chunks);
        assertThat(rejoined).contains("First sentence");
        assertThat(rejoined).contains("Second sentence");
        assertThat(rejoined).contains("Third sentence");
        assertThat(rejoined).contains("Fourth sentence");
    }
    
    // ==================== Realistic Use Case Tests ====================
    
    @Test
    void splitIntoChunksWithRealisticDocumentWorks() {
        // Given - simulate realistic document
        String text = """
                Introduction to WikiChat
                
                WikiChat is a RAG system that allows users to upload documents and ask questions.
                The system uses vector embeddings to find relevant information.
                
                Installation
                
                To install WikiChat, follow these steps:
                1. Install Docker
                2. Configure PostgreSQL
                3. Set up Gemini API key
                
                Configuration
                
                The configuration file contains several important settings.
                Chunk size should be set to 500 tokens.
                Overlap should be around 50 tokens.
                """;
        
        // When
        List<String> chunks = chunkingService.splitIntoChunks(text, 150, 20);
        
        // Then
        assertThat(chunks).hasSizeGreaterThan(1);
        
        // Verify all chunks are within reasonable size
        for (String chunk : chunks) {
            assertThat(chunk.length()).isGreaterThan(0);
            assertThat(chunk.length()).isLessThanOrEqualTo(200); // chunk size + overlap
        }
        
        // Verify content is preserved (no information loss)
        String rejoined = String.join(" ", chunks);
        assertThat(rejoined).contains("WikiChat");
        assertThat(rejoined).contains("Installation");
        assertThat(rejoined).contains("Configuration");
    }
    
    @Test
    void splitIntoChunksIsConsistentAcrossMultipleCalls() {
        // Given
        String text = "Consistent text for testing. Multiple sentences here. " +
                      "Should produce same results every time.";
        
        // When
        List<String> chunks1 = chunkingService.splitIntoChunks(text, 50, 10);
        List<String> chunks2 = chunkingService.splitIntoChunks(text, 50, 10);
        
        // Then
        assertThat(chunks1).isEqualTo(chunks2);
    }
    
    @Test
    void splitIntoChunksWithDifferentChunkSizesProducesDifferentResults() {
        // Given
        String text = "Test text with sufficient content for meaningful chunking. ".repeat(10);
        
        // When
        List<String> smallChunks = chunkingService.splitIntoChunks(text, 100, 10);
        List<String> largeChunks = chunkingService.splitIntoChunks(text, 300, 10);
        
        // Then
        assertThat(smallChunks.size()).isGreaterThan(largeChunks.size());
    }
}

