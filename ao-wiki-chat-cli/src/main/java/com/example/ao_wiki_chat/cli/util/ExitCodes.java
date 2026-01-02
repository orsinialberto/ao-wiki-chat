package com.example.ao_wiki_chat.cli.util;

/**
 * Exit codes for the CLI application.
 * Following standard Unix exit code conventions.
 */
public final class ExitCodes {

    /**
     * Success - command completed successfully.
     */
    public static final int SUCCESS = 0;

    /**
     * General error - unexpected or unhandled error.
     */
    public static final int GENERAL_ERROR = 1;

    /**
     * Usage error - invalid arguments or command usage.
     */
    public static final int USAGE_ERROR = 2;

    /**
     * API error - error communicating with or from the API server.
     */
    public static final int API_ERROR = 3;

    /**
     * Configuration error - error loading or validating configuration.
     */
    public static final int CONFIG_ERROR = 4;

    private ExitCodes() {
        // Utility class
    }
}
