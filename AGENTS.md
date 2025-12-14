# AGENTS.md

## Project Overview
This is a Java backend project intended for production use.
The codebase prioritizes readability, maintainability, and testability.

AI agents interacting with this repository must strictly follow the rules below.

---

## Tech Stack
- Java 25
- Maven or Gradle
- JUnit 5
- Mockito
- AssertJ
- (Spring Boot if present)

---

## Project Structure
- src/main/java
- src/test/java
- build.gradle or pom.xml

Prefer package-by-layer structure (controller, service, repository, model).

---

## Build & Test

Use only the commands defined in this repository.

### Maven
- Build: `mvn clean package`
- Test: `mvn test`

### Gradle
- Build: `./gradlew build`
- Test: `./gradlew test`

Do not invent or assume alternative commands.

---

## Coding Conventions (MANDATORY)

### Naming
- Classes & interfaces: PascalCase, meaningful nouns  
  Example: `OrderService`, `UserRepository`
- Methods: camelCase, verb-based  
  Example: `calculateTotal()`, `findById()`
- Variables: camelCase, descriptive names (no abbreviations)
- Constants: `UPPER_SNAKE_CASE`

---

### Class Design
- One class = one responsibility (SRP)
- Avoid “God classes” (classes > ~300 lines are suspicious)
- Prefer `final` for classes, methods, and fields when possible

---

### Dependency Injection
- Use **constructor injection only**
- Field injection is forbidden

```java
public OrderService(OrderRepository repository) {
    this.repository = repository;
}
```

---

### Immutability
- Prefer immutable objects
- Avoid public setters
- Use `record` for:
  - DTOs
  - Value Objects

---

### Optional Usage
- `Optional` is allowed **only as a return type**
- Never use `Optional` as:
  - fields
  - method parameters

---

### Exception Handling
- Use domain-specific exceptions
- Do not throw generic `RuntimeException`
- Do not use exceptions for normal control flow
- Never swallow exceptions silently

---

### Methods & Readability
- A method should do one thing
- Prefer early returns over nested conditionals
- Keep methods small (ideally < 30 lines)
- Limit parameters (max 3 where possible)

---

### Comments & Javadoc
- Avoid obvious comments
- Comment **why**, not **what**
- Javadoc only for:
  - public APIs
  - non-obvious logic
  - complex business rules

---

## Testing Guidelines (MANDATORY)

- Unit tests are required for business logic
- Follow **Given / When / Then** structure
- Test naming format: `methodNameConditionExpectedResult`
  
Example:
```java
calculateTotalWhenDiscountAppliedReturnsReducedTotal()
```

**Do NOT**:
- test frameworks
- test getters/setters
- mock value objects

**Use**:
- Mockito for dependencies
- AssertJ for assertions

---

## Formatting Rules

- Line length: max 100–120 characters
- Avoid reformatting unrelated code
- Follow existing formatting in the codebase

---

## Security Rules

- Never hardcode secrets
- Never log:
  - passwords
  - tokens
  - personal or sensitive data
- Always validate external input

---

## Lombok (If Present)

**Allowed**:
- `@Getter`
- `@Value`
- `@Builder`
- `@Data` **only for DTOs** (not for JPA Entities)
- `@RequiredArgsConstructor` for constructor injection

**Avoid**:
- `@Data` on JPA Entities (causes issues with lazy loading)
- uncontrolled `@EqualsAndHashCode` on entities with relationships

---

## Restrictions

AI agents **MUST NOT**:
- introduce new frameworks or libraries
- change public APIs without explicit request
- refactor unrelated code
- remove or weaken existing tests
- change architectural style

---

## Commit Guidelines (If Applicable)

- Use conventional commits (`feat`, `fix`, `refactor`, `test`)
- Keep commits focused and minimal
- Do not mix refactors with functional changes

---

## WikiChat-Specific Guidelines

### RAG Service Layer
- **ChunkingService**: always use configurable chunk size from properties
- **EmbeddingService**: batch processing mandatory (max 100 items per batch)
- **VectorSearchService**: always log similarity scores for debugging
- **RAGService**: use structured prompts, avoid string concatenation

### Gemini API Integration
- Wrap all API calls with retry logic (exponential backoff)
- Log token usage for quota monitoring
- Never log response contents (privacy concerns)
- Handle rate limiting with `@Async` and queue mechanism
- Free tier limits: 2M tokens/day, 32K tokens/minute

### Vector Operations
- Always normalize embeddings before saving
- pgvector queries must include similarity threshold (avoid irrelevant results)
- Use `<=>` operator (cosine distance), not `<->` (L2 distance)
- Embedding dimension: 768 for Gemini text-embedding-004

### Document Processing
- Validate content-type before parsing
- Check max file size before loading into memory
- Status transitions: `PROCESSING` → `COMPLETED`/`FAILED` (no intermediate states)
- Always use async processing, never block upload endpoint
- Preserve original filename and metadata in JSONB column

### Database Guidelines
- Use UUID for all primary keys
- Timestamp columns: `created_at`, `updated_at` (use `@CreationTimestamp`, `@UpdateTimestamp`)
- JSONB for flexible metadata storage
- CASCADE DELETE for chunks when document is deleted
- HNSW index on embedding columns (better performance than IVFFlat)

---

## Quick Reference

### Common Patterns

```java
// Constructor Injection
@RequiredArgsConstructor
private final ServiceA serviceA;
private final ServiceB serviceB;

// Transactional
@Transactional(readOnly = true)  // default at class level for read-only services
@Transactional                    // at method level for write operations

// Validation
@Valid @RequestBody ChatRequest request

// Exception Handling
throw new DomainException("Descriptive message", cause);

// Optional Return
public Optional<Document> findById(UUID id) {
    return repository.findById(id);
}

// Async Processing
@Async
public CompletableFuture<Result> processAsync(UUID id) {
    // long-running operation
    return CompletableFuture.completedFuture(result);
}
```

### Pre-Commit Checklist

- [ ] Code compiles without errors
- [ ] `mvn test` passes
- [ ] No hardcoded values (use `@Value` or properties)
- [ ] Logging added for important operations
- [ ] No TODO/FIXME left behind
- [ ] Exception handling is complete
- [ ] No unused imports
- [ ] API keys not hardcoded (use environment variables)
