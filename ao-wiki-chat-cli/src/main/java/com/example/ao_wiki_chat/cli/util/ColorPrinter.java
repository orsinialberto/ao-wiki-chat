package com.example.ao_wiki_chat.cli.util;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.PrintStream;

/**
 * Utility class for printing colored output to the terminal.
 * Supports ANSI colors via Jansi library and respects the output.colors configuration.
 */
public final class ColorPrinter {

    private final boolean colorsEnabled;
    private final PrintStream out;
    private final PrintStream err;

    /**
     * Creates a ColorPrinter instance.
     *
     * @param colorsEnabled whether colors should be enabled
     */
    public ColorPrinter(boolean colorsEnabled) {
        this.colorsEnabled = colorsEnabled;
        this.out = System.out;
        this.err = System.err;
        if (colorsEnabled) {
            AnsiConsole.systemInstall();
        }
    }

    /**
     * Creates a ColorPrinter instance with custom output streams.
     *
     * @param colorsEnabled whether colors should be enabled
     * @param out           the output stream for normal messages
     * @param err           the error stream for error messages
     */
    public ColorPrinter(boolean colorsEnabled, PrintStream out, PrintStream err) {
        this.colorsEnabled = colorsEnabled;
        this.out = out;
        this.err = err;
        if (colorsEnabled) {
            AnsiConsole.systemInstall();
        }
    }

    /**
     * Prints a success message in green.
     *
     * @param message the message to print
     */
    public void success(String message) {
        printColored(message, Ansi.Color.GREEN, "✓", out);
    }

    /**
     * Prints an error message in red.
     *
     * @param message the message to print
     */
    public void error(String message) {
        printColored(message, Ansi.Color.RED, "✗", err);
    }

    /**
     * Prints an info message in blue.
     *
     * @param message the message to print
     */
    public void info(String message) {
        printColored(message, Ansi.Color.BLUE, "ℹ", out);
    }

    /**
     * Prints a warning message in yellow.
     *
     * @param message the message to print
     */
    public void warning(String message) {
        printColored(message, Ansi.Color.YELLOW, "⚠", out);
    }

    /**
     * Prints a colored message with optional emoji/icon.
     *
     * @param message the message to print
     * @param color   the ANSI color to use
     * @param icon    optional emoji/icon prefix (null to skip)
     * @param stream  the stream to print to
     */
    private void printColored(String message, Ansi.Color color, String icon, PrintStream stream) {
        if (colorsEnabled) {
            Ansi ansi = Ansi.ansi();
            ansi.fg(color);
            if (icon != null) {
                ansi.a(icon).a(" ");
            }
            ansi.a(message).reset();
            stream.println(ansi);
        } else {
            if (icon != null) {
                stream.println(icon + " " + message);
            } else {
                stream.println(message);
            }
        }
    }

    /**
     * Prints a plain message without color.
     *
     * @param message the message to print
     */
    public void println(String message) {
        out.println(message);
    }

    /**
     * Prints a plain error message without color.
     *
     * @param message the message to print
     */
    public void printlnError(String message) {
        err.println(message);
    }

    /**
     * Formats a message with color as a string (without printing).
     *
     * @param message the message to format
     * @param color   the ANSI color to use
     * @param icon    optional emoji/icon prefix (null to skip)
     * @return formatted message string
     */
    public String formatColored(String message, Ansi.Color color, String icon) {
        if (colorsEnabled) {
            Ansi ansi = Ansi.ansi();
            ansi.fg(color);
            if (icon != null) {
                ansi.a(icon).a(" ");
            }
            ansi.a(message).reset();
            return ansi.toString();
        } else {
            if (icon != null) {
                return icon + " " + message;
            } else {
                return message;
            }
        }
    }
}
