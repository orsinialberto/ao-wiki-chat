package com.example.ao_wiki_chat.integration;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.example.ao_wiki_chat.model.entity.Chunk;
import com.example.ao_wiki_chat.model.entity.Document;
import com.example.ao_wiki_chat.model.enums.DocumentStatus;
import com.example.ao_wiki_chat.repository.ChunkRepository;
import com.example.ao_wiki_chat.repository.DocumentRepository;

/**
 * Integration test for Chunk entity with vector embedding storage.
 * Verifies that embeddings are correctly saved and retrieved from PostgreSQL using pgvector.
 * 
 * This test validates the fix for the embedding type conversion issue:
 * - Verifies VectorAttributeConverter works correctly with Hibernate
 * - Ensures embeddings are stored as vector type, not bytea
 * - Tests round-trip conversion (save → retrieve → verify)
 * 
 * This test is self-contained:
 * - Creates test data in @BeforeEach
 * - Cleans up all test data in @AfterEach
 * - Uses @Transactional for automatic rollback (additional safety)
 * - Database remains clean after test execution
 * 
 * Note: This test requires a running PostgreSQL database with pgvector extension.
 * Run docker-compose up before executing this test.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/wikichat",
    "spring.datasource.username=wikichat_user",
    "spring.datasource.password=wikichat_password",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.show-sql=false",
    "gemini.api.key=test-api-key-for-integration-test"
})
class ChunkEmbeddingIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Set GEMINI_API_KEY environment variable for test
        registry.add("GEMINI_API_KEY", () -> "test-api-key-for-integration-test");
    }

    @Autowired
    private ChunkRepository chunkRepository;

    @Autowired
    private DocumentRepository documentRepository;

    private Document testDocument;
    private UUID testDocumentId;

    @BeforeEach
    @Transactional
    void setUp() {
        // Create a test document - let Hibernate generate the ID
        testDocument = Document.builder()
            .filename("test-document-" + UUID.randomUUID() + ".txt")
            .contentType("text/plain")
            .fileSize(1024L)
            .status(DocumentStatus.COMPLETED)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        testDocument = documentRepository.save(testDocument);
        // Get the generated ID after save
        testDocumentId = testDocument.getId();
    }

    @AfterEach
    @Transactional
    void tearDown() {
        // Explicit cleanup: delete all chunks associated with test document
        if (testDocumentId != null) {
            chunkRepository.deleteByDocument_Id(testDocumentId);
            // Delete the test document
            documentRepository.deleteById(testDocumentId);
            // Flush to ensure cleanup is executed
            chunkRepository.flush();
            documentRepository.flush();
        }
    }

    @Test
    @Transactional
    void saveChunkWithEmbeddingWhenValidDataPersistsCorrectly() {
        // Given: Create a chunk with embedding (768 dimensions like Gemini gemini-embedding-001)
        float[] embedding = new float[768];
        for (int i = 0; i < 768; i++) {
            embedding[i] = (float) (Math.random() * 2 - 1); // Random values between -1 and 1
        }

        Chunk chunk = Chunk.builder()
            .document(testDocument)
            .content("Test chunk content for embedding verification")
            .chunkIndex(0)
            .embedding(embedding)
            .metadata(null)
            .build();

        // When: Save the chunk
        Chunk savedChunk = chunkRepository.save(chunk);
        chunkRepository.flush(); // Force flush to database to trigger conversion

        // Then: Verify it was saved with ID
        assertThat(savedChunk.getId()).isNotNull();
        assertThat(savedChunk.getEmbedding()).isNotNull();
        assertThat(savedChunk.getEmbedding()).hasSize(768);

        // Then: Retrieve from database and verify embedding is preserved
        Chunk retrievedChunk = chunkRepository.findById(savedChunk.getId())
            .orElseThrow(() -> new AssertionError("Chunk should exist in database"));

        assertThat(retrievedChunk.getEmbedding()).isNotNull();
        assertThat(retrievedChunk.getEmbedding()).hasSize(768);
        assertThat(retrievedChunk.getEmbedding()).containsExactly(embedding);
        assertThat(retrievedChunk.getContent()).isEqualTo("Test chunk content for embedding verification");
        assertThat(retrievedChunk.getChunkIndex()).isEqualTo(0);
    }

    @Test
    @Transactional
    void saveChunkWithNullEmbeddingWhenEmbeddingIsNullPersistsCorrectly() {
        // Given: Create a chunk without embedding
        Chunk chunk = Chunk.builder()
            .document(testDocument)
            .content("Test chunk without embedding")
            .chunkIndex(1)
            .embedding(null)
            .metadata(null)
            .build();

        // When: Save the chunk
        Chunk savedChunk = chunkRepository.save(chunk);
        chunkRepository.flush();

        // Then: Verify it was saved
        assertThat(savedChunk.getId()).isNotNull();
        assertThat(savedChunk.getEmbedding()).isNull();

        // Then: Retrieve from database
        Chunk retrievedChunk = chunkRepository.findById(savedChunk.getId())
            .orElseThrow(() -> new AssertionError("Chunk should exist in database"));

        assertThat(retrievedChunk.getEmbedding()).isNull();
        assertThat(retrievedChunk.getContent()).isEqualTo("Test chunk without embedding");
    }

    @Test
    @Transactional
    void saveMultipleChunksWithEmbeddingsWhenMultipleChunksPersistCorrectly() {
        // Given: Create multiple chunks with different embeddings
        Chunk chunk1 = Chunk.builder()
            .document(testDocument)
            .content("First chunk")
            .chunkIndex(0)
            .embedding(createTestEmbedding(0.1f))
            .metadata(null)
            .build();

        Chunk chunk2 = Chunk.builder()
            .document(testDocument)
            .content("Second chunk")
            .chunkIndex(1)
            .embedding(createTestEmbedding(0.2f))
            .metadata(null)
            .build();

        Chunk chunk3 = Chunk.builder()
            .document(testDocument)
            .content("Third chunk")
            .chunkIndex(2)
            .embedding(createTestEmbedding(0.3f))
            .metadata(null)
            .build();

        // When: Save all chunks
        chunkRepository.saveAll(java.util.List.of(chunk1, chunk2, chunk3));
        chunkRepository.flush();

        // Then: Verify all chunks are saved
        var allChunks = chunkRepository.findByDocument_IdOrderByChunkIndexAsc(testDocument.getId());
        assertThat(allChunks).hasSize(3);
        assertThat(allChunks.get(0).getContent()).isEqualTo("First chunk");
        assertThat(allChunks.get(1).getContent()).isEqualTo("Second chunk");
        assertThat(allChunks.get(2).getContent()).isEqualTo("Third chunk");

        // Verify embeddings are preserved
        assertThat(allChunks.get(0).getEmbedding()).isNotNull();
        assertThat(allChunks.get(1).getEmbedding()).isNotNull();
        assertThat(allChunks.get(2).getEmbedding()).isNotNull();
        assertThat(allChunks.get(0).getEmbedding()[0]).isCloseTo(0.1f, org.assertj.core.data.Offset.offset(0.001f));
        assertThat(allChunks.get(1).getEmbedding()[0]).isCloseTo(0.2f, org.assertj.core.data.Offset.offset(0.001f));
        assertThat(allChunks.get(2).getEmbedding()[0]).isCloseTo(0.3f, org.assertj.core.data.Offset.offset(0.001f));
    }

    @Test
    @Transactional
    void saveChunkWithEmbeddingWhenEmbeddingHasSpecificValuesPreservesPrecision() {
        // Given: Create embedding with specific known values
        float[] embedding = new float[768];
        embedding[0] = 1.0f;
        embedding[1] = -1.0f;
        embedding[2] = 0.5f;
        embedding[3] = -0.5f;
        embedding[767] = 0.123456f;
        // Fill rest with zeros
        for (int i = 4; i < 767; i++) {
            embedding[i] = 0.0f;
        }

        Chunk chunk = Chunk.builder()
            .document(testDocument)
            .content("Precision test chunk")
            .chunkIndex(0)
            .embedding(embedding)
            .metadata(null)
            .build();

        // When: Save and retrieve
        Chunk savedChunk = chunkRepository.save(chunk);
        chunkRepository.flush();
        Chunk retrievedChunk = chunkRepository.findById(savedChunk.getId())
            .orElseThrow(() -> new AssertionError("Chunk should exist"));

        // Then: Verify precision is preserved
        assertThat(retrievedChunk.getEmbedding()).hasSize(768);
        assertThat(retrievedChunk.getEmbedding()[0]).isEqualTo(1.0f);
        assertThat(retrievedChunk.getEmbedding()[1]).isEqualTo(-1.0f);
        assertThat(retrievedChunk.getEmbedding()[2]).isEqualTo(0.5f);
        assertThat(retrievedChunk.getEmbedding()[3]).isEqualTo(-0.5f);
        assertThat(retrievedChunk.getEmbedding()[767]).isCloseTo(0.123456f, org.assertj.core.data.Offset.offset(0.000001f));
    }

    /**
     * Creates a test embedding with a specific first value for testing.
     */
    private float[] createTestEmbedding(float firstValue) {
        float[] embedding = new float[768];
        embedding[0] = firstValue;
        for (int i = 1; i < 768; i++) {
            embedding[i] = (float) (Math.random() * 2 - 1);
        }
        return embedding;
    }
}

