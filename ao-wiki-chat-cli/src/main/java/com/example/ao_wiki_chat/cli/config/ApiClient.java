package com.example.ao_wiki_chat.cli.config;

import com.example.ao_wiki_chat.cli.exception.ApiClientException;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.util.ErrorMessageFormatter;
import com.example.ao_wiki_chat.cli.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * HTTP client for communicating with the WikiChat REST API backend.
 * Provides methods for all API endpoints with retry logic and error handling.
 */
public class ApiClient {

    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 60;
    private static final int DEFAULT_READ_TIMEOUT_SECONDS = 300;
    private static final int DEFAULT_WRITE_TIMEOUT_SECONDS = 300;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final boolean verbose;

    /**
     * Constructs an ApiClient with default configuration.
     */
    public ApiClient() {
        this(DEFAULT_BASE_URL, false);
    }

    /**
     * Constructs an ApiClient with custom base URL.
     *
     * @param baseUrl the base URL of the API server
     */
    public ApiClient(String baseUrl) {
        this(baseUrl, false);
    }

    /**
     * Constructs an ApiClient with custom base URL and verbose logging.
     *
     * @param baseUrl the base URL of the API server
     * @param verbose enable verbose request/response logging
     */
    public ApiClient(String baseUrl, boolean verbose) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.verbose = verbose;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(this::createLoggingInterceptor)
                .build();
    }

    /**
     * Constructs an ApiClient with fully customizable configuration.
     *
     * @param baseUrl the base URL of the API server
     * @param connectTimeout connection timeout
     * @param readTimeout read timeout
     * @param writeTimeout write timeout
     * @param verbose enable verbose request/response logging
     */
    public ApiClient(String baseUrl, Duration connectTimeout, Duration readTimeout, Duration writeTimeout, boolean verbose) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.verbose = verbose;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout.toSeconds(), TimeUnit.SECONDS)
                .readTimeout(readTimeout.toSeconds(), TimeUnit.SECONDS)
                .writeTimeout(writeTimeout.toSeconds(), TimeUnit.SECONDS)
                .addInterceptor(this::createLoggingInterceptor)
                .build();
    }

    /**
     * Uploads a document to the backend.
     *
     * @param filePath the path to the file to upload
     * @return document upload response
     * @throws ApiException if the upload fails
     */
    public CliDocumentUpload uploadDocument(Path filePath) {
        if (!Files.exists(filePath)) {
            throw new ApiClientException("File not found: " + filePath);
        }

        try {
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            RequestBody fileBody = RequestBody.create(
                    Files.readAllBytes(filePath),
                    MediaType.parse(contentType)
            );

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", filePath.getFileName().toString(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(baseUrl + "/api/documents/upload")
                    .post(requestBody)
                    .build();

            return executeRequest(request, CliDocumentUpload.class);
        } catch (IOException e) {
            throw new ApiClientException("Failed to read file: " + filePath, e);
        }
    }

    /**
     * Lists all documents.
     *
     * @return list of documents
     * @throws ApiException if the request fails
     */
    public CliDocumentList listDocuments() {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/documents")
                .get()
                .build();

        return executeRequest(request, CliDocumentList.class);
    }

    /**
     * Retrieves a document by ID.
     *
     * @param id the document ID
     * @return document details
     * @throws ApiException if the document is not found or request fails
     */
    public CliDocument getDocument(UUID id) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/documents/" + id)
                .get()
                .build();

        return executeRequest(request, CliDocument.class);
    }

    /**
     * Deletes a document by ID.
     *
     * @param id the document ID
     * @throws ApiException if the document is not found or request fails
     */
    public void deleteDocument(UUID id) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/documents/" + id)
                .delete()
                .build();

        executeRequest(request, Void.class);
    }

    /**
     * Retrieves all chunks for a document.
     *
     * @param id the document ID
     * @return list of chunks
     * @throws ApiException if the document is not found or request fails
     */
    public List<CliChunk> getDocumentChunks(UUID id) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/documents/" + id + "/chunks")
                .get()
                .build();

        return executeRequest(request, new com.fasterxml.jackson.core.type.TypeReference<List<CliChunk>>() {});
    }

    /**
     * Sends a chat query to the backend.
     *
     * @param query the user's query
     * @param sessionId the session ID for conversation tracking
     * @return chat response with answer and sources
     * @throws ApiException if the request fails
     */
    public CliChatResponse query(String query, String sessionId) {
        CliChatRequest chatRequest = new CliChatRequest(query, sessionId);

        try {
            String jsonBody = objectMapper.writeValueAsString(chatRequest);
            RequestBody requestBody = RequestBody.create(jsonBody, MediaType.parse("application/json"));

            Request request = new Request.Builder()
                    .url(baseUrl + "/api/chat/query")
                    .post(requestBody)
                    .build();

            return executeRequest(request, CliChatResponse.class);
        } catch (IOException e) {
            throw new ApiClientException("Failed to serialize request", e);
        }
    }

    /**
     * Retrieves conversation history for a session.
     *
     * @param sessionId the session ID
     * @return list of messages
     * @throws ApiException if the session is not found or request fails
     */
    public List<CliMessage> getHistory(String sessionId) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/chat/history/" + sessionId)
                .get()
                .build();

        return executeRequest(request, new com.fasterxml.jackson.core.type.TypeReference<List<CliMessage>>() {});
    }

    /**
     * Clears conversation history for a session.
     *
     * @param sessionId the session ID
     * @throws ApiException if the session is not found or request fails
     */
    public void clearHistory(String sessionId) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/chat/" + sessionId)
                .delete()
                .build();

        executeRequest(request, Void.class);
    }

    /**
     * Checks general application health.
     *
     * @return health response
     * @throws ApiException if the request fails
     */
    public CliHealthResponse health() {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/health")
                .get()
                .build();

        return executeRequest(request, CliHealthResponse.class);
    }

    /**
     * Checks database health.
     *
     * @return database health response
     * @throws ApiException if the request fails
     */
    public CliDatabaseHealthResponse healthDb() {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/health/db")
                .get()
                .build();

        return executeRequest(request, CliDatabaseHealthResponse.class);
    }

    /**
     * Checks Gemini API health.
     *
     * @return Gemini health response
     * @throws ApiException if the request fails
     */
    public CliGeminiHealthResponse healthGemini() {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/health/gemini")
                .get()
                .build();

        return executeRequest(request, CliGeminiHealthResponse.class);
    }

    /**
     * Executes an HTTP request with retry logic and error handling.
     *
     * @param request the HTTP request
     * @param responseType the expected response type
     * @param <T> the response type
     * @return the deserialized response
     * @throws ApiException if the request fails after retries
     */
    private <T> T executeRequest(Request request, Class<T> responseType) {
        return executeRequestWithRetry(request, responseType, 0);
    }

    /**
     * Executes an HTTP request with retry logic and error handling.
     *
     * @param request the HTTP request
     * @param responseType the expected response type reference
     * @param <T> the response type
     * @return the deserialized response
     * @throws ApiException if the request fails after retries
     */
    private <T> T executeRequest(Request request, com.fasterxml.jackson.core.type.TypeReference<T> responseType) {
        return executeRequestWithRetry(request, responseType, 0);
    }

    /**
     * Executes a request with exponential backoff retry logic.
     *
     * @param request the HTTP request
     * @param responseType the expected response type
     * @param attempt the current retry attempt (0-based)
     * @param <T> the response type
     * @return the deserialized response
     * @throws ApiException if the request fails after all retries
     */
    @SuppressWarnings("unchecked")
    private <T> T executeRequestWithRetry(Request request, Object responseType, int attempt) {
        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                if (responseType == Void.class || response.body() == null) {
                    return null;
                }

                String responseBody = response.body().string();
                if (verbose) {
                    log.debug("Response body: {}", responseBody);
                }

                if (responseType instanceof Class) {
                    return objectMapper.readValue(responseBody, (Class<T>) responseType);
                } else if (responseType instanceof com.fasterxml.jackson.core.type.TypeReference) {
                    return objectMapper.readValue(responseBody, (com.fasterxml.jackson.core.type.TypeReference<T>) responseType);
                } else {
                    throw new ApiClientException("Unsupported response type: " + responseType.getClass());
                }
            } else {
                String errorBody = response.body() != null ? response.body().string() : "No error details";
                int statusCode = response.code();

                // Retry on 5xx errors
                if (statusCode >= 500 && statusCode < 600 && attempt < MAX_RETRIES) {
                    long delayMs = INITIAL_RETRY_DELAY_MS * (1L << attempt);
                    log.warn("Server error {} (attempt {}/{}), retrying in {} ms", statusCode, attempt + 1, MAX_RETRIES, delayMs);

                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new ApiClientException("Request interrupted during retry", e);
                    }

                    return executeRequestWithRetry(request, responseType, attempt + 1);
                }

                // Don't retry on 4xx errors
                String userMessage = ErrorMessageFormatter.formatError(
                        new ApiException(errorBody, statusCode)
                );
                throw new ApiException(userMessage, statusCode);
            }
        } catch (IOException e) {
            // Retry on network errors
            if (attempt < MAX_RETRIES) {
                long delayMs = INITIAL_RETRY_DELAY_MS * (1L << attempt);
                log.warn("Network error (attempt {}/{}), retrying in {} ms: {}", attempt + 1, MAX_RETRIES, delayMs, e.getMessage());

                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ApiClientException("Request interrupted during retry", ie);
                }

                return executeRequestWithRetry(request, responseType, attempt + 1);
            }

            String errorMessage = ErrorMessageFormatter.formatError(e);
            if (e instanceof java.net.SocketTimeoutException) {
                throw new ApiClientException("Request timed out. " + errorMessage, e);
            } else if (e instanceof java.net.ConnectException) {
                throw new ApiClientException("Cannot connect to server. " + errorMessage, e);
            } else {
                throw new ApiClientException("Network error after " + (attempt + 1) + " attempts: " + errorMessage, e);
            }
        }
    }

    /**
     * Creates a logging interceptor for verbose mode.
     *
     * @param chain the interceptor chain
     * @return the response
     * @throws IOException if the request fails
     */
    private Response createLoggingInterceptor(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();

        if (verbose) {
            log.debug("Request: {} {}", request.method(), request.url());
            if (request.body() != null && request.body().contentType() != null) {
                log.debug("Content-Type: {}", request.body().contentType());
            }
            if (request.headers() != null && request.headers().size() > 0) {
                log.debug("Request headers: {}", request.headers());
            }
        }

        Response response = chain.proceed(request);

        if (verbose) {
            log.debug("Response: {} {}", response.code(), response.message());
            if (response.headers() != null && response.headers().size() > 0) {
                log.debug("Response headers: {}", response.headers());
            }
        }

        return response;
    }
}
