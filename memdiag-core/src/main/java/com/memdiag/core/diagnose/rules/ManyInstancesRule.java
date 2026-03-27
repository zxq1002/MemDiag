package com.memdiag.core.diagnose.rules;

import com.memdiag.core.diagnose.DiagnosisContext;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Severity;
import com.memdiag.core.heap.ClassStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule that detects classes with too many instances.
 */
public class ManyInstancesRule extends AbstractDiagnosisRule {

    public static final String RULE_ID = "MANY_INSTANCES";
    public static final int DEFAULT_THRESHOLD = 100000;

    private final int threshold;

    public ManyInstancesRule() {
        this(DEFAULT_THRESHOLD);
    }

    public ManyInstancesRule(int threshold) {
        super(RULE_ID,
              "Many Instances Detection",
              "Detects classes that have more than the configured number of instances");
        this.threshold = threshold;
    }

    @Override
    public List<Issue> evaluate(DiagnosisContext context) {
        List<Issue> issues = new ArrayList<>();

        if (context.getHeapHistogram() == null) {
            return issues;
        }

        for (ClassStats stats : context.getHeapHistogram().getTopByObjectCount(10)) {
            if (stats.getObjectCount() > threshold) {
                issues.add(Issue.builder()
                    .severity(Severity.INFO)
                    .type(RULE_ID)
                    .title("Many instances: " + truncateClassName(stats.getClassName(), 40))
                    .description(String.format("Class %s has %,d instances",
                        truncateClassName(stats.getClassName(), 40),
                        stats.getObjectCount()))
                    .affectedClassName(stats.getClassName())
                    .affectedObjectCount(stats.getObjectCount())
                    .affectedBytes(stats.getShallowBytes())
                    .build());
            }
        }

        return issues;
    }

    public int getThreshold() {
        return threshold;
    }
}
