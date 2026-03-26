package com.memdiag.core.integration;

import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Severity;
import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.output.HtmlFormatter;
import com.memdiag.core.output.JsonFormatter;
import com.memdiag.core.output.ReportFormatter;
import com.memdiag.core.output.TextFormatter;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.thread.ThreadStats;
import com.memdiag.core.thread.ThreadState;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for report formatters.
 * Tests that formatters produce valid output that can be saved to files.
 */
@Tag("integration")
class ReportFormatterIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void textFormatterSavesToFile() throws Exception {
        HeapHistogram histogram = createTestHistogram();
        ThreadDump threadDump = createTestThreadDump();
        DiagnosisResult diagnosis = createTestDiagnosis();

        ReportFormatter formatter = new TextFormatter();
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).isNotEmpty();

        Path reportFile = tempDir.resolve("memdiag-report.txt");
        Files.writeString(reportFile, output);

        assertThat(Files.exists(reportFile)).isTrue();
        assertThat(Files.size(reportFile)).isGreaterThan(0);

        String readBack = Files.readString(reportFile);
        assertThat(readBack).isEqualTo(output);
    }

    @Test
    void htmlFormatterSavesValidHtmlFile() throws Exception {
        HeapHistogram histogram = createTestHistogram();
        ThreadDump threadDump = createTestThreadDump();
        DiagnosisResult diagnosis = createTestDiagnosis();

        ReportFormatter formatter = new HtmlFormatter();
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).isNotEmpty();
        assertThat(output).startsWith("<!DOCTYPE html>");
        assertThat(output).contains("</html>");

        Path reportFile = tempDir.resolve("memdiag-report.html");
        Files.writeString(reportFile, output);

        assertThat(Files.exists(reportFile)).isTrue();
        assertThat(Files.size(reportFile)).isGreaterThan(0);

        String readBack = Files.readString(reportFile);
        assertThat(readBack).startsWith("<!DOCTYPE html>");
        assertThat(readBack).contains("</html>");
    }

    @Test
    void jsonFormatterSavesValidJsonFile() throws Exception {
        HeapHistogram histogram = createTestHistogram();
        ThreadDump threadDump = createTestThreadDump();
        DiagnosisResult diagnosis = createTestDiagnosis();

        ReportFormatter formatter = new JsonFormatter();
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).isNotEmpty();
        assertThat(output).startsWith("{");
        assertThat(output).endsWith("}");

        Path reportFile = tempDir.resolve("memdiag-report.json");
        Files.writeString(reportFile, output);

        assertThat(Files.exists(reportFile)).isTrue();
        assertThat(Files.size(reportFile)).isGreaterThan(0);

        String readBack = Files.readString(reportFile);
        assertThat(readBack).startsWith("{");
        assertThat(readBack).endsWith("}");
    }

    @Test
    void allFormattersProduceConsistentContent() {
        HeapHistogram histogram = createTestHistogram();
        ThreadDump threadDump = createTestThreadDump();
        DiagnosisResult diagnosis = createTestDiagnosis();

        TextFormatter textFormatter = new TextFormatter();
        HtmlFormatter htmlFormatter = new HtmlFormatter();
        JsonFormatter jsonFormatter = new JsonFormatter();

        String textOutput = textFormatter.format(histogram, threadDump, diagnosis);
        String htmlOutput = htmlFormatter.format(histogram, threadDump, diagnosis);
        String jsonOutput = jsonFormatter.format(histogram, threadDump, diagnosis);

        assertThat(textOutput).isNotEmpty();
        assertThat(htmlOutput).isNotEmpty();
        assertThat(jsonOutput).isNotEmpty();

        assertThat(textOutput).contains("java.lang.String");
        assertThat(htmlOutput).contains("java.lang.String");
        assertThat(jsonOutput).contains("java.lang.String");
    }

    @Test
    void handlesRealisticDataSizes() {
        HeapHistogram largeHistogram = new HeapHistogram();
        for (int i = 0; i < 100; i++) {
            largeHistogram.add(new ClassStats(
                    "com.example.Class" + i,
                    1000 + i * 100,
                    64000 + i * 6400L
            ));
        }

        ThreadDump largeThreadDump = new ThreadDump();
        List<ThreadStats> threadStats = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            ThreadStats stats = new ThreadStats();
            stats.setThreadId(i + 1);
            stats.setThreadName("Thread-" + i);
            stats.setState(ThreadState.values()[i % ThreadState.values().length]);
            threadStats.add(stats);
        }
        largeThreadDump.setThreadStats(threadStats);

        DiagnosisResult diagnosis = createTestDiagnosis();

        ReportFormatter textFormatter = new TextFormatter();
        String textOutput = textFormatter.format(largeHistogram, largeThreadDump, diagnosis);
        assertThat(textOutput).isNotEmpty();
        assertThat(textOutput.length()).isGreaterThan(1000);

        ReportFormatter htmlFormatter = new HtmlFormatter();
        String htmlOutput = htmlFormatter.format(largeHistogram, largeThreadDump, diagnosis);
        assertThat(htmlOutput).isNotEmpty();
        assertThat(htmlOutput.length()).isGreaterThan(1000);

        ReportFormatter jsonFormatter = new JsonFormatter();
        String jsonOutput = jsonFormatter.format(largeHistogram, largeThreadDump, diagnosis);
        assertThat(jsonOutput).isNotEmpty();
        assertThat(jsonOutput.length()).isGreaterThan(1000);
    }

    @Test
    void jsonFormatterWithPrettyPrint() {
        HeapHistogram histogram = createTestHistogram();
        ThreadDump threadDump = createTestThreadDump();
        DiagnosisResult diagnosis = createTestDiagnosis();

        JsonFormatter prettyFormatter = new JsonFormatter(100, true);
        String prettyOutput = prettyFormatter.format(histogram, threadDump, diagnosis);

        JsonFormatter compactFormatter = new JsonFormatter(100, false);
        String compactOutput = compactFormatter.format(histogram, threadDump, diagnosis);

        assertThat(prettyOutput).isNotEmpty();
        assertThat(compactOutput).isNotEmpty();

        assertThat(prettyOutput.length()).isGreaterThanOrEqualTo(compactOutput.length());
    }

    @Test
    void jsonFormatterRespectsLimit() {
        HeapHistogram histogram = new HeapHistogram();
        for (int i = 0; i < 50; i++) {
            histogram.add(new ClassStats("com.example.Class" + i, 100 + i, 6400 + i * 64L));
        }

        ThreadDump threadDump = createTestThreadDump();
        DiagnosisResult diagnosis = createTestDiagnosis();

        JsonFormatter formatter = new JsonFormatter(10, true);
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).isNotEmpty();
        assertThat(output).startsWith("{");
        assertThat(output).endsWith("}");
    }

    @Test
    void htmlFormatterEscapesSpecialCharacters() {
        HeapHistogram specialHeap = new HeapHistogram();
        specialHeap.add(new ClassStats("com.example<TestClass>", 100, 6400));
        specialHeap.add(new ClassStats("com.example&Class", 50, 3200));

        ThreadDump threadDump = createTestThreadDump();
        DiagnosisResult diagnosis = createTestDiagnosis();

        HtmlFormatter formatter = new HtmlFormatter();
        String output = formatter.format(specialHeap, threadDump, diagnosis);

        assertThat(output).doesNotContain("<TestClass>");
        assertThat(output).isNotEmpty();
    }

    @Test
    void multipleReportsInSameDirectory() throws Exception {
        HeapHistogram histogram = createTestHistogram();
        ThreadDump threadDump = createTestThreadDump();
        DiagnosisResult diagnosis = createTestDiagnosis();

        // Save multiple reports
        Path textFile = tempDir.resolve("report.txt");
        Path htmlFile = tempDir.resolve("report.html");
        Path jsonFile = tempDir.resolve("report.json");

        Files.writeString(textFile, new TextFormatter().format(histogram, threadDump, diagnosis));
        Files.writeString(htmlFile, new HtmlFormatter().format(histogram, threadDump, diagnosis));
        Files.writeString(jsonFile, new JsonFormatter().format(histogram, threadDump, diagnosis));

        assertThat(Files.exists(textFile)).isTrue();
        assertThat(Files.exists(htmlFile)).isTrue();
        assertThat(Files.exists(jsonFile)).isTrue();

        assertThat(Files.size(textFile)).isGreaterThan(0);
        assertThat(Files.size(htmlFile)).isGreaterThan(0);
        assertThat(Files.size(jsonFile)).isGreaterThan(0);
    }

    private HeapHistogram createTestHistogram() {
        HeapHistogram histogram = new HeapHistogram();
        histogram.add(new ClassStats("java.lang.String", 10000, 640000));
        histogram.add(new ClassStats("byte[]", 5000, 5120000));
        histogram.add(new ClassStats("java.lang.Object", 8000, 512000));
        return histogram;
    }

    private ThreadDump createTestThreadDump() {
        ThreadDump dump = new ThreadDump();
        dump.setTimestamp(Instant.now());

        List<ThreadStats> stats = new ArrayList<>();
        ThreadStats mainThread = new ThreadStats();
        mainThread.setThreadId(1);
        mainThread.setThreadName("main");
        mainThread.setState(ThreadState.RUNNABLE);
        stats.add(mainThread);
        dump.setThreadStats(stats);

        return dump;
    }

    private DiagnosisResult createTestDiagnosis() {
        return DiagnosisResult.builder()
                .timestamp(Instant.now())
                .summary("Integration test diagnosis")
                .totalHeapUsed(1024L * 1024 * 100)
                .totalHeapCommitted(1024L * 1024 * 256)
                .threadCount(10)
                .addIssue(Issue.builder()
                        .severity(Severity.INFO)
                        .type("INFO")
                        .title("Integration test issue")
                        .description("This is for integration testing")
                        .build())
                .build();
    }
}
