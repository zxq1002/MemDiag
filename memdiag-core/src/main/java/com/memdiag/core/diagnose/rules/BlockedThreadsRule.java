package com.memdiag.core.diagnose.rules;

import com.memdiag.core.diagnose.DiagnosisContext;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Recommendation;
import com.memdiag.core.diagnose.Severity;
import com.memdiag.core.thread.ThreadState;

import java.util.ArrayList;
import java.util.List;

/**
 * Rule that detects a high number of blocked threads.
 */
public class BlockedThreadsRule extends AbstractDiagnosisRule {

    public static final String RULE_ID = "MANY_BLOCKED_THREADS";
    public static final int DEFAULT_THRESHOLD = 10;

    private final int threshold;

    public BlockedThreadsRule() {
        this(DEFAULT_THRESHOLD);
    }

    public BlockedThreadsRule(int threshold) {
        super(RULE_ID,
              "Blocked Threads Detection",
              "Detects when the number of BLOCKED threads exceeds the configured threshold");
        this.threshold = threshold;
    }

    @Override
    public List<Issue> evaluate(DiagnosisContext context) {
        List<Issue> issues = new ArrayList<>();

        if (context.getThreadDump() == null) {
            return issues;
        }

        List<com.memdiag.core.thread.ThreadDump.ThreadInfo> blockedThreads =
            context.getThreadDump().getThreadsByState(ThreadState.BLOCKED);

        if (blockedThreads.size() > threshold) {
            issues.add(Issue.builder()
                .severity(Severity.CRITICAL)
                .type(RULE_ID)
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

    public int getThreshold() {
        return threshold;
    }
}
