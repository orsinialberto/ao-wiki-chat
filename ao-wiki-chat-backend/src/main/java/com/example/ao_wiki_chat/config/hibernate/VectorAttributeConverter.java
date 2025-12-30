package com.example.ao_wiki_chat.config.hibernate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for mapping PostgreSQL vector type to Java float array.
 * Handles conversion between float[] (Java) and vector string representation.
 * 
 * The vector is stored as a string in the format: "[1.0,2.0,3.0,...]"
 * which is the standard pgvector text representation.
 * 
 * Used explicitly via @Convert annotation on embedding fields.
 */
@Converter
public class VectorAttributeConverter implements AttributeConverter<float[], String> {

    private static final Logger log = LoggerFactory.getLogger(VectorAttributeConverter.class);

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        
        try {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < attribute.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(attribute[i]);
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to convert float[] to vector string: {}", e.getMessage());
            throw new IllegalArgumentException("Cannot convert embedding to database format", e);
        }
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        
        try {
            // Remove brackets and split by comma
            String cleaned = dbData.substring(1, dbData.length() - 1);
            if (cleaned.isEmpty()) {
                return new float[0];
            }
            
            String[] parts = cleaned.split(",");
            float[] result = new float[parts.length];
            
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            
            return result;
        } catch (Exception e) {
            log.error("Failed to convert vector string to float[]: {}", e.getMessage());
            throw new IllegalArgumentException("Cannot convert database vector to float array", e);
        }
    }
}

