# Chunking Logic - WikiChat RAG System

## Overview

The `ChunkingService` implements a **Hybrid Semantic + Sliding Window** strategy to split documents into chunks optimised for embedding and vector search. The goal is to produce text fragments that:

1. Preserve semantic meaning (no mid-sentence splits)
2. Have uniform size (~125 tokens / 500 characters)
3. Keep context at boundaries (10% overlap)
4. Are suitable for embedding with Gemini

---

## Chunking Pipeline

```
Input Text (full document)
    ↓
[1] Preprocessing
    ↓
[2] Semantic Split (paragraphs → sentences → words)
    ↓
[3] Overlap Application
    ↓
[4] Small Chunk Filtering
    ↓
Output: List<String> chunks
```

---

## 1. Preprocessing

### Operations

```java
// Normalise line endings
text = text.replace("\r\n", "\n").replace("\r", "\n");

// Remove control characters (except \n and \t)
text = text.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

// Trim leading/trailing whitespace
text = text.trim();
```

### Purpose

- **Consistency**: all documents use the same format
- **Cleanup**: removes hidden/invisible characters
- **Structure preservation**: keeps newlines and tabs for semantics

### Example

**Before**:
```
"  Text with\r\nwindows endings\u0000  "
```

**After**:
```
"Text with\nwindows endings"
```

---

## 2. Semantic Split (Cascading Strategy)

### 3-Level Algorithm

```
Level 1: PARAGRAPHS (\n\n)
    ↓ if paragraph > chunkSize
Level 2: SENTENCES (. ! ?)
    ↓ if sentence > chunkSize
Level 3: WORDS (whitespace)
    ↓ if word > chunkSize (rare)
Level 4: CHARACTERS (forced)
```

### Level 1: Split on Paragraphs

**Delimiter**: `\n\n+` (double newline or more)

```java
String[] paragraphs = text.split("\n\n+");
```

**Logic**:
- Try to accumulate paragraphs into one chunk
- If adding the next paragraph exceeds `chunkSize`, save the current chunk
- If a single paragraph is too large → go to Level 2

**Example**:

```
Input (chunkSize=100):
"""
Paragraph 1: short text here.

Paragraph 2: more short text.

Paragraph 3: final text.
"""

Output:
Chunk 1: "Paragraph 1: short text here.\n\nParagraph 2: more short text."
Chunk 2: "Paragraph 3: final text."
```

---

### Level 2: Split on Sentences

**Delimiter**: `(?<=[.!?])\\s+` (regex for end of sentence)

Used when: `paragraph.length() > chunkSize`

```java
String[] sentences = SENTENCE_PATTERN.split(paragraph);
```

**Logic**:
- Accumulate sentences until the chunk is full
- Preserve punctuation and structure
- If a sentence is too large → go to Level 3

**Example**:

```
Input (chunkSize=50):
"First very long sentence here. Second sentence. Third sentence with text."

Output:
Chunk 1: "First very long sentence here. Second sentence."
Chunk 2: "Third sentence with text."
```

---

### Level 3: Split on Words

**Delimiter**: `\\s+` (whitespace)

Used when: `sentence.length() > chunkSize`

```java
String[] words = text.split("\\s+");
```

**Logic**:
- Forced split on word boundaries to respect `chunkSize`
- Fallback for text without punctuation
- Preserves word integrity

**Example**:

```
Input (chunkSize=30):
"word1 word2 word3 word4 word5 word6"

Output:
Chunk 1: "word1 word2 word3"
Chunk 2: "word4 word5 word6"
```

---

### Level 4: Character Split (Edge Case)

Used when: `word.length() > chunkSize` (very rare)

**Example**:
```
Input (chunkSize=10):
"supercalifragilisticexpialidocious"

Output:
Chunk 1: "supercalif"
Chunk 2: "ragilistic"
Chunk 3: "expialidoc"
Chunk 4: "ious"
```

---

