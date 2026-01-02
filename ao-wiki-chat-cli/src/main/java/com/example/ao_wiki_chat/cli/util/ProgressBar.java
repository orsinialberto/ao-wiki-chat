package com.example.ao_wiki_chat.cli.util;

import java.io.PrintStream;

/**
 * Utility class for displaying progress bars in the terminal.
 * Supports real-time updates for async operations.
 */
public final class ProgressBar {

    private static final int BAR_WIDTH = 20;
    private static final String FILLED_CHAR = "█";
    private static final String EMPTY_CHAR = "░";

    private final PrintStream out;
    private final boolean colorsEnabled;
    private String lastLine = "";

    /**
     * Creates a ProgressBar instance.
     *
     * @param colorsEnabled whether colors should be enabled
     */
    public ProgressBar(boolean colorsEnabled) {
        this.colorsEnabled = colorsEnabled;
        this.out = System.out;
    }

    /**
     * Creates a ProgressBar instance with a custom output stream.
     *
     * @param colorsEnabled whether colors should be enabled
     * @param out           the output stream
     */
    public ProgressBar(boolean colorsEnabled, PrintStream out) {
        this.colorsEnabled = colorsEnabled;
        this.out = out;
    }

    /**
     * Updates the progress bar with the current progress.
     *
     * @param progress the progress value (0.0 to 1.0)
     * @param message  optional message to display after the progress bar
     */
    public void update(double progress, String message) {
        update((int) (progress * 100), message);
    }

    /**
     * Updates the progress bar with the current progress percentage.
     *
     * @param percent the progress percentage (0 to 100)
     * @param message optional message to display after the progress bar
     */
    public void update(int percent, String message) {
        percent = Math.max(0, Math.min(100, percent));
        int filled = (int) (BAR_WIDTH * percent / 100.0);
        int empty = BAR_WIDTH - filled;

        StringBuilder bar = new StringBuilder();
        bar.append("[");
        bar.append(FILLED_CHAR.repeat(filled));
        bar.append(EMPTY_CHAR.repeat(empty));
        bar.append("] ");
        bar.append(String.format("%3d%%", percent));

        if (message != null && !message.isBlank()) {
            bar.append(" ").append(message);
        }

        String newLine = bar.toString();
        if (!newLine.equals(lastLine)) {
            clearLine();
            out.print(newLine);
            out.flush();
            lastLine = newLine;
        }
    }

    /**
     * Completes the progress bar and prints a newline.
     *
     * @param message optional completion message
     */
    public void complete(String message) {
        update(100, message);
        out.println();
        lastLine = "";
    }

    /**
     * Clears the current line.
     */
    private void clearLine() {
        if (!lastLine.isEmpty()) {
            out.print("\r");
            out.print(" ".repeat(Math.max(0, lastLine.length())));
            out.print("\r");
            out.flush();
        }
    }

    /**
     * Resets the progress bar.
     */
    public void reset() {
        clearLine();
        lastLine = "";
    }
}
