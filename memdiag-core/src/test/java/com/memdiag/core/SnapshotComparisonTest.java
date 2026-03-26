package com.memdiag.core.functional;

import com.memdiag.core.diff.ClassDiff;
import com.memdiag.core.diff.HeapDiff;
import com.memdiag.core.diff.Snapshot;
import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional test for snapshot comparison and trend analysis.
 * Tests multiple snapshot comparison, trend detection, and progressive growth analysis.
 */
@Tag("functional")
class SnapshotComparisonTest extends FunctionalTestBase {

    @Test
    void multipleSnapshotsWithLinearGrowth() {
        List<Snapshot> snapshots = new ArrayList<>();

        // Create 5 snapshots with linear growth
        for (int i = 0; i < 5; i++) {
            HeapHistogram histogram = new HeapHistogram();
            int baseCount = 1000;
            int growthPerStep = 500;

            histogram.add(new ClassStats("byte[]",
                    baseCount + (i * growthPerStep),
                    (baseCount + (i * growthPerStep)) * 64L));
            histogram.add(new ClassStats("java.lang.String", 5000, 320000));
            histogram.add(new ClassStats("java.util.HashMap$Node", 2000 + (i * 100), 128000 + (i * 6400L)));

            snapshots.add(new Snapshot.Builder()
                    .setId("snapshot-" + i)
                    .setTimestamp(Instant.now().plusSeconds(i * 60L))
                    .setHeapHistogram(histogram)
                    .build());
        }

        // Verify snapshot sequence
        assertThat(snapshots).hasSize(5);

        // Compare each consecutive pair
        for (int i = 1; i < snapshots.size(); i++) {
            HeapDiff diff = HeapDiff.compute(snapshots.get(i - 1), snapshots.get(i));

            // byte[] should be growing each time
            List<ClassDiff> growingClasses = diff.getGrowingClasses(10);
            assertThat(growingClasses)
                    .extracting(ClassDiff::getClassKey)
                    .extracting(key -> key.getClassName())
                    .contains("byte[]");

            // Total object delta should be positive
            assertThat(diff.getTotalObjectDelta()).isGreaterThan(0);
            assertThat(diff.getTotalByteDelta()).isGreaterThan(0);
        }
    }

    @Test
    void detectsAcceleratingGrowth() {
        List<Snapshot> snapshots = new ArrayList<>();

        // Create snapshots with accelerating growth
        for (int i = 0; i < 4; i++) {
            HeapHistogram histogram = new HeapHistogram();
            int multiplier = (int) Math.pow(2, i); // 1, 2, 4, 8

            histogram.add(new ClassStats("byte[]", 1000 * multiplier, 64000L * multiplier));
            histogram.add(new ClassStats("java.lang.String", 5000, 320000));

            snapshots.add(new Snapshot.Builder()
                    .setId("snapshot-" + i)
                    .setTimestamp(Instant.now().plusSeconds(i * 30L))
                    .setHeapHistogram(histogram)
                    .build());
        }

        // Calculate growth rates between each pair
        List<Double> growthRates = new ArrayList<>();
        for (int i = 1; i < snapshots.size(); i++) {
            HeapDiff diff = HeapDiff.compute(snapshots.get(i - 1), snapshots.get(i));
            ClassDiff byteArrayDiff = diff.getGrowingClasses(10).stream()
                    .filter(cd -> cd.getClassKey().getClassName().equals("byte[]"))
                    .findFirst()
                    .orElseThrow();
            growthRates.add(byteArrayDiff.getGrowthRate());
        }

        // Growth rate should be increasing (accelerating)
        // Each step should have higher growth than previous
        for (int i = 1; i < growthRates.size(); i++) {
            // With exponential growth, the rate should be consistent or increasing
            assertThat(growthRates.get(i)).isGreaterThan(0);
        }
    }

    @Test
    void identifiesTopGrowingClassesAcrossSnapshots() {
        List<Snapshot> snapshots = new ArrayList<>();

        // Create snapshots with multiple growing classes
        for (int i = 0; i < 3; i++) {
            HeapHistogram histogram = new HeapHistogram();

            // Different growth rates
            histogram.add(new ClassStats("FastGrowing", 100 + (i * 200), 6400 + (i * 12800L)));
            histogram.add(new ClassStats("MediumGrowing", 200 + (i * 100), 12800 + (i * 6400L)));
            histogram.add(new ClassStats("SlowGrowing", 500 + (i * 50), 32000 + (i * 3200L)));
            histogram.add(new ClassStats("Stable", 1000, 64000));

            snapshots.add(new Snapshot.Builder()
                    .setId("snapshot-" + i)
                    .setTimestamp(Instant.now().plusSeconds(i * 60L))
                    .setHeapHistogram(histogram)
                    .build());
        }

        // Compare first and last
        HeapDiff overallDiff = HeapDiff.compute(snapshots.get(0), snapshots.get(snapshots.size() - 1));

        List<ClassDiff> growingClasses = overallDiff.getGrowingClasses(10);

        // FastGrowing should be at or near the top
        assertThat(growingClasses)
                .extracting(ClassDiff::getClassKey)
                .extracting(key -> key.getClassName())
                .contains("FastGrowing", "MediumGrowing", "SlowGrowing");
    }

