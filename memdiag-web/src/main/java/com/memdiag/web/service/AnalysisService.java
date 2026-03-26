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

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisService {

    private final Map<String, JmxClient> connections = new ConcurrentHashMap<>();
    private final Map<String, HeapAnalyzer> heapAnalyzers = new ConcurrentHashMap<>();
    private final Map<String, DiagnosisEngine> diagnosisEngines = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Initialize with current JVM
        try {
            JmxClient client = JmxClient.attachToCurrentJvm();
            String id = "current";
            connections.put(id, client);
            heapAnalyzers.put(id, new JmxHeapAnalyzer(client));
            ThreadAnalyzer threadAnalyzer = new ThreadAnalyzer(client);
            diagnosisEngines.put(id, new DiagnosisEngine(client, heapAnalyzers.get(id), threadAnalyzer));
        } catch (Exception e) {
            // Ignore if current JVM connection fails
        }
    }

    public boolean connect(String id, String pid) {
        try {
            JmxClient client = JmxClient.attachToPid(pid);
            connections.put(id, client);
            heapAnalyzers.put(id, new JmxHeapAnalyzer(client));
            ThreadAnalyzer threadAnalyzer = new ThreadAnalyzer(client);
            diagnosisEngines.put(id, new DiagnosisEngine(client, heapAnalyzers.get(id), threadAnalyzer));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void disconnect(String id) {
        connections.remove(id);
        heapAnalyzers.remove(id);
        diagnosisEngines.remove(id);
    }

    public HeapHistogram getHistogram(String id, int limit) {
        HeapAnalyzer analyzer = heapAnalyzers.get(id);
        if (analyzer == null) {
            throw new IllegalArgumentException("No connection found for id: " + id);
        }
        return analyzer.getHistogram(limit);
    }

    public DiagnosisResult diagnose(String id) {
        DiagnosisEngine engine = diagnosisEngines.get(id);
        if (engine == null) {
            throw new IllegalArgumentException("No connection found for id: " + id);
        }
        return engine.analyze();
    }

    public ThreadDump getThreads(String id) {
        JmxClient client = connections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No connection found for id: " + id);
        }
        ThreadAnalyzer analyzer = new ThreadAnalyzer(client);
        return analyzer.getThreadDump();
    }

    public NmtSnapshot getNmtSnapshot(String id, boolean detail) {
        JmxClient client = connections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No connection found for id: " + id);
        }
        JmxNmtAnalyzer analyzer = new JmxNmtAnalyzer(client);
        if (detail) {
            return analyzer.getDetailSnapshot();
        } else {
            return analyzer.getSummarySnapshot();
        }
    }

    public Map<String, String> getConnections() {
        Map<String, String> result = new ConcurrentHashMap<>();
        for (String id : connections.keySet()) {
            result.put(id, "connected");
        }
        return result;
    }
}
