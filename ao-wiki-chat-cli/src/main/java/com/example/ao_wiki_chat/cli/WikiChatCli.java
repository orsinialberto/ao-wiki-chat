package com.example.ao_wiki_chat.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

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
		HelpCommand.class
		// TODO: Add subcommands here
	}
)
public class WikiChatCli implements Runnable {

	@CommandLine.Spec
	CommandLine.Model.CommandSpec spec;

	@Override
	public void run() {
		// If no subcommand is provided, show usage
		CommandLine.usage(this, System.out);
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new WikiChatCli()).execute(args);
		System.exit(exitCode);
	}
}
