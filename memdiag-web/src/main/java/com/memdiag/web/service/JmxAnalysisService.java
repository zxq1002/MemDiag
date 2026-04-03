package com.memdiag.web.service;

import com.memdiag.core.diagnose.DiagnosisEngine;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.heap.HeapAnalyzer;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.heap.JmxHeapAnalyzer;
import com.memdiag.core.nmt.JmxNmtAnalyzer;
import com.memdiag.core.nmt.NmtSnapshot;
import com.memdiag.core.thread.ThreadAnalyzer;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.util.JmxClient;
import org.springframework.stereotype.Service;

@Service
public class JmxAnalysisService {

    public HeapHistogram getHistogram(JmxClient client, int limit) {
        HeapAnalyzer analyzer = new JmxHeapAnalyzer(client);
        return analyzer.getHistogram(limit);
    }

    public ThreadDump getThreadDump(JmxClient client) {
        ThreadAnalyzer analyzer = new ThreadAnalyzer(client);
        return analyzer.getThreadDump();
    }

    public NmtSnapshot getNmtSnapshot(JmxClient client, boolean detail) {
        JmxNmtAnalyzer analyzer = new JmxNmtAnalyzer(client);
        if (detail) {
            return analyzer.getDetailSnapshot();
        } else {
            return analyzer.getSummarySnapshot();
        }
    }

    public DiagnosisResult diagnose(DiagnosisEngine engine) {
        return engine.analyze();
    }
}
