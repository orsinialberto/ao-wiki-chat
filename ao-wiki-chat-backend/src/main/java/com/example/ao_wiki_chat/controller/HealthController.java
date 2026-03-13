package com.example.ao_wiki_chat.controller;

import com.example.ao_wiki_chat.model.dto.DatabaseHealthResponse;
import com.example.ao_wiki_chat.model.dto.EmbeddingHealthResponse;
import com.example.ao_wiki_chat.model.dto.HealthResponse;
import com.example.ao_wiki_chat.service.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

/**
 * REST controller for health check endpoints.
 * Provides endpoints for monitoring application health, database connectivity,
 * and external service availability.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String DATABASE_CONNECTED = "connected";
    private static final String DATABASE_DISCONNECTED = "disconnected";
    private static final String EMBEDDING_AVAILABLE = "available";
    private static final String EMBEDDING_UNAVAILABLE = "unavailable";

    private final DataSource dataSource;
    private final EmbeddingService embeddingService;

    /**
     * Constructs a HealthController with required dependencies.
     *
     * @param dataSource the data source for database connectivity checks
     * @param embeddingService the embedding service for availability checks
     */
    public HealthController(
            DataSource dataSource,
            EmbeddingService embeddingService
    ) {
        this.dataSource = dataSource;
        this.embeddingService = embeddingService;
    }

    /**
     * Returns general application health status.
     * This endpoint always returns UP if the application is running.
     *
     * @return 200 OK with health status
     */
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        log.debug("Health check requested");
        return ResponseEntity.ok(new HealthResponse(STATUS_UP));
    }

    /**
     * Checks PostgreSQL database connectivity.
     * Performs a lightweight query to verify database accessibility.
     *
     * @return 200 OK if database is accessible, 503 Service Unavailable if database is down
     */
    @GetMapping("/db")
    public ResponseEntity<DatabaseHealthResponse> databaseHealth() {
        log.debug("Database health check requested");

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Perform a lightweight query to verify connectivity
            statement.executeQuery("SELECT 1");

            log.debug("Database health check successful");
            return ResponseEntity.ok(
                    new DatabaseHealthResponse(STATUS_UP, DATABASE_CONNECTED)
            );

        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new DatabaseHealthResponse(STATUS_DOWN, DATABASE_DISCONNECTED));
        }
    }

    /**
     * Checks embedding service availability.
     * Verifies that the configured embedding provider (e.g. Ollama) is accessible.
     *
     * @return 200 OK if embedding service is healthy, 503 Service Unavailable otherwise
     */
    @GetMapping("/embedding")
    public ResponseEntity<EmbeddingHealthResponse> embeddingHealth() {
        log.debug("Embedding service health check requested");

        try {
            boolean isHealthy = embeddingService.isHealthy();

            if (isHealthy) {
                log.debug("Embedding service health check successful");
                return ResponseEntity.ok(
                        new EmbeddingHealthResponse(STATUS_UP, EMBEDDING_AVAILABLE)
                );
            } else {
                log.warn("Embedding service health check returned unhealthy status");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new EmbeddingHealthResponse(STATUS_DOWN, EMBEDDING_UNAVAILABLE));
            }

        } catch (Exception e) {
            log.error("Embedding service health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new EmbeddingHealthResponse(STATUS_DOWN, EMBEDDING_UNAVAILABLE));
        }
    }
}
