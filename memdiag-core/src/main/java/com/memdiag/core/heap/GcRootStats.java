package com.memdiag.core.heap;

import java.util.EnumMap;
import java.util.Map;

public class GcRootStats {
    private final Map<GcRootType, Long> countsByType;
    private final long totalRoots;

    public GcRootStats() {
        this.countsByType = new EnumMap<>(GcRootType.class);
        this.totalRoots = 0;
    }

    public GcRootStats(Map<GcRootType, Long> countsByType) {
        this.countsByType = new EnumMap<>(countsByType);
        this.totalRoots = countsByType.values().stream().mapToLong(Long::longValue).sum();
    }

    public long getCount(GcRootType type) {
        return countsByType.getOrDefault(type, 0L);
    }

    public Map<GcRootType, Long> getCountsByType() {
        return new EnumMap<>(countsByType);
    }

    public long getTotalRoots() {
        return totalRoots;
    }

    @Override
    public String toString() {
        return "GcRootStats{" +
            "countsByType=" + countsByType +
            ", totalRoots=" + totalRoots +
            '}';
    }
}
