package com.example.ao_wiki_chat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ao_wiki_chat.exception.LLMException;
import com.example.ao_wiki_chat.model.dto.ChatRequest;
import com.example.ao_wiki_chat.model.dto.ChatResponse;
import com.example.ao_wiki_chat.model.dto.SourceReference;
import com.example.ao_wiki_chat.model.entity.Chunk;
import com.example.ao_wiki_chat.model.entity.Conversation;
import com.example.ao_wiki_chat.model.entity.Message;
import com.example.ao_wiki_chat.model.enums.MessageRole;
import com.example.ao_wiki_chat.repository.ConversationRepository;
import com.example.ao_wiki_chat.repository.MessageRepository;

/**
 * RAG (Retrieval-Augmented Generation) orchestrator service.
 * Coordinates the complete RAG pipeline:
 * 1. Generate embedding for user query
 * 2. Find relevant document chunks using vector search
 * 3. Build context from chunks
 * 4. Generate structured prompt
 * 5. Call LLM to generate answer
 * 6. Save conversation and messages to database
 * 7. Return response with source references
 */
@Service
public class RAGService {
    
    private static final Logger log = LoggerFactory.getLogger(RAGService.class);
    private static final String PROMPT_TEMPLATE = 
        """
        You are a helpful assistant that answers questions based on the provided context.
        
        Context from documents:
        %s
        
        Question: %s
        
        Answer based on the context above. If the context does not contain enough information to answer the question, say so. Use only the information provided in the context.
        
        Answer:""";
    
    private final GeminiEmbeddingService embeddingService;
    private final VectorSearchService vectorSearchService;
    private final LLMService llmService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    
    /**
     * Constructs a RAGService with required dependencies.
     *
     * @param embeddingService service for generating query embeddings
     * @param vectorSearchService service for finding similar chunks
     * @param llmService service for generating LLM responses
     * @param conversationRepository repository for conversation operations
     * @param messageRepository repository for message operations
     */
    public RAGService(
            GeminiEmbeddingService embeddingService,
            VectorSearchService vectorSearchService,
            LLMService llmService,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository
    ) {
        this.embeddingService = embeddingService;
        this.vectorSearchService = vectorSearchService;
        this.llmService = llmService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        
        log.info("RAGService initialized");
    }
    
    /**
     * Processes a chat query through the complete RAG pipeline.
     * 
     * @param request the chat request containing query and session ID
     * @return chat response with answer and source references
     * @throws IllegalArgumentException if request is null or invalid
     * @throws com.example.ao_wiki_chat.exception.EmbeddingException if embedding generation fails
     * @throws com.example.ao_wiki_chat.exception.VectorSearchException if vector search fails
     * @throws LLMException if LLM generation fails
     */
    @Transactional
    public ChatResponse processQuery(ChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Chat request cannot be null");
        }
        
