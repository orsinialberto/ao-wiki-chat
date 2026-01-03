package com.example.ao_wiki_chat.config.hibernate;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.postgresql.util.PGobject;

/**
 * Custom Hibernate type for PostgreSQL vector type.
 * Handles conversion between Java float[] and PostgreSQL vector type.
 * 
 * The vector is stored as a string in the format: "[1.0,2.0,3.0,...]"
 * which is the standard pgvector text representation.
 */
public class VectorType implements JdbcType {

    @Override
    public int getJdbcTypeCode() {
        return Types.OTHER;
    }

    @Override
    public int getDefaultSqlTypeCode() {
        return SqlTypes.OTHER;
    }

    @Override
    public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
        return new BasicBinder<>(javaType, this) {
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
                    throws SQLException {
                if (value == null) {
                    st.setNull(index, Types.OTHER);
                } else {
                    float[] embedding = (float[]) value;
                    String vectorString = convertToVectorString(embedding);
                    // Use PGobject to explicitly set the type as 'vector'
                    // This tells PostgreSQL to treat the value as a vector type
                    PGobject pgObject = new PGobject();
                    pgObject.setType("vector");
                    pgObject.setValue(vectorString);
                    st.setObject(index, pgObject, Types.OTHER);
                }
            }

            @Override
            protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
                    throws SQLException {
                if (value == null) {
                    st.setNull(name, Types.OTHER);
                } else {
                    float[] embedding = (float[]) value;
                    String vectorString = convertToVectorString(embedding);
                    PGobject pgObject = new PGobject();
                    pgObject.setType("vector");
                    pgObject.setValue(vectorString);
                    st.setObject(name, pgObject, Types.OTHER);
                }
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
        return new BasicExtractor<>(javaType, this) {
            @Override
            protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
                String vectorString = rs.getString(paramIndex);
                if (vectorString == null) {
                    return null;
                }
                float[] embedding = convertFromVectorString(vectorString);
                @SuppressWarnings("unchecked")
                X result = (X) embedding;
                return result;
            }

            @Override
            protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                String vectorString = statement.getString(index);
                if (vectorString == null) {
                    return null;
                }
                float[] embedding = convertFromVectorString(vectorString);
                @SuppressWarnings("unchecked")
                X result = (X) embedding;
                return result;
            }

            @Override
            protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
                String vectorString = statement.getString(name);
                if (vectorString == null) {
                    return null;
                }
                float[] embedding = convertFromVectorString(vectorString);
                @SuppressWarnings("unchecked")
                X result = (X) embedding;
                return result;
            }
        };
    }

    private String convertToVectorString(float[] embedding) {
        if (embedding == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    private float[] convertFromVectorString(String vectorString) {
        if (vectorString == null || vectorString.isEmpty()) {
            return null;
        }
        // Remove brackets and split by comma
        String cleaned = vectorString.substring(1, vectorString.length() - 1);
        if (cleaned.isEmpty()) {
            return new float[0];
        }
        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}