## 3. Overlap Application

### Sliding Window

After the semantic split, overlap is applied between consecutive chunks:

```
Chunk 1: [A B C D E F G H]
                    ↓ overlap (last N characters)
Chunk 2:         [F G H | I J K L M]
                          ↓ overlap
Chunk 3:                 [L M | N O P Q R]
```

### Implementation

```java
for (int i = 1; i < chunks.size(); i++) {
    String previousChunk = chunks.get(i - 1);
    String currentChunk = chunks.get(i);
    
    // Extract last N characters of previous chunk
    String overlapText = getLastNCharacters(previousChunk, overlap);
    
    // Prepend to current chunk
    currentChunk = overlapText + "\n" + currentChunk;
}
```

### Smart Overlap

The `getLastNCharacters()` function is smart:

```java
// Look for first space to avoid cutting words
String substring = text.substring(text.length() - n);
int firstSpace = substring.indexOf(' ');

if (firstSpace > 0 && firstSpace < n / 2) {
    // Skip to first space so we don't start mid-word
    return substring.substring(firstSpace + 1);
}
```

### Why Overlap?

1. **Captures boundary info**: if a sentence is split across 2 chunks, overlap recovers it
2. **Better retrieval**: higher chance of finding the right chunk
3. **Context continuity**: the model has more context to answer

**Practical Example**:

```
Without overlap:
Chunk 1: "...the main cause is the"
Chunk 2: "processor overheating..."

Query: "What is the cause of overheating?"
→ Can only match Chunk 2 (incomplete)

With overlap (15%):
Chunk 1: "...the main cause is the"
Chunk 2: "main cause is the processor overheating..."

Query: "What is the cause of overheating?"
→ Matches Chunk 2 with full context! ✅
```

---

## 4. Small Chunk Filtering

### Constant

```java
private static final int MIN_CHUNK_SIZE = 50;
```

### Logic

Removes chunks that are too small to provide useful context:

```java
List<String> filtered = chunks.stream()
    .filter(chunk -> chunk.length() >= MIN_CHUNK_SIZE)
    .toList();
```

### Why?

- 10–20 character chunks are poor for embedding
- They waste resources (API calls, storage)
- They reduce retrieval quality (noise)

**Example**:

```
Before filtering:
["Complete paragraph here.", "x", "Another good chunk.", "ok"]

After filtering:
["Complete paragraph here.", "Another good chunk."]
```

---

## Configuration Parameters

### application.yml

```yaml
rag:
  chunk:
    size: 500       # ~125 tokens (500 / 4 char/token)
    overlap: 50     # 10% overlap (50 / 500)
```

### Why 500 characters?

- **Token estimate**: ~4 characters/token → 500 char ≈ 125 tokens
- **Context window**: smaller chunks for more precise retrieval
- **Gemini embedding**: good size for fine granularity with gemini-embedding-001
- **Balance**: 100–200 tokens allow accurate retrieval without losing context

### Why 10% overlap?

- **Balance**: captures boundaries without too much duplication
- **Storage**: avoids blowing up the DB
- **Retrieval quality**: improves accuracy without overload
- **Best practice**: 10–15% is standard in RAG systems

---

## Complete Examples

### Example 1: Markdown Document

**Input**:
```markdown
# Introduction to WikiChat

WikiChat is a RAG system for document Q&A.

## Features

- Document parsing (MD, HTML, PDF)
- Vector embeddings with Gemini
- Semantic search with pgvector

## Installation

Run the following commands:
1. docker-compose up
2. mvn spring-boot:run
```

**Config**: chunkSize=150, overlap=20

**Output Chunks**:

```
Chunk 1 (106 chars):
"Introduction to WikiChat

WikiChat is a RAG system for document Q&A.

Features

- Document parsing"

Chunk 2 (131 chars):
"ument parsing (MD, HTML, PDF)
- Vector embeddings with Gemini
- Semantic search with pgvector

Installation

Run the following"

Chunk 3 (75 chars):
"ng commands:
1. docker-compose up
2. mvn spring-boot:run"
```

