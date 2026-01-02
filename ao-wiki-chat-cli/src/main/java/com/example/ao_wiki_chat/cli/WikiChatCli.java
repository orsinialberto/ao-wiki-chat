package com.example.ao_wiki_chat.cli;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.example.ao_wiki_chat.cli.command.chat.*;
import com.example.ao_wiki_chat.cli.command.document.*;
import com.example.ao_wiki_chat.cli.command.system.*;
import com.example.ao_wiki_chat.cli.util.ExceptionHandler;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;

/**
 * WikiChat CLI - Command-line interface for WikiChat RAG System.
 * 
 * This is the main entry point for the CLI application.
 */
@Command(
	name = "wikichat",
	mixinStandardHelpOptions = true,
	version = "0.0.1-SNAPSHOT",
	description = "WikiChat CLI - Command-line interface for WikiChat RAG System",
	subcommands = {
		HelpCommand.class,
		UploadCommand.class,
		ListCommand.class,
		ShowCommand.class,
		DeleteCommand.class,
		ChunksCommand.class,
		QueryCommand.class,
		HistoryCommand.class,
		ClearCommand.class,
		InteractiveCommand.class,
		HealthCommand.class,
		ConfigCommand.class
	}
)
public class WikiChatCli implements Runnable {

	@CommandLine.Spec
	CommandLine.Model.CommandSpec spec;

	@Option(
		names = {"--verbose", "-v"},
		description = "Enable verbose output and detailed error messages"
	)
	boolean verbose;

	@Override
	public void run() {
		// If no subcommand is provided, show usage
		CommandLine.usage(this, System.out);
	}

	public static void main(String[] args) {
		WikiChatCli cli = new WikiChatCli();
		CommandLine commandLine = new CommandLine(cli);

		// Check for verbose flag before parsing
		boolean verbose = false;
		for (String arg : args) {
			if ("--verbose".equals(arg) || "-v".equals(arg)) {
				verbose = true;
				break;
			}
		}

		// Configure logging level based on verbose flag
		if (verbose) {
			configureLogging(Level.DEBUG);
		}

		// Set exception handler
		commandLine.setExecutionExceptionHandler(new ExceptionHandler(verbose));

		int exitCode = commandLine.execute(args);
		System.exit(exitCode);
	}

	/**
	 * Configures the logging level for the application.
	 *
	 * @param level the log level to set
	 */
	private static void configureLogging(Level level) {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
		rootLogger.setLevel(level);

		// Set specific loggers to DEBUG in verbose mode
		Logger apiClientLogger = loggerContext.getLogger("com.example.ao_wiki_chat.cli.config.ApiClient");
		apiClientLogger.setLevel(Level.DEBUG);
	}
}
