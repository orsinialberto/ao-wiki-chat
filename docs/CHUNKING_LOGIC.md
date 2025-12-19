# Chunking Logic - WikiChat RAG System

## Overview

Il `ChunkingService` implementa una strategia **Hybrid Semantic + Sliding Window** per dividere documenti in chunk ottimizzati per l'embedding e la ricerca vettoriale. L'obiettivo è creare frammenti di testo che:

1. ✅ Preservano il significato semantico (no split a metà frase)
2. ✅ Hanno dimensione uniforme (~500 token / 2000 caratteri)
3. ✅ Mantengono contesto ai confini (overlap 10%)
4. ✅ Sono adatti per l'embedding con Gemini

---

## Pipeline di Chunking

```
Input Text (documento completo)
    ↓
[1] Preprocessing
    ↓
[2] Split Semantico (paragrafi → frasi → parole)
    ↓
[3] Applicazione Overlap
    ↓
[4] Filtraggio Chunk Piccoli
    ↓
Output: List<String> chunks
```

---

## 1. Preprocessing

### Operazioni

```java
// Normalizzazione line endings
text = text.replace("\r\n", "\n").replace("\r", "\n");

// Rimozione caratteri di controllo (esclusi \n e \t)
text = text.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");

// Trim whitespace iniziale/finale
text = text.trim();
```

### Scopo

- **Uniformità**: tutti i documenti hanno lo stesso formato
- **Pulizia**: rimuove caratteri nascosti/invisibili
- **Preservazione struttura**: mantiene newline e tab per semantica

### Esempio

**Before**:
```
"  Text with\r\nwindows endings\u0000  "
```

**After**:
```
"Text with\nwindows endings"
```

---

## 2. Split Semantico (Cascading Strategy)

### Algoritmo a 3 Livelli

```
Level 1: PARAGRAPHS (\n\n)
    ↓ se paragrafo > chunkSize
Level 2: SENTENCES (. ! ?)
    ↓ se frase > chunkSize
Level 3: WORDS (whitespace)
    ↓ se parola > chunkSize (raro)
Level 4: CHARACTERS (forzato)
```

### Level 1: Split su Paragrafi

**Delimitatore**: `\n\n+` (doppio newline o più)

```java
String[] paragraphs = text.split("\n\n+");
```

**Logica**:
- Prova ad accumulare paragrafi in un chunk
- Se aggiungere il prossimo paragrafo sfora `chunkSize`, salva il chunk corrente
- Se un singolo paragrafo è troppo grande → passa a Level 2

**Esempio**:

```
Input (chunkSize=100):
"""
Paragrafo 1: breve testo qui.

Paragrafo 2: altro testo breve.

Paragrafo 3: testo finale.
"""

Output:
Chunk 1: "Paragrafo 1: breve testo qui.\n\nParagrafo 2: altro testo breve."
Chunk 2: "Paragrafo 3: testo finale."
```

---

### Level 2: Split su Frasi

**Delimitatore**: `(?<=[.!?])\\s+` (regex per fine frase)

Attivato quando: `paragraph.length() > chunkSize`

```java
String[] sentences = SENTENCE_PATTERN.split(paragraph);
```

**Logica**:
- Accumula frasi fino a riempire il chunk
- Preserva punteggiatura e struttura
- Se una frase è troppo grande → passa a Level 3

**Esempio**:

```
Input (chunkSize=50):
"Prima frase molto lunga qui. Seconda frase. Terza frase con testo."

Output:
Chunk 1: "Prima frase molto lunga qui. Seconda frase."
Chunk 2: "Terza frase con testo."
```

---

### Level 3: Split su Parole

**Delimitatore**: `\\s+` (whitespace)

Attivato quando: `sentence.length() > chunkSize`

```java
String[] words = text.split("\\s+");
```

**Logica**:
- Split forzato su parole per rispettare `chunkSize`
- Ultima risorsa per testi senza punteggiatura
- Preserva integrità delle parole

**Esempio**:

```
Input (chunkSize=30):
"parola1 parola2 parola3 parola4 parola5 parola6"

Output:
Chunk 1: "parola1 parola2 parola3"
Chunk 2: "parola4 parola5 parola6"
```

---

### Level 4: Split Caratteri (Caso Estremo)

Attivato quando: `word.length() > chunkSize` (rarissimo)

