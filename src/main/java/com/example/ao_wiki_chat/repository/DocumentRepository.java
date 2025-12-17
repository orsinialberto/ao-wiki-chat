package com.example.ao_wiki_chat.repository;

import com.example.ao_wiki_chat.model.entity.Document;
import com.example.ao_wiki_chat.model.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Document entity operations.
 * Provides standard CRUD operations and custom queries for document management.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Find all documents with a specific processing status.
     * Useful for monitoring processing pipeline and error handling.
     *
     * @param status the document status to filter by
     * @return list of documents matching the status
     */
    List<Document> findByStatus(DocumentStatus status);

    /**
     * Find all documents by status ordered by creation date descending.
     * Most recent documents appear first.
     *
     * @param status the document status to filter by
     * @return list of documents matching the status, newest first
     */
    List<Document> findByStatusOrderByCreatedAtDesc(DocumentStatus status);

    /**
     * Check if a document with the given filename already exists.
     * Can be used to prevent duplicate uploads.
     *
     * @param filename the filename to check
     * @return true if a document with this filename exists
     */
    boolean existsByFilename(String filename);

    /**
     * Find all documents ordered by creation date descending.
     *
     * @return list of all documents, newest first
     */
    List<Document> findAllByOrderByCreatedAtDesc();
}