        String query = request.query();
        String sessionId = request.sessionId();
        
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }
        
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        log.info("Processing query for session: {}, query length: {}", sessionId, query.length());
        
        try {
            // Step 1: Generate embedding for the query
            log.debug("Step 1: Generating query embedding");
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            log.debug("Query embedding generated, dimension: {}", queryEmbedding.length);
            
            // Step 2: Find relevant chunks using vector search
            log.debug("Step 2: Searching for similar chunks");
            List<Chunk> relevantChunks = vectorSearchService.findSimilarChunks(queryEmbedding);
            log.info("Found {} relevant chunks", relevantChunks.size());
            
            if (relevantChunks.isEmpty()) {
                log.warn("No relevant chunks found for query, returning empty response");
                String noContextAnswer = "I couldn't find any relevant information in the documents to answer your question.";
                return saveAndReturnResponse(sessionId, query, noContextAnswer, List.of());
            }
            
            // Step 3: Build context from chunks
            log.debug("Step 3: Building context from chunks");
            String context = buildContext(relevantChunks);
            log.debug("Context built, length: {} characters", context.length());
            
            // Step 4: Generate structured prompt
            log.debug("Step 4: Generating structured prompt");
            String prompt = String.format(PROMPT_TEMPLATE, context, query);
            log.debug("Prompt generated, length: {} characters", prompt.length());
            
            // Step 5: Call LLM to generate answer
            log.debug("Step 5: Calling LLM to generate answer");
            String answer = llmService.generate(prompt);
            log.info("Answer generated, length: {} characters", answer.length());
            
            // Step 6: Build source references
            List<SourceReference> sources = buildSourceReferences(relevantChunks);
            
            // Step 7: Save conversation and messages, return response
            return saveAndReturnResponse(sessionId, query, answer, sources);
            
        } catch (Exception e) {
            log.error("Error processing RAG query: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Builds a context string from relevant chunks.
     * Formats chunks with document names and content.
     *
     * @param chunks the relevant chunks
     * @return formatted context string
     */
    private String buildContext(List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        
        for (int i = 0; i < chunks.size(); i++) {
            Chunk chunk = chunks.get(i);
            String documentName = chunk.getDocument().getFilename();
            String content = chunk.getContent();
            
            context.append(String.format("[Document: %s, Chunk %d]\n%s\n\n", 
                    documentName, chunk.getChunkIndex(), content));
        }
        
        return context.toString().trim();
    }
    
    /**
     * Builds source references from relevant chunks.
     * Note: Similarity scores are not directly available from VectorSearchService,
     * so we use a placeholder value. In a production system, you might want to
     * modify VectorSearchService to return similarity scores.
     *
     * @param chunks the relevant chunks
     * @return list of source references
     */
    private List<SourceReference> buildSourceReferences(List<Chunk> chunks) {
        List<SourceReference> sources = new ArrayList<>();
        
        for (Chunk chunk : chunks) {
            String documentName = chunk.getDocument().getFilename();
            String content = chunk.getContent();
            Integer chunkIndex = chunk.getChunkIndex();
            
            // Similarity score is not directly available, using placeholder
            // In production, modify VectorSearchService to return scores
            Double similarityScore = 0.85; // Placeholder
            
            sources.add(new SourceReference(documentName, content, similarityScore, chunkIndex));
        }
        
        return sources;
    }
    
    /**
     * Saves the conversation and messages to the database and returns the response.
     *
     * @param sessionId the session identifier
     * @param query the user query
     * @param answer the generated answer
     * @param sources the source references
     * @return chat response
     */
    private ChatResponse saveAndReturnResponse(
            String sessionId,
            String query,
            String answer,
            List<SourceReference> sources
    ) {
        log.debug("Saving conversation and messages for session: {}", sessionId);
        
        // Get or create conversation
        Conversation conversation = getOrCreateConversation(sessionId);
        
        // Save user message
        Message userMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.USER)
                .content(query)
                .build();
        messageRepository.save(userMessage);
        log.debug("Saved user message");
        
        // Save assistant message with sources
        String sourcesJson = serializeSources(sources);
        Message assistantMessage = Message.builder()
                .conversation(conversation)
                .role(MessageRole.ASSISTANT)
                .content(answer)
                .sources(sourcesJson)
                .build();
        messageRepository.save(assistantMessage);
        log.debug("Saved assistant message with {} sources", sources.size());
        
        log.info("Conversation saved successfully for session: {}", sessionId);
        
        return new ChatResponse(answer, sources);
    }
    
    /**
     * Gets an existing conversation or creates a new one for the session ID.
     *
     * @param sessionId the session identifier
     * @return the conversation entity
     */
    private Conversation getOrCreateConversation(String sessionId) {
        Optional<Conversation> existing = conversationRepository.findBySessionId(sessionId);
        
        if (existing.isPresent()) {
            log.debug("Found existing conversation for session: {}", sessionId);
            return existing.get();
        }
        
        log.debug("Creating new conversation for session: {}", sessionId);
        Conversation newConversation = Conversation.builder()
                .sessionId(sessionId)
                .build();
        
        return conversationRepository.save(newConversation);
    }
    
    /**
     * Serializes source references to JSON string for storage in JSONB column.
     * Uses simple JSON serialization without external dependencies.
     *
     * @param sources the source references
     * @return JSON string representation
     */
    private String serializeSources(List<SourceReference> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        
        try {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < sources.size(); i++) {
                if (i > 0) {
                    json.append(",");
                }
                SourceReference source = sources.get(i);
                json.append("{");
                json.append("\"documentName\":").append(escapeJson(source.documentName())).append(",");
                json.append("\"chunkContent\":").append(escapeJson(source.chunkContent())).append(",");
                json.append("\"similarityScore\":").append(source.similarityScore()).append(",");
                json.append("\"chunkIndex\":").append(source.chunkIndex());
                json.append("}");
            }
            json.append("]");
            return json.toString();
        } catch (Exception e) {
            log.error("Failed to serialize sources to JSON: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Escapes a string for JSON representation.
     *
     * @param value the string to escape
     * @return escaped JSON string
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\")
                          .replace("\"", "\\\"")
                          .replace("\n", "\\n")
                          .replace("\r", "\\r")
                          .replace("\t", "\\t") + "\"";
    }
}

