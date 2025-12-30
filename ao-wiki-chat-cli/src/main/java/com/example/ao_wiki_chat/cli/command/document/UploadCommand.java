package com.example.ao_wiki_chat.cli.command.document;

import com.example.ao_wiki_chat.cli.config.ApiClient;
import com.example.ao_wiki_chat.cli.config.ConfigManager;
import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.model.CliDocument;
import com.example.ao_wiki_chat.cli.model.CliDocumentUpload;
import picocli.CommandLine;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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

    @Override
    public void run() {
        try {
            // Validate file
            Path path = Paths.get(filePath);
            DocumentCommandUtils.validateFile(path);

            // Initialize API client
            ApiClient apiClient = createApiClient();

            // Upload document
            System.out.println("Uploading document: " + path.getFileName());
            CliDocumentUpload uploadResponse = apiClient.uploadDocument(path);

            if ("json".equalsIgnoreCase(format)) {
                System.out.println(uploadResponse);
            } else {
                System.out.println("Document uploaded successfully!");
                System.out.println("Document ID: " + uploadResponse.documentId());
                System.out.println("Status: " + uploadResponse.status());
                System.out.println("Filename: " + uploadResponse.filename());
            }

            // Wait for processing if requested
            if (wait) {
                waitForProcessing(apiClient, uploadResponse.documentId(), timeoutSeconds);
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            throw new CommandLine.ParameterException(spec.commandLine(), e.getMessage());
        } catch (ApiException e) {
            System.err.println("API Error: " + e.getMessage());
            throw new CommandLine.ExecutionException(spec.commandLine(), "API Error: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            throw new CommandLine.ExecutionException(spec.commandLine(), "Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Waits for document processing to complete, polling the status.
     *
     * @param apiClient  the API client
     * @param documentId the document ID
     * @param timeoutSeconds the timeout in seconds
     */
    private void waitForProcessing(ApiClient apiClient, UUID documentId, int timeoutSeconds) {
        System.out.println("Waiting for processing to complete (timeout: " + timeoutSeconds + "s)...");

        long startTime = System.currentTimeMillis();
        long timeoutMillis = timeoutSeconds * 1000L;
        int pollIntervalSeconds = 2;

        try {
            while (true) {
                // Check timeout
                if (System.currentTimeMillis() - startTime > timeoutMillis) {
                    System.err.println("Timeout waiting for document processing");
                    throw new CommandLine.ExecutionException(spec.commandLine(), "Timeout waiting for document processing");
                }

                // Poll document status
                CliDocument document = apiClient.getDocument(documentId);
                String status = document.status();

                // Show progress
                System.out.print("Status: " + status);
                if ("PROCESSING".equals(status)) {
                    System.out.print(" (waiting...)");
                }
                System.out.println();

                // Check if processing is complete
                if ("COMPLETED".equals(status)) {
                    System.out.println("Document processing completed successfully!");
                    return;
                } else if ("FAILED".equals(status)) {
                    System.err.println("Document processing failed");
                    throw new CommandLine.ExecutionException(spec.commandLine(), "Document processing failed");
                }

                // Wait before next poll
                Thread.sleep(pollIntervalSeconds * 1000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while waiting for processing");
            throw new CommandLine.ExecutionException(spec.commandLine(), "Interrupted while waiting for processing", e);
        } catch (ApiException e) {
            System.err.println("Error checking document status: " + e.getMessage());
            throw new CommandLine.ExecutionException(spec.commandLine(), "Error checking document status: " + e.getMessage(), e);
        }
    }
}
