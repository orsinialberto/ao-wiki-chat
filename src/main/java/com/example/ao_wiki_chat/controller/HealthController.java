package com.example.ao_wiki_chat.controller;

import com.example.ao_wiki_chat.model.dto.DatabaseHealthResponse;
import com.example.ao_wiki_chat.model.dto.GeminiHealthResponse;
import com.example.ao_wiki_chat.model.dto.HealthResponse;
import com.example.ao_wiki_chat.service.GeminiEmbeddingService;
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
    private static final String GEMINI_AVAILABLE = "available";
    private static final String GEMINI_UNAVAILABLE = "unavailable";

    private final DataSource dataSource;
    private final GeminiEmbeddingService geminiEmbeddingService;

    /**
     * Constructs a HealthController with required dependencies.
     *
     * @param dataSource the data source for database connectivity checks
     * @param geminiEmbeddingService the Gemini embedding service for API availability checks
     */
    public HealthController(
            DataSource dataSource,
            GeminiEmbeddingService geminiEmbeddingService
    ) {
        this.dataSource = dataSource;
        this.geminiEmbeddingService = geminiEmbeddingService;
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
     * Checks Gemini API availability.
     * Makes a simple API call to verify the service is accessible.
     *
     * @return 200 OK if API is accessible, 503 Service Unavailable if API is down
     */
    @GetMapping("/gemini")
    public ResponseEntity<GeminiHealthResponse> geminiHealth() {
        log.debug("Gemini API health check requested");

        try {
            boolean isHealthy = geminiEmbeddingService.isHealthy();

            if (isHealthy) {
                log.debug("Gemini API health check successful");
                return ResponseEntity.ok(
                        new GeminiHealthResponse(STATUS_UP, GEMINI_AVAILABLE)
                );
            } else {
                log.warn("Gemini API health check returned unhealthy status");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new GeminiHealthResponse(STATUS_DOWN, GEMINI_UNAVAILABLE));
            }

        } catch (Exception e) {
            log.error("Gemini API health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new GeminiHealthResponse(STATUS_DOWN, GEMINI_UNAVAILABLE));
        }
    }
}
