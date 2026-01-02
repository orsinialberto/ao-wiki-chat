package com.example.ao_wiki_chat.cli.util;

import com.example.ao_wiki_chat.cli.exception.ApiException;
import com.example.ao_wiki_chat.cli.exception.ConfigException;
import com.example.ao_wiki_chat.cli.exception.CliException;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionHandlerTest {

    @Test
    void handleExecutionExceptionWhenParameterExceptionReturnsUsageError() {
        ExceptionHandler handler = new ExceptionHandler(false);
        CommandLine commandLine = new CommandLine(new TestCommand());
        CommandLine.ParseResult parseResult = commandLine.parseArgs();

        CommandLine.ParameterException exception = new CommandLine.ParameterException(
                commandLine, "Invalid parameter"
        );

        int exitCode = handler.handleExecutionException(exception, commandLine, parseResult);
        assertThat(exitCode).isEqualTo(ExitCodes.USAGE_ERROR);
    }

    @Test
    void handleExecutionExceptionWhenApiExceptionReturnsApiError() {
        ExceptionHandler handler = new ExceptionHandler(false);
        CommandLine commandLine = new CommandLine(new TestCommand());
        CommandLine.ParseResult parseResult = commandLine.parseArgs();

        ApiException exception = new ApiException("API error", 500);

        int exitCode = handler.handleExecutionException(exception, commandLine, parseResult);
        assertThat(exitCode).isEqualTo(ExitCodes.API_ERROR);
    }

    @Test
    void handleExecutionExceptionWhenConfigExceptionReturnsConfigError() {
        ExceptionHandler handler = new ExceptionHandler(false);
        CommandLine commandLine = new CommandLine(new TestCommand());
        CommandLine.ParseResult parseResult = commandLine.parseArgs();

        ConfigException exception = new ConfigException("Config error");

        int exitCode = handler.handleExecutionException(exception, commandLine, parseResult);
        assertThat(exitCode).isEqualTo(ExitCodes.CONFIG_ERROR);
    }

    @Test
    void handleExecutionExceptionWhenCliExceptionReturnsUsageError() {
        ExceptionHandler handler = new ExceptionHandler(false);
        CommandLine commandLine = new CommandLine(new TestCommand());
        CommandLine.ParseResult parseResult = commandLine.parseArgs();

        CliException exception = new CliException("CLI error");

        int exitCode = handler.handleExecutionException(exception, commandLine, parseResult);
        assertThat(exitCode).isEqualTo(ExitCodes.USAGE_ERROR);
    }

    @Test
    void handleExecutionExceptionWhenIllegalArgumentExceptionReturnsUsageError() {
        ExceptionHandler handler = new ExceptionHandler(false);
        CommandLine commandLine = new CommandLine(new TestCommand());
        CommandLine.ParseResult parseResult = commandLine.parseArgs();

        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        int exitCode = handler.handleExecutionException(exception, commandLine, parseResult);
        assertThat(exitCode).isEqualTo(ExitCodes.USAGE_ERROR);
    }

    @Test
    void handleExecutionExceptionWhenGenericExceptionReturnsGeneralError() {
        ExceptionHandler handler = new ExceptionHandler(false);
        CommandLine commandLine = new CommandLine(new TestCommand());
        CommandLine.ParseResult parseResult = commandLine.parseArgs();

        RuntimeException exception = new RuntimeException("Generic error");

        int exitCode = handler.handleExecutionException(exception, commandLine, parseResult);
        assertThat(exitCode).isEqualTo(ExitCodes.GENERAL_ERROR);
    }

    @Test
    void handleExecutionExceptionWhenApiExceptionInCauseReturnsApiError() {
        ExceptionHandler handler = new ExceptionHandler(false);
        CommandLine commandLine = new CommandLine(new TestCommand());
        CommandLine.ParseResult parseResult = commandLine.parseArgs();

        Exception exception = new Exception("Wrapper", new ApiException("API error", 500));

        int exitCode = handler.handleExecutionException(exception, commandLine, parseResult);
        assertThat(exitCode).isEqualTo(ExitCodes.API_ERROR);
    }

    // Test command for exception handler tests
    @CommandLine.Command(name = "test")
    static class TestCommand implements Runnable {
        @Override
        public void run() {
            // Empty command for testing
        }
    }
}