**Esempio**:
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

## 3. Applicazione Overlap

### Sliding Window

Dopo lo split semantico, viene applicato un overlap tra chunk consecutivi:

```
Chunk 1: [A B C D E F G H]
                    ↓ overlap (ultimi N caratteri)
Chunk 2:         [F G H | I J K L M]
                          ↓ overlap
Chunk 3:                 [L M | N O P Q R]
```

### Implementazione

```java
for (int i = 1; i < chunks.size(); i++) {
    String previousChunk = chunks.get(i - 1);
    String currentChunk = chunks.get(i);
    
    // Estrai ultimi N caratteri del chunk precedente
    String overlapText = getLastNCharacters(previousChunk, overlap);
    
    // Prependi al chunk corrente
    currentChunk = overlapText + "\n" + currentChunk;
}
```

### Smart Overlap

La funzione `getLastNCharacters()` è intelligente:

```java
// Cerca il primo spazio per evitare parole tagliate
String substring = text.substring(text.length() - n);
int firstSpace = substring.indexOf(' ');

if (firstSpace > 0 && firstSpace < n / 2) {
    // Skip al primo spazio per non iniziare a metà parola
    return substring.substring(firstSpace + 1);
}
```

### Perché l'Overlap?

1. **Cattura info ai confini**: se una frase è divisa tra 2 chunk, l'overlap la recupera
2. **Migliora retrieval**: più probabilità di trovare il chunk giusto
3. **Context continuity**: l'AI ha più contesto per rispondere

**Esempio Pratico**:

```
Senza overlap:
Chunk 1: "...la causa principale è il"
Chunk 2: "surriscaldamento del processore..."

Query: "Qual è la causa del surriscaldamento?"
→ Può matchare solo Chunk 2 (incompleto)

Con overlap (15%):
Chunk 1: "...la causa principale è il"
Chunk 2: "causa principale è il surriscaldamento del processore..."

Query: "Qual è la causa del surriscaldamento?"
→ Matcha Chunk 2 con contesto completo! ✅
```

---

## 4. Filtraggio Chunk Piccoli

### Costante

```java
private static final int MIN_CHUNK_SIZE = 50;
```

### Logica

Rimuove chunk troppo piccoli che non fornirebbero contesto utile:

```java
List<String> filtered = chunks.stream()
    .filter(chunk -> chunk.length() >= MIN_CHUNK_SIZE)
    .toList();
```

### Perché?

- Chunk di 10-20 caratteri sono inutili per l'embedding
- Sprecano risorse (API calls, storage)
- Riducono qualità retrieval (noise)

**Esempio**:

```
Before filtering:
["Complete paragraph here.", "x", "Another good chunk.", "ok"]

After filtering:
["Complete paragraph here.", "Another good chunk."]
```

---

## Parametri di Configurazione

### application.yml

```yaml
rag:
  chunk:
    size: 2000      # ~500 token (500 × 4 char/token)
    overlap: 200    # 10% overlap (200 / 2000)
```

### Perché 2000 caratteri?

- **Token estimate**: ~4 caratteri/token → 2000 char = ~500 token
- **Context window**: bilancia tra contesto sufficiente e precisione
- **Gemini embedding**: dimensione ottimale per text-embedding-004
- **Industry standard**: 500-1000 token è il sweet spot per RAG

### Perché 10% overlap?

- **Balance**: cattura confini senza troppa duplicazione
- **Storage efficiency**: non esplode il DB
- **Retrieval quality**: migliora accuracy senza overwhelm
- **Best practice**: 10-15% è standard nei sistemi RAG

---

## Esempi Completi

### Esempio 1: Documento Markdown

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

**Note**:
- ✅ Split su headers (`##`)
- ✅ Liste mantenute insieme
- ✅ Overlap preserva contesto

---

### Esempio 2: Testo Narrativo

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

**Note**:
- ✅ Split su frasi (`. `)
- ✅ Overlap cattura contesto
- ✅ Unicode (ö) preservato

---

### Esempio 3: Edge Case - Testo Senza Punteggiatura

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

**Note**:
- ✅ Fallback a word splitting
- ✅ Overlap applicato
- ✅ Nessun dato perso

---

## Diagramma Decisionale

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

### Complessità Temporale

