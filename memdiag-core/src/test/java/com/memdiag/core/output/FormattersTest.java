package com.memdiag.core.output;

import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Severity;
import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.thread.ThreadStats;
import com.memdiag.core.thread.ThreadState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class FormattersTest {

    private HeapHistogram histogram;
    private ThreadDump threadDump;
    private DiagnosisResult diagnosis;

    @BeforeEach
    void setUp() {
        histogram = new HeapHistogram();
        histogram.add(new ClassStats("java.lang.String", 1000, 64000));
        histogram.add(new ClassStats("byte[]", 500, 512000));

        threadDump = new ThreadDump();
        List<ThreadStats> threadStats = new ArrayList<>();
        ThreadStats mainThread = new ThreadStats();
        mainThread.setThreadId(1);
        mainThread.setThreadName("main");
        mainThread.setState(ThreadState.RUNNABLE);
        threadStats.add(mainThread);
        threadDump.setThreadStats(threadStats);

        diagnosis = DiagnosisResult.builder()
                .summary("Test diagnosis")
                .addIssue(Issue.builder()
                        .severity(Severity.INFO)
                        .type("INFO")
                        .title("Test issue")
                        .description("Test description")
                        .build())
                .build();
    }

    @Test
    void textFormatterProducesNonEmptyOutput() {
        TextFormatter formatter = new TextFormatter();
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).isNotEmpty();
        assertThat(output).contains("java.lang.String");
        assertThat(output).contains("堆直方图");
    }

    @Test
    void textFormatterIncludesSummary() {
        TextFormatter formatter = new TextFormatter();
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).contains("总对象数");
        assertThat(output).contains("总大小");
    }

    @Test
    void htmlFormatterProducesValidHtml() {
        HtmlFormatter formatter = new HtmlFormatter();
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).isNotEmpty();
        assertThat(output).startsWith("<!DOCTYPE html>");
        assertThat(output).contains("</html>");
    }

    @Test
    void htmlFormatterIncludesCss() {
        HtmlFormatter formatter = new HtmlFormatter();
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).contains("<style");
        assertThat(output).contains("</style>");
    }

    @Test
    void htmlFormatterIncludesAllSections() {
        HtmlFormatter formatter = new HtmlFormatter();
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).contains("堆直方图");
        assertThat(output).contains("线程分析");
        assertThat(output).contains("诊断概要");
    }

    @Test
    void jsonFormatterProducesValidJson() {
        JsonFormatter formatter = new JsonFormatter();
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).isNotEmpty();
        assertThat(output).startsWith("{");
        assertThat(output).endsWith("}");
    }

    @Test
    void jsonFormatterContainsAllData() {
        JsonFormatter formatter = new JsonFormatter();
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).contains("heapHistogram");
        assertThat(output).contains("threadDump");
        assertThat(output).contains("diagnosis");
    }

    @Test
    void jsonFormatterContainsClassStats() {
        JsonFormatter formatter = new JsonFormatter();
        String output = formatter.format(histogram, threadDump, diagnosis);

        assertThat(output).contains("java.lang.String");
        assertThat(output).contains("objectCount");
        assertThat(output).contains("shallowBytes");
    }

    @Test
    void formattersHandleEmptyData() {
        HeapHistogram emptyHistogram = new HeapHistogram();
        ThreadDump emptyThreadDump = new ThreadDump();
        emptyThreadDump.setTimestamp(Instant.now());
        emptyThreadDump.setThreadStats(new ArrayList<>());
        DiagnosisResult emptyDiagnosis = DiagnosisResult.builder().build();

        TextFormatter textFormatter = new TextFormatter();
        assertThat(textFormatter.format(emptyHistogram, emptyThreadDump, emptyDiagnosis)).isNotEmpty();

        HtmlFormatter htmlFormatter = new HtmlFormatter();
        assertThat(htmlFormatter.format(emptyHistogram, emptyThreadDump, emptyDiagnosis)).isNotEmpty();

        JsonFormatter jsonFormatter = new JsonFormatter();
        assertThat(jsonFormatter.format(emptyHistogram, emptyThreadDump, emptyDiagnosis)).isNotEmpty();
    }

    @Test
    void reportFormatterInterface() {
        ReportFormatter formatter = new TextFormatter();
        assertThat(formatter).isInstanceOf(ReportFormatter.class);

        String output = formatter.format(histogram, threadDump, diagnosis);
        assertThat(output).isNotNull();
        assertThat(formatter.getFormatName()).isEqualTo("text");
    }

    @Test
    void htmlFormatterEscapesSpecialCharacters() {
        HeapHistogram specialHeap = new HeapHistogram();
        specialHeap.add(new ClassStats("com.example<Test>", 100, 1000));

        HtmlFormatter formatter = new HtmlFormatter();
        String output = formatter.format(specialHeap, threadDump, diagnosis);

        assertThat(output).doesNotContain("<Test>");
    }
}
