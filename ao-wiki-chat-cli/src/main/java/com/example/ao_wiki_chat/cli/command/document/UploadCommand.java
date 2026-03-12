package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.CliConfig;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.exception.CliException;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import com.example.ao_wiki_chat.cli.model.CliDocumentUpload;
import com.example.ao_wiki_chat.cli.util.ColorPrinter;
import com.example.ao_wiki_chat.cli.util.FileValidator;
import com.example.ao_wiki_chat.cli.util.InputValidator;
import com.example.ao_wiki_chat.cli.util.ProgressBar;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Result of a single file upload (success or failure).
 */
record UploadResult(Path path, CliDocumentUpload response, String error) {
    static UploadResult success(Path path, CliDocumentUpload response) {
        return new UploadResult(path, response, null);
    }
    static UploadResult failure(Path path, String error) {
        return new UploadResult(path, null, error);
    }
    boolean isSuccess() { return response != null; }
}

/**
 * Command for uploading documents to the WikiChat backend.
 * Accepts a single file or a directory (recursively uploads all supported files).
 * Supports waiting for processing completion with timeout (single file only).
 */
@CommandLine.Command(
        name = "upload",
        description = "Upload a document or a folder of documents to the WikiChat backend"
)
public class UploadCommand implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            description = "Path to the file or folder to upload"
    )
    String filePath;

    @CommandLine.Option(
            names = {"--wait"},
            description = "Wait for document processing to complete"
    )
    boolean wait;

    @CommandLine.Option(
            names = {"--timeout"},
            description = "Timeout in seconds when waiting for processing (default: 300)",
            defaultValue = "300"
    )
    int timeoutSeconds;

    @CommandLine.Option(
            names = {"--format"},
            description = "Output format: text or json (default: text)",
            defaultValue = "text"
    )
    String format;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    /**
     * Creates an ApiClient instance. Can be overridden in tests.
     *
     * @return ApiClient instance
     */
    ApiClient createApiClient() {
        ConfigManager configManager = new ConfigManager();
        return new ApiClient(
                configManager.get().getApiUrl(),
                configManager.get().getApiConnectTimeout(),
                configManager.get().getApiReadTimeout(),
                configManager.get().getApiWriteTimeout(),
                false
        );
    }

    /**
     * Creates a ConfigManager instance. Can be overridden in tests.
     *
     * @return ConfigManager instance
     */
    ConfigManager createConfigManager() {
        return new ConfigManager();
    }

    /**
     * Creates a ColorPrinter instance. Can be overridden in tests.
     *
     * @param colorsEnabled whether colors are enabled
     * @return ColorPrinter instance
     */
    ColorPrinter createColorPrinter(boolean colorsEnabled) {
        return new ColorPrinter(colorsEnabled);
    }

    /**
     * Creates a FileValidator instance. Can be overridden in tests.
     *
     * @param maxFileSizeBytes   maximum file size
     * @param supportedFileTypes supported file types
     * @return FileValidator instance
     */
    FileValidator createFileValidator(long maxFileSizeBytes, String supportedFileTypes) {
        return new FileValidator(maxFileSizeBytes, supportedFileTypes);
    }

    /**
     * Creates a ProgressBar instance. Can be overridden in tests.
     *
     * @param colorsEnabled whether colors are enabled
     * @return ProgressBar instance
     */
    ProgressBar createProgressBar(boolean colorsEnabled) {
        return new ProgressBar(colorsEnabled);
    }

    @Override
    public void run() {
        ConfigManager configManager = createConfigManager();
        CliConfig config = configManager.get();
        ColorPrinter colorPrinter = createColorPrinter(config.isOutputColors());
        FileValidator fileValidator = createFileValidator(
                config.getMaxFileSizeBytes(),
                config.getSupportedFileTypes()
        );

        try {
            Path path = InputValidator.validateFilePathOrDirectory(filePath);

            if (Files.isRegularFile(path)) {
                runSingleFileUpload(path, fileValidator, config, colorPrinter);
            } else {
                runDirectoryUpload(path, fileValidator, colorPrinter);
            }
        } catch (CliException | IllegalArgumentException e) {
            colorPrinter.error("Error: " + e.getMessage());
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
        } catch (ApiException e) {
            colorPrinter.error("API Error: " + e.getMessage());
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        } catch (IOException e) {
            colorPrinter.error("Error reading directory: " + e.getMessage());
            throw new CommandLine.ExecutionException(spec.commandLine(), "Error reading directory: " + e.getMessage(), e);
        }
    }

    private void runSingleFileUpload(Path path, FileValidator fileValidator, CliConfig config, ColorPrinter colorPrinter) {
        fileValidator.validate(path);
        ApiClient apiClient = createApiClient();

        colorPrinter.info("Uploading document: " + path.getFileName());
        CliDocumentUpload uploadResponse = apiClient.uploadDocument(path);

        if ("json".equalsIgnoreCase(format)) {
            System.out.println(uploadResponse);
        } else {
            colorPrinter.success("Document uploaded successfully!");
            colorPrinter.println("Document ID: " + uploadResponse.documentId());
            colorPrinter.println("Status: " + uploadResponse.status());
            colorPrinter.println("Filename: " + uploadResponse.filename());
        }

        if (wait) {
            waitForProcessing(apiClient, uploadResponse.documentId(), timeoutSeconds, config.isOutputColors());
        }
    }

    private void runDirectoryUpload(Path root, FileValidator fileValidator, ColorPrinter colorPrinter) throws IOException {
        List<Path> files = collectFiles(root, fileValidator);
        if (files.isEmpty()) {
            colorPrinter.info("No supported files found in " + root);
            return;
        }

        colorPrinter.info("Uploading " + files.size() + " file(s)...");
        ApiClient apiClient = createApiClient();
        List<UploadResult> results = new ArrayList<>();

        for (Path file : files) {
            try {
                CliDocumentUpload response = apiClient.uploadDocument(file);
                results.add(UploadResult.success(file, response));
            } catch (ApiException e) {
                results.add(UploadResult.failure(file, e.getMessage()));
            }
        }

        printUploadSummary(results, root, colorPrinter);
    }

    private List<Path> collectFiles(Path root, FileValidator fileValidator) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        try {
                            fileValidator.validate(p);
                            return true;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    })
                    .toList();
        }
    }

    private void printUploadSummary(List<UploadResult> results, Path root, ColorPrinter colorPrinter) {
        List<UploadResult> succeeded = results.stream().filter(UploadResult::isSuccess).toList();
        List<UploadResult> failed = results.stream().filter(r -> !r.isSuccess()).toList();

        if ("json".equalsIgnoreCase(format)) {
            printUploadSummaryJson(succeeded, failed, root);
        } else {
            colorPrinter.success("Upload complete. " + succeeded.size() + " file(s) uploaded.");
            colorPrinter.println("Uploaded files:");
            for (UploadResult r : succeeded) {
                Path rel = root.relativize(r.path());
                colorPrinter.println("  - " + rel + " (ID: " + r.response().documentId() + ", " + r.response().filename() + ")");
            }
            if (!failed.isEmpty()) {
                colorPrinter.error("Failed (" + failed.size() + "):");
                for (UploadResult r : failed) {
                    colorPrinter.println("  - " + root.relativize(r.path()) + ": " + r.error());
                }
            }
        }
    }

    private void printUploadSummaryJson(List<UploadResult> succeeded, List<UploadResult> failed, Path root) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"uploaded\":[");
        for (int i = 0; i < succeeded.size(); i++) {
            UploadResult r = succeeded.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"path\":\"").append(escapeJson(root.relativize(r.path()).toString())).append("\"");
            sb.append(",\"documentId\":\"").append(r.response().documentId()).append("\"");
            sb.append(",\"filename\":\"").append(escapeJson(r.response().filename())).append("\"");
            sb.append(",\"status\":\"").append(escapeJson(r.response().status())).append("\"}");
        }
        sb.append("],\"failed\":[");
        for (int i = 0; i < failed.size(); i++) {
            UploadResult r = failed.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"path\":\"").append(escapeJson(root.relativize(r.path()).toString())).append("\"");
            sb.append(",\"error\":\"").append(escapeJson(r.error())).append("\"}");
        }
        sb.append("]}");
        System.out.println(sb);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /**
     * Waits for document processing to complete, polling the status.
     *
     * @param apiClient     the API client
     * @param documentId    the document ID
     * @param timeoutSeconds the timeout in seconds
     * @param colorsEnabled whether colors are enabled
     */
    private void waitForProcessing(ApiClient apiClient, UUID documentId, int timeoutSeconds, boolean colorsEnabled) {
        ColorPrinter colorPrinter = createColorPrinter(colorsEnabled);
        ProgressBar progressBar = createProgressBar(colorsEnabled);

        colorPrinter.info("Waiting for processing to complete (timeout: " + timeoutSeconds + "s)...");

        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;
        int pollIntervalSeconds = 2;

        try {
            while (true) {
                // Check timeout
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > timeoutMillis) {
                    progressBar.reset();
                    colorPrinter.error("Timeout waiting for document processing");
                    throw new CommandLine.ExecutionException(spec.commandLine(), "Timeout waiting for document processing");
                }

                // Poll document status
                CliDocument document = apiClient.getDocument(documentId);
                String status = document.status();

                // Calculate progress based on elapsed time (rough estimate)
                double progress = Math.min(0.95, elapsed / (double) timeoutMillis);
                String statusMessage = "Status: " + status;

                // Show progress
                if ("PROCESSING".equals(status)) {
                    progressBar.update(progress, statusMessage);
                } else {
                    progressBar.update(1.0, statusMessage);
                }

                // Check if processing is complete
                if ("COMPLETED".equals(status)) {
                    progressBar.complete("Processing completed");
                    colorPrinter.success("Document processing completed successfully!");
                    return;
                } else if ("FAILED".equals(status)) {
                    progressBar.reset();
                    colorPrinter.error("Document processing failed");
                    throw new CommandLine.ExecutionException(spec.commandLine(), "Document processing failed");
                }

                // Wait before next poll
                Thread.sleep(pollIntervalSeconds * 1000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            progressBar.reset();
            colorPrinter.error("Interrupted while waiting for processing");
            throw new CommandLine.ExecutionException(spec.commandLine(), "Interrupted while waiting for processing", e);
        } catch (ApiException e) {
            progressBar.reset();
            colorPrinter.error("Error checking document status: " + e.getMessage());
            throw new CommandLine.ExecutionException(spec.commandLine(), "Error checking document status: " + e.getMessage(), e);
        }
    }
}
