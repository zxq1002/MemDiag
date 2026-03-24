package com.memdiag.core.heap;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class HeapHistogramTest {

    @Test
    void createHistogramWithClassStats() {
        ClassStats stats1 = new ClassStats("java.lang.String", 1000, 64000);
        ClassStats stats2 = new ClassStats("byte[]", 500, 512000);

        HeapHistogram histogram = new HeapHistogram();
        histogram.add(stats1);
        histogram.add(stats2);

        assertThat(histogram.getClassStats()).hasSize(2);
        assertThat(histogram.getTotalObjects()).isEqualTo(1500);
        assertThat(histogram.getTotalBytes()).isEqualTo(576000);
    }

    @Test
    void sortByObjectCountDesc() {
        ClassStats stats1 = new ClassStats("A", 100, 1000);
        ClassStats stats2 = new ClassStats("B", 300, 3000);
        ClassStats stats3 = new ClassStats("C", 200, 2000);

        HeapHistogram histogram = new HeapHistogram();
        histogram.add(stats1);
        histogram.add(stats2);
        histogram.add(stats3);

        assertThat(histogram.getTopByObjectCount(2))
            .extracting(ClassStats::getClassName)
            .containsExactly("B", "C");
    }
}
