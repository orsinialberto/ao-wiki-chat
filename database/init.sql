-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- DOCUMENTS TABLE
-- ============================================
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED')),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for status queries
CREATE INDEX idx_documents_status ON documents(status);

-- Index for timestamp queries
CREATE INDEX idx_documents_created_at ON documents(created_at DESC);

-- ============================================
-- CHUNKS TABLE
-- ============================================
CREATE TABLE chunks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding vector(768),  -- Gemini gemini-embedding-001 uses 768 dimensions (via Matryoshka scaling)
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key with cascade delete
    CONSTRAINT fk_chunks_document 
        FOREIGN KEY (document_id) 
        REFERENCES documents(id) 
        ON DELETE CASCADE
);

-- Index for document lookup
CREATE INDEX idx_chunks_document_id ON chunks(document_id);

-- Index for chunk ordering
CREATE INDEX idx_chunks_document_chunk ON chunks(document_id, chunk_index);

-- HNSW index for vector similarity search (better performance than IVFFlat)
-- Using cosine distance operator (<=>)
CREATE INDEX idx_chunks_embedding ON chunks 
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- ============================================
-- CONVERSATIONS TABLE
-- ============================================
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(500),
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for session lookup
CREATE INDEX idx_conversations_session_id ON conversations(session_id);

-- Index for timestamp queries
CREATE INDEX idx_conversations_created_at ON conversations(created_at DESC);

-- ============================================
-- MESSAGES TABLE
-- ============================================
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    conversation_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content TEXT NOT NULL,
    sources JSONB,  -- Array of source references with document names, chunks, similarity scores
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key with cascade delete
    CONSTRAINT fk_messages_conversation 
        FOREIGN KEY (conversation_id) 
        REFERENCES conversations(id) 
        ON DELETE CASCADE
);

-- Index for conversation lookup
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);

-- Index for chronological ordering
CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at);

-- ============================================
-- TRIGGER FOR UPDATED_AT
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply trigger to documents table
CREATE TRIGGER update_documents_updated_at 
    BEFORE UPDATE ON documents 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Apply trigger to conversations table
CREATE TRIGGER update_conversations_updated_at 
    BEFORE UPDATE ON conversations 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- VERIFICATION QUERIES (for debugging)
-- ============================================
-- Uncomment to verify installation:
-- SELECT version();
-- SELECT * FROM pg_extension WHERE extname = 'vector';
-- \d documents
-- \d chunks
-- \d conversations
-- \d messages

