# Phase 3.2 Implementation - Gemini API Client with Abstraction Layer

## Overview

Phase 3.2 has been successfully completed with a **vendor-agnostic abstraction layer** that allows easy switching between different LLM providers (Gemini, OpenAI, Claude, etc.) without changing business logic code.

## Architecture Decision

Instead of directly coupling the application to Gemini API, we implemented:

1. **Service Interfaces** (`LLMService`, `EmbeddingService`) - Define contracts for LLM operations
2. **Gemini Implementations** (`GeminiLLMService`, `GeminiEmbeddingService`) - Concrete implementations for Gemini
3. **Configuration Layer** (`GeminiConfig`) - Spring beans for LangChain4j models
4. **Custom Exceptions** (`LLMException`, `EmbeddingException`) - Domain-specific error handling

This design follows the **Dependency Inversion Principle** and makes the system highly maintainable and testable.

---

## Implemented Components

### 1. Service Interfaces

#### `LLMService` Interface
Location: `com.example.ao_wiki_chat.service.LLMService`

**Methods**:
- `String generate(String prompt)` - Generate text using configured temperature
- `boolean isHealthy()` - Health check for the LLM service

**Purpose**: Abstracts text generation operations, allowing any LLM provider to be used.

#### `EmbeddingService` Interface
Location: `com.example.ao_wiki_chat.service.EmbeddingService`

**Methods**:
- `float[] generateEmbedding(String text)` - Generate single embedding
- `List<float[]> generateEmbeddings(List<String> texts)` - Batch embedding generation
- `int getEmbeddingDimension()` - Get embedding vector dimension
- `boolean isHealthy()` - Health check for the embedding service

**Purpose**: Abstracts vector embedding operations, supporting different embedding models.

---

### 2. Gemini Implementations

#### `GeminiLLMService`
Location: `com.example.ao_wiki_chat.service.impl.GeminiLLMService`

**Features**:
- Uses LangChain4j's `ChatLanguageModel` for Gemini API integration
- Temperature configured in application.yml (gemini.chat.temperature: 0.7)
- Input validation (null/empty prompt checks)
- Response validation (null/empty response checks)
- Comprehensive error handling with `LLMException`
- Health check via simple ping/pong test
- Detailed logging for debugging and monitoring

**Configuration**:
```yaml
gemini:
  chat:
    model: gemini-2.0-flash-exp
    temperature: 0.7
    max-tokens: 2048
```

#### `GeminiEmbeddingService`
Location: `com.example.ao_wiki_chat.service.impl.GeminiEmbeddingService`

**Features**:
- Uses LangChain4j's `EmbeddingModel` for Gemini text-embedding-004
- Batch processing with max 100 items per batch (respects API limits)
- Input validation for all texts in batch
- Response validation (checks embedding count matches input count)
- Dimension validation (768 for Gemini text-embedding-004)
- Comprehensive error handling with `EmbeddingException`
- Health check with dimension verification
- Detailed logging for batch processing

**Configuration**:
```yaml
gemini:
  embedding:
    model: text-embedding-004
    dimension: 768
```

---

### 3. Configuration Layer

#### `GeminiConfig`
Location: `com.example.ao_wiki_chat.integration.gemini.GeminiConfig`

**Beans**:
- `geminiChatModel` - `ChatLanguageModel` bean for text generation
- `geminiEmbeddingModel` - `EmbeddingModel` bean for embeddings

**Features**:
- Loads configuration from `application.yml`
- Uses `@Qualifier` for bean identification
- Configures timeouts (60s for chat, 30s for embeddings)
- Supports request/response logging for debugging

**Dependencies**:
- `langchain4j-google-ai-gemini` version 0.36.2
- Uses `GoogleAiGeminiChatModel` and `GoogleAiEmbeddingModel`

---

### 4. Custom Exceptions

#### `LLMException`
Location: `com.example.ao_wiki_chat.exception.LLMException`

**Purpose**: Thrown when LLM operations fail (API errors, rate limiting, configuration issues)

#### `EmbeddingException`
Location: `com.example.ao_wiki_chat.exception.EmbeddingException`

**Purpose**: Thrown when embedding generation fails (API errors, validation errors)

Both extend `RuntimeException` for unchecked exception handling.

---

## Testing

### Unit Tests Coverage

All components have comprehensive unit tests with high coverage:

1. **`LLMExceptionTest`** - Exception construction and inheritance
2. **`EmbeddingExceptionTest`** - Exception construction and inheritance
3. **`GeminiConfigTest`** - Bean creation validation
4. **`GeminiLLMServiceTest`** - 12 test cases covering:
   - Successful text generation
   - Null/empty prompt validation
   - Null/empty response handling
   - API error handling
   - Health check scenarios
5. **`GeminiEmbeddingServiceTest`** - 15 test cases covering:
   - Single and batch embedding generation
   - Null/empty text validation
   - Batch size handling
   - Response validation (count, dimension)
   - API error handling
   - Health check scenarios

### Test Strategy

- **Mocking**: Uses Mockito to mock LangChain4j models
- **Assertions**: Uses AssertJ for fluent assertions
- **Naming**: Follows `methodNameConditionExpectedResult` convention
- **Structure**: Given/When/Then pattern

---

## How to Switch LLM Providers

### Adding OpenAI Support (Example)

