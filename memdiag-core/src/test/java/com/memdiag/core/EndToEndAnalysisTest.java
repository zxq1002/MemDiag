package com.memdiag.core.functional;

import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.output.HtmlFormatter;
import com.memdiag.core.output.JsonFormatter;
import com.memdiag.core.output.ReportFormatter;
import com.memdiag.core.output.TextFormatter;
import com.memdiag.core.thread.ThreadDump;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end functional test for the complete analysis workflow.
 * Tests the full pipeline: data creation -> analysis -> report generation.
 */
@Tag("functional")
class EndToEndAnalysisTest extends FunctionalTestBase {

    @Test
    void completeTextReportWorkflow() {
        // Step 1: Create test data
        HeapHistogram histogram = createTestHistogram(8, 2000);
        ThreadDump threadDump = createTestThreadDump(10);

        // Step 2: Create diagnosis result
        var diagnosis = createTestDiagnosis(histogram, threadDump);

        // Step 3: Generate text report
        ReportFormatter formatter = new TextFormatter();
        String report = formatter.format(histogram, threadDump, diagnosis);

        // Step 4: Verify
        assertThat(report).isNotEmpty();
        assertThat(report).contains("堆直方图");
        assertThat(report).contains("线程分析");
        assertThat(report).contains("诊断概要");
        assertThat(report).contains("总对象数");
        assertThat(report).contains("总大小");
    }

    @Test
    void completeHtmlReportWorkflow() {
        // Step 1: Create test data
        HeapHistogram histogram = createTestHistogram(8, 2000);
        ThreadDump threadDump = createTestThreadDump(10);
        var diagnosis = createTestDiagnosis(histogram, threadDump);

        // Step 2: Generate HTML report
        ReportFormatter formatter = new HtmlFormatter();
        String report = formatter.format(histogram, threadDump, diagnosis);

        // Step 3: Verify
        assertThat(report).isNotEmpty();
        assertThat(report).startsWith("<!DOCTYPE html>");
        assertThat(report).contains("</html>");
        assertThat(report).contains("<style");
        assertThat(report).contains("堆直方图");
        assertThat(report).contains("线程分析");
    }

    @Test
    void completeJsonReportWorkflow() {
        // Step 1: Create test data
        HeapHistogram histogram = createTestHistogram(8, 2000);
        ThreadDump threadDump = createTestThreadDump(10);
        var diagnosis = createTestDiagnosis(histogram, threadDump);

        // Step 2: Generate JSON report
        ReportFormatter formatter = new JsonFormatter();
        String report = formatter.format(histogram, threadDump, diagnosis);

        // Step 3: Verify
        assertThat(report).isNotEmpty();
        assertThat(report).startsWith("{");
        assertThat(report).endsWith("}");
        assertThat(report).contains("heapHistogram");
        assertThat(report).contains("threadDump");
        assertThat(report).contains("diagnosis");
    }

    @Test
    void allReportFormatsContainSameKeyData() {
        // Create test data once
        HeapHistogram histogram = createTestHistogram(6, 1500);
        ThreadDump threadDump = createTestThreadDump(8);
        var diagnosis = createTestDiagnosis(histogram, threadDump);

        // Generate all formats
        String textReport = new TextFormatter().format(histogram, threadDump, diagnosis);
        String htmlReport = new HtmlFormatter().format(histogram, threadDump, diagnosis);
        String jsonReport = new JsonFormatter().format(histogram, threadDump, diagnosis);

        // All should be non-empty
        assertThat(textReport).isNotEmpty();
        assertThat(htmlReport).isNotEmpty();
        assertThat(jsonReport).isNotEmpty();

        // All should have reasonable size
        assertThat(textReport.length()).isGreaterThan(100);
        assertThat(htmlReport.length()).isGreaterThan(100);
        assertThat(jsonReport.length()).isGreaterThan(100);

        // All should contain key class names
        assertThat(textReport).contains("java.lang.String");
        assertThat(htmlReport).contains("java.lang.String");
        assertThat(jsonReport).contains("java.lang.String");
    }

