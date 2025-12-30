package com.example.ao_wiki_chat.cli.config;

import com.example.ao_wiki_chat.cli.exception.ApiClientException;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ApiClient using MockWebServer.
 * Tests all endpoints and error handling scenarios.
 */
class ApiClientTest {

    private MockWebServer mockWebServer;
    private ApiClient apiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        apiClient = new ApiClient(mockWebServer.url("/").toString(), false);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void uploadDocumentWhenFileExistsReturnsDocumentUpload() throws Exception {
        // Given
        Path tempFile = Files.createTempFile("test", ".pdf");
        Files.write(tempFile, "test content".getBytes());

        CliDocumentUpload expectedResponse = new CliDocumentUpload(
                UUID.randomUUID(),
                "test.pdf",
                "application/pdf",
                12L,
                "PROCESSING",
                LocalDateTime.now()
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        // When
        CliDocumentUpload result = apiClient.uploadDocument(tempFile);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.filename()).isEqualTo("test.pdf");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.status()).isEqualTo("PROCESSING");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/documents/upload");

        Files.deleteIfExists(tempFile);
    }

    @Test
    void uploadDocumentWhenFileNotFoundThrowsException() {
        // Given
        Path nonExistentFile = Path.of("/nonexistent/file.pdf");

        // When/Then
        assertThatThrownBy(() -> apiClient.uploadDocument(nonExistentFile))
                .isInstanceOf(ApiClientException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void listDocumentsWhenSuccessfulReturnsDocumentList() throws Exception {
        // Given
        CliDocument document = new CliDocument(
                UUID.randomUUID(),
                "test.pdf",
                "application/pdf",
                100L,
                "COMPLETED",
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        CliDocumentList expectedResponse = new CliDocumentList(
                List.of(document),
                1
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        // When
        CliDocumentList result = apiClient.listDocuments();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.documents()).hasSize(1);
        assertThat(result.documents().get(0).filename()).isEqualTo("test.pdf");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/documents");
    }

    @Test
    void getDocumentWhenDocumentExistsReturnsDocument() throws Exception {
        // Given
        UUID documentId = UUID.randomUUID();
        CliDocument expectedResponse = new CliDocument(
                documentId,
                "test.pdf",
                "application/pdf",
                100L,
                "COMPLETED",
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        // When
        CliDocument result = apiClient.getDocument(documentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.documentId()).isEqualTo(documentId);
        assertThat(result.filename()).isEqualTo("test.pdf");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/documents/" + documentId);
    }

    @Test
    void getDocumentWhenDocumentNotFoundThrowsException() {
        // Given
        UUID documentId = UUID.randomUUID();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("Document not found"));

        // When/Then
        assertThatThrownBy(() -> apiClient.getDocument(documentId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiException = (ApiException) e;
                    assertThat(apiException.getStatusCode()).isEqualTo(404);
                });
    }

    @Test
    void deleteDocumentWhenSuccessfulReturnsVoid() throws Exception {
        // Given
        UUID documentId = UUID.randomUUID();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204));

        // When
        apiClient.deleteDocument(documentId);

        // Then
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/api/documents/" + documentId);
    }

    @Test
    void deleteDocumentWhenDocumentNotFoundThrowsException() {
        // Given
        UUID documentId = UUID.randomUUID();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("Document not found"));

        // When/Then
        assertThatThrownBy(() -> apiClient.deleteDocument(documentId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiException = (ApiException) e;
                    assertThat(apiException.getStatusCode()).isEqualTo(404);
                });
    }

    @Test
    void getDocumentChunksWhenSuccessfulReturnsChunkList() throws Exception {
        // Given
        UUID documentId = UUID.randomUUID();
        CliChunk chunk = new CliChunk(
                UUID.randomUUID(),
                documentId,
                "chunk content",
                0,
                LocalDateTime.now()
        );

        List<CliChunk> expectedResponse = List.of(chunk);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        // When
        List<CliChunk> result = apiClient.getDocumentChunks(documentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("chunk content");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/documents/" + documentId + "/chunks");
    }

    @Test
    void queryWhenSuccessfulReturnsChatResponse() throws Exception {
        // Given
        String query = "What is the meaning of life?";
        String sessionId = "session-123";

        CliSourceReference source = new CliSourceReference(
                "test.pdf",
                "relevant content",
                0.95,
                0
        );

        CliChatResponse expectedResponse = new CliChatResponse(
                "The answer is 42.",
                List.of(source)
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        // When
        CliChatResponse result = apiClient.query(query, sessionId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.answer()).isEqualTo("The answer is 42.");
        assertThat(result.sources()).hasSize(1);

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).isEqualTo("/api/chat/query");

        CliChatRequest requestBody = objectMapper.readValue(
                request.getBody().readUtf8(),
                CliChatRequest.class
        );
        assertThat(requestBody.query()).isEqualTo(query);
        assertThat(requestBody.sessionId()).isEqualTo(sessionId);
    }

    @Test
    void getHistoryWhenSuccessfulReturnsMessageList() throws Exception {
        // Given
        String sessionId = "session-123";
        CliMessage message = new CliMessage(
                "Hello",
                "USER",
                LocalDateTime.now(),
                null
        );

        List<CliMessage> expectedResponse = List.of(message);

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        // When
        List<CliMessage> result = apiClient.getHistory(sessionId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("Hello");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/chat/history/" + sessionId);
    }

    @Test
    void clearHistoryWhenSuccessfulReturnsVoid() throws Exception {
        // Given
        String sessionId = "session-123";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(204));

        // When
        apiClient.clearHistory(sessionId);

        // Then
        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("DELETE");
        assertThat(request.getPath()).isEqualTo("/api/chat/" + sessionId);
    }

    @Test
    void healthWhenSuccessfulReturnsHealthResponse() throws Exception {
        // Given
        CliHealthResponse expectedResponse = new CliHealthResponse("UP");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        // When
        CliHealthResponse result = apiClient.health();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("UP");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/health");
    }

    @Test
    void healthDbWhenSuccessfulReturnsDatabaseHealthResponse() throws Exception {
        // Given
        CliDatabaseHealthResponse expectedResponse = new CliDatabaseHealthResponse("UP", "connected");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        // When
        CliDatabaseHealthResponse result = apiClient.healthDb();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.database()).isEqualTo("connected");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/health/db");
    }

    @Test
    void healthGeminiWhenSuccessfulReturnsGeminiHealthResponse() throws Exception {
        // Given
        CliGeminiHealthResponse expectedResponse = new CliGeminiHealthResponse("UP", "available");

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        // When
        CliGeminiHealthResponse result = apiClient.healthGemini();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.gemini()).isEqualTo("available");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("GET");
        assertThat(request.getPath()).isEqualTo("/api/health/gemini");
    }

    @Test
    void executeRequestWhenServerErrorRetriesWithExponentialBackoff() throws Exception {
        // Given
        UUID documentId = UUID.randomUUID();
        CliDocument expectedResponse = new CliDocument(
                documentId,
                "test.pdf",
                "application/pdf",
                100L,
                "COMPLETED",
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );

        // First two attempts return 500, third succeeds
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(objectMapper.writeValueAsString(expectedResponse))
                .addHeader("Content-Type", "application/json"));

        // When
        CliDocument result = apiClient.getDocument(documentId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.documentId()).isEqualTo(documentId);

        // Verify 3 requests were made
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void executeRequestWhenClientErrorDoesNotRetry() {
        // Given
        UUID documentId = UUID.randomUUID();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setBody("Bad request"));

        // When/Then
        assertThatThrownBy(() -> apiClient.getDocument(documentId))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException apiException = (ApiException) e;
                    assertThat(apiException.getStatusCode()).isEqualTo(400);
                });

        // Verify only one request was made (no retry)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void constructorWhenBaseUrlEndsWithSlashRemovesTrailingSlash() {
        // Given
        String baseUrlWithSlash = mockWebServer.url("/").toString();

        // When
        ApiClient client = new ApiClient(baseUrlWithSlash);

        // Then - verify it works by making a request
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\":\"UP\"}")
                .addHeader("Content-Type", "application/json"));

        CliHealthResponse result = client.health();
        assertThat(result.status()).isEqualTo("UP");
    }

    @Test
    void constructorWhenCustomTimeoutsUsesProvidedValues() {
        // Given
        Duration connectTimeout = Duration.ofSeconds(10);
        Duration readTimeout = Duration.ofSeconds(20);
        Duration writeTimeout = Duration.ofSeconds(30);

        // When
        ApiClient client = new ApiClient(
                mockWebServer.url("/").toString(),
                connectTimeout,
                readTimeout,
                writeTimeout,
                false
        );

        // Then - verify it works
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\":\"UP\"}")
                .addHeader("Content-Type", "application/json"));

        CliHealthResponse result = client.health();
        assertThat(result.status()).isEqualTo("UP");
    }
}
