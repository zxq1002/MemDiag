package com.memdiag.core.heap;

import com.memdiag.core.util.JmxClient;

public class JmxHeapAnalyzer implements HeapAnalyzer {
    private final JmxClient jmxClient;

    public JmxHeapAnalyzer(JmxClient jmxClient) {
        this.jmxClient = jmxClient;
    }

    @Override
    public HeapHistogram getHistogram(int limit) {
        HeapHistogram histogram = new HeapHistogram();
        // 简单测试数据，后续通过 HotSpotDiagnosticMXBean 实现
        histogram.add(new ClassStats("java.lang.String", 1000, 64000));
        histogram.add(new ClassStats("byte[]", 500, 512000));
        histogram.add(new ClassStats("java.lang.Object", 2000, 32000));
        return histogram;
    }
}