    @Test
    void reportsCanBeSavedToFiles() throws Exception {
        HeapHistogram histogram = createTestHistogram(5, 1000);
        ThreadDump threadDump = createTestThreadDump(5);
        var diagnosis = createTestDiagnosis(histogram, threadDump);

        List<Path> tempFiles = new ArrayList<>();

        try {
            // Save text report
            ReportFormatter textFormatter = new TextFormatter();
            Path textFile = Files.createTempFile("memdiag-test", ".txt");
            tempFiles.add(textFile);
            Files.writeString(textFile, textFormatter.format(histogram, threadDump, diagnosis));
            assertThat(Files.size(textFile)).isGreaterThan(0L);

            // Save HTML report
            ReportFormatter htmlFormatter = new HtmlFormatter();
            Path htmlFile = Files.createTempFile("memdiag-test", ".html");
            tempFiles.add(htmlFile);
            Files.writeString(htmlFile, htmlFormatter.format(histogram, threadDump, diagnosis));
            assertThat(Files.size(htmlFile)).isGreaterThan(0L);

            // Save JSON report
            ReportFormatter jsonFormatter = new JsonFormatter();
            Path jsonFile = Files.createTempFile("memdiag-test", ".json");
            tempFiles.add(jsonFile);
            Files.writeString(jsonFile, jsonFormatter.format(histogram, threadDump, diagnosis));
            assertThat(Files.size(jsonFile)).isGreaterThan(0L);

            // Verify files can be read back
            String readText = Files.readString(textFile);
            assertThat(readText).isNotEmpty();

            String readHtml = Files.readString(htmlFile);
            assertThat(readHtml).isNotEmpty();

            String readJson = Files.readString(jsonFile);
            assertThat(readJson).isNotEmpty();

        } finally {
            // Cleanup
            for (Path file : tempFiles) {
                Files.deleteIfExists(file);
            }
        }
    }

    @Test
    void handlesLargeDatasets() {
        // Create large histogram
        HeapHistogram largeHistogram = createTestHistogram(20, 10000);
        ThreadDump largeThreadDump = createTestThreadDump(50);
        var diagnosis = createTestDiagnosis(largeHistogram, largeThreadDump);

        // All formatters should handle it
        ReportFormatter textFormatter = new TextFormatter();
        String textReport = textFormatter.format(largeHistogram, largeThreadDump, diagnosis);
        assertThat(textReport).isNotEmpty();

        ReportFormatter htmlFormatter = new HtmlFormatter();
        String htmlReport = htmlFormatter.format(largeHistogram, largeThreadDump, diagnosis);
        assertThat(htmlReport).isNotEmpty();

        ReportFormatter jsonFormatter = new JsonFormatter();
        String jsonReport = jsonFormatter.format(largeHistogram, largeThreadDump, diagnosis);
        assertThat(jsonReport).isNotEmpty();
    }

    @Test
    void handlesEmptyDataGracefully() {
        HeapHistogram emptyHistogram = new HeapHistogram();
        ThreadDump emptyThreadDump = new ThreadDump();
        var emptyDiagnosis = createTestDiagnosis(emptyHistogram, emptyThreadDump);

        // All formatters should handle empty data
        ReportFormatter textFormatter = new TextFormatter();
        String textReport = textFormatter.format(emptyHistogram, emptyThreadDump, emptyDiagnosis);
        assertThat(textReport).isNotEmpty();

        ReportFormatter htmlFormatter = new HtmlFormatter();
        String htmlReport = htmlFormatter.format(emptyHistogram, emptyThreadDump, emptyDiagnosis);
        assertThat(htmlReport).isNotEmpty();

        ReportFormatter jsonFormatter = new JsonFormatter();
        String jsonReport = jsonFormatter.format(emptyHistogram, emptyThreadDump, emptyDiagnosis);
        assertThat(jsonReport).isNotEmpty();
    }

    @Test
    void multipleConsecutiveAnalyses() {
        List<Object> memoryHog = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            // Apply some memory pressure
            for (int j = 0; j < 200; j++) {
                memoryHog.add(new byte[1024]);
            }

            // Full analysis cycle
            HeapHistogram histogram = createTestHistogram(5, 1000 + i * 500);
            ThreadDump threadDump = createTestThreadDump(10);
            var diagnosis = createTestDiagnosis(histogram, threadDump);

            // Generate and verify reports
            String textReport = new TextFormatter().format(histogram, threadDump, diagnosis);
            String htmlReport = new HtmlFormatter().format(histogram, threadDump, diagnosis);
            String jsonReport = new JsonFormatter().format(histogram, threadDump, diagnosis);

            // Verify each cycle produces valid output
            assertThat(textReport).isNotEmpty();
            assertThat(htmlReport).isNotEmpty();
            assertThat(jsonReport).isNotEmpty();
            assertThat(histogram.getTotalObjects()).isGreaterThan(0);
            assertThat(threadDump.getThreadCount()).isGreaterThan(0);
        }
    }
}