**Notes**:
- Split on headers (`##`)
- Lists kept together
- Overlap preserves context

---

### Example 2: Narrative Text

**Input**:
```
In the year 1895, Wilhelm Röntgen discovered X-rays by accident. 
He was experimenting with cathode ray tubes when he noticed a 
fluorescent glow. This discovery revolutionized medical imaging.

The impact was immediate. Within months, X-rays were being used 
for medical diagnosis worldwide. Röntgen refused to patent his 
discovery, believing it should benefit all humanity.
```

**Config**: chunkSize=120, overlap=15

**Output Chunks**:

```
Chunk 1 (118 chars):
"In the year 1895, Wilhelm Röntgen discovered X-rays by accident. 
He was experimenting with cathode ray tubes when"

Chunk 2 (135 chars):
"ode ray tubes when he noticed a fluorescent glow. This discovery 
revolutionized medical imaging.

The impact was immediate."

Chunk 3 (122 chars):
"ediate. Within months, X-rays were being used for medical diagnosis 
worldwide. Röntgen refused to patent his discovery,"
```

**Notes**:
- Split on sentences (`. `)
- Overlap captures context
- Unicode (ö) preserved

---

### Example 3: Edge Case – Text Without Punctuation

**Input**:
```
word1 word2 word3 word4 word5 word6 word7 word8 word9 word10 
word11 word12 word13 word14 word15
```

**Config**: chunkSize=40, overlap=5

**Output Chunks**:

```
Chunk 1: "word1 word2 word3 word4 word5 word6"
Chunk 2: "word6 word7 word8 word9 word10"
Chunk 3: "word10 word11 word12 word13 word14"
Chunk 4: "word14 word15"
```

**Notes**:
- Fallback to word splitting
- Overlap applied
- No data lost

---

## Decision Diagram

```
┌─────────────────────────────────┐
│  Input Text                     │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│  text.length <= chunkSize?      │
└────────────┬────────────────────┘
      YES ───┘  NO
       │         │
       │         ▼
       │    ┌─────────────────────┐
       │    │ Split on \n\n       │
       │    │ (paragraphs)         │
       │    └─────────┬────────────┘
       │              │
       │              ▼
       │    ┌─────────────────────┐
       │    │ paragraph > size?    │
       │    └─────────┬────────────┘
       │         YES ─┤  NO
       │              │   │
       │              │   └─→ Accumulate
       │              │        paragraphs
       │              ▼
       │    ┌─────────────────────┐
       │    │ Split on [.!?]      │
       │    │ (sentences)          │
       │    └─────────┬────────────┘
       │              │
       │              ▼
       │    ┌─────────────────────┐
       │    │ sentence > size?     │
       │    └─────────┬────────────┘
       │         YES ─┤  NO
       │              │   │
       │              │   └─→ Accumulate
       │              │        sentences
       │              ▼
       │    ┌─────────────────────┐
       │    │ Split on whitespace  │
       │    │ (words)              │
       │    └─────────┬────────────┘
       │              │
       └──────────────┴────────────┐
                                   │
                                   ▼
                      ┌────────────────────┐
                      │ Apply Overlap      │
                      └────────┬───────────┘
                               │
                               ▼
                      ┌────────────────────┐
                      │ Filter < 50 chars  │
                      └────────┬───────────┘
                               │
                               ▼
                      ┌────────────────────┐
                      │  Output Chunks     │
                      └────────────────────┘
```

---

## Performance Considerations

### Time Complexity

- **Average case**: O(n) where n = text length
  - Regex split: O(n)
  - Chunk accumulation: O(n)
  - Apply overlap: O(k) where k = number of chunks

- **Worst case**: O(n²) for pathological text without whitespace

### Space Complexity

- O(n + k×m) where:
  - n = original text
  - k = number of chunks
  - m = average chunk size

