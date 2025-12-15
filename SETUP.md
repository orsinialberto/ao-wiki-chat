# Guida Setup WikiChat

Questa guida ti accompagna passo-passo nel setup completo dell'ambiente di sviluppo per WikiChat.

---

## Prerequisiti

### Software Richiesto
- **Java 25** (o superiore)
- **Maven 3.9+**
- **Docker** e **Docker Compose**
- **Git**
- **IDE** (IntelliJ IDEA / VS Code con Java extensions)

### Account/API Keys
- **Google AI Studio** account per Gemini API Key (gratuito)

---

## Step 1: Verifica Prerequisiti

```bash
# Verifica Java 25
java -version
# Output atteso: java version "25..." o superiore

# Verifica Maven
mvn -version
# Output atteso: Apache Maven 3.9.x

# Verifica Docker
docker --version
docker-compose --version
```

---

## Step 2: Setup Gemini API Key

### 2.1 Ottieni API Key
1. Vai su [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Effettua login con account Google
3. Clicca **"Get API Key"**
4. Copia l'API key generata

### 2.2 Configura Environment Variable
**macOS/Linux:**
```bash
# Aggiungi al tuo .bashrc o .zshrc
export GEMINI_API_KEY="la_tua_api_key_qui"

# Ricarica shell
source ~/.bashrc  # oppure ~/.zshrc
```

**Windows (PowerShell):**
```powershell
[Environment]::SetEnvironmentVariable("GEMINI_API_KEY", "la_tua_api_key_qui", "User")
```

### 2.3 Crea file .env (opzionale per Docker)
```bash
cd ao-wiki-chat
echo "GEMINI_API_KEY=la_tua_api_key_qui" > .env
```

⚠️ **IMPORTANTE**: Aggiungi `.env` al `.gitignore` per non committare la chiave!

---

## Step 3: Setup PostgreSQL con pgvector

### 3.1 Crea docker-compose.yml
```yaml
version: '3.8'

services:
  postgres:
    image: pgvector/pgvector:pg15
    container_name: wikichat-db
    environment:
      POSTGRES_DB: wikichat
      POSTGRES_USER: wikichat_user
      POSTGRES_PASSWORD: wikichat_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U wikichat_user -d wikichat"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

### 3.2 Crea init.sql
```sql
-- Abilita estensione pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Tabella documenti
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PROCESSING',
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella chunks
CREATE TABLE chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    embedding vector(768),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);

-- Indice HNSW per ricerca vettoriale
CREATE INDEX chunks_embedding_idx ON chunks 
USING hnsw (embedding vector_cosine_ops);

-- Tabella conversazioni
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella messaggi
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    sources JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indici per performance
CREATE INDEX idx_chunks_document_id ON chunks(document_id);
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_conversations_session_id ON conversations(session_id);
```

### 3.3 Avvia PostgreSQL
```bash
cd ao-wiki-chat
docker-compose up -d

# Verifica status
docker-compose ps

# Verifica logs
docker-compose logs postgres

# Test connessione
docker exec -it wikichat-db psql -U wikichat_user -d wikichat -c "\dt"
# Dovresti vedere le tabelle create
```

---

## Step 4: Configurazione Maven (pom.xml)

Aggiorna `pom.xml` con le seguenti dipendenze:

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <dependency>
        <groupId>com.pgvector</groupId>
        <artifactId>pgvector</artifactId>
        <version>0.1.6</version>
    </dependency>
    
    <!-- LangChain4j + Gemini -->
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j</artifactId>
        <version>0.36.2</version>
    </dependency>
    
    <dependency>
        <groupId>dev.langchain4j</groupId>
        <artifactId>langchain4j-google-ai-gemini</artifactId>
        <version>0.36.2</version>
    </dependency>
    
    <!-- Document Parsers -->
    <dependency>
        <groupId>org.apache.pdfbox</groupId>
        <artifactId>pdfbox</artifactId>
        <version>3.0.3</version>
    </dependency>
    
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.18.1</version>
    </dependency>
    
    <dependency>
        <groupId>org.commonmark</groupId>
        <artifactId>commonmark</artifactId>
        <version>0.24.0</version>
    </dependency>
    
    <!-- Lombok (opzionale) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    
    <!-- Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Step 5: Configurazione application.yml

Crea/aggiorna `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: ao-wiki-chat
  output:
    ansi:
      enabled: ALWAYS  # Enable colored console logs
  datasource:
    url: jdbc:postgresql://localhost:5432/wikichat
    username: wikichat_user
    password: wikichat_password
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
  jpa:
    # Note: dialect auto-detected from driver (no need to specify)
    hibernate:
      ddl-auto: validate
    show-sql: false
    open-in-view: false  # Best practice for REST APIs
    properties:
      hibernate:
        format_sql: true
        jdbc:
          lob:
            non_contextual_creation: true
  servlet:
    multipart:
      enabled: true
      max-file-size: 50MB
      max-request-size: 50MB
  task:
    execution:
      pool:
        core-size: 4
        max-size: 8
        queue-capacity: 100
      thread-name-prefix: doc-processor-

server:
  port: 8080

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

upload:
  allowed-types: application/pdf,text/markdown,text/html,text/plain

rag:
  chunk:
    size: 500
    overlap: 50
  search:
    top-k: 5
    similarity-threshold: 0.7
  vector:
    dimension: 768
    distance-metric: cosine

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when-authorized

