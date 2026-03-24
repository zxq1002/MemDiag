package com.memdiag.core.diff;

import com.memdiag.core.heap.ClassStats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class HeapDiff {
    private final Snapshot baseline;
    private final Snapshot current;
    private final List<ClassDiff> allDiffs;
    private final long totalObjectDelta;
    private final long totalByteDelta;

    private HeapDiff(Snapshot baseline, Snapshot current, List<ClassDiff> allDiffs) {
        this.baseline = baseline;
        this.current = current;
        this.allDiffs = new ArrayList<>(allDiffs);
        this.totalObjectDelta = allDiffs.stream().mapToLong(ClassDiff::getObjectCountDelta).sum();
        this.totalByteDelta = allDiffs.stream().mapToLong(ClassDiff::getBytesDelta).sum();
    }

    public static HeapDiff compute(Snapshot baseline, Snapshot current) {
        List<ClassDiff> diffs = new ArrayList<>();

        Map<ClassKey, ClassStats> baselineStats = baseline.getClassStats();
        Map<ClassKey, ClassStats> currentStats = current.getClassStats();

        for (Map.Entry<ClassKey, ClassStats> entry : currentStats.entrySet()) {
            ClassKey key = entry.getKey();
            ClassStats currentClassStats = entry.getValue();
            ClassStats baselineClassStats = baselineStats.get(key);

            if (baselineClassStats == null) {
                // 新增的类
                diffs.add(ClassDiff.builder()
                    .classKey(key)
                    .objectCountDelta(currentClassStats.getObjectCount())
                    .bytesDelta(currentClassStats.getShallowBytes())
                    .growthRate(Double.POSITIVE_INFINITY)
                    .currentObjectCount(currentClassStats.getObjectCount())
                    .currentBytes(currentClassStats.getShallowBytes())
                    .build());
            } else {
                // 有变化的类
                long objDelta = currentClassStats.getObjectCount() - baselineClassStats.getObjectCount();
                long byteDelta = currentClassStats.getShallowBytes() - baselineClassStats.getShallowBytes();
                double growthRate = baselineClassStats.getObjectCount() > 0
                    ? (double) objDelta / baselineClassStats.getObjectCount()
                    : (objDelta > 0 ? Double.POSITIVE_INFINITY : 0.0);

                diffs.add(ClassDiff.builder()
                    .classKey(key)
                    .objectCountDelta(objDelta)
                    .bytesDelta(byteDelta)
                    .growthRate(growthRate)
                    .baselineObjectCount(baselineClassStats.getObjectCount())
                    .baselineBytes(baselineClassStats.getShallowBytes())
                    .currentObjectCount(currentClassStats.getObjectCount())
                    .currentBytes(currentClassStats.getShallowBytes())
                    .build());
            }
        }

        for (Map.Entry<ClassKey, ClassStats> entry : baselineStats.entrySet()) {
            ClassKey key = entry.getKey();
            if (!currentStats.containsKey(key)) {
                // 消失的类
                ClassStats baselineClassStats = entry.getValue();
                diffs.add(ClassDiff.builder()
                    .classKey(key)
                    .objectCountDelta(-baselineClassStats.getObjectCount())
                    .bytesDelta(-baselineClassStats.getShallowBytes())
                    .growthRate(-1.0)
                    .baselineObjectCount(baselineClassStats.getObjectCount())
                    .baselineBytes(baselineClassStats.getShallowBytes())
                    .build());
            }
        }

        return new HeapDiff(baseline, current, diffs);
    }

    public Snapshot getBaseline() {
        return baseline;
    }

    public Snapshot getCurrent() {
        return current;
    }

    public List<ClassDiff> getAllDiffs() {
        return new ArrayList<>(allDiffs);
    }

    public long getTotalObjectDelta() {
        return totalObjectDelta;
    }

    public long getTotalByteDelta() {
        return totalByteDelta;
    }

    public List<ClassDiff> getGrowingClasses(int limit) {
        return allDiffs.stream()
            .filter(ClassDiff::isGrowing)
            .sorted(Comparator.comparingLong(ClassDiff::getBytesDelta).reversed())
            .limit(limit)
            .toList();
    }

    public List<ClassDiff> getShrinkingClasses(int limit) {
        return allDiffs.stream()
            .filter(ClassDiff::isShrinking)
            .sorted(Comparator.comparingLong(ClassDiff::getBytesDelta))
            .limit(limit)
            .toList();
    }

    public List<ClassDiff> getTopByGrowthRate(int limit) {
        return allDiffs.stream()
            .filter(d -> !d.isDisappeared())
            .sorted(Comparator.comparingDouble(ClassDiff::getGrowthRate).reversed())
            .limit(limit)
            .toList();
    }

    public List<ClassDiff> getNewClasses() {
        return allDiffs.stream()
            .filter(ClassDiff::isNewClass)
            .toList();
    }

    public List<ClassDiff> getDisappearedClasses() {
        return allDiffs.stream()
            .filter(ClassDiff::isDisappeared)
            .toList();
    }

    @Override
    public String toString() {
        return "HeapDiff{" +
            "baseline=" + baseline.getTimestamp() +
            ", current=" + current.getTimestamp() +
            ", totalObjectDelta=" + totalObjectDelta +
            ", totalByteDelta=" + totalByteDelta +
            ", changedClasses=" + allDiffs.size() +
            '}';
    }
}
