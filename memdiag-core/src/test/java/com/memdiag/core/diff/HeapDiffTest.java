package com.memdiag.core.diff;

import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapHistogram;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class HeapDiffTest {

    private Snapshot baseline;
    private Snapshot current;

    @BeforeEach
    void setUp() {
        baseline = createTestSnapshot("baseline");
        current = createTestSnapshot("current");
    }

    @Test
    void computeDiffBetweenSnapshots() {
        HeapDiff diff = HeapDiff.compute(baseline, current);

        assertThat(diff).isNotNull();
        assertThat(diff.getBaseline()).isEqualTo(baseline);
        assertThat(diff.getCurrent()).isEqualTo(current);
    }

    @Test
    void detectNewClasses() {
        HeapHistogram baselineHistogram = new HeapHistogram();
        baselineHistogram.add(new ClassStats("com.example.OldClass", 100, 10_000));
        Snapshot baseline = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(baselineHistogram)
                .build();

        HeapHistogram currentHistogram = new HeapHistogram();
        currentHistogram.add(new ClassStats("com.example.OldClass", 100, 10_000));
        currentHistogram.add(new ClassStats("com.example.NewClass", 50, 5_000));
        Snapshot current = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(currentHistogram)
                .build();

        HeapDiff diff = HeapDiff.compute(baseline, current);
        List<ClassDiff> newClasses = diff.getNewClasses();

        assertThat(newClasses).hasSize(1);
    }

    @Test
    void detectDisappearedClasses() {
        HeapHistogram baselineHistogram = new HeapHistogram();
        baselineHistogram.add(new ClassStats("com.example.StayingClass", 100, 10_000));
        baselineHistogram.add(new ClassStats("com.example.DisappearingClass", 50, 5_000));
        Snapshot baseline = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(baselineHistogram)
                .build();

        HeapHistogram currentHistogram = new HeapHistogram();
        currentHistogram.add(new ClassStats("com.example.StayingClass", 100, 10_000));
        Snapshot current = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(currentHistogram)
                .build();

        HeapDiff diff = HeapDiff.compute(baseline, current);
        List<ClassDiff> disappeared = diff.getDisappearedClasses();

        assertThat(disappeared).hasSize(1);
    }

    @Test
    void detectGrowingClasses() {
        HeapHistogram baselineHistogram = new HeapHistogram();
        baselineHistogram.add(new ClassStats("com.example.GrowingClass", 100, 10_000));
        baselineHistogram.add(new ClassStats("com.example.ShrinkingClass", 200, 20_000));
        Snapshot baseline = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(baselineHistogram)
                .build();

        HeapHistogram currentHistogram = new HeapHistogram();
        currentHistogram.add(new ClassStats("com.example.GrowingClass", 200, 20_000));
        currentHistogram.add(new ClassStats("com.example.ShrinkingClass", 100, 10_000));
        Snapshot current = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(currentHistogram)
                .build();

        HeapDiff diff = HeapDiff.compute(baseline, current);
        List<ClassDiff> growing = diff.getGrowingClasses(10);

        assertThat(growing).hasSize(1);
        assertThat(growing.get(0).getBytesDelta()).isGreaterThan(0);
    }

    @Test
    void detectShrinkingClasses() {
        HeapHistogram baselineHistogram = new HeapHistogram();
        baselineHistogram.add(new ClassStats("com.example.GrowingClass", 100, 10_000));
        baselineHistogram.add(new ClassStats("com.example.ShrinkingClass", 200, 20_000));
        Snapshot baseline = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(baselineHistogram)
                .build();

        HeapHistogram currentHistogram = new HeapHistogram();
        currentHistogram.add(new ClassStats("com.example.GrowingClass", 200, 20_000));
        currentHistogram.add(new ClassStats("com.example.ShrinkingClass", 100, 10_000));
        Snapshot current = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(currentHistogram)
                .build();

        HeapDiff diff = HeapDiff.compute(baseline, current);
        List<ClassDiff> shrinking = diff.getShrinkingClasses(10);

        assertThat(shrinking).hasSize(1);
        assertThat(shrinking.get(0).getBytesDelta()).isLessThan(0);
    }

    @Test
    void calculateTotalObjectDelta() {
        HeapHistogram baselineHistogram = new HeapHistogram();
        baselineHistogram.add(new ClassStats("com.example.Class1", 100, 10_000));
        Snapshot baseline = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(baselineHistogram)
                .build();

        HeapHistogram currentHistogram = new HeapHistogram();
        currentHistogram.add(new ClassStats("com.example.Class1", 150, 15_000));
        Snapshot current = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(currentHistogram)
                .build();

        HeapDiff diff = HeapDiff.compute(baseline, current);

        assertThat(diff.getTotalObjectDelta()).isEqualTo(50);
        assertThat(diff.getTotalByteDelta()).isEqualTo(5_000);
    }

    @Test
    void emptyDiffWithIdenticalSnapshots() {
        HeapHistogram histogram = new HeapHistogram();
        histogram.add(new ClassStats("com.example.Class", 100, 10_000));
        Snapshot baseline = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(histogram)
                .build();

        HeapHistogram histogram2 = new HeapHistogram();
        histogram2.add(new ClassStats("com.example.Class", 100, 10_000));
        Snapshot current = new Snapshot.Builder()
                .setTimestamp(Instant.now())
                .setHeapHistogram(histogram2)
                .build();

        HeapDiff diff = HeapDiff.compute(baseline, current);

        assertThat(diff.getTotalObjectDelta()).isZero();
        assertThat(diff.getTotalByteDelta()).isZero();
        assertThat(diff.getGrowingClasses(10)).isEmpty();
        assertThat(diff.getShrinkingClasses(10)).isEmpty();
    }

    private Snapshot createTestSnapshot(String id) {
        HeapHistogram histogram = new HeapHistogram();
        histogram.add(new ClassStats("java.lang.String", 1000, 64000));

        return new Snapshot.Builder()
                .setId(id)
                .setTimestamp(Instant.now())
                .setHeapHistogram(histogram)
                .build();
    }
}
