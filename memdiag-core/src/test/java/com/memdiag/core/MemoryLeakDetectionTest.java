package com.memdiag.core.functional;

import com.memdiag.core.diff.ClassDiff;
import com.memdiag.core.diff.HeapDiff;
import com.memdiag.core.diff.Snapshot;
import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapHistogram;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Functional test for memory leak detection capabilities.
 * Tests the heap diff analysis to detect growing classes over time.
 */
@Tag("functional")
class MemoryLeakDetectionTest extends FunctionalTestBase {

    @Test
    void detectsSingleGrowingClass() {
        // Baseline snapshot
        HeapHistogram baseline = createTestHistogram(5, 1000);
        Snapshot baselineSnapshot = new Snapshot.Builder()
                .setId("baseline")
                .setTimestamp(Instant.now())
                .setHeapHistogram(baseline)
                .build();

        // Simulate leak - make byte[] grow 3x
        HeapHistogram afterLeak = createGrowingHistogram(baseline, "byte[]", 3);
        Snapshot afterSnapshot = new Snapshot.Builder()
                .setId("after-leak")
                .setTimestamp(Instant.now().plusSeconds(60))
                .setHeapHistogram(afterLeak)
                .build();

        // Analyze diff
        HeapDiff diff = HeapDiff.compute(baselineSnapshot, afterSnapshot);

        // Verify byte[] is in growing classes
        List<ClassDiff> growingClasses = diff.getGrowingClasses(10);
        assertThat(growingClasses)
                .extracting(ClassDiff::getClassKey)
                .extracting(key -> key.getClassName())
                .contains("byte[]");

        // Verify growth rate is positive
        ClassDiff byteArrayDiff = growingClasses.stream()
                .filter(cd -> cd.getClassKey().getClassName().equals("byte[]"))
                .findFirst()
                .orElseThrow();

        assertThat(byteArrayDiff.getGrowthRate()).isGreaterThan(1.0);
        assertThat(byteArrayDiff.getObjectCountDelta()).isGreaterThan(0);
        assertThat(byteArrayDiff.getBytesDelta()).isGreaterThan(0);
    }

    @Test
    void detectsMultipleGrowingClasses() {
        // Create baseline
        HeapHistogram baseline = new HeapHistogram();
        baseline.add(new ClassStats("java.util.HashMap", 1000, 64000));
        baseline.add(new ClassStats("java.util.ArrayList", 2000, 128000));
        baseline.add(new ClassStats("java.lang.String", 5000, 320000));

        Snapshot baselineSnapshot = new Snapshot.Builder()
                .setId("baseline")
                .setTimestamp(Instant.now())
                .setHeapHistogram(baseline)
                .build();

        // Create after with multiple growing classes
        HeapHistogram after = new HeapHistogram();
        after.add(new ClassStats("java.util.HashMap", 3000, 192000));  // 3x
        after.add(new ClassStats("java.util.ArrayList", 6000, 384000)); // 3x
        after.add(new ClassStats("java.lang.String", 5000, 320000));  // same

        Snapshot afterSnapshot = new Snapshot.Builder()
                .setId("after")
                .setTimestamp(Instant.now().plusSeconds(120))
                .setHeapHistogram(after)
                .build();

        HeapDiff diff = HeapDiff.compute(baselineSnapshot, afterSnapshot);

        List<ClassDiff> growingClasses = diff.getGrowingClasses(10);

        assertThat(growingClasses)
                .extracting(ClassDiff::getClassKey)
                .extracting(key -> key.getClassName())
                .contains("java.util.HashMap", "java.util.ArrayList");
    }

    @Test
    void detectsNewClasses() {
        HeapHistogram baseline = createTestHistogram(3, 1000);
        Snapshot baselineSnapshot = new Snapshot.Builder()
                .setId("baseline")
                .setTimestamp(Instant.now())
                .setHeapHistogram(baseline)
                .build();

        HeapHistogram after = createTestHistogram(3, 1000);
        // Add new classes
        after.add(new ClassStats("com.example.LeakedClass1", 10000, 640000));
        after.add(new ClassStats("com.example.LeakedClass2", 5000, 320000));

        Snapshot afterSnapshot = new Snapshot.Builder()
                .setId("after")
                .setTimestamp(Instant.now().plusSeconds(300))
                .setHeapHistogram(after)
                .build();

        HeapDiff diff = HeapDiff.compute(baselineSnapshot, afterSnapshot);

        List<ClassDiff> newClasses = diff.getNewClasses();

        assertThat(newClasses)
                .extracting(ClassDiff::getClassKey)
                .extracting(key -> key.getClassName())
                .contains("com.example.LeakedClass1", "com.example.LeakedClass2");
    }

