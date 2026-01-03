package com.example.ao_wiki_chat.unit.config.hibernate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.ao_wiki_chat.config.hibernate.VectorAttributeConverter;

/**
 * Unit tests for VectorAttributeConverter.
 * Tests conversion between float[] (Java) and vector string representation (PostgreSQL).
 */
class VectorAttributeConverterTest {

    private VectorAttributeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new VectorAttributeConverter();
    }

    // ========== convertToDatabaseColumn Tests ==========

    @Test
    void convertToDatabaseColumnWhenValidArrayReturnsCorrectString() {
        // Given
        float[] embedding = {1.0f, 2.5f, 3.14f};

        // When
        String result = converter.convertToDatabaseColumn(embedding);

        // Then
        assertThat(result).isEqualTo("[1.0,2.5,3.14]");
    }

    @Test
    void convertToDatabaseColumnWhenSingleElementReturnsCorrectString() {
        // Given
        float[] embedding = {42.0f};

        // When
        String result = converter.convertToDatabaseColumn(embedding);

        // Then
        assertThat(result).isEqualTo("[42.0]");
    }

    @Test
    void convertToDatabaseColumnWhenEmptyArrayReturnsEmptyBrackets() {
        // Given
        float[] embedding = {};

        // When
        String result = converter.convertToDatabaseColumn(embedding);

        // Then
        assertThat(result).isEqualTo("[]");
    }

    @Test
    void convertToDatabaseColumnWhenNullReturnsNull() {
        // Given
        float[] embedding = null;

        // When
        String result = converter.convertToDatabaseColumn(embedding);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void convertToDatabaseColumnWhenNegativeValuesReturnsCorrectString() {
        // Given
        float[] embedding = {-1.5f, 0.0f, -3.14f};

        // When
        String result = converter.convertToDatabaseColumn(embedding);

        // Then
        assertThat(result).isEqualTo("[-1.5,0.0,-3.14]");
    }

    @Test
    void convertToDatabaseColumnWhen768DimensionsReturnsCorrectString() {
        // Given: Simulate Gemini text-embedding-004 dimensions
        float[] embedding = new float[768];
        for (int i = 0; i < 768; i++) {
            embedding[i] = i * 0.1f;
        }

        // When
        String result = converter.convertToDatabaseColumn(embedding);

        // Then
        assertThat(result)
            .startsWith("[0.0,0.1,0.2")
            .endsWith("]")
            .contains("76.6")
            .contains("76.7"); // Check values are present (float precision may vary)
    }

    // ========== convertToEntityAttribute Tests ==========

    @Test
    void convertToEntityAttributeWhenValidStringReturnsCorrectArray() {
        // Given
        String dbData = "[1.0,2.5,3.14]";

        // When
        float[] result = converter.convertToEntityAttribute(dbData);

        // Then
        assertThat(result)
            .hasSize(3)
            .containsExactly(1.0f, 2.5f, 3.14f);
    }

    @Test
    void convertToEntityAttributeWhenSingleElementReturnsCorrectArray() {
        // Given
        String dbData = "[42.0]";

        // When
        float[] result = converter.convertToEntityAttribute(dbData);

        // Then
        assertThat(result)
            .hasSize(1)
            .containsExactly(42.0f);
    }

    @Test
    void convertToEntityAttributeWhenEmptyBracketsReturnsEmptyArray() {
        // Given
        String dbData = "[]";

        // When
        float[] result = converter.convertToEntityAttribute(dbData);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void convertToEntityAttributeWhenNullReturnsNull() {
        // Given
        String dbData = null;

        // When
        float[] result = converter.convertToEntityAttribute(dbData);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void convertToEntityAttributeWhenEmptyStringReturnsNull() {
        // Given
        String dbData = "";

        // When
        float[] result = converter.convertToEntityAttribute(dbData);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void convertToEntityAttributeWhenNegativeValuesReturnsCorrectArray() {
        // Given
        String dbData = "[-1.5,0.0,-3.14]";

        // When
        float[] result = converter.convertToEntityAttribute(dbData);

        // Then
        assertThat(result)
            .hasSize(3)
            .containsExactly(-1.5f, 0.0f, -3.14f);
    }

    @Test
    void convertToEntityAttributeWhenWhitespaceReturnsCorrectArray() {
        // Given: PostgreSQL might add whitespace
        String dbData = "[1.0, 2.5, 3.14]";

        // When
        float[] result = converter.convertToEntityAttribute(dbData);

        // Then
        assertThat(result)
            .hasSize(3)
            .containsExactly(1.0f, 2.5f, 3.14f);
    }

    @Test
    void convertToEntityAttributeWhen768DimensionsReturnsCorrectArray() {
        // Given: Build a 768-dimensional vector string
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 768; i++) {
            if (i > 0) sb.append(",");
            sb.append(i * 0.1f);
        }
        sb.append("]");
        String dbData = sb.toString();

        // When
        float[] result = converter.convertToEntityAttribute(dbData);

        // Then
        assertThat(result).hasSize(768);
        assertThat(result[0]).isEqualTo(0.0f);
        assertThat(result[767]).isCloseTo(76.7f, within(0.001f)); // Float precision tolerance
    }

    // ========== Error Handling Tests ==========

    @Test
    void convertToEntityAttributeWhenInvalidFormatThrowsException() {
        // Given
        String dbData = "not-a-vector";

        // When / Then
        assertThatThrownBy(() -> converter.convertToEntityAttribute(dbData))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert database vector to float array");
    }

    @Test
    void convertToEntityAttributeWhenInvalidNumberFormatThrowsException() {
        // Given
        String dbData = "[1.0,invalid,3.0]";

        // When / Then
        assertThatThrownBy(() -> converter.convertToEntityAttribute(dbData))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot convert database vector to float array");
    }

    // ========== Round-trip Tests ==========

    @Test
    void roundTripConversionPreservesData() {
        // Given
        float[] original = {1.0f, 2.5f, 3.14f, -0.5f, 0.0f};

        // When: Convert to DB and back
        String dbString = converter.convertToDatabaseColumn(original);
        float[] roundTrip = converter.convertToEntityAttribute(dbString);

        // Then
        assertThat(roundTrip).containsExactly(original);
    }

    @Test
    void roundTripConversionWithLargeDimensionsPreservesData() {
        // Given: 768-dimensional vector
        float[] original = new float[768];
        for (int i = 0; i < 768; i++) {
            original[i] = (float) Math.random();
        }

        // When: Convert to DB and back
        String dbString = converter.convertToDatabaseColumn(original);
        float[] roundTrip = converter.convertToEntityAttribute(dbString);

        // Then
        assertThat(roundTrip).containsExactly(original);
    }
}