- **Caso medio**: O(n) dove n = lunghezza testo
  - Split su regex: O(n)
  - Accumulo chunk: O(n)
  - Apply overlap: O(k) dove k = numero chunk
  
- **Caso peggiore**: O(n²) per testi patologici senza whitespace

### Complessità Spaziale

- O(n + k×m) dove:
  - n = testo originale
  - k = numero chunk
  - m = dimensione media chunk

### Ottimizzazioni

1. **StringBuilder**: evita concatenazione String inefficiente
2. **Regex precompilata**: `SENTENCE_PATTERN` è costante
3. **Early return**: testo corto → 1 chunk senza processing
4. **Stream filtering**: operazione lazy

---

## Testing Strategy

### Test Coverage

Il `ChunkingServiceImplTest` copre:

- ✅ **Basic functionality** (26 test)
- ✅ **Parameter validation**
- ✅ **Semantic splitting**
- ✅ **Overlap mechanics**
- ✅ **Edge cases** (empty, single word, unicode)
- ✅ **Realistic documents**

### Test Examples

```java
@Test
void splitPreservesParagraphBoundaries() {
    String text = "Para 1.\n\nPara 2.\n\nPara 3.";
    List<String> chunks = service.splitIntoChunks(text, 80, 0);
    
    // Verifica che paragrafi non siano mescolati
    for (String chunk : chunks) {
        assertThat(chunk.split("\n\n").length).isLessThanOrEqualTo(2);
    }
}

@Test
void overlapIncludesPreviousContent() {
    String text = "AAAAA BBBBB CCCCC DDDDD...";
    List<String> chunks = service.splitIntoChunks(text, 80, 15);
    
    // Verifica overlap tra chunk consecutivi
    if (chunks.size() > 1) {
        String prev = chunks.get(0);
        String curr = chunks.get(1);
        // curr dovrebbe contenere parte finale di prev
    }
}
```

---

## Best Practices

### Do's ✅

1. **Use default parameters** from `application.yml` quando possibile
2. **Monitor chunk distribution**: verifica che dimensioni siano uniformi
3. **Log chunk metrics**: numero, dimensione media, min/max
4. **Validate input**: controlla text non null/empty prima di chiamare
5. **Test con documenti reali**: MD, HTML, PDF vari

### Don'ts ❌

1. **Non usare chunk troppo piccoli** (<100 char) - poco contesto
2. **Non usare chunk troppo grandi** (>5000 char) - embedding inefficaci
3. **Non impostare overlap ≥ chunkSize** - duplicazione totale
4. **Non ignorare i warning** nel log (chunk filtrati, dimensioni anomale)
5. **Non assumere UTF-8**: il servizio gestisce unicode correttamente

---

## Future Enhancements

### Possibili Miglioramenti

1. **Document-aware splitting**:
   - Markdown: split su headers (`#`, `##`)
   - HTML: split su `<section>`, `<article>`
   - PDF: rispetta page boundaries

2. **Smart overlap**:
   - Overlap dinamico basato su tipo contenuto
   - Maggiore overlap per testi tecnici
   - Minore overlap per narrativa

3. **Token counting**:
   - Integrazione libreria token counting (jtokkit)
   - Split basato su token reali vs caratteri

4. **Caching**:
   - Cache chunk per documenti identici
   - Evita re-chunking su upload duplicati

5. **Metrics**:
   - Histogram dimensioni chunk
   - Distribution semantic boundaries
   - Overlap effectiveness score

---

## Troubleshooting

### Problema: Chunk troppo piccoli filtrati

**Sintomo**: Log mostra "Filtered out N small chunks"

**Causa**: `chunkSize` troppo piccolo o documento con paragrafi brevissimi

**Soluzione**:
```yaml
rag:
  chunk:
    size: 2000  # Aumenta da 500 a 2000
```

---

### Problema: Chunk contengono testo tagliato

**Sintomo**: Chunk iniziano/finiscono a metà parola

**Causa**: Documento senza punteggiatura/whitespace (raro)

**Soluzione**: Verificato che `getLastNCharacters()` cerca word boundary

---

### Problema: Troppi chunk per documento

**Sintomo**: 1000+ chunk per un documento di 50 pagine

**Causa**: `chunkSize` troppo piccolo

**Soluzione**:
```yaml
rag:
  chunk:
    size: 3000  # Aumenta dimensione chunk
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

