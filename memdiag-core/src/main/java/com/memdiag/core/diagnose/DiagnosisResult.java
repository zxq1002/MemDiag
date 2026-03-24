package com.memdiag.core.diagnose;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiagnosisResult {
    private final Instant timestamp;
    private final List<Issue> issues;
    private final long totalHeapUsed;
    private final long totalHeapCommitted;
    private final int threadCount;
    private final String summary;

    private DiagnosisResult(Builder builder) {
        this.timestamp = builder.timestamp != null ? builder.timestamp : Instant.now();
        this.issues = Collections.unmodifiableList(new ArrayList<>(builder.issues));
        this.totalHeapUsed = builder.totalHeapUsed;
        this.totalHeapCommitted = builder.totalHeapCommitted;
        this.threadCount = builder.threadCount;
        this.summary = builder.summary;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public List<Issue> getCriticalIssues() {
        return issues.stream()
            .filter(i -> i.getSeverity() == Severity.CRITICAL)
            .toList();
    }

    public List<Issue> getWarningIssues() {
        return issues.stream()
            .filter(i -> i.getSeverity() == Severity.WARNING)
            .toList();
    }

    public List<Issue> getInfoIssues() {
        return issues.stream()
            .filter(i -> i.getSeverity() == Severity.INFO)
            .toList();
    }

    public long getTotalHeapUsed() {
        return totalHeapUsed;
    }

    public long getTotalHeapCommitted() {
        return totalHeapCommitted;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public String getSummary() {
        return summary;
    }

    public boolean hasCriticalIssues() {
        return !getCriticalIssues().isEmpty();
    }

    public boolean hasWarningIssues() {
        return !getWarningIssues().isEmpty();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "DiagnosisResult{" +
            "timestamp=" + timestamp +
            ", issues=" + issues.size() +
            ", critical=" + getCriticalIssues().size() +
            ", warnings=" + getWarningIssues().size() +
            '}';
    }

    public static class Builder {
        private Instant timestamp;
        private List<Issue> issues = new ArrayList<>();
        private long totalHeapUsed;
        private long totalHeapCommitted;
        private int threadCount;
        private String summary;

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder issues(List<Issue> issues) {
            this.issues = new ArrayList<>(issues);
            return this;
        }

        public Builder addIssue(Issue issue) {
            this.issues.add(issue);
            return this;
        }

        public Builder totalHeapUsed(long totalHeapUsed) {
            this.totalHeapUsed = totalHeapUsed;
            return this;
        }

        public Builder totalHeapCommitted(long totalHeapCommitted) {
            this.totalHeapCommitted = totalHeapCommitted;
            return this;
        }

        public Builder threadCount(int threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        public DiagnosisResult build() {
            return new DiagnosisResult(this);
        }
    }
}
