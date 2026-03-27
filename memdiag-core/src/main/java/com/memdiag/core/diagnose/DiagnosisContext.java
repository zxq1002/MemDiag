package com.memdiag.core.diagnose;

import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;

/**
 * Context data for diagnosis rules.
 * Provides access to all data needed for rule evaluation.
 */
public class DiagnosisContext {
    private final HeapHistogram heapHistogram;
    private final ThreadDump threadDump;
    private final long totalHeapUsed;
    private final long totalHeapCommitted;

    private DiagnosisContext(Builder builder) {
        this.heapHistogram = builder.heapHistogram;
        this.threadDump = builder.threadDump;
        this.totalHeapUsed = builder.totalHeapUsed;
        this.totalHeapCommitted = builder.totalHeapCommitted;
    }

    public HeapHistogram getHeapHistogram() {
        return heapHistogram;
    }

    public ThreadDump getThreadDump() {
        return threadDump;
    }

    public long getTotalHeapUsed() {
        return totalHeapUsed;
    }

    public long getTotalHeapCommitted() {
        return totalHeapCommitted;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private HeapHistogram heapHistogram;
        private ThreadDump threadDump;
        private long totalHeapUsed;
        private long totalHeapCommitted;

        public Builder heapHistogram(HeapHistogram heapHistogram) {
            this.heapHistogram = heapHistogram;
            return this;
        }

        public Builder threadDump(ThreadDump threadDump) {
            this.threadDump = threadDump;
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

        public DiagnosisContext build() {
            return new DiagnosisContext(this);
        }
    }
}
