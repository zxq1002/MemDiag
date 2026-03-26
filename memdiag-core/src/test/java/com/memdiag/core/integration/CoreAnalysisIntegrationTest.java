package com.memdiag.core.integration;

import com.memdiag.core.diff.HeapDiff;
import com.memdiag.core.diff.Snapshot;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.heap.JmxHeapAnalyzer;
import com.memdiag.core.thread.ThreadAnalyzer;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.util.JmxClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for core analysis components working together.
 * Uses the current JVM for testing.
 */
@Tag("integration")
class CoreAnalysisIntegrationTest {

    @Test
    void jmxClientConnectsToCurrentJvm() {
        JmxClient client = JmxClient.attachToCurrentJvm();
        assertThat(client).isNotNull();
        assertThat(client.getConnection()).isNotNull();
        assertThat(client.getHeapMemoryUsage()).isNotNull();
        assertThat(client.getHeapMemoryUsage().getUsed()).isGreaterThan(0);
    }

    @Test
    void heapAnalyzerWorksWithCurrentJvm() {
        JmxClient jmxClient = JmxClient.attachToCurrentJvm();
        JmxHeapAnalyzer heapAnalyzer = new JmxHeapAnalyzer(jmxClient);

        HeapHistogram histogram = heapAnalyzer.getFullHistogram();

        assertThat(histogram).isNotNull();
        assertThat(histogram.getTotalObjects()).isGreaterThan(0);
        assertThat(histogram.getTotalBytes()).isGreaterThan(0);
        assertThat(histogram.getClassStats()).isNotEmpty();

        // Should have common classes
        List<String> classNames = histogram.getClassStats().stream()
                .map(stats -> stats.getClassName())
                .toList();
        assertThat(classNames).anyMatch(name -> name.contains("java.lang"));
    }

    @Test
    void threadAnalyzerWorksWithCurrentJvm() {
        JmxClient jmxClient = JmxClient.attachToCurrentJvm();
        ThreadAnalyzer threadAnalyzer = new ThreadAnalyzer(jmxClient);

        ThreadDump dump = threadAnalyzer.getThreadDump();

        assertThat(dump).isNotNull();
        assertThat(dump.getThreadCount()).isGreaterThan(0);
        assertThat(dump.getThreadStats()).isNotEmpty();

        // Should have main thread
        boolean foundMain = dump.getThreadStats().stream()
                .anyMatch(ts -> "main".equals(ts.getThreadName()));
        assertThat(foundMain).isTrue();
    }

    @Test
    void heapHistogramLimitWorks() {
        JmxClient jmxClient = JmxClient.attachToCurrentJvm();
        JmxHeapAnalyzer heapAnalyzer = new JmxHeapAnalyzer(jmxClient);

        HeapHistogram fullHistogram = heapAnalyzer.getFullHistogram();
        HeapHistogram limitedHistogram = heapAnalyzer.getHistogram(10);

        assertThat(fullHistogram.getClassStats().size())
                .isGreaterThanOrEqualTo(limitedHistogram.getClassStats().size());
        assertThat(limitedHistogram.getClassStats()).hasSizeLessThanOrEqualTo(10);
    }

    @Test
    void multipleHeapSnapshotsComparison() {
        JmxClient jmxClient = JmxClient.attachToCurrentJvm();
        JmxHeapAnalyzer heapAnalyzer = new JmxHeapAnalyzer(jmxClient);

        // Create some activity
        List<byte[]> tempList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tempList.add(new byte[1024]);
        }

        // First snapshot
        HeapHistogram first = heapAnalyzer.getFullHistogram();
        Snapshot firstSnapshot = new Snapshot.Builder()
                .setId("first")
                .setTimestamp(Instant.now())
                .setHeapHistogram(first)
                .build();

        // More activity
        for (int i = 0; i < 100; i++) {
            tempList.add(new byte[1024]);
        }

        // Second snapshot
        HeapHistogram second = heapAnalyzer.getFullHistogram();
        Snapshot secondSnapshot = new Snapshot.Builder()
                .setId("second")
                .setTimestamp(Instant.now().plusSeconds(1))
                .setHeapHistogram(second)
                .build();

        // Compare
        HeapDiff diff = HeapDiff.compute(firstSnapshot, secondSnapshot);
        assertThat(diff).isNotNull();

        // Clean up
        tempList.clear();
    }

    @Test
    void threadStatsContainValidData() {
        JmxClient jmxClient = JmxClient.attachToCurrentJvm();
        ThreadAnalyzer threadAnalyzer = new ThreadAnalyzer(jmxClient);

        ThreadDump dump = threadAnalyzer.getThreadDump();

        dump.getThreadStats().forEach(stats -> {
            assertThat(stats.getThreadId()).isGreaterThan(0);
            assertThat(stats.getThreadName()).isNotNull();
            assertThat(stats.getThreadName()).isNotEmpty();
            assertThat(stats.getState()).isNotNull();
        });
    }

    @Test
    void topClassesByShallowBytes() {
        JmxClient jmxClient = JmxClient.attachToCurrentJvm();
        JmxHeapAnalyzer heapAnalyzer = new JmxHeapAnalyzer(jmxClient);

        HeapHistogram histogram = heapAnalyzer.getFullHistogram();
        var topClasses = histogram.getTopByShallowBytes(5);

        assertThat(topClasses).hasSizeLessThanOrEqualTo(5);
        assertThat(topClasses).isNotEmpty();

        // Verify they are ordered by shallow bytes descending
        long previousBytes = Long.MAX_VALUE;
        for (var stats : topClasses) {
            assertThat(stats.getShallowBytes()).isLessThanOrEqualTo(previousBytes);
            previousBytes = stats.getShallowBytes();
        }
    }

    @Test
    void topClassesByObjectCount() {
        JmxClient jmxClient = JmxClient.attachToCurrentJvm();
        JmxHeapAnalyzer heapAnalyzer = new JmxHeapAnalyzer(jmxClient);

        HeapHistogram histogram = heapAnalyzer.getFullHistogram();
        var topClasses = histogram.getTopByObjectCount(5);

        assertThat(topClasses).hasSizeLessThanOrEqualTo(5);
        assertThat(topClasses).isNotEmpty();

        // Verify they are ordered by object count descending
        long previousCount = Long.MAX_VALUE;
        for (var stats : topClasses) {
            assertThat(stats.getObjectCount()).isLessThanOrEqualTo(previousCount);
            previousCount = stats.getObjectCount();
        }
    }
}
