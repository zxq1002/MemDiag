package com.memdiag.core.heap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HeapHistogram {
    private final List<ClassStats> classStats = new ArrayList<>();

    public void add(ClassStats stats) {
        classStats.add(stats);
    }

    public List<ClassStats> getClassStats() {
        return new ArrayList<>(classStats);
    }

    public long getTotalObjects() {
        return classStats.stream().mapToLong(ClassStats::getObjectCount).sum();
    }

    public long getTotalBytes() {
        return classStats.stream().mapToLong(ClassStats::getShallowBytes).sum();
    }

    public List<ClassStats> getTopByObjectCount(int limit) {
        return classStats.stream()
            .sorted(Comparator.comparingLong(ClassStats::getObjectCount).reversed())
            .limit(limit)
            .toList();
    }

    public List<ClassStats> getTopByShallowBytes(int limit) {
        return classStats.stream()
            .sorted(Comparator.comparingLong(ClassStats::getShallowBytes).reversed())
            .limit(limit)
            .toList();
    }
}