1. **Add dependency** to `pom.xml`:
```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

2. **Create implementations**:
```java
@Service
@Profile("openai")
public class OpenAILLMService implements LLMService {
    // Implementation using OpenAI
}

@Service
@Profile("openai")
public class OpenAIEmbeddingService implements EmbeddingService {
    // Implementation using OpenAI
}
```

3. **Create configuration**:
```java
@Configuration
@Profile("openai")
public class OpenAIConfig {
    @Bean("openaiChatModel")
    public ChatLanguageModel openaiChatModel() {
        // OpenAI configuration
    }
}
```

4. **Switch provider** via `application.yml`:
```yaml
spring:
  profiles:
    active: openai  # or 'gemini'
```

### No Changes Required In:
- Business logic services (RAGService, VectorSearchService, etc.)
- Controllers
- DTOs
- Repositories
- Tests (just mock the interfaces)

---

## Usage Example

```java
@Service
public class RAGService {
    
    private final LLMService llmService;  // Interface, not implementation
    private final EmbeddingService embeddingService;
    
    public RAGService(LLMService llmService, EmbeddingService embeddingService) {
        this.llmService = llmService;
        this.embeddingService = embeddingService;
    }
    
    public ChatResponse processQuery(String query) {
        // Generate query embedding
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        
        // ... vector search logic ...
        
        // Generate response with LLM
        String prompt = buildPrompt(context, query);
        String answer = llmService.generate(prompt);
        
        return new ChatResponse(answer, sources);
    }
}
```

The `RAGService` depends only on interfaces, making it completely independent of the LLM provider.

---

## Configuration Reference

### Environment Variables

```bash
# Required
export GEMINI_API_KEY=your_api_key_here

# Optional (already configured in application.yml)
export GEMINI_CHAT_MODEL=gemini-2.0-flash-exp
export GEMINI_EMBEDDING_MODEL=text-embedding-004
```

### Application Properties

```yaml
gemini:
  api:
    key: ${GEMINI_API_KEY}
  chat:
    model: gemini-2.0-flash-exp
    temperature: 0.7
    max-tokens: 2048
  embedding:
    model: text-embedding-004
    dimension: 768
  rate-limit:
    requests-per-minute: 50
    tokens-per-day: 2000000
```

---

## Key Design Decisions

### Why Interfaces?
- **Flexibility**: Easy to switch between LLM providers
- **Testability**: Mock interfaces in unit tests
- **Maintainability**: Changes to one provider don't affect others
- **SOLID Principles**: Follows Dependency Inversion Principle

### Why LangChain4j?
- **Abstraction**: Provides unified interface for multiple LLM providers
- **Features**: Built-in retry logic, streaming, memory management
- **Community**: Active development and good documentation
- **Java-native**: Better integration than Python-based alternatives

### Why Batch Processing?
- **Efficiency**: Reduces API calls for multiple embeddings
- **Rate Limiting**: Respects Gemini API limits (50 req/min)
- **Performance**: Faster than sequential single embeddings

---

## Next Steps (Phase 4)

With Phase 3.2 complete, the next phase will implement:

1. **ChunkingService** - Split documents into chunks with overlap
2. **VectorSearchService** - Semantic search using pgvector
3. **DocumentService** - Document upload and processing pipeline
4. **RAGService** - Orchestrate the complete RAG flow

All these services will use the `LLMService` and `EmbeddingService` interfaces, remaining independent of the LLM provider.

---

## Files Created

### Source Files
- `src/main/java/com/example/ao_wiki_chat/service/LLMService.java`
- `src/main/java/com/example/ao_wiki_chat/service/EmbeddingService.java`
- `src/main/java/com/example/ao_wiki_chat/service/impl/GeminiLLMService.java`
- `src/main/java/com/example/ao_wiki_chat/service/impl/GeminiEmbeddingService.java`
- `src/main/java/com/example/ao_wiki_chat/integration/gemini/GeminiConfig.java`
- `src/main/java/com/example/ao_wiki_chat/exception/LLMException.java`
- `src/main/java/com/example/ao_wiki_chat/exception/EmbeddingException.java`

### Test Files
- `src/test/java/com/example/ao_wiki_chat/service/impl/GeminiLLMServiceTest.java`
- `src/test/java/com/example/ao_wiki_chat/service/impl/GeminiEmbeddingServiceTest.java`
- `src/test/java/com/example/ao_wiki_chat/integration/gemini/GeminiConfigTest.java`
- `src/test/java/com/example/ao_wiki_chat/exception/LLMExceptionTest.java`
- `src/test/java/com/example/ao_wiki_chat/exception/EmbeddingExceptionTest.java`

### Documentation
- `docs/PHASE_3_2_IMPLEMENTATION.md` (this file)

---

## Compliance with Project Guidelines

✅ **All code and comments in English** (AGENTS.md requirement)  
✅ **Constructor injection only** (no field injection)  
✅ **SLF4J logging** (not Lombok @Slf4j due to Java 25)  
✅ **Comprehensive unit tests** (27 test cases total)  
✅ **Proper exception handling** (domain-specific exceptions)  
✅ **Documentation updated** (README.md, DEVELOPMENT_PLAN.md)  
✅ **Interface-based design** (easy provider switching)  
✅ **No hardcoded values** (all configuration in application.yml)  

---

**Phase 3.2 Status**: ✅ **COMPLETED**  
**Date**: December 2025  
**Version**: 0.6.0

