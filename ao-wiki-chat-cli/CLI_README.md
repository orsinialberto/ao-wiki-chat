# WikiChat CLI - User Guide

Command-line interface for the WikiChat RAG System. This guide covers installation, usage, configuration, and troubleshooting.

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Commands](#commands)
- [Configuration](#configuration)
- [Output Formats](#output-formats)
- [Troubleshooting](#troubleshooting)
- [Advanced Usage](#advanced-usage)

---

## Installation

### Prerequisites

- **Java 25 or later** (required)
- **Maven** (for building from source)
- **WikiChat Backend** running and accessible

### Building the CLI

From the project root:

```bash
# Build the CLI module
./mvnw clean package -pl ao-wiki-chat-cli -am

# The fat JAR will be created at:
# ao-wiki-chat-cli/target/ao-wiki-chat-cli-0.0.1-SNAPSHOT.jar
```

### Installing Wrapper Scripts

#### Unix/macOS

```bash
# Make the script executable
chmod +x scripts/wikichat

# Option 1: Add to PATH (recommended)
# Add to ~/.bashrc or ~/.zshrc:
export PATH="$PATH:/path/to/ao-wiki-chat/scripts"

# Option 2: Create symlink in /usr/local/bin
sudo ln -s /path/to/ao-wiki-chat/scripts/wikichat /usr/local/bin/wikichat

# Option 3: Copy to a directory in PATH
cp scripts/wikichat ~/bin/wikichat
```

#### Windows

```batch
REM Option 1: Add scripts directory to PATH
REM Add to System Environment Variables:
REM C:\path\to\ao-wiki-chat\scripts

REM Option 2: Copy to a directory in PATH
copy scripts\wikichat.bat C:\Windows\System32\wikichat.bat
```

### Verification

After installation, verify the CLI works:

```bash
wikichat --version
wikichat --help
```

---

## Quick Start

### 1. Configure the Backend URL

```bash
# Set the backend API URL
wikichat config set api.url http://localhost:8080/api

# Verify configuration
wikichat config list
```

### 2. Check System Health

```bash
# Check overall system health
wikichat health

# Check database only
wikichat health --db

# Check Gemini API only
wikichat health --gemini
```

### 3. Upload a Document

```bash
# Upload a document
wikichat upload document.md

# Upload and wait for processing
wikichat upload document.md --wait

# Upload with custom timeout (default: 300 seconds)
wikichat upload document.md --wait --timeout 600
```

### 4. Query the System

```bash
# Simple query
wikichat query "What is the main topic?"

# Query with session tracking
wikichat query "Tell me more" --session my-session-id

# Query with source references
wikichat query "What are the key points?" --sources
```

---

## Commands

### General Options

All commands support:
- `--help`, `-h`: Show command-specific help
- `--verbose`, `-v`: Enable verbose output and detailed error messages

### Document Commands

#### `upload` - Upload a Document

Upload a document to the WikiChat backend for processing.

**Usage:**
```bash
wikichat upload <file-path> [options]
```

**Options:**
- `--wait`: Wait for document processing to complete
- `--timeout <seconds>`: Timeout when waiting (default: 300)
- `--format <format>`: Output format: `text` or `json` (default: `text`)

**Examples:**
```bash
# Upload a markdown file
wikichat upload README.md

# Upload and wait for completion
wikichat upload report.pdf --wait

# Upload with JSON output
wikichat upload document.html --format json
```

**Supported File Types:**
- Markdown (`.md`)
- HTML (`.html`, `.htm`)
- PDF (`.pdf`)

#### `list` - List Documents

List all documents in the system.

**Usage:**
```bash
wikichat list [options]
```

**Options:**
- `--status <status>`: Filter by status: `PROCESSING`, `COMPLETED`, or `FAILED`
- `--format <format>`: Output format: `table`, `json`, `markdown`, or `plain` (default: `table`)

**Examples:**
```bash
# List all documents
wikichat list

# List only completed documents
wikichat list --status COMPLETED

# List in JSON format
wikichat list --format json

# List processing documents in plain format
wikichat list --status PROCESSING --format plain
```

#### `show` - Show Document Details

Display detailed information about a specific document.

**Usage:**
```bash
wikichat show <document-id> [options]
```

**Options:**
- `--format <format>`: Output format: `table`, `json`, `markdown`, or `plain` (default: `table`)

**Examples:**
```bash
# Show document details
wikichat show 550e8400-e29b-41d4-a716-446655440000

# Show in JSON format
wikichat show 550e8400-e29b-41d4-a716-446655440000 --format json
```

#### `delete` - Delete a Document

Delete a document and all its associated chunks.

**Usage:**
```bash
wikichat delete <document-id> [options]
```

**Options:**
- `--format <format>`: Output format: `text` or `json` (default: `text`)

**Examples:**
```bash
# Delete a document
wikichat delete 550e8400-e29b-41d4-a716-446655440000

# Delete with JSON confirmation
wikichat delete 550e8400-e29b-41d4-a716-446655440000 --format json
```

#### `chunks` - List Document Chunks

List all chunks for a specific document.

**Usage:**
```bash
wikichat chunks <document-id> [options]
```

**Options:**
- `--format <format>`: Output format: `table`, `json`, `markdown`, or `plain` (default: `table`)

**Examples:**
```bash
# List chunks for a document
wikichat chunks 550e8400-e29b-41d4-a716-446655440000

# List chunks in JSON format
wikichat chunks 550e8400-e29b-41d4-a716-446655440000 --format json
```

### Chat Commands

#### `query` - Send a Query

Send a chat query to the WikiChat system.

**Usage:**
```bash
wikichat query <query-text> [options]
```

**Options:**
- `--session <session-id>`, `-s`: Session ID for conversation tracking (auto-generated if not provided)
- `--sources`: Show source references in the response
- `--format <format>`, `-f`: Output format: `table`, `json`, `markdown`, or `plain` (default: `table`)

**Examples:**
```bash
# Simple query
wikichat query "What is the main topic of the documents?"

# Query with session tracking
wikichat query "Tell me more" --session my-session-123

# Query with source references
wikichat query "What are the key points?" --sources

# Query in JSON format
wikichat query "Summarize the content" --format json
```

#### `interactive` - Interactive Mode

Start an interactive chat session (REPL).

**Usage:**
```bash
wikichat interactive [options]
```

**Aliases:** `i`, `repl`

**Options:**
- `--session <session-id>`, `-s`: Session ID for conversation tracking (auto-generated if not provided)
- `--sources`: Show source references in responses
- `--format <format>`, `-f`: Output format: `table`, `json`, `markdown`, or `plain` (default: `table`)

**Examples:**
```bash
# Start interactive mode
wikichat interactive

# Start with session ID
wikichat interactive --session my-session-123

# Start with source references
wikichat interactive --sources

# Use alias
wikichat i
```

**Interactive Commands:**
- Type your question and press Enter
- Type `exit`, `quit`, or `q` to exit
- Type `clear` to clear the conversation history
- Type `help` to show available commands

#### `history` - Show Conversation History

Display conversation history for a session.

**Usage:**
```bash
wikichat history <session-id> [options]
```

**Options:**
- `--format <format>`, `-f`: Output format: `table`, `json`, `markdown`, or `plain` (default: `table`)

**Examples:**
```bash
# Show history for a session
wikichat history my-session-123

# Show history in JSON format
wikichat history my-session-123 --format json
```

#### `clear` - Clear Conversation History

Delete conversation history for a session.

**Usage:**
```bash
wikichat clear <session-id> [options]
```

**Options:**
- `--format <format>`, `-f`: Output format: `text` or `json` (default: `text`)

**Examples:**
```bash
# Clear history for a session
wikichat clear my-session-123

# Clear with JSON confirmation
wikichat clear my-session-123 --format json
```

### System Commands

#### `health` - Check System Health

Check the health status of the WikiChat system.

**Usage:**
```bash
wikichat health [options]
```

**Options:**
- `--db`: Check database health only
- `--gemini`: Check Gemini API health only
- `--format <format>`, `-f`: Output format: `text` or `json` (default: `text`)

**Examples:**
```bash
# Check overall health
wikichat health

# Check database only
wikichat health --db

# Check Gemini API only
wikichat health --gemini

# Health check in JSON format
wikichat health --format json
```

**Exit Codes:**
- `0`: System is healthy (status: UP)
- `1`: System is unhealthy (status: DOWN)

#### `config` - Manage Configuration

Manage CLI configuration settings.

**Subcommands:**
- `set <key> <value>`: Set a configuration value
- `get <key>`: Get a configuration value
- `list`: List all configuration values
- `reset`: Reset configuration to default values

**Usage:**
```bash
wikichat config <subcommand> [options]
```

**Examples:**
```bash
# Set API URL
wikichat config set api.url http://localhost:8080/api

# Set connection timeout
wikichat config set api.timeout.connect 5000

# Get a configuration value
wikichat config get api.url

# List all configuration
wikichat config list

# Reset to defaults
wikichat config reset
```

**Configuration Keys:**
- `api.url`: Backend API URL (default: `http://localhost:8080/api`)
- `api.timeout.connect`: Connection timeout in milliseconds (default: `5000`)
- `api.timeout.read`: Read timeout in milliseconds (default: `30000`)
- `api.timeout.write`: Write timeout in milliseconds (default: `30000`)
- `output.format`: Default output format (default: `table`)
- `output.colors`: Enable colored output (default: `true`)

---

## Configuration

### Configuration File Location

Configuration is stored in:
- **Unix/macOS**: `~/.wikichat/config.properties`
- **Windows**: `%USERPROFILE%\.wikichat\config.properties`

### Default Configuration

```properties
api.url=http://localhost:8080/api
api.timeout.connect=5000
api.timeout.read=30000
api.timeout.write=30000
output.format=table
output.colors=true
```

### Environment Variables

You can override configuration using environment variables:

```bash
# Set API URL via environment variable
export WIKICHAT_API_URL=http://localhost:8080/api

# Set timeout
export WIKICHAT_API_TIMEOUT_CONNECT=10000
```

### Configuration Priority

1. Command-line arguments (highest priority)
2. Environment variables
3. Configuration file
4. Default values (lowest priority)

---

## Output Formats

The CLI supports multiple output formats for different use cases:

### `table` (Default)

Human-readable table format with colors and formatting.

```bash
wikichat list --format table
```

### `json`

JSON format for programmatic processing.

```bash
wikichat list --format json
```

### `markdown`

Markdown format for documentation.

```bash
wikichat list --format markdown
```

### `plain`

Plain text format without formatting.

```bash
wikichat list --format plain
```

---

## Troubleshooting

### Common Issues

#### "Java is not installed or not in PATH"

**Problem:** The wrapper script cannot find Java.

**Solution:**
1. Install Java 25 or later
2. Ensure Java is in your PATH:
   ```bash
   java -version
   ```
3. If Java is installed but not in PATH, add it:
   ```bash
   export PATH="$PATH:/path/to/java/bin"
   ```

#### "JAR file not found"

**Problem:** The wrapper script cannot find the CLI JAR file.

**Solution:**
1. Build the CLI:
   ```bash
   ./mvnw clean package -pl ao-wiki-chat-cli -am
   ```
2. Verify the JAR exists:
   ```bash
   ls ao-wiki-chat-cli/target/ao-wiki-chat-cli-0.0.1-SNAPSHOT.jar
   ```

#### "API Error: Connection refused"

**Problem:** Cannot connect to the backend API.

**Solution:**
1. Verify the backend is running:
   ```bash
   curl http://localhost:8080/api/health
   ```
2. Check the API URL configuration:
   ```bash
   wikichat config get api.url
   ```
3. Update the API URL if needed:
   ```bash
   wikichat config set api.url http://localhost:8080/api
   ```

#### "API Error: Timeout"

**Problem:** API requests are timing out.

**Solution:**
1. Increase timeout values:
   ```bash
   wikichat config set api.timeout.connect 10000
   wikichat config set api.timeout.read 60000
   ```
2. Check network connectivity
3. Verify the backend is responsive

#### "Document upload failed"

**Problem:** Document upload is failing.

**Solution:**
1. Check file size limits
2. Verify file format is supported (MD, HTML, PDF)
3. Check backend logs for detailed error messages
4. Use `--verbose` flag for more details:
   ```bash
   wikichat upload document.md --verbose
   ```

#### "Invalid format" Error

**Problem:** Output format is not recognized.

**Solution:**
- Valid formats: `table`, `json`, `markdown`, `plain`
- Check command help for format options:
  ```bash
  wikichat <command> --help
  ```

### Debug Mode

Enable verbose output for debugging:

```bash
# Enable verbose mode
wikichat --verbose <command> [options]

# Or use -v
wikichat -v <command> [options]
```

Verbose mode provides:
- Detailed error messages
- API request/response logging
- Configuration values
- Execution flow information

### Getting Help

```bash
# General help
wikichat --help

# Command-specific help
wikichat upload --help
wikichat query --help
wikichat config --help
```

---

## Advanced Usage

### Using Without Wrapper Script

If you prefer to use Java directly:

```bash
# Run the CLI directly
java -jar ao-wiki-chat-cli/target/ao-wiki-chat-cli-0.0.1-SNAPSHOT.jar <command> [options]
```

### Scripting and Automation

The CLI is designed for scripting and automation:

```bash
#!/bin/bash
# Example script

# Upload a document
DOC_ID=$(wikichat upload document.md --format json | jq -r '.id')

# Wait for processing
wikichat upload document.md --wait

# Query the system
wikichat query "What is this document about?" --format json

# List documents
wikichat list --format json | jq '.[] | select(.status == "COMPLETED")'
```

### Session Management

Use session IDs to maintain conversation context:

```bash
# Create a session ID
SESSION_ID="my-session-$(date +%s)"

# Query with session
wikichat query "What is the main topic?" --session "$SESSION_ID"

# Continue conversation
wikichat query "Tell me more" --session "$SESSION_ID"

# View history
wikichat history "$SESSION_ID"

# Clear history when done
wikichat clear "$SESSION_ID"
```

### Batch Operations

Process multiple documents:

```bash
# Upload multiple documents
for file in documents/*.md; do
    wikichat upload "$file" --wait
done

# Query multiple questions
while IFS= read -r question; do
    wikichat query "$question" --format json
done < questions.txt
```

### Integration with CI/CD

Example GitHub Actions workflow:

```yaml
name: Upload Documents
on: [push]
jobs:
  upload:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '25'
      - name: Build CLI
        run: ./mvnw clean package -pl ao-wiki-chat-cli -am
      - name: Upload Documents
        run: |
          chmod +x scripts/wikichat
          ./scripts/wikichat config set api.url ${{ secrets.API_URL }}
          ./scripts/wikichat upload docs/*.md --wait
```

---

## Additional Resources

- [Main README](../README.md) - Project overview
- [SETUP.md](../SETUP.md) - Detailed setup guide
- [AGENTS.md](../AGENTS.md) - Development guidelines

---

**Version:** 0.0.1-SNAPSHOT  
**Last Updated:** December 2025