### Optimisations

1. **StringBuilder**: avoids inefficient String concatenation
2. **Precompiled regex**: `SENTENCE_PATTERN` is a constant
3. **Early return**: short text → 1 chunk without processing
4. **Stream filtering**: lazy operation

---

## Testing Strategy

### Test Coverage

`ChunkingServiceImplTest` covers:

- Basic functionality (26 tests)
- Parameter validation
- Semantic splitting
- Overlap behaviour
- Edge cases (empty, single word, unicode)
- Realistic documents

### Test Examples

```java
@Test
void splitPreservesParagraphBoundaries() {
    String text = "Para 1.\n\nPara 2.\n\nPara 3.";
    List<String> chunks = service.splitIntoChunks(text, 80, 0);
    
    // Ensure paragraphs are not mixed
    for (String chunk : chunks) {
        assertThat(chunk.split("\n\n").length).isLessThanOrEqualTo(2);
    }
}

@Test
void overlapIncludesPreviousContent() {
    String text = "AAAAA BBBBB CCCCC DDDDD...";
    List<String> chunks = service.splitIntoChunks(text, 80, 15);
    
    // Verify overlap between consecutive chunks
    if (chunks.size() > 1) {
        String prev = chunks.get(0);
        String curr = chunks.get(1);
        // curr should contain end portion of prev
    }
}
```

---

## Best Practices

### Do's

1. Use default parameters from `application.yml` when possible
2. Monitor chunk distribution: check that sizes are uniform
3. Log chunk metrics: count, average size, min/max
4. Validate input: ensure text is not null/empty before calling
5. Test with real documents: various MD, HTML, PDF

### Don'ts

1. Don't use chunks that are too small (<100 char) – little context
2. Don't use chunks that are too large (>5000 char) – poor embedding
3. Don't set overlap ≥ chunkSize – full duplication
4. Don't ignore log warnings (filtered chunks, unusual sizes)
5. Don't assume UTF-8: the service handles unicode correctly

---

## Future Enhancements

### Possible Improvements

1. **Document-aware splitting**:
   - Markdown: split on headers (`#`, `##`)
   - HTML: split on `<section>`, `<article>`
   - PDF: respect page boundaries

2. **Smart overlap**:
   - Dynamic overlap by content type
   - Higher overlap for technical text
   - Lower overlap for narrative

3. **Token counting**:
   - Integrate a token-counting library (e.g. jtokkit)
   - Split on real tokens instead of characters

4. **Caching**:
   - Cache chunks for identical documents
   - Avoid re-chunking on duplicate uploads

5. **Metrics**:
   - Chunk size histogram
   - Semantic boundary distribution
   - Overlap effectiveness score

---

## Troubleshooting

### Issue: Too many small chunks filtered

**Symptom**: Log shows "Filtered out N small chunks"

**Cause**: `chunkSize` too small or document with very short paragraphs

**Solution**:
```yaml
rag:
  chunk:
    size: 1000  # Increase from 500 to 1000 if needed
```

---

### Issue: Chunks contain cut-off text

**Symptom**: Chunks start/end mid-word

**Cause**: Document without punctuation/whitespace (rare)

**Solution**: Confirm that `getLastNCharacters()` looks for word boundaries

---

### Issue: Too many chunks per document

**Symptom**: 1000+ chunks for a 50-page document

**Cause**: `chunkSize` too small

**Solution**:
```yaml
rag:
  chunk:
    size: 1000  # Increase chunk size if too many fragments
```

---

## References

- **LangChain Chunking**: https://python.langchain.com/docs/modules/data_connection/document_transformers/
- **RAG Best Practices**: https://www.pinecone.io/learn/chunking-strategies/
- **Semantic Search**: https://www.sbert.net/examples/applications/semantic-search/README.html

---

**Last Updated**: December 2025  
**Version**: 1.0  
**Author**: WikiChat Development Team
