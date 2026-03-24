package com.memdiag.core.heap;

import com.memdiag.core.util.JmxClient;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JmxHeapAnalyzerTest {

    @Test
    void canGetHistogram() {
        JmxClient client = JmxClient.attachToCurrentJvm();
        HeapAnalyzer analyzer = new JmxHeapAnalyzer(client);

        HeapHistogram histogram = analyzer.getHistogram(10);

        assertThat(histogram).isNotNull();
        assertThat(histogram.getTotalObjects()).isGreaterThan(0);
    }
}
