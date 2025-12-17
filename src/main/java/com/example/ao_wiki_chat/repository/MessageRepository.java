package com.example.ao_wiki_chat.repository;

import com.example.ao_wiki_chat.model.entity.Message;
import com.example.ao_wiki_chat.model.enums.MessageRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Message entity operations.
 * Manages individual messages within conversations.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * Find all messages in a conversation ordered chronologically.
     * Used to retrieve conversation history in correct order.
     *
     * @param conversationId the conversation UUID
     * @return list of messages ordered by creation time (oldest first)
     */
    List<Message> findByConversation_IdOrderByCreatedAtAsc(UUID conversationId);

    /**
     * Find all messages in a conversation with a specific role.
     * Useful for extracting only user queries or assistant responses.
     *
     * @param conversationId the conversation UUID
     * @param role the message role (USER or ASSISTANT)
     * @return list of messages with the specified role, ordered chronologically
     */
    List<Message> findByConversation_IdAndRoleOrderByCreatedAtAsc(
            UUID conversationId,
            MessageRole role
    );

    /**
     * Count messages in a specific conversation.
     *
     * @param conversationId the conversation UUID
     * @return number of messages in the conversation
     */
    long countByConversation_Id(UUID conversationId);

    /**
     * Delete all messages in a conversation.
     * Note: This is handled automatically by CASCADE DELETE,
     * but can be called explicitly if needed.
     *
     * @param conversationId the conversation UUID
     */
    void deleteByConversation_Id(UUID conversationId);
}