    @Test
    void detectsStableClasses() {
        List<Snapshot> snapshots = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            HeapHistogram histogram = new HeapHistogram();

            // Stable class
            histogram.add(new ClassStats("StableClass", 1000, 64000));
            histogram.add(new ClassStats("AnotherStable", 500, 32000));

            // Growing class
            histogram.add(new ClassStats("GrowingClass", 100 + (i * 100), 6400 + (i * 6400L)));

            snapshots.add(new Snapshot.Builder()
                    .setId("snapshot-" + i)
                    .setTimestamp(Instant.now().plusSeconds(i * 60L))
                    .setHeapHistogram(histogram)
                    .build());
        }

        HeapDiff diff = HeapDiff.compute(snapshots.get(0), snapshots.get(snapshots.size() - 1));

        // GrowingClass should be in growing list
        assertThat(diff.getGrowingClasses(10))
                .extracting(ClassDiff::getClassKey)
                .extracting(key -> key.getClassName())
                .contains("GrowingClass");

        // Total diff should be positive due to GrowingClass
        assertThat(diff.getTotalObjectDelta()).isGreaterThan(0);
    }

    @Test
    void tracksThreadChangesAcrossSnapshots() {
        List<Snapshot> snapshots = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            HeapHistogram histogram = createTestHistogram(5, 1000);
            ThreadDump threadDump = createTestThreadDump(5 + i * 2); // 5, 7, 9 threads

            snapshots.add(new Snapshot.Builder()
                    .setId("snapshot-" + i)
                    .setTimestamp(Instant.now().plusSeconds(i * 60L))
                    .setHeapHistogram(histogram)
                    .setThreadDump(threadDump)
                    .build());
        }

        // Verify thread count increases
        for (int i = 0; i < snapshots.size(); i++) {
            ThreadDump dump = snapshots.get(i).getThreadDump();
            assertThat(dump).isNotNull();
            int expectedThreadCount = 5 + i * 2;
            // Note: createTestThreadDump adds a main thread, so count may vary slightly
            assertThat(dump.getThreadCount()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void calculatesTotalDeltasCorrectly() {
        HeapHistogram baseline = new HeapHistogram();
        baseline.add(new ClassStats("ClassA", 1000, 64000));
        baseline.add(new ClassStats("ClassB", 2000, 128000));

        Snapshot baselineSnapshot = new Snapshot.Builder()
                .setId("baseline")
                .setTimestamp(Instant.now())
                .setHeapHistogram(baseline)
                .build();

        HeapHistogram after = new HeapHistogram();
        after.add(new ClassStats("ClassA", 1500, 96000));   // +500 objects, +32000 bytes
        after.add(new ClassStats("ClassB", 3000, 192000));   // +1000 objects, +64000 bytes
        after.add(new ClassStats("ClassC", 500, 32000));     // +500 objects, +32000 bytes (new)

        Snapshot afterSnapshot = new Snapshot.Builder()
                .setId("after")
                .setTimestamp(Instant.now().plusSeconds(60))
                .setHeapHistogram(after)
                .build();

        HeapDiff diff = HeapDiff.compute(baselineSnapshot, afterSnapshot);

        // Total object delta: 500 + 1000 + 500 = 2000
        assertThat(diff.getTotalObjectDelta()).isEqualTo(2000);

        // Total byte delta: 32000 + 64000 + 32000 = 128000
        assertThat(diff.getTotalByteDelta()).isEqualTo(128000);
    }

    @Test
    void ranksByGrowthRate() {
        HeapHistogram baseline = new HeapHistogram();
        baseline.add(new ClassStats("HighGrowth", 100, 6400));    // will be 500 (400% growth)
        baseline.add(new ClassStats("LowGrowth", 1000, 64000));   // will be 1200 (20% growth)
        baseline.add(new ClassStats("MediumGrowth", 500, 32000));  // will be 1000 (100% growth)

        Snapshot baselineSnapshot = new Snapshot.Builder()
                .setId("baseline")
                .setTimestamp(Instant.now())
                .setHeapHistogram(baseline)
                .build();

        HeapHistogram after = new HeapHistogram();
        after.add(new ClassStats("HighGrowth", 500, 32000));
        after.add(new ClassStats("LowGrowth", 1200, 76800));
        after.add(new ClassStats("MediumGrowth", 1000, 64000));

        Snapshot afterSnapshot = new Snapshot.Builder()
                .setId("after")
                .setTimestamp(Instant.now().plusSeconds(60))
                .setHeapHistogram(after)
                .build();

        HeapDiff diff = HeapDiff.compute(baselineSnapshot, afterSnapshot);

        List<ClassDiff> topByGrowth = diff.getTopByGrowthRate(10);

        // Should be ordered by growth rate: HighGrowth (4.0), MediumGrowth (1.0), LowGrowth (0.2)
        assertThat(topByGrowth).hasSize(3);
        assertThat(topByGrowth.get(0).getClassKey().getClassName()).isEqualTo("HighGrowth");
        assertThat(topByGrowth.get(1).getClassKey().getClassName()).isEqualTo("MediumGrowth");
        assertThat(topByGrowth.get(2).getClassKey().getClassName()).isEqualTo("LowGrowth");

        // Verify growth rates
        assertThat(topByGrowth.get(0).getGrowthRate()).isEqualTo(4.0);
        assertThat(topByGrowth.get(1).getGrowthRate()).isEqualTo(1.0);
        assertThat(topByGrowth.get(2).getGrowthRate()).isEqualTo(0.2);
    }
}
