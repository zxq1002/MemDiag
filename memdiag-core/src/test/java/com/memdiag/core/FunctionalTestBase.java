package com.memdiag.core.functional;

import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Severity;
import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.thread.ThreadStats;
import com.memdiag.core.thread.ThreadState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Base class for functional tests with utility methods.
 */
class FunctionalTestBase {

    protected static final Random RANDOM = new Random();

    protected HeapHistogram createTestHistogram(int classCount, int avgObjectsPerClass) {
        HeapHistogram histogram = new HeapHistogram();
        String[] classNames = {
            "java.lang.String", "byte[]", "char[]", "java.lang.Object",
            "java.util.HashMap$Node", "java.util.ArrayList", "java.lang.Integer",
            "java.lang.Long", "java.util.HashMap", "java.util.LinkedHashMap"
        };

        for (int i = 0; i < Math.min(classCount, classNames.length); i++) {
            int objectCount = avgObjectsPerClass + RANDOM.nextInt(avgObjectsPerClass);
            long bytes = (long) objectCount * 64L + RANDOM.nextInt(1000);
            histogram.add(new ClassStats(classNames[i], objectCount, bytes));
        }

        return histogram;
    }

    protected HeapHistogram createGrowingHistogram(HeapHistogram baseline, String growingClass, int growthFactor) {
        HeapHistogram histogram = new HeapHistogram();

        // Copy baseline
        for (ClassStats stats : baseline.getClassStats()) {
            if (stats.getClassName().equals(growingClass)) {
                // Grow this class
                long newCount = stats.getObjectCount() * growthFactor;
                long newBytes = stats.getShallowBytes() * growthFactor;
                histogram.add(new ClassStats(growingClass, newCount, newBytes));
            } else {
                // Keep others same
                histogram.add(stats);
            }
        }

        // Add some new classes
        histogram.add(new ClassStats("com.example.NewClass1", 100, 6400));
        histogram.add(new ClassStats("com.example.NewClass2", 50, 3200));

        return histogram;
    }

    protected ThreadDump createTestThreadDump(int threadCount) {
        ThreadDump dump = new ThreadDump();
        dump.setTimestamp(Instant.now());

        List<ThreadStats> stats = new ArrayList<>();

        ThreadStats mainThread = new ThreadStats();
        mainThread.setThreadId(1);
        mainThread.setThreadName("main");
        mainThread.setState(ThreadState.RUNNABLE);
        mainThread.setBlockedCount(0);
        mainThread.setWaitedCount(5);
        stats.add(mainThread);

        for (int i = 2; i <= threadCount; i++) {
            ThreadStats thread = new ThreadStats();
            thread.setThreadId(i);
            thread.setThreadName("Thread-" + i);
            thread.setState(getRandomThreadState());
            thread.setBlockedCount(RANDOM.nextInt(10));
            thread.setWaitedCount(RANDOM.nextInt(20));
            stats.add(thread);
        }

        dump.setThreadStats(stats);
        return dump;
    }

    private ThreadState getRandomThreadState() {
        ThreadState[] states = ThreadState.values();
        return states[RANDOM.nextInt(states.length)];
    }

    protected DiagnosisResult createTestDiagnosis(HeapHistogram histogram, ThreadDump threadDump) {
        DiagnosisResult.Builder builder = DiagnosisResult.builder()
                .timestamp(Instant.now())
                .summary("Functional test diagnosis")
                .totalHeapUsed(histogram.getTotalBytes())
                .totalHeapCommitted(histogram.getTotalBytes() * 2)
                .threadCount(threadDump.getThreadCount());

        // Add some issues based on the data
        if (histogram.getTotalBytes() > 100_000_000) {
            builder.addIssue(Issue.builder()
                    .severity(Severity.WARNING)
                    .type("HIGH_MEMORY")
                    .title("High memory usage detected")
                    .description("Total heap usage exceeds 100MB")
                    .affectedBytes(histogram.getTotalBytes())
                    .build());
        }

        List<ThreadDump.ThreadInfo> blockedThreads = threadDump.getThreadsByState(ThreadState.BLOCKED);
        if (blockedThreads.size() > 5) {
            builder.addIssue(Issue.builder()
                    .severity(Severity.CRITICAL)
                    .type("MANY_BLOCKED_THREADS")
                    .title("Many blocked threads")
                    .description("There are " + blockedThreads.size() + " blocked threads")
                    .build());
        }

        return builder.build();
    }

    protected List<byte[]> allocateMemory(int allocationCount, int allocationSize) {
        List<byte[]> allocations = new ArrayList<>();
        for (int i = 0; i < allocationCount; i++) {
            allocations.add(new byte[allocationSize]);
        }
        return allocations;
    }
}
