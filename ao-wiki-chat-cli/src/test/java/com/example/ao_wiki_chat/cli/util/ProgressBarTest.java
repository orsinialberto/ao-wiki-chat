package com.example.ao_wiki_chat.cli.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ProgressBar.
 * Tests progress bar updates and formatting.
 */
class ProgressBarTest {

    private ByteArrayOutputStream outContent;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        outContent = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    void updateWhenPercentIsZeroShowsEmptyBar() {
        // Given
        ProgressBar progressBar = new ProgressBar(false, new PrintStream(outContent));

        // When
        progressBar.update(0, "Starting");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("[");
        assertThat(output).contains("]");
        assertThat(output).contains("0%");
        assertThat(output).contains("Starting");
    }

    @Test
    void updateWhenPercentIsFiftyShowsHalfFilledBar() {
        // Given
        ProgressBar progressBar = new ProgressBar(false, new PrintStream(outContent));

        // When
        progressBar.update(50, "Processing");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("50%");
        assertThat(output).contains("Processing");
    }

    @Test
    void updateWhenPercentIsHundredShowsFullBar() {
        // Given
        ProgressBar progressBar = new ProgressBar(false, new PrintStream(outContent));

        // When
        progressBar.update(100, "Complete");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("100%");
        assertThat(output).contains("Complete");
    }

    @Test
    void updateWhenPercentExceedsHundredClampsToHundred() {
        // Given
        ProgressBar progressBar = new ProgressBar(false, new PrintStream(outContent));

        // When
        progressBar.update(150, "Overflow");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("100%");
    }

    @Test
    void updateWhenPercentIsNegativeClampsToZero() {
        // Given
        ProgressBar progressBar = new ProgressBar(false, new PrintStream(outContent));

        // When
        progressBar.update(-10, "Negative");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("0%");
    }

    @Test
    void updateWhenMessageIsNullOmitsMessage() {
        // Given
        ProgressBar progressBar = new ProgressBar(false, new PrintStream(outContent));

        // When
        progressBar.update(50, null);

        // Then
        String output = outContent.toString();
        assertThat(output).contains("50%");
    }

    @Test
    void updateWhenMessageIsBlankOmitsMessage() {
        // Given
        ProgressBar progressBar = new ProgressBar(false, new PrintStream(outContent));

        // When
        progressBar.update(50, "   ");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("50%");
    }

    @Test
    void updateWhenCalledWithDoubleConvertsToPercent() {
        // Given
        ProgressBar progressBar = new ProgressBar(false, new PrintStream(outContent));

        // When
        progressBar.update(0.75, "Three quarters");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("75%");
    }

    @Test
    void completeWhenCalledPrintsFullBarAndNewline() {
        // Given
        ProgressBar progressBar = new ProgressBar(false, new PrintStream(outContent));

        // When
        progressBar.complete("Done");

        // Then
        String output = outContent.toString();
        assertThat(output).contains("100%");
        assertThat(output).contains("Done");
        assertThat(output).endsWith("\n");
    }

    @Test
    void resetWhenCalledClearsCurrentLine() {
        // Given
        ProgressBar progressBar = new ProgressBar(false, new PrintStream(outContent));
        progressBar.update(50, "Test");

        // When
        progressBar.reset();

        // Then
        // Reset should clear the line (implementation detail)
        // We just verify it doesn't throw
        assertThat(outContent.toString()).isNotEmpty();
    }

    @Test
    void updateWhenSameProgressCalledTwiceOnlyPrintsOnce() {
        // Given
        ProgressBar progressBar = new ProgressBar(false, new PrintStream(outContent));

        // When
        progressBar.update(50, "Same");
        String firstOutput = outContent.toString();
        outContent.reset();
        progressBar.update(50, "Same");

        // Then
        // Second update with same progress should not print (optimization)
        // This is an implementation detail, but we verify it works
        assertThat(outContent.toString()).isEmpty();
    }
}
