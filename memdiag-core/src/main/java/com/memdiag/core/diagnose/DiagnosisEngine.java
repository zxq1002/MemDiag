package com.memdiag.core.diagnose;

import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapAnalyzer;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadAnalyzer;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.thread.ThreadState;
import com.memdiag.core.thread.ThreadStats;
import com.memdiag.core.util.JmxClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DiagnosisEngine {
    private static final long LARGE_OBJECT_THRESHOLD = 100 * 1024 * 1024; // 100MB
    private static final double HIGH_GROWTH_RATE = 2.0;
    private static final int MANY_BLOCKED_THREADS_THRESHOLD = 10;

    private final JmxClient jmxClient;
    private final HeapAnalyzer heapAnalyzer;
    private final ThreadAnalyzer threadAnalyzer;

    public DiagnosisEngine(JmxClient jmxClient, HeapAnalyzer heapAnalyzer, ThreadAnalyzer threadAnalyzer) {
        this.jmxClient = jmxClient;
        this.heapAnalyzer = heapAnalyzer;
        this.threadAnalyzer = threadAnalyzer;
    }

    public DiagnosisResult analyze() {
        DiagnosisResult.Builder result = DiagnosisResult.builder()
            .timestamp(Instant.now());

        List<Issue> issues = new ArrayList<>();

        try {
            HeapHistogram histogram = heapAnalyzer.getHistogram(100);
            ThreadDump threadDump = threadAnalyzer.getThreadDump();

            result.totalHeapUsed(jmxClient.getHeapMemoryUsage().getUsed())
                .totalHeapCommitted(jmxClient.getHeapMemoryUsage().getCommitted())
                .threadCount(threadDump.getThreadCount());

            issues.addAll(checkLargeObjects(histogram));
            issues.addAll(checkManyInstances(histogram));
            issues.addAll(checkBlockedThreads(threadDump));
            issues.addAll(checkCollectionClasses(histogram));

            String summary = generateSummary(issues, histogram, threadDump);
            result.summary(summary);

        } catch (Exception e) {
            issues.add(Issue.builder()
                .severity(Severity.WARNING)
                .type("ANALYSIS_ERROR")
                .title("Analysis encountered errors")
                .description("Some analysis functions failed: " + e.getMessage())
                .build());
        }

        issues.forEach(result::addIssue);
        return result.build();
    }

    private List<Issue> checkLargeObjects(HeapHistogram histogram) {
        List<Issue> issues = new ArrayList<>();

        for (ClassStats stats : histogram.getTopByShallowBytes(10)) {
            if (stats.getShallowBytes() > LARGE_OBJECT_THRESHOLD) {
                issues.add(Issue.builder()
                    .severity(Severity.WARNING)
                    .type("LARGE_CLASS")
                    .title("Large memory usage by class: " + truncateClassName(stats.getClassName()))
                    .description(String.format("Class %s uses %,d bytes across %,d instances",
                        truncateClassName(stats.getClassName()),
                        stats.getShallowBytes(),
                        stats.getObjectCount()))
                    .affectedClassName(stats.getClassName())
                    .affectedObjectCount(stats.getObjectCount())
                    .affectedBytes(stats.getShallowBytes())
                    .addRecommendation(Recommendation.builder()
                        .priority("HIGH")
                        .title("Analyze object graph")
                        .description("Use GC Root analysis to understand why these objects are being retained")
                        .action("Run gc-roots command on this class")
                        .build())
                    .build());
            }
        }

        return issues;
    }

    private List<Issue> checkManyInstances(HeapHistogram histogram) {
        List<Issue> issues = new ArrayList<>();

        for (ClassStats stats : histogram.getTopByObjectCount(10)) {
            if (stats.getObjectCount() > 100000) {
                issues.add(Issue.builder()
                    .severity(Severity.INFO)
                    .type("MANY_INSTANCES")
                    .title("Many instances: " + truncateClassName(stats.getClassName()))
                    .description(String.format("Class %s has %,d instances",
                        truncateClassName(stats.getClassName()),
                        stats.getObjectCount()))
                    .affectedClassName(stats.getClassName())
                    .affectedObjectCount(stats.getObjectCount())
                    .affectedBytes(stats.getShallowBytes())
                    .build());
            }
        }

        return issues;
    }

    private List<Issue> checkBlockedThreads(ThreadDump threadDump) {
        List<Issue> issues = new ArrayList<>();

        List<ThreadDump.ThreadInfo> blockedThreads = threadDump.getThreadsByState(ThreadState.BLOCKED);
        if (blockedThreads.size() > MANY_BLOCKED_THREADS_THRESHOLD) {
            issues.add(Issue.builder()
                .severity(Severity.CRITICAL)
                .type("MANY_BLOCKED_THREADS")
                .title("High number of blocked threads")
                .description(String.format("There are %,d blocked threads", blockedThreads.size()))
                .addRecommendation(Recommendation.builder()
                    .priority("CRITICAL")
                    .title("Check for deadlocks or lock contention")
                    .description("Review thread stacks to identify bottlenecks")
                    .action("Analyze thread dump for blocked threads")
                    .build())
                .build());
        }

        return issues;
    }

    private List<Issue> checkCollectionClasses(HeapHistogram histogram) {
        List<Issue> issues = new ArrayList<>();

        for (ClassStats stats : histogram.getClassStats()) {
            String className = stats.getClassName();
            if (isCollectionClass(className) && stats.getObjectCount() > 50000) {
                issues.add(Issue.builder()
                    .severity(Severity.INFO)
                    .type("LARGE_COLLECTION")
                    .title("Large collection usage: " + truncateClassName(className))
                    .description(String.format("Collection %s has %,d instances totaling %,d bytes",
                        truncateClassName(className),
                        stats.getObjectCount(),
                        stats.getShallowBytes()))
                    .affectedClassName(className)
                    .affectedObjectCount(stats.getObjectCount())
                    .affectedBytes(stats.getShallowBytes())
                    .build());
            }
        }

        return issues;
    }

    private boolean isCollectionClass(String className) {
        return className.startsWith("java.util.") &&
            (className.contains("List") || className.contains("Map") ||
                className.contains("Set") || className.contains("Collection"));
    }

    private String generateSummary(List<Issue> issues, HeapHistogram histogram, ThreadDump threadDump) {
        long criticalCount = issues.stream().filter(i -> i.getSeverity() == Severity.CRITICAL).count();
        long warningCount = issues.stream().filter(i -> i.getSeverity() == Severity.WARNING).count();
        long infoCount = issues.stream().filter(i -> i.getSeverity() == Severity.INFO).count();

        return String.format("Analysis complete: %,d critical, %,d warning, %,d info issues found. " +
                "Heap: %,d bytes used, %,d classes. Threads: %,d active.",
            criticalCount, warningCount, infoCount,
            histogram.getTotalBytes(),
            histogram.getClassStats().size(),
            threadDump.getThreadCount());
    }

    private String truncateClassName(String className) {
        if (className.length() <= 40) return className;
        return "..." + className.substring(className.length() - 37);
    }
}
