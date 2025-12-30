package com.example.ao_wiki_chat.repository;

import com.example.ao_wiki_chat.model.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Conversation entity operations.
 * Manages chat sessions and conversation history.
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Find a conversation by its session ID.
     * Session ID is unique and used to identify chat sessions.
     *
     * @param sessionId the session identifier
     * @return Optional containing the conversation if found
     */
    Optional<Conversation> findBySessionId(String sessionId);

    /**
     * Check if a conversation with the given session ID exists.
     *
     * @param sessionId the session identifier
     * @return true if a conversation with this session ID exists
     */
    boolean existsBySessionId(String sessionId);

    /**
     * Find all conversations ordered by creation date descending.
     * Most recent conversations appear first.
     *
     * @return list of all conversations, newest first
     */
    List<Conversation> findAllByOrderByCreatedAtDesc();

    /**
     * Find all conversations ordered by last update descending.
     * Useful for showing "recently active" conversations.
     *
     * @return list of all conversations, most recently updated first
     */
    List<Conversation> findAllByOrderByUpdatedAtDesc();
}

