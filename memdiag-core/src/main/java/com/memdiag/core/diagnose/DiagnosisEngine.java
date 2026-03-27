package com.memdiag.core.diagnose;

import com.memdiag.core.heap.HeapAnalyzer;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadAnalyzer;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.util.JmxClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Extensible diagnosis engine that uses registered rules to identify issues.
 * <p>
 * This engine supports:
 * <ul>
 *   <li>Built-in default rules</li>
 *   <li>Programmatic rule registration</li>
 *   <li>ServiceLoader-based rule discovery</li>
 *   <li>Custom rule implementations</li>
 * </ul>
 */
public class DiagnosisEngine {

    private final JmxClient jmxClient;
    private final HeapAnalyzer heapAnalyzer;
    private final ThreadAnalyzer threadAnalyzer;
    private final RuleRegistry ruleRegistry;

    /**
     * Create a new DiagnosisEngine with the default rules.
     *
     * @param jmxClient JMX client for accessing JVM metrics
     * @param heapAnalyzer heap analyzer for heap data
     * @param threadAnalyzer thread analyzer for thread data
     */
    public DiagnosisEngine(JmxClient jmxClient, HeapAnalyzer heapAnalyzer, ThreadAnalyzer threadAnalyzer) {
        this(jmxClient, heapAnalyzer, threadAnalyzer, RuleRegistry.withDefaults());
    }

    /**
     * Create a new DiagnosisEngine with a custom rule registry.
     *
     * @param jmxClient JMX client for accessing JVM metrics
     * @param heapAnalyzer heap analyzer for heap data
     * @param threadAnalyzer thread analyzer for thread data
     * @param ruleRegistry registry of rules to use
     */
    public DiagnosisEngine(JmxClient jmxClient, HeapAnalyzer heapAnalyzer,
                           ThreadAnalyzer threadAnalyzer, RuleRegistry ruleRegistry) {
        this.jmxClient = jmxClient;
        this.heapAnalyzer = heapAnalyzer;
        this.threadAnalyzer = threadAnalyzer;
        this.ruleRegistry = ruleRegistry;
    }

    /**
     * Get the rule registry for this engine.
     *
     * @return the rule registry
     */
    public RuleRegistry getRuleRegistry() {
        return ruleRegistry;
    }

    /**
     * Run the diagnosis with all enabled rules.
     *
     * @return diagnosis result containing all found issues
     */
    public DiagnosisResult analyze() {
        DiagnosisResult.Builder result = DiagnosisResult.builder()
            .timestamp(Instant.now());

        List<Issue> issues = new ArrayList<>();

        try {
            HeapHistogram histogram = heapAnalyzer.getHistogram(100);
            ThreadDump threadDump = threadAnalyzer.getThreadDump();
            long heapUsed = jmxClient.getHeapMemoryUsage().getUsed();
            long heapCommitted = jmxClient.getHeapMemoryUsage().getCommitted();

            result.totalHeapUsed(heapUsed)
                .totalHeapCommitted(heapCommitted)
                .threadCount(threadDump.getThreadCount());

            // Build context for rules
            DiagnosisContext context = DiagnosisContext.builder()
                .heapHistogram(histogram)
                .threadDump(threadDump)
                .totalHeapUsed(heapUsed)
                .totalHeapCommitted(heapCommitted)
                .build();

            // Evaluate all enabled rules
            for (DiagnosisRule rule : ruleRegistry.getEnabledRules()) {
                try {
                    issues.addAll(rule.evaluate(context));
                } catch (Exception e) {
                    issues.add(Issue.builder()
                        .severity(Severity.WARNING)
                        .type("RULE_ERROR")
                        .title("Rule execution error: " + rule.getName())
                        .description("Rule " + rule.getId() + " failed: " + e.getMessage())
                        .build());
                }
            }

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

    private String generateSummary(List<Issue> issues, HeapHistogram histogram, ThreadDump threadDump) {
        long criticalCount = issues.stream().filter(i -> i.getSeverity() == Severity.CRITICAL).count();
        long warningCount = issues.stream().filter(i -> i.getSeverity() == Severity.WARNING).count();
        long infoCount = issues.stream().filter(i -> i.getSeverity() == Severity.INFO).count();

        return String.format("Analysis complete: %,d critical, %,d warning, %,d info issues found. " +
                "Heap: %,d bytes used, %,d classes. Threads: %,d active. " +
                "Rules executed: %d.",
            criticalCount, warningCount, infoCount,
            histogram.getTotalBytes(),
            histogram.getClassStats().size(),
            threadDump.getThreadCount(),
            ruleRegistry.getEnabledRules().size());
    }
}
