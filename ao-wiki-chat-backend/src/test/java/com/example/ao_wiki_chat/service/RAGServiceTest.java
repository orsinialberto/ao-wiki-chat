package com.example.ao_wiki_chat.service;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ao_wiki_chat.exception.EmbeddingException;
import com.example.ao_wiki_chat.exception.LLMException;
import com.example.ao_wiki_chat.exception.VectorSearchException;
import com.example.ao_wiki_chat.model.dto.ChatRequest;
import com.example.ao_wiki_chat.model.dto.ChatResponse;
import com.example.ao_wiki_chat.model.dto.SourceReference;
import com.example.ao_wiki_chat.model.entity.Chunk;
import com.example.ao_wiki_chat.model.entity.Conversation;
import com.example.ao_wiki_chat.model.entity.Document;
import com.example.ao_wiki_chat.model.enums.DocumentStatus;
import com.example.ao_wiki_chat.model.enums.MessageRole;
import com.example.ao_wiki_chat.repository.ConversationRepository;
import com.example.ao_wiki_chat.repository.MessageRepository;

/**
 * Unit tests for RAGService.
 * Tests the complete RAG pipeline orchestration: embedding generation, vector search,
 * context building, prompt generation, LLM call, and conversation persistence.
 */
@ExtendWith(MockitoExtension.class)
class RAGServiceTest {
    
    @Mock
    private GeminiEmbeddingService embeddingService;
    
    @Mock
    private VectorSearchService vectorSearchService;
    
    @Mock
    private LLMService llmService;
    
    @Mock
    private ConversationRepository conversationRepository;
    
    @Mock
    private MessageRepository messageRepository;
    
    private RAGService ragService;
    
    private static final int EMBEDDING_DIMENSION = 768;
    private static final String TEST_SESSION_ID = "test-session-123";
    private static final String TEST_QUERY = "What is machine learning?";
    
    private float[] testQueryEmbedding;
    private Document testDocument;
    private Chunk testChunk1;
    private Chunk testChunk2;
    private Conversation testConversation;
    
    @BeforeEach
    void setUp() {
        ragService = new RAGService(
            embeddingService,
            vectorSearchService,
            llmService,
            conversationRepository,
            messageRepository,
            10, // maxHistoryMessages
            true // includeHistory
        );
        
        // Create test embedding
        testQueryEmbedding = createTestVector(EMBEDDING_DIMENSION, 0.5f);
        
        // Create test document
        UUID documentId = UUID.randomUUID();
        testDocument = Document.builder()
                .id(documentId)
                .filename("test-document.md")
                .contentType("text/markdown")
                .fileSize(2048L)
                .status(DocumentStatus.COMPLETED)
                .metadata(null)
                .build();
        
        // Create test chunks
        testChunk1 = Chunk.builder()
                .id(UUID.randomUUID())
                .document(testDocument)
                .content("Machine learning is a subset of artificial intelligence.")
                .chunkIndex(0)
                .embedding(createTestVector(EMBEDDING_DIMENSION, 0.3f))
                .metadata(null)
                .build();
        
        testChunk2 = Chunk.builder()
                .id(UUID.randomUUID())
                .document(testDocument)
                .content("It enables systems to learn from data without explicit programming.")
                .chunkIndex(1)
                .embedding(createTestVector(EMBEDDING_DIMENSION, 0.4f))
                .metadata(null)
                .build();
        
        // Create test conversation
        testConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .sessionId(TEST_SESSION_ID)
                .title(null)
                .metadata(null)
                .build();
    }
    
