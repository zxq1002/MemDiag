package com.memdiag.core.diff;

import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Snapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final Instant timestamp;
    private final HeapHistogram heapHistogram;
    private final ThreadDump threadDump;

    private Snapshot(Builder builder) {
        this.id = builder.id;
        this.timestamp = builder.timestamp;
        this.heapHistogram = builder.heapHistogram;
        this.threadDump = builder.threadDump;
    }

    public String getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public HeapHistogram getHeapHistogram() {
        return heapHistogram;
    }

    public ThreadDump getThreadDump() {
        return threadDump;
    }

    @Override
    public String toString() {
        return "Snapshot{" +
            "id='" + id + '\'' +
            ", timestamp=" + timestamp +
            ", heapHistogram=" + (heapHistogram != null ? heapHistogram.getClassStats().size() + " classes" : "null") +
            ", threadDump=" + (threadDump != null ? threadDump.getThreadStats().size() + " threads" : "null") +
            '}';
    }

    public static class Builder {
        private String id;
        private Instant timestamp = Instant.now();
        private HeapHistogram heapHistogram;
        private ThreadDump threadDump;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setHeapHistogram(HeapHistogram heapHistogram) {
            this.heapHistogram = heapHistogram;
            return this;
        }

        public Builder setThreadDump(ThreadDump threadDump) {
            this.threadDump = threadDump;
            return this;
        }

        public Snapshot build() {
            return new Snapshot(this);
        }
    }

    @Deprecated
    public static Snapshot create(Instant timestamp, Map<ClassKey, ClassStats> classStats) {
        HeapHistogram histogram = new HeapHistogram();
        for (Map.Entry<ClassKey, ClassStats> entry : classStats.entrySet()) {
            histogram.add(entry.getValue());
        }
        return new Builder()
            .setTimestamp(timestamp)
            .setHeapHistogram(histogram)
            .build();
    }

    @Deprecated
    public Map<ClassKey, ClassStats> getClassStats() {
        Map<ClassKey, ClassStats> result = new HashMap<>();
        if (heapHistogram != null) {
            for (ClassStats stats : heapHistogram.getClassStats()) {
                result.put(new ClassKey(stats.getClassName(), 0), stats);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @Deprecated
    public long getTotalObjects() {
        return heapHistogram != null ? heapHistogram.getTotalObjects() : 0;
    }

    @Deprecated
    public long getTotalBytes() {
        return heapHistogram != null ? heapHistogram.getTotalBytes() : 0;
    }

    @Deprecated
    public ClassStats getClassStats(ClassKey key) {
        if (heapHistogram == null) return null;
        return heapHistogram.getClassStats().stream()
            .filter(s -> s.getClassName().equals(key.getClassName()))
            .findFirst()
            .orElse(null);
    }
}
