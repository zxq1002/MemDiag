package com.memdiag.core.diagnose.rules;

import com.memdiag.core.diagnose.DiagnosisContext;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Recommendation;
import com.memdiag.core.diagnose.Severity;
import com.memdiag.core.heap.ClassStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule that detects potential heap memory leaks.
 * <p>
 * This rule looks for common indicators of memory leaks:
 * <ul>
 *   <li>Classes with large instance counts that occupy significant memory</li>
 *   <li>Collection classes that dominate the heap</li>
 *   <li>Cache-like classes that may not be evicting entries</li>
 * </ul>
 */
public class HeapLeakDetectionRule extends AbstractDiagnosisRule {

    public static final String RULE_ID = "HEAP_LEAK_SUSPECT";

    // Thresholds for leak suspicion
    private static final long SUSPICIOUS_INSTANCE_COUNT = 50000;
    private static final long SUSPICIOUS_CLASS_BYTES = 50 * 1024 * 1024; // 50MB
    private static final double DOMINANCE_THRESHOLD = 0.2; // 20% of total heap

    public HeapLeakDetectionRule() {
        super(RULE_ID,
              "Heap Leak Suspect Detection",
              "Detects classes that may indicate a memory leak based on size and instance patterns");
    }

    @Override
    public List<Issue> evaluate(DiagnosisContext context) {
        List<Issue> issues = new ArrayList<>();

        if (context.getHeapHistogram() == null) {
            return issues;
        }

        long totalHeapBytes = context.getTotalHeapUsed();
        List<ClassStats> classStats = context.getHeapHistogram().getClassStats();

        // Check top classes for potential leaks
        int checked = 0;
        for (ClassStats stats : context.getHeapHistogram().getTopByShallowBytes(20)) {
            checked++;

            boolean isSuspicious = false;
            List<String> reasons = new ArrayList<>();

            // Check 1: High instance count + significant bytes
            if (stats.getObjectCount() > SUSPICIOUS_INSTANCE_COUNT &&
                stats.getShallowBytes() > SUSPICIOUS_CLASS_BYTES) {
                isSuspicious = true;
                reasons.add(String.format("High instance count (%,d) with large footprint (%,d bytes)",
                    stats.getObjectCount(), stats.getShallowBytes()));
            }

            // Check 2: Dominates a large portion of heap
            if (totalHeapBytes > 0) {
                double dominance = (double) stats.getShallowBytes() / totalHeapBytes;
                if (dominance > DOMINANCE_THRESHOLD) {
                    isSuspicious = true;
                    reasons.add(String.format("Dominates %.1f%% of total heap", dominance * 100));
                }
            }

            // Check 3: Suspicious class names (caches, buffers, etc.)
            String className = stats.getClassName().toLowerCase();
            if (isCacheOrBufferClass(className) &&
                stats.getObjectCount() > SUSPICIOUS_INSTANCE_COUNT / 2) {
                isSuspicious = true;
                reasons.add("Cache/buffer-like class with many instances");
            }

            // Check 4: Large collections
            if (isCollectionClass(className) &&
                stats.getObjectCount() > SUSPICIOUS_INSTANCE_COUNT) {
                isSuspicious = true;
                reasons.add(String.format("Large collection with %,d instances", stats.getObjectCount()));
            }

            if (isSuspicious) {
                issues.add(createLeakIssue(stats, reasons, totalHeapBytes));
            }
        }

        return issues;
    }

    private boolean isCacheOrBufferClass(String className) {
        return className.contains("cache") ||
               className.contains("buffer") ||
               className.contains("pool") ||
               className.contains("entry") && !className.contains("map");
    }

    private boolean isCollectionClass(String className) {
        return className.startsWith("java.util.") &&
            (className.contains("list") || className.contains("map") ||
             className.contains("set") || className.contains("queue") ||
             className.contains("concurrent") || className.contains("cache"));
    }

    private Issue createLeakIssue(ClassStats stats, List<String> reasons, long totalHeapBytes) {
        StringBuilder desc = new StringBuilder();
        desc.append(String.format("Class %s shows potential leak indicators:%n",
            truncateClassName(stats.getClassName(), 50)));
        for (String reason : reasons) {
            desc.append("  - ").append(reason).append("%n");
        }
        desc.append(String.format("  Total: %,d instances, %,d bytes",
            stats.getObjectCount(), stats.getShallowBytes()));
        if (totalHeapBytes > 0) {
            double pct = (double) stats.getShallowBytes() / totalHeapBytes * 100;
            desc.append(String.format(" (%.1f%% of heap)", pct));
        }

        return Issue.builder()
            .severity(Severity.WARNING)
            .type(RULE_ID)
            .title("Potential memory leak suspect: " + truncateClassName(stats.getClassName(), 40))
            .description(String.format(desc.toString()))
            .affectedClassName(stats.getClassName())
            .affectedObjectCount(stats.getObjectCount())
            .affectedBytes(stats.getShallowBytes())
            .addRecommendation(Recommendation.builder()
                .priority("HIGH")
                .title("Take heap snapshots for comparison")
                .description("Capture multiple snapshots over time to see if this class grows continuously")
                .action("Use 'memdiag snapshot --save' at different times, then 'memdiag diff'")
                .build())
            .addRecommendation(Recommendation.builder()
                .priority("MEDIUM")
                .title("Check for proper cleanup")
                .description("Verify if this class has clear()/remove()/evict() methods that are being called")
                .action("Review code for proper resource cleanup patterns")
                .build())
            .build();
    }
}
