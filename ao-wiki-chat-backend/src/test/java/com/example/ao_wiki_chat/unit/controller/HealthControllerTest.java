package com.example.ao_wiki_chat.unit.controller;

import com.example.ao_wiki_chat.controller.HealthController;
import com.example.ao_wiki_chat.model.dto.DatabaseHealthResponse;
import com.example.ao_wiki_chat.model.dto.EmbeddingHealthResponse;
import com.example.ao_wiki_chat.model.dto.HealthResponse;
import com.example.ao_wiki_chat.service.EmbeddingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HealthController.
 * Tests general health, database connectivity, and embedding service availability checks.
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    private HealthController controller;

    @BeforeEach
    void setUp() {
        controller = new HealthController(dataSource, embeddingService);
    }

    @Test
    void healthWhenApplicationRunningReturnsOkWithUpStatus() {
        // When
        ResponseEntity<HealthResponse> response = controller.health();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("UP");
    }

    @Test
    void databaseHealthWhenDatabaseAccessibleReturnsOkWithConnectedStatus() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(resultSet);

        // When
        ResponseEntity<DatabaseHealthResponse> response = controller.databaseHealth();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("UP");
        assertThat(response.getBody().database()).isEqualTo("connected");
        verify(connection).close();
        verify(statement).close();
    }

    @Test
    void databaseHealthWhenConnectionFailsReturnsServiceUnavailable() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        // When
        ResponseEntity<DatabaseHealthResponse> response = controller.databaseHealth();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DOWN");
        assertThat(response.getBody().database()).isEqualTo("disconnected");
    }

    @Test
    void databaseHealthWhenQueryFailsReturnsServiceUnavailable() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenThrow(new SQLException("Query failed"));

        // When
        ResponseEntity<DatabaseHealthResponse> response = controller.databaseHealth();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DOWN");
        assertThat(response.getBody().database()).isEqualTo("disconnected");
        verify(connection).close();
        verify(statement).close();
    }

    @Test
    void databaseHealthWhenStatementCreationFailsReturnsServiceUnavailable() throws SQLException {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenThrow(new SQLException("Statement creation failed"));

        // When
        ResponseEntity<DatabaseHealthResponse> response = controller.databaseHealth();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DOWN");
        assertThat(response.getBody().database()).isEqualTo("disconnected");
        verify(connection).close();
        verify(statement, never()).executeQuery(anyString());
    }

    @Test
    void embeddingHealthWhenServiceHealthyReturnsOkWithAvailableStatus() {
        // Given
        when(embeddingService.isHealthy()).thenReturn(true);

        // When
        ResponseEntity<EmbeddingHealthResponse> response = controller.embeddingHealth();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("UP");
        assertThat(response.getBody().embedding()).isEqualTo("available");
        verify(embeddingService).isHealthy();
    }

    @Test
    void embeddingHealthWhenServiceUnhealthyReturnsServiceUnavailable() {
        // Given
        when(embeddingService.isHealthy()).thenReturn(false);

        // When
        ResponseEntity<EmbeddingHealthResponse> response = controller.embeddingHealth();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DOWN");
        assertThat(response.getBody().embedding()).isEqualTo("unavailable");
        verify(embeddingService).isHealthy();
    }

    @Test
    void embeddingHealthWhenServiceThrowsExceptionReturnsServiceUnavailable() {
        // Given
        when(embeddingService.isHealthy())
                .thenThrow(new RuntimeException("Service unavailable"));

        // When
        ResponseEntity<EmbeddingHealthResponse> response = controller.embeddingHealth();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("DOWN");
        assertThat(response.getBody().embedding()).isEqualTo("unavailable");
        verify(embeddingService).isHealthy();
    }
}
