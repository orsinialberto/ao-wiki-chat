package com.example.ao_wiki_chat.cli.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ColorPrinter.
 * Tests colored output with and without colors enabled.
 */
class ColorPrinterTest {

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void successWhenColorsEnabledPrintsGreenMessage() {
        // Given
        ColorPrinter printer = new ColorPrinter(true, new PrintStream(outContent), new PrintStream(errContent));

        // When
        printer.success("Operation completed");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("Operation completed");
        assertThat(output).contains("✓");
    }

    @Test
    void successWhenColorsDisabledPrintsPlainMessage() {
        // Given
        ColorPrinter printer = new ColorPrinter(false, new PrintStream(outContent), new PrintStream(errContent));

        // When
        printer.success("Operation completed");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("✓ Operation completed");
        assertThat(output).doesNotContain("\u001B"); // No ANSI escape codes
    }

    @Test
    void errorWhenColorsEnabledPrintsRedMessage() {
        // Given
        ColorPrinter printer = new ColorPrinter(true, new PrintStream(outContent), new PrintStream(errContent));

        // When
        printer.error("Operation failed");

        // Then
        String output = errContent.toString();
        assertThat(output).contains("Operation failed");
        assertThat(output).contains("✗");
    }

    @Test
    void errorWhenColorsDisabledPrintsPlainMessage() {
        // Given
        ColorPrinter printer = new ColorPrinter(false, new PrintStream(outContent), new PrintStream(errContent));

        // When
        printer.error("Operation failed");

        // Then
        String output = errContent.toString();
        assertThat(output).contains("✗ Operation failed");
        assertThat(output).doesNotContain("\u001B");
    }

    @Test
    void infoWhenColorsEnabledPrintsBlueMessage() {
        // Given
        ColorPrinter printer = new ColorPrinter(true, new PrintStream(outContent), new PrintStream(errContent));

        // When
        printer.info("Processing data");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("Processing data");
        assertThat(output).contains("ℹ");
    }

    @Test
    void infoWhenColorsDisabledPrintsPlainMessage() {
        // Given
        ColorPrinter printer = new ColorPrinter(false, new PrintStream(outContent), new PrintStream(errContent));

        // When
        printer.info("Processing data");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("ℹ Processing data");
    }

    @Test
    void warningWhenColorsEnabledPrintsYellowMessage() {
        // Given
        ColorPrinter printer = new ColorPrinter(true, new PrintStream(outContent), new PrintStream(errContent));

        // When
        printer.warning("This is a warning");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("This is a warning");
        assertThat(output).contains("⚠");
    }

    @Test
    void warningWhenColorsDisabledPrintsPlainMessage() {
        // Given
        ColorPrinter printer = new ColorPrinter(false, new PrintStream(outContent), new PrintStream(errContent));

        // When
        printer.warning("This is a warning");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("⚠ This is a warning");
    }

    @Test
    void printlnWhenCalledPrintsPlainMessage() {
        // Given
        ColorPrinter printer = new ColorPrinter(true, new PrintStream(outContent), new PrintStream(errContent));

        // When
        printer.println("Plain message");

        // Then
        String output = outContent.toString();
        assertThat(output).isEqualTo("Plain message\n");
    }

    @Test
    void printlnErrorWhenCalledPrintsPlainErrorMessage() {
        // Given
        ColorPrinter printer = new ColorPrinter(true, new PrintStream(outContent), new PrintStream(errContent));

        // When
        printer.printlnError("Error message");

        // Then
        String output = errContent.toString();
        assertThat(output).isEqualTo("Error message\n");
    }

    @Test
    void formatColoredWhenColorsEnabledReturnsColoredString() {
        // Given
        ColorPrinter printer = new ColorPrinter(true);

        // When
        String result = printer.formatColored("Test message", org.fusesource.jansi.Ansi.Color.GREEN, "✓");

        // Then
        assertThat(result).contains("Test message");
        assertThat(result).contains("✓");
    }

    @Test
    void formatColoredWhenColorsDisabledReturnsPlainString() {
        // Given
        ColorPrinter printer = new ColorPrinter(false);

        // When
        String result = printer.formatColored("Test message", org.fusesource.jansi.Ansi.Color.GREEN, "✓");

        // Then
        assertThat(result).isEqualTo("✓ Test message");
        assertThat(result).doesNotContain("\u001B");
    }

    @Test
    void formatColoredWhenIconIsNullOmitsIcon() {
        // Given
        ColorPrinter printer = new ColorPrinter(false);

        // When
        String result = printer.formatColored("Test message", org.fusesource.jansi.Ansi.Color.GREEN, null);

        // Then
        assertThat(result).isEqualTo("Test message");
    }
}