    @Test
    void processQueryWhenValidRequestReturnsChatResponse() {
        // Given
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1, testChunk2);
        String expectedAnswer = "Machine learning is a subset of AI that enables systems to learn from data.";
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString())).thenReturn(expectedAnswer);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(Collections.emptyList());
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ChatResponse response = ragService.processQuery(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo(expectedAnswer);
        assertThat(response.sources()).hasSize(2);
        assertThat(response.sources().get(0).documentName()).isEqualTo("test-document.md");
        assertThat(response.sources().get(0).chunkContent()).contains("Machine learning");
        
        // Verify interactions
        verify(embeddingService).generateEmbedding(TEST_QUERY);
        verify(vectorSearchService).findSimilarChunks(testQueryEmbedding);
        verify(llmService).generate(anyString());
        verify(conversationRepository).findBySessionId(TEST_SESSION_ID);
        verify(messageRepository).findByConversation_IdOrderByCreatedAtAsc(testConversation.getId());
        verify(messageRepository, times(2)).save(any(com.example.ao_wiki_chat.model.entity.Message.class));
    }
    
    @Test
    void processQueryWhenNoRelevantChunksReturnsEmptyContextResponse() {
        // Given
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> emptyChunks = Collections.emptyList();
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(emptyChunks);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(Collections.emptyList());
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ChatResponse response = ragService.processQuery(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.answer()).contains("couldn't find any relevant information");
        assertThat(response.sources()).isEmpty();
        
        // Verify LLM was not called
        verify(llmService, never()).generate(anyString());
        verify(messageRepository).findByConversation_IdOrderByCreatedAtAsc(testConversation.getId());
        verify(messageRepository, times(2)).save(any(com.example.ao_wiki_chat.model.entity.Message.class));
    }
    
    @Test
    void processQueryWhenNewSessionCreatesNewConversation() {
        // Given
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1);
        String expectedAnswer = "Test answer";
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString())).thenReturn(expectedAnswer);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(testConversation);
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(Collections.emptyList());
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ChatResponse response = ragService.processQuery(request);
        
        // Then
        assertThat(response).isNotNull();
        verify(conversationRepository).findBySessionId(TEST_SESSION_ID);
        verify(conversationRepository).save(any(Conversation.class));
        verify(messageRepository).findByConversation_IdOrderByCreatedAtAsc(testConversation.getId());
    }
    
    @Test
    void processQueryWhenNullRequestThrowsException() {
        // When/Then
        assertThatThrownBy(() -> ragService.processQuery(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }
    
    @Test
    void processQueryWhenEmptyQueryThrowsException() {
        // Given
        ChatRequest request = new ChatRequest("", TEST_SESSION_ID);
        
        // When/Then
        assertThatThrownBy(() -> ragService.processQuery(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }
    
    @Test
    void processQueryWhenEmptySessionIdThrowsException() {
        // Given
        ChatRequest request = new ChatRequest(TEST_QUERY, "");
        
        // When/Then
        assertThatThrownBy(() -> ragService.processQuery(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null or empty");
    }
    
    @Test
    void processQueryWhenEmbeddingFailsThrowsException() {
        // Given
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(Collections.emptyList());
        when(embeddingService.generateEmbedding(TEST_QUERY))
                .thenThrow(new EmbeddingException("Embedding generation failed"));
        
        // When/Then
        assertThatThrownBy(() -> ragService.processQuery(request))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("Embedding generation failed");
        
        verify(vectorSearchService, never()).findSimilarChunks(any());
        verify(llmService, never()).generate(anyString());
    }
    
    @Test
    void processQueryWhenVectorSearchFailsThrowsException() {
        // Given
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(Collections.emptyList());
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding))
                .thenThrow(new VectorSearchException("Vector search failed"));
        
        // When/Then
        assertThatThrownBy(() -> ragService.processQuery(request))
                .isInstanceOf(VectorSearchException.class)
                .hasMessageContaining("Vector search failed");
        
        verify(llmService, never()).generate(anyString());
    }
    
    @Test
    void processQueryWhenLLMFailsThrowsException() {
        // Given
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1);
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(Collections.emptyList());
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString()))
                .thenThrow(new LLMException("LLM generation failed"));
        
        // When/Then
        assertThatThrownBy(() -> ragService.processQuery(request))
                .isInstanceOf(LLMException.class)
                .hasMessageContaining("LLM generation failed");
    }
    
    @Test
    void processQueryWhenMultipleChunksBuildsCorrectContext() {
        // Given
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1, testChunk2);
        String expectedAnswer = "Test answer";
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString())).thenReturn(expectedAnswer);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(Collections.emptyList());
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ChatResponse response = ragService.processQuery(request);
        
        // Then
        assertThat(response.sources()).hasSize(2);
        assertThat(response.sources().get(0).chunkIndex()).isEqualTo(0);
        assertThat(response.sources().get(1).chunkIndex()).isEqualTo(1);
        
        // Verify prompt contains both chunks
        verify(llmService).generate(org.mockito.ArgumentMatchers.argThat(prompt -> 
            prompt.contains(testChunk1.getContent()) && 
            prompt.contains(testChunk2.getContent())
        ));
    }
    
    @Test
    void processQueryWhenValidRequestSavesUserAndAssistantMessages() {
        // Given
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1);
        String expectedAnswer = "Test answer";
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString())).thenReturn(expectedAnswer);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(Collections.emptyList());
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ragService.processQuery(request);
        
        // Then
        verify(messageRepository).findByConversation_IdOrderByCreatedAtAsc(testConversation.getId());
        verify(messageRepository, times(2)).save(any(com.example.ao_wiki_chat.model.entity.Message.class));
        
        // Verify user message
        verify(messageRepository).save(org.mockito.ArgumentMatchers.argThat(message -> {
            com.example.ao_wiki_chat.model.entity.Message msg = 
                    (com.example.ao_wiki_chat.model.entity.Message) message;
            return msg.getRole() == MessageRole.USER && 
                   msg.getContent().equals(TEST_QUERY);
        }));
        
        // Verify assistant message
        verify(messageRepository).save(org.mockito.ArgumentMatchers.argThat(message -> {
            com.example.ao_wiki_chat.model.entity.Message msg = 
                    (com.example.ao_wiki_chat.model.entity.Message) message;
            return msg.getRole() == MessageRole.ASSISTANT && 
                   msg.getContent().equals(expectedAnswer) &&
                   msg.getSources() != null;
        }));
    }
    
    @Test
    void processQueryWhenPreviousMessagesExistRetrievesConversationHistory() {
        // Given
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1);
        String expectedAnswer = "Test answer";
        
        // Create previous messages
        com.example.ao_wiki_chat.model.entity.Message previousUserMessage = 
                com.example.ao_wiki_chat.model.entity.Message.builder()
                .id(UUID.randomUUID())
                .conversation(testConversation)
                .role(MessageRole.USER)
                .content("Previous question")
                .build();
        
        com.example.ao_wiki_chat.model.entity.Message previousAssistantMessage = 
                com.example.ao_wiki_chat.model.entity.Message.builder()
                .id(UUID.randomUUID())
                .conversation(testConversation)
                .role(MessageRole.ASSISTANT)
                .content("Previous answer")
                .build();
        
        List<com.example.ao_wiki_chat.model.entity.Message> previousMessages = 
                Arrays.asList(previousUserMessage, previousAssistantMessage);
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString())).thenReturn(expectedAnswer);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(previousMessages);
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ChatResponse response = ragService.processQuery(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo(expectedAnswer);
        
        // Verify previous messages were retrieved
        verify(messageRepository).findByConversation_IdOrderByCreatedAtAsc(testConversation.getId());
        verify(messageRepository, times(2)).save(any(com.example.ao_wiki_chat.model.entity.Message.class));
    }
    
    @Test
    void processQueryWhenNoPreviousMessagesHandlesEmptyList() {
        // Given
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1);
        String expectedAnswer = "Test answer";
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString())).thenReturn(expectedAnswer);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(Collections.emptyList());
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        ChatResponse response = ragService.processQuery(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo(expectedAnswer);
        
        // Verify empty list was handled correctly (no previous messages - first query)
        verify(messageRepository).findByConversation_IdOrderByCreatedAtAsc(testConversation.getId());
        verify(messageRepository, times(2)).save(any(com.example.ao_wiki_chat.model.entity.Message.class));
    }
    
    @Test
    void serializeSourcesWhenNullListReturnsNull() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("serializeSources", List.class);
        method.setAccessible(true);
        
        // When
        String result = (String) method.invoke(ragService, (List<SourceReference>) null);
        
        // Then
        assertThat(result).isNull();
    }
    
    @Test
    void serializeSourcesWhenEmptyListReturnsNull() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("serializeSources", List.class);
        method.setAccessible(true);
        List<SourceReference> emptyList = Collections.emptyList();
        
        // When
        String result = (String) method.invoke(ragService, emptyList);
        
        // Then
        assertThat(result).isNull();
    }
    
    @Test
    void serializeSourcesWhenValidSourcesReturnsCorrectJson() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("serializeSources", List.class);
        method.setAccessible(true);
        List<SourceReference> sources = Arrays.asList(
            new SourceReference("doc1.md", "Content 1", 0.85, 0),
            new SourceReference("doc2.md", "Content 2", 0.90, 1)
        );
        
        // When
        String result = (String) method.invoke(ragService, sources);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
        assertThat(result).contains("\"documentName\":\"doc1.md\"");
        assertThat(result).contains("\"documentName\":\"doc2.md\"");
        assertThat(result).contains("\"chunkContent\":\"Content 1\"");
        assertThat(result).contains("\"chunkContent\":\"Content 2\"");
        assertThat(result).contains("\"similarityScore\":0.85");
        assertThat(result).contains("\"similarityScore\":0.9");
        assertThat(result).contains("\"chunkIndex\":0");
        assertThat(result).contains("\"chunkIndex\":1");
    }
    
    @Test
    void serializeSourcesWhenSourceReferenceHasNullValuesHandlesCorrectly() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("serializeSources", List.class);
        method.setAccessible(true);
        List<SourceReference> sources = Arrays.asList(
            new SourceReference(null, null, null, null)
        );
        
        // When
        String result = (String) method.invoke(ragService, sources);
        
        // Then - null values should be serialized as "null" strings
        assertThat(result).isNotNull();
        assertThat(result).contains("\"documentName\":null");
        assertThat(result).contains("\"chunkContent\":null");
        assertThat(result).contains("\"similarityScore\":null");
        assertThat(result).contains("\"chunkIndex\":null");
    }
    
    @Test
    void escapeJsonWhenNullStringReturnsNullString() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("escapeJson", String.class);
        method.setAccessible(true);
        
        // When
        String result = (String) method.invoke(ragService, (String) null);
        
        // Then
        assertThat(result).isEqualTo("null");
    }
    
    @Test
    void escapeJsonWhenEmptyStringReturnsQuotedEmptyString() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("escapeJson", String.class);
        method.setAccessible(true);
        
        // When
        String result = (String) method.invoke(ragService, "");
        
        // Then
        assertThat(result).isEqualTo("\"\"");
    }
    
    @Test
    void escapeJsonWhenContainsBackslashEscapesCorrectly() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("escapeJson", String.class);
        method.setAccessible(true);
        String input = "path\\to\\file";
        
        // When
        String result = (String) method.invoke(ragService, input);
        
        // Then
        assertThat(result).isEqualTo("\"path\\\\to\\\\file\"");
    }
    
    @Test
    void escapeJsonWhenContainsDoubleQuoteEscapesCorrectly() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("escapeJson", String.class);
        method.setAccessible(true);
        String input = "He said \"Hello\"";
        
        // When
        String result = (String) method.invoke(ragService, input);
        
        // Then
        assertThat(result).isEqualTo("\"He said \\\"Hello\\\"\"");
    }
    
    @Test
    void escapeJsonWhenContainsNewlineEscapesCorrectly() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("escapeJson", String.class);
        method.setAccessible(true);
        String input = "Line 1\nLine 2";
        
        // When
        String result = (String) method.invoke(ragService, input);
        
        // Then
        assertThat(result).isEqualTo("\"Line 1\\nLine 2\"");
    }
    
    @Test
    void escapeJsonWhenContainsCarriageReturnEscapesCorrectly() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("escapeJson", String.class);
        method.setAccessible(true);
        String input = "Line 1\rLine 2";
        
        // When
        String result = (String) method.invoke(ragService, input);
        
        // Then
        assertThat(result).isEqualTo("\"Line 1\\rLine 2\"");
    }
    
    @Test
    void escapeJsonWhenContainsTabEscapesCorrectly() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("escapeJson", String.class);
        method.setAccessible(true);
        String input = "Column1\tColumn2";
        
        // When
        String result = (String) method.invoke(ragService, input);
        
        // Then
        assertThat(result).isEqualTo("\"Column1\\tColumn2\"");
    }
    
    @Test
    void escapeJsonWhenContainsAllSpecialCharactersEscapesCorrectly() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("escapeJson", String.class);
        method.setAccessible(true);
        String input = "Text with \"quotes\", \\backslashes\\, \nnewlines, \rreturns, \ttabs";
        
        // When
        String result = (String) method.invoke(ragService, input);
        
        // Then
        assertThat(result).isEqualTo("\"Text with \\\"quotes\\\", \\\\backslashes\\\\, \\nnewlines, \\rreturns, \\ttabs\"");
    }
    
    @Test
    void serializeSourcesWhenContainsSpecialCharactersProducesValidJson() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("serializeSources", List.class);
        method.setAccessible(true);
        List<SourceReference> sources = Arrays.asList(
            new SourceReference("file\"with\"quotes.md", "Content with\nnewline and\ttab", 0.85, 0)
        );
        
        // When
        String result = (String) method.invoke(ragService, sources);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("\\\"");
        assertThat(result).contains("\\n");
        assertThat(result).contains("\\t");
        // Verify it's valid JSON structure
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
        assertThat(result).contains("\"documentName\":");
        assertThat(result).contains("\"chunkContent\":");
    }
    
    @Test
    void serializeSourcesWhenMultipleSourcesWithSpecialCharactersProducesValidJson() throws Exception {
        // Given
        Method method = RAGService.class.getDeclaredMethod("serializeSources", List.class);
        method.setAccessible(true);
        List<SourceReference> sources = Arrays.asList(
            new SourceReference("doc\\1.md", "Content\\with\\backslashes", 0.85, 0),
            new SourceReference("doc\"2\".md", "Content\nwith\nnewlines", 0.90, 1),
            new SourceReference("doc\t3.md", "Content\twith\ttabs", 0.75, 2)
        );
        
        // When
        String result = (String) method.invoke(ragService, sources);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
        // Verify all three sources are present
        assertThat(result).contains("\"chunkIndex\":0");
        assertThat(result).contains("\"chunkIndex\":1");
        assertThat(result).contains("\"chunkIndex\":2");
        // Verify special characters are escaped
        assertThat(result).contains("\\\\"); // escaped backslashes
        assertThat(result).contains("\\\""); // escaped quotes
        assertThat(result).contains("\\n"); // escaped newlines
        assertThat(result).contains("\\t"); // escaped tabs
    }
    
    // ========== Conversational Context Tests ==========
    
    @Test
    void processQueryWhenFirstQueryWithoutContextDoesNotIncludePreviousConversation() {
        // Given - first query, no previous messages
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1);
        String expectedAnswer = "Test answer";
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString())).thenReturn(expectedAnswer);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(Collections.emptyList());
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        
        // When
        ChatResponse response = ragService.processQuery(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo(expectedAnswer);
        
        // Verify prompt does not contain "Previous conversation:"
        verify(llmService).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).doesNotContain("Previous conversation:");
        assertThat(prompt).contains("Question: " + TEST_QUERY);
        assertThat(prompt).contains(testChunk1.getContent());
    }
    
    @Test
    void processQueryWhenPreviousMessagesExistIncludesConversationHistory() {
        // Given - query with previous messages
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1);
        String expectedAnswer = "Test answer";
        
        // Create previous messages
        com.example.ao_wiki_chat.model.entity.Message previousUserMessage = 
                com.example.ao_wiki_chat.model.entity.Message.builder()
                .id(UUID.randomUUID())
                .conversation(testConversation)
                .role(MessageRole.USER)
                .content("What is AI?")
                .build();
        
        com.example.ao_wiki_chat.model.entity.Message previousAssistantMessage = 
                com.example.ao_wiki_chat.model.entity.Message.builder()
                .id(UUID.randomUUID())
                .conversation(testConversation)
                .role(MessageRole.ASSISTANT)
                .content("AI is artificial intelligence.")
                .build();
        
        List<com.example.ao_wiki_chat.model.entity.Message> previousMessages = 
                Arrays.asList(previousUserMessage, previousAssistantMessage);
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString())).thenReturn(expectedAnswer);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(previousMessages);
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        
        // When
        ChatResponse response = ragService.processQuery(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo(expectedAnswer);
        
        // Verify prompt contains "Previous conversation:" and previous messages
        verify(llmService).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("Previous conversation:");
        assertThat(prompt).contains("User: What is AI?");
        assertThat(prompt).contains("Assistant: AI is artificial intelligence.");
        assertThat(prompt).contains("Question: " + TEST_QUERY);
    }
    
    @Test
    void processQueryWhenMoreMessagesThanMaxHistoryLimitsToLastN() {
        // Given - more messages than maxHistoryMessages (10)
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1);
        String expectedAnswer = "Test answer";
        
        // Create 15 messages (more than maxHistoryMessages = 10)
        List<com.example.ao_wiki_chat.model.entity.Message> previousMessages = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            com.example.ao_wiki_chat.model.entity.Message userMsg = 
                    com.example.ao_wiki_chat.model.entity.Message.builder()
                    .id(UUID.randomUUID())
                    .conversation(testConversation)
                    .role(MessageRole.USER)
                    .content("Question " + i)
                    .build();
            previousMessages.add(userMsg);
            
            com.example.ao_wiki_chat.model.entity.Message assistantMsg = 
                    com.example.ao_wiki_chat.model.entity.Message.builder()
                    .id(UUID.randomUUID())
                    .conversation(testConversation)
                    .role(MessageRole.ASSISTANT)
                    .content("Answer " + i)
                    .build();
            previousMessages.add(assistantMsg);
        }
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString())).thenReturn(expectedAnswer);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(previousMessages);
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        
        // When
        ChatResponse response = ragService.processQuery(request);
        
        // Then
        assertThat(response).isNotNull();
        
        // Verify prompt contains only last 10 messages (20 messages total, last 10 = messages 10-19)
        verify(llmService).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("Previous conversation:");
        
        // Should contain messages from index 10 onwards (last 10 messages)
        assertThat(prompt).contains("Question 10");
        assertThat(prompt).contains("Answer 10");
        assertThat(prompt).contains("Question 14");
        assertThat(prompt).contains("Answer 14");
        
        // Should NOT contain early messages (first 5 messages)
        assertThat(prompt).doesNotContain("Question 0");
        assertThat(prompt).doesNotContain("Answer 0");
        assertThat(prompt).doesNotContain("Question 4");
        assertThat(prompt).doesNotContain("Answer 4");
    }
    
    @Test
    void processQueryWhenIncludeHistoryDisabledDoesNotIncludePreviousMessages() {
        // Given - RAGService with includeHistory = false
        RAGService ragServiceWithoutHistory = new RAGService(
            embeddingService,
            vectorSearchService,
            llmService,
            conversationRepository,
            messageRepository,
            10, // maxHistoryMessages
            false // includeHistory = false
        );
        
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1);
        String expectedAnswer = "Test answer";
        
        // Create previous messages
        com.example.ao_wiki_chat.model.entity.Message previousUserMessage = 
                com.example.ao_wiki_chat.model.entity.Message.builder()
                .id(UUID.randomUUID())
                .conversation(testConversation)
                .role(MessageRole.USER)
                .content("Previous question")
                .build();
        
        com.example.ao_wiki_chat.model.entity.Message previousAssistantMessage = 
                com.example.ao_wiki_chat.model.entity.Message.builder()
                .id(UUID.randomUUID())
                .conversation(testConversation)
                .role(MessageRole.ASSISTANT)
                .content("Previous answer")
                .build();
        
        List<com.example.ao_wiki_chat.model.entity.Message> previousMessages = 
                Arrays.asList(previousUserMessage, previousAssistantMessage);
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString())).thenReturn(expectedAnswer);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(previousMessages);
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        
        // When
        ChatResponse response = ragServiceWithoutHistory.processQuery(request);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.answer()).isEqualTo(expectedAnswer);
        
        // Verify prompt does NOT contain "Previous conversation:" even though messages exist
        verify(llmService).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).doesNotContain("Previous conversation:");
        assertThat(prompt).doesNotContain("Previous question");
        assertThat(prompt).doesNotContain("Previous answer");
        assertThat(prompt).contains("Question: " + TEST_QUERY);
    }
    
    @Test
    void processQueryWhenPreviousMessagesExistFormatsPromptCorrectly() {
        // Given - query with previous messages to verify formatting
        ChatRequest request = new ChatRequest(TEST_QUERY, TEST_SESSION_ID);
        List<Chunk> relevantChunks = Arrays.asList(testChunk1);
        String expectedAnswer = "Test answer";
        
        // Create previous messages with specific content
        com.example.ao_wiki_chat.model.entity.Message previousUserMessage = 
                com.example.ao_wiki_chat.model.entity.Message.builder()
                .id(UUID.randomUUID())
                .conversation(testConversation)
                .role(MessageRole.USER)
                .content("What is artificial intelligence?")
                .build();
        
        com.example.ao_wiki_chat.model.entity.Message previousAssistantMessage = 
                com.example.ao_wiki_chat.model.entity.Message.builder()
                .id(UUID.randomUUID())
                .conversation(testConversation)
                .role(MessageRole.ASSISTANT)
                .content("Artificial intelligence is the simulation of human intelligence by machines.")
                .build();
        
        List<com.example.ao_wiki_chat.model.entity.Message> previousMessages = 
                Arrays.asList(previousUserMessage, previousAssistantMessage);
        
        when(embeddingService.generateEmbedding(TEST_QUERY)).thenReturn(testQueryEmbedding);
        when(vectorSearchService.findSimilarChunks(testQueryEmbedding)).thenReturn(relevantChunks);
        when(llmService.generate(anyString())).thenReturn(expectedAnswer);
        when(conversationRepository.findBySessionId(TEST_SESSION_ID))
                .thenReturn(Optional.of(testConversation));
        when(messageRepository.findByConversation_IdOrderByCreatedAtAsc(testConversation.getId()))
                .thenReturn(previousMessages);
        when(messageRepository.save(any(com.example.ao_wiki_chat.model.entity.Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        
        // When
        ChatResponse response = ragService.processQuery(request);
        
        // Then
        assertThat(response).isNotNull();
        
        // Verify prompt formatting
        verify(llmService).generate(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        
        // Verify structure: should contain "Previous conversation:" section
        assertThat(prompt).contains("Previous conversation:");
        
        // Verify message formatting with correct prefixes
        assertThat(prompt).contains("User: What is artificial intelligence?");
        assertThat(prompt).contains("Assistant: Artificial intelligence is the simulation of human intelligence by machines.");
        
        // Verify order: User message should come before Assistant message
        int userIndex = prompt.indexOf("User: What is artificial intelligence?");
        int assistantIndex = prompt.indexOf("Assistant: Artificial intelligence");
        assertThat(userIndex).isLessThan(assistantIndex);
        
        // Verify current question is present
        assertThat(prompt).contains("Question: " + TEST_QUERY);
        
        // Verify document context is present
        assertThat(prompt).contains("Context from documents:");
        assertThat(prompt).contains(testChunk1.getContent());
    }
    
    /**
     * Creates a test vector with specified dimension and fill value.
     */
    private float[] createTestVector(int dimension, float fillValue) {
        float[] vector = new float[dimension];
        Arrays.fill(vector, fillValue);
        return vector;
    }
}

