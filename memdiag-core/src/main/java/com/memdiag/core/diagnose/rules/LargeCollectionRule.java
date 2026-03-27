package com.memdiag.core.diagnose.rules;

import com.memdiag.core.diagnose.DiagnosisContext;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Severity;
import com.memdiag.core.heap.ClassStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule that detects large collection usage.
 */
public class LargeCollectionRule extends AbstractDiagnosisRule {

    public static final String RULE_ID = "LARGE_COLLECTION";
    public static final int DEFAULT_THRESHOLD = 50000;

    private final int threshold;

    public LargeCollectionRule() {
        this(DEFAULT_THRESHOLD);
    }

    public LargeCollectionRule(int threshold) {
        super(RULE_ID,
              "Large Collection Detection",
              "Detects collection classes (List, Map, Set) with many instances");
        this.threshold = threshold;
    }

    @Override
    public List<Issue> evaluate(DiagnosisContext context) {
        List<Issue> issues = new ArrayList<>();

        if (context.getHeapHistogram() == null) {
            return issues;
        }

        for (ClassStats stats : context.getHeapHistogram().getClassStats()) {
            String className = stats.getClassName();
            if (isCollectionClass(className) && stats.getObjectCount() > threshold) {
                issues.add(Issue.builder()
                    .severity(Severity.INFO)
                    .type(RULE_ID)
                    .title("Large collection usage: " + truncateClassName(className, 40))
                    .description(String.format("Collection %s has %,d instances totaling %,d bytes",
                        truncateClassName(className, 40),
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

    public int getThreshold() {
        return threshold;
    }
}
