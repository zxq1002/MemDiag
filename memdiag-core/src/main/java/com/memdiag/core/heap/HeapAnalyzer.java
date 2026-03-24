package com.memdiag.core.heap;

public interface HeapAnalyzer {
    HeapHistogram getHistogram(int limit);

    HeapHistogram getFullHistogram();
}