logging:
  level:
    root: INFO
    com.example.ao_wiki_chat: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    dev.langchain4j: DEBUG
  pattern:
    console: "%clr(%d{yyyy-MM-dd HH:mm:ss}){blue} %clr([%5p]){highlight} %clr(%-40.40logger{39}){cyan} : %m%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

**Note importanti**:
- ✅ Formato YAML (più leggibile di .properties)
- ✅ ANSI colors abilitati per log colorati in console
- ✅ Dialect PostgreSQL auto-rilevato (no warning)
- ✅ `open-in-view: false` per best practice REST API
- ✅ HikariCP connection pool configurato
- ✅ Actuator endpoints per health checks

---

## Step 6: Build e Avvio Applicazione

### 6.1 Download Dipendenze
```bash
cd ao-wiki-chat
mvn clean install
```

### 6.2 Avvio Applicazione
```bash
mvn spring-boot:run
```

Oppure con IDE:
- IntelliJ: Right-click su `AoWikiChatApplication.java` → Run
- VS Code: Debug → Start Debugging

### 6.3 Verifica Avvio
```bash
# Test health endpoint (una volta implementato)
curl http://localhost:8080/api/health

# Check logs
# Dovresti vedere: "Started AoWikiChatApplication in X seconds"
```

---

## Step 7: Test Setup Completo

### 7.1 Verifica Database Connection
```bash
# Controlla logs applicazione
# Cerca: "HikariPool-1 - Start completed"
```

### 7.2 Test PostgreSQL
```bash
docker exec -it wikichat-db psql -U wikichat_user -d wikichat

# Query test
SELECT * FROM documents;
SELECT * FROM chunks LIMIT 1;

# Verifica estensione pgvector
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
```

### 7.3 Test Gemini API (opzionale)
Dopo aver implementato `HealthController`:
```bash
curl http://localhost:8080/api/health/gemini
```

---

## Troubleshooting

### Problema: "Port 5432 already in use"
```bash
# Verifica processi su porta 5432
lsof -i :5432

# Ferma PostgreSQL locale se esiste
brew services stop postgresql  # macOS
sudo systemctl stop postgresql # Linux

# Oppure cambia porta in docker-compose.yml:
ports:
  - "5433:5432"
# E aggiorna application.properties di conseguenza
```

### Problema: "GEMINI_API_KEY not found"
```bash
# Verifica variabile ambiente
echo $GEMINI_API_KEY

# Se vuota, aggiungi a ~/.bashrc o ~/.zshrc
export GEMINI_API_KEY="tua_api_key"
source ~/.bashrc
```

### Problema: "pgvector extension not found"
```bash
# Ricrea database
docker-compose down -v
docker-compose up -d

# Verifica logs
docker-compose logs postgres
```

### Problema: Maven build fallisce
```bash
# Clear cache Maven
mvn clean
rm -rf ~/.m2/repository

# Re-download dependencies
mvn clean install -U
```

---

## Comandi Utili

### Docker
```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f postgres

# Rebuild
docker-compose up -d --build

# Remove volumes (ATTENZIONE: cancella dati)
docker-compose down -v
```

### Database
```bash
# Connect to PostgreSQL
docker exec -it wikichat-db psql -U wikichat_user -d wikichat

# Backup database
docker exec -t wikichat-db pg_dump -U wikichat_user wikichat > backup.sql

# Restore database
docker exec -i wikichat-db psql -U wikichat_user -d wikichat < backup.sql
```

### Maven
```bash
# Clean build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Run specific test
mvn test -Dtest=DocumentServiceTest

# Package JAR
mvn package
```

---

## Next Steps

Dopo aver completato il setup:

1. ✅ Verifica che l'applicazione si avvii senza errori
2. ✅ Conferma connessione al database
3. ✅ Test variabile GEMINI_API_KEY caricata
4. 📝 Inizia implementazione seguendo `DEVELOPMENT_PLAN.md`
5. 📚 Leggi `ARCHITECTURE.md` per comprendere il design

---

## Struttura Progetto Finale

```
ao-wiki-chat/
├── docker-compose.yml          ✅ Creato
├── init.sql                    ✅ Creato
├── .env                        ✅ Creato (non committare!)
├── pom.xml                     ⏳ Da aggiornare
├── DEVELOPMENT_PLAN.md         📚 Piano sviluppo
├── ARCHITECTURE.md             📚 Documentazione architettura
├── SETUP.md                    📚 Questa guida
└── src/
    ├── main/
    │   ├── java/com/example/ao_wiki_chat/
    │   │   ├── AoWikiChatApplication.java
    │   │   ├── config/         ⏳ Da creare
    │   │   ├── controller/     ⏳ Da creare
    │   │   ├── service/        ⏳ Da creare
    │   │   ├── repository/     ⏳ Da creare
    │   │   ├── model/          ⏳ Da creare
    │   │   ├── integration/    ⏳ Da creare
    │   │   └── exception/      ⏳ Da creare
    │   └── resources/
    │       └── application.yml         ✅ Configurato
    └── test/
```

---

## Supporto

Per problemi durante il setup:
1. Controlla logs: `docker-compose logs` e console Spring Boot
2. Verifica prerequisiti installati correttamente
3. Consulta `ARCHITECTURE.md` per dettagli tecnici
4. Rivedi questo documento passo-passo

Buon sviluppo! 🚀

