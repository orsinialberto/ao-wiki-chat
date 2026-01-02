package com.example.ao_wiki_chat.cli.util;

import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.exception.ConfigException;
import com.example.ao_wiki_chat.cli.exception.CliException;
import picocli.CommandLine;

/**
 * Exception handler for picocli commands.
 * Maps exceptions to appropriate exit codes and formats error messages.
 */
public final class ExceptionHandler implements CommandLine.IExecutionExceptionHandler {

    private final boolean verbose;

    /**
     * Creates an exception handler.
     *
     * @param verbose whether to show verbose error details
     */
    public ExceptionHandler(boolean verbose) {
        this.verbose = verbose;
    }

    @Override
    public int handleExecutionException(Exception exception, CommandLine commandLine, CommandLine.ParseResult parseResult) {
        String errorMessage = ErrorMessageFormatter.formatError(exception);
        int exitCode = determineExitCode(exception);

        // Print error message
        System.err.println("Error: " + errorMessage);

        // Print verbose details if enabled
        if (verbose && exception.getCause() != null) {
            System.err.println("\nDetails:");
            exception.getCause().printStackTrace(System.err);
        } else if (verbose) {
            exception.printStackTrace(System.err);
        }

        return exitCode;
    }

    /**
     * Determines the exit code based on the exception type.
     *
     * @param exception the exception
     * @return the appropriate exit code
     */
    private int determineExitCode(Exception exception) {
        if (exception instanceof CommandLine.ParameterException) {
            return ExitCodes.USAGE_ERROR;
        }

        if (exception instanceof ApiException) {
            return ExitCodes.API_ERROR;
        }

        if (exception instanceof ConfigException) {
            return ExitCodes.CONFIG_ERROR;
        }

        if (exception instanceof CliException) {
            return ExitCodes.USAGE_ERROR;
        }

        if (exception instanceof IllegalArgumentException) {
            return ExitCodes.USAGE_ERROR;
        }

        // Check cause chain
        Throwable cause = exception.getCause();
        if (cause instanceof ApiException) {
            return ExitCodes.API_ERROR;
        }
        if (cause instanceof ConfigException) {
            return ExitCodes.CONFIG_ERROR;
        }

        return ExitCodes.GENERAL_ERROR;
    }
}
