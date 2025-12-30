package com.example.ao_wiki_chat.cli.command.chat;

import com.example.ao_wiki_chat.cli.model.CliChatResponse;
import com.example.ao_wiki_chat.cli.model.CliMessage;
import com.example.ao_wiki_chat.cli.model.CliSourceReference;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for ChatCommandUtils.
 */
class ChatCommandUtilsTest {

    @Test
    void validateQueryWhenQueryIsNullThrowsException() {
        // When/Then
        assertThatThrownBy(() -> ChatCommandUtils.validateQuery(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Query cannot be null or empty");
    }

    @Test
    void validateQueryWhenQueryIsBlankThrowsException() {
        // When/Then
        assertThatThrownBy(() -> ChatCommandUtils.validateQuery("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Query cannot be null or empty");
    }

    @Test
    void validateQueryWhenQueryExceedsMaxLengthThrowsException() {
        // Given
        String longQuery = "a".repeat(10001);

        // When/Then
        assertThatThrownBy(() -> ChatCommandUtils.validateQuery(longQuery))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum length");
    }

    @Test
    void validateQueryWhenQueryIsValidDoesNotThrow() {
        // When/Then - should not throw
        ChatCommandUtils.validateQuery("What is the meaning of life?");
    }

    @Test
    void validateSessionIdWhenSessionIdIsNullThrowsException() {
        // When/Then
        assertThatThrownBy(() -> ChatCommandUtils.validateSessionId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session ID cannot be null or blank");
    }

    @Test
    void validateSessionIdWhenSessionIdIsBlankThrowsException() {
        // When/Then
        assertThatThrownBy(() -> ChatCommandUtils.validateSessionId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session ID cannot be null or blank");
    }

    @Test
    void validateSessionIdWhenSessionIdExceedsMaxLengthThrowsException() {
        // Given
        String longSessionId = "a".repeat(256);

        // When/Then
        assertThatThrownBy(() -> ChatCommandUtils.validateSessionId(longSessionId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed 255 characters");
    }

    @Test
    void validateSessionIdWhenSessionIdIsInvalidUuidThrowsException() {
        // When/Then
        assertThatThrownBy(() -> ChatCommandUtils.validateSessionId("invalid-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a valid UUID format");
    }

    @Test
    void validateSessionIdWhenSessionIdIsValidUuidDoesNotThrow() {
        // Given
        String validUuid = UUID.randomUUID().toString();

        // When/Then - should not throw
        ChatCommandUtils.validateSessionId(validUuid);
    }

    @Test
    void generateSessionIdReturnsValidUuid() {
        // When
        String sessionId = ChatCommandUtils.generateSessionId();

        // Then
        assertThat(sessionId).isNotNull();
        assertThat(UUID.fromString(sessionId)).isNotNull();
    }

    @Test
    void formatChatResponseTextWhenResponseIsNullReturnsMessage() {
        // When
        String result = ChatCommandUtils.formatChatResponseText(null, false);

        // Then
        assertThat(result).contains("No response received");
    }

    @Test
    void formatChatResponseTextWhenResponseHasAnswerFormatsCorrectly() {
        // Given
        CliChatResponse response = new CliChatResponse("Test answer", null);

        // When
        String result = ChatCommandUtils.formatChatResponseText(response, false);

        // Then
        assertThat(result).contains("Answer");
        assertThat(result).contains("Test answer");
    }

    @Test
    void formatChatResponseTextWhenShowSourcesTrueIncludesSources() {
        // Given
        CliSourceReference source = new CliSourceReference(
                "test.pdf", "chunk content", 0.95, 0
        );
        CliChatResponse response = new CliChatResponse("Test answer", List.of(source));

        // When
        String result = ChatCommandUtils.formatChatResponseText(response, true);

        // Then
        assertThat(result).contains("Sources");
        assertThat(result).contains("test.pdf");
        assertThat(result).contains("0.95");
    }

    @Test
    void formatChatResponseTextWhenShowSourcesFalseExcludesSources() {
        // Given
        CliSourceReference source = new CliSourceReference(
                "test.pdf", "chunk content", 0.95, 0
        );
        CliChatResponse response = new CliChatResponse("Test answer", List.of(source));

        // When
        String result = ChatCommandUtils.formatChatResponseText(response, false);

        // Then
        assertThat(result).doesNotContain("Sources");
    }

    @Test
    void formatChatResponseMarkdownWhenResponseIsNullReturnsMessage() {
        // When
        String result = ChatCommandUtils.formatChatResponseMarkdown(null, false);

        // Then
        assertThat(result).contains("No response received");
    }

    @Test
    void formatChatResponseMarkdownFormatsCorrectly() {
        // Given
        CliChatResponse response = new CliChatResponse("Test answer", null);

        // When
        String result = ChatCommandUtils.formatChatResponseMarkdown(response, false);

        // Then
        assertThat(result).contains("## Answer");
        assertThat(result).contains("Test answer");
    }

    @Test
    void formatChatResponseJsonFormatsCorrectly() {
        // Given
        CliChatResponse response = new CliChatResponse("Test answer", null);

        // When
        String result = ChatCommandUtils.formatChatResponseJson(response);

        // Then
        assertThat(result).contains("answer");
        assertThat(result).contains("Test answer");
    }

    @Test
    void formatHistoryTextWhenMessagesIsEmptyReturnsMessage() {
        // When
        String result = ChatCommandUtils.formatHistoryText(List.of(), false);

        // Then
        assertThat(result).contains("No messages");
    }

    @Test
    void formatHistoryTextFormatsMessagesCorrectly() {
        // Given
        CliMessage message1 = new CliMessage(
                "User question", "USER", LocalDateTime.now(), null
        );
        CliMessage message2 = new CliMessage(
                "Assistant answer", "ASSISTANT", LocalDateTime.now(), null
        );

        // When
        String result = ChatCommandUtils.formatHistoryText(List.of(message1, message2), false);

        // Then
        assertThat(result).contains("Conversation History");
        assertThat(result).contains("User question");
        assertThat(result).contains("Assistant answer");
        assertThat(result).contains("User");
        assertThat(result).contains("Assistant");
    }

    @Test
    void formatHistoryMarkdownFormatsMessagesCorrectly() {
        // Given
        CliMessage message = new CliMessage(
                "User question", "USER", LocalDateTime.now(), null
        );

        // When
        String result = ChatCommandUtils.formatHistoryMarkdown(List.of(message), false);

        // Then
        assertThat(result).contains("# Conversation History");
        assertThat(result).contains("User question");
    }

    @Test
    void formatHistoryJsonFormatsMessagesCorrectly() {
        // Given
        CliMessage message = new CliMessage(
                "User question", "USER", LocalDateTime.now(), null
        );

        // When
        String result = ChatCommandUtils.formatHistoryJson(List.of(message));

        // Then
        assertThat(result).contains("content");
        assertThat(result).contains("User question");
    }
}
