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

import java.nio.file.Path;
import java.util.UUID;

/**
 * Command for uploading documents to the WikiChat backend.
 * Supports waiting for processing completion with timeout.
 */
@CommandLine.Command(
        name = "upload",
        description = "Upload a document to the WikiChat backend"
)
public class UploadCommand implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            description = "Path to the file to upload"
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

        try {
            // Validate file path
            Path path = InputValidator.validateFilePath(filePath);
            
            // Validate file properties
            FileValidator fileValidator = createFileValidator(
                    config.getMaxFileSizeBytes(),
                    config.getSupportedFileTypes()
            );
            fileValidator.validate(path);

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Upload document
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

            // Wait for processing if requested
            if (wait) {
                waitForProcessing(apiClient, uploadResponse.documentId(), timeoutSeconds, config.isOutputColors());
            }

        } catch (CliException | IllegalArgumentException e) {
            colorPrinter.error("Error: " + e.getMessage());
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
        } catch (ApiException e) {
            colorPrinter.error("API Error: " + e.getMessage());
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
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