    @Test
    void detectsDisappearedClasses() {
        HeapHistogram baseline = new HeapHistogram();
        baseline.add(new ClassStats("java.lang.String", 1000, 64000));
        baseline.add(new ClassStats("com.example.TemporaryClass", 500, 32000));

        Snapshot baselineSnapshot = new Snapshot.Builder()
                .setId("baseline")
                .setTimestamp(Instant.now())
                .setHeapHistogram(baseline)
                .build();

        HeapHistogram after = new HeapHistogram();
        after.add(new ClassStats("java.lang.String", 1000, 64000));
        // TemporaryClass is gone

        Snapshot afterSnapshot = new Snapshot.Builder()
                .setId("after")
                .setTimestamp(Instant.now().plusSeconds(60))
                .setHeapHistogram(after)
                .build();

        HeapDiff diff = HeapDiff.compute(baselineSnapshot, afterSnapshot);

        List<ClassDiff> disappearedClasses = diff.getDisappearedClasses();

        assertThat(disappearedClasses)
                .extracting(ClassDiff::getClassKey)
                .extracting(key -> key.getClassName())
                .contains("com.example.TemporaryClass");
    }

    @Test
    void calculatesCorrectGrowthRate() {
        HeapHistogram baseline = new HeapHistogram();
        baseline.add(new ClassStats("byte[]", 1000, 64000));

        Snapshot baselineSnapshot = new Snapshot.Builder()
                .setId("baseline")
                .setTimestamp(Instant.now())
                .setHeapHistogram(baseline)
                .build();

        HeapHistogram after = new HeapHistogram();
        after.add(new ClassStats("byte[]", 2500, 160000)); // 2.5x growth

        Snapshot afterSnapshot = new Snapshot.Builder()
                .setId("after")
                .setTimestamp(Instant.now().plusSeconds(60))
                .setHeapHistogram(after)
                .build();

        HeapDiff diff = HeapDiff.compute(baselineSnapshot, afterSnapshot);

        ClassDiff byteArrayDiff = diff.getGrowingClasses(10).stream()
                .filter(cd -> cd.getClassKey().getClassName().equals("byte[]"))
                .findFirst()
                .orElseThrow();

        // Growth rate should be (2500 - 1000) / 1000 = 1.5
        assertThat(byteArrayDiff.getGrowthRate()).isEqualTo(1.5);
    }

    @Test
    void ranksGrowingClassesByByteDelta() {
        HeapHistogram baseline = new HeapHistogram();
        baseline.add(new ClassStats("ClassA", 1000, 100000));  // +400000
        baseline.add(new ClassStats("ClassB", 1000, 50000));   // +150000
        baseline.add(new ClassStats("ClassC", 1000, 10000));   // +40000

        Snapshot baselineSnapshot = new Snapshot.Builder()
                .setId("baseline")
                .setTimestamp(Instant.now())
                .setHeapHistogram(baseline)
                .build();

        HeapHistogram after = new HeapHistogram();
        after.add(new ClassStats("ClassA", 5000, 500000));
        after.add(new ClassStats("ClassB", 4000, 200000));
        after.add(new ClassStats("ClassC", 5000, 50000));

        Snapshot afterSnapshot = new Snapshot.Builder()
                .setId("after")
                .setTimestamp(Instant.now().plusSeconds(60))
                .setHeapHistogram(after)
                .build();

        HeapDiff diff = HeapDiff.compute(baselineSnapshot, afterSnapshot);

        List<ClassDiff> growingClasses = diff.getGrowingClasses(10);

        // Should be ordered by bytes delta descending: ClassA, ClassB, ClassC
        assertThat(growingClasses).hasSize(3);
        assertThat(growingClasses.get(0).getClassKey().getClassName()).isEqualTo("ClassA");
        assertThat(growingClasses.get(1).getClassKey().getClassName()).isEqualTo("ClassB");
        assertThat(growingClasses.get(2).getClassKey().getClassName()).isEqualTo("ClassC");
    }

    @Test
    void multipleSnapshotsShowProgressiveGrowth() {
        List<Snapshot> snapshots = new ArrayList<>();

        // Create 3 snapshots with progressive growth
        for (int i = 0; i < 3; i++) {
            HeapHistogram histogram = new HeapHistogram();
            int byteArrayCount = 1000 + (i * 1000); // 1000, 2000, 3000
            histogram.add(new ClassStats("byte[]", byteArrayCount, byteArrayCount * 64L));
            histogram.add(new ClassStats("java.lang.String", 5000, 320000));

            snapshots.add(new Snapshot.Builder()
                    .setId("snapshot-" + i)
                    .setTimestamp(Instant.now().plusSeconds(i * 60L))
                    .setHeapHistogram(histogram)
                    .build());
        }

        // Compare consecutive snapshots
        for (int i = 1; i < snapshots.size(); i++) {
            HeapDiff diff = HeapDiff.compute(snapshots.get(i - 1), snapshots.get(i));
            List<ClassDiff> growingClasses = diff.getGrowingClasses(10);

            // byte[] should be growing each time
            assertThat(growingClasses)
                    .extracting(ClassDiff::getClassKey)
                    .extracting(key -> key.getClassName())
                    .contains("byte[]");
        }
    }
}
