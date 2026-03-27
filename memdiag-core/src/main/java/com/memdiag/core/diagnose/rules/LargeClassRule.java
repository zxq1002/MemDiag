package com.memdiag.core.diagnose.rules;

import com.memdiag.core.diagnose.DiagnosisContext;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Recommendation;
import com.memdiag.core.diagnose.Severity;
import com.memdiag.core.heap.ClassStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule that detects classes with large memory usage.
 */
public class LargeClassRule extends AbstractDiagnosisRule {

    public static final String RULE_ID = "LARGE_CLASS";
    public static final long DEFAULT_THRESHOLD_BYTES = 100 * 1024 * 1024; // 100MB

    private final long thresholdBytes;

    public LargeClassRule() {
        this(DEFAULT_THRESHOLD_BYTES);
    }

    public LargeClassRule(long thresholdBytes) {
        super(RULE_ID,
              "Large Class Detection",
              "Detects classes that occupy more than the configured threshold of memory");
        this.thresholdBytes = thresholdBytes;
    }

    @Override
    public List<Issue> evaluate(DiagnosisContext context) {
        List<Issue> issues = new ArrayList<>();

        if (context.getHeapHistogram() == null) {
            return issues;
        }

        for (ClassStats stats : context.getHeapHistogram().getTopByShallowBytes(10)) {
            if (stats.getShallowBytes() > thresholdBytes) {
                issues.add(Issue.builder()
                    .severity(Severity.WARNING)
                    .type(RULE_ID)
                    .title("Large memory usage by class: " + truncateClassName(stats.getClassName(), 40))
                    .description(String.format("Class %s uses %,d bytes across %,d instances",
                        truncateClassName(stats.getClassName(), 40),
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

    public long getThresholdBytes() {
        return thresholdBytes;
    }
}
