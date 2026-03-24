package com.memdiag.core.diff;

import com.memdiag.core.heap.ClassStats;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Snapshot {
    private final Instant timestamp;
    private final Map<ClassKey, ClassStats> classStats;
    private final long totalObjects;
    private final long totalBytes;

    private Snapshot(Instant timestamp, Map<ClassKey, ClassStats> classStats) {
        this.timestamp = timestamp;
        this.classStats = Collections.unmodifiableMap(new HashMap<>(classStats));
        this.totalObjects = classStats.values().stream().mapToLong(ClassStats::getObjectCount).sum();
        this.totalBytes = classStats.values().stream().mapToLong(ClassStats::getShallowBytes).sum();
    }

    public static Snapshot create(Instant timestamp, Map<ClassKey, ClassStats> classStats) {
        return new Snapshot(timestamp, classStats);
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<ClassKey, ClassStats> getClassStats() {
        return classStats;
    }

    public long getTotalObjects() {
        return totalObjects;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public ClassStats getClassStats(ClassKey key) {
        return classStats.get(key);
    }

    @Override
    public String toString() {
        return "Snapshot{" +
            "timestamp=" + timestamp +
            ", classes=" + classStats.size() +
            ", objects=" + totalObjects +
            ", bytes=" + totalBytes +
            '}';
    }
}
