package com.memdiag.web.service;

import com.google.gson.JsonObject;
import com.memdiag.core.agent.AgentClient;
import com.memdiag.core.diagnose.DiagnosisEngine;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diff.Snapshot;
import com.memdiag.core.diff.SnapshotManager;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisService {

    private final Map<String, JmxClient> jmxConnections = new ConcurrentHashMap<>();
    private final Map<String, AgentClient> agentConnections = new ConcurrentHashMap<>();
    private final Map<String, HeapAnalyzer> heapAnalyzers = new ConcurrentHashMap<>();
    private final Map<String, DiagnosisEngine> diagnosisEngines = new ConcurrentHashMap<>();
    private final Map<String, ConnectionType> connectionTypes = new ConcurrentHashMap<>();
    private final Map<String, SnapshotManager> snapshotManagers = new ConcurrentHashMap<>();

    public enum ConnectionType {
        JMX,
        AGENT
    }

    @PostConstruct
    public void init() {
        // Initialize with current JVM
        try {
            JmxClient client = JmxClient.attachToCurrentJvm();
            String id = "current";
            jmxConnections.put(id, client);
            heapAnalyzers.put(id, new JmxHeapAnalyzer(client));
            ThreadAnalyzer threadAnalyzer = new ThreadAnalyzer(client);
            diagnosisEngines.put(id, new DiagnosisEngine(client, heapAnalyzers.get(id), threadAnalyzer));
            connectionTypes.put(id, ConnectionType.JMX);
        } catch (Exception e) {
            // Ignore if current JVM connection fails
        }
    }

    public boolean connect(String id, String pidOrAgentAddress) {
        // Check if it's an agent address (host:port format)
        if (pidOrAgentAddress != null && pidOrAgentAddress.contains(":")) {
            return connectAgent(id, pidOrAgentAddress);
        } else {
            return connectJmx(id, pidOrAgentAddress);
        }
    }

    private boolean connectJmx(String id, String pid) {
        try {
            JmxClient client = pid != null && !pid.isEmpty()
                ? JmxClient.attachToPid(pid)
                : JmxClient.attachToCurrentJvm();
            jmxConnections.put(id, client);
            heapAnalyzers.put(id, new JmxHeapAnalyzer(client));
            ThreadAnalyzer threadAnalyzer = new ThreadAnalyzer(client);
            diagnosisEngines.put(id, new DiagnosisEngine(client, heapAnalyzers.get(id), threadAnalyzer));
            connectionTypes.put(id, ConnectionType.JMX);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean connectAgent(String id, String agentAddress) {
        try {
            String[] parts = agentAddress.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 6789;
            AgentClient client = new AgentClient(host, port);
            if (client.isReachable()) {
                agentConnections.put(id, client);
                connectionTypes.put(id, ConnectionType.AGENT);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void disconnect(String id) {
        jmxConnections.remove(id);
        agentConnections.remove(id);
        heapAnalyzers.remove(id);
        diagnosisEngines.remove(id);
        connectionTypes.remove(id);
    }

    public ConnectionType getConnectionType(String id) {
        return connectionTypes.get(id);
    }

    public JmxClient getJmxClient(String id) {
        return jmxConnections.get(id);
    }

    public AgentClient getAgentClient(String id) {
        return agentConnections.get(id);
    }

    public HeapHistogram getHistogram(String id, int limit) {
        ConnectionType type = connectionTypes.get(id);
        if (type == ConnectionType.AGENT) {
            AgentClient client = agentConnections.get(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.getHistogram(limit);
        } else {
            HeapAnalyzer analyzer = heapAnalyzers.get(id);
            if (analyzer == null) {
                throw new IllegalArgumentException("No connection found for id: " + id);
            }
            return analyzer.getHistogram(limit);
        }
    }

    public DiagnosisResult diagnose(String id) {
        ConnectionType type = connectionTypes.get(id);
        if (type == ConnectionType.AGENT) {
            AgentClient client = agentConnections.get(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.getDiagnosis();
        } else {
            DiagnosisEngine engine = diagnosisEngines.get(id);
            if (engine == null) {
                throw new IllegalArgumentException("No connection found for id: " + id);
            }
            return engine.analyze();
        }
    }

    public ThreadDump getThreads(String id) {
        ConnectionType type = connectionTypes.get(id);
        if (type == ConnectionType.AGENT) {
            AgentClient client = agentConnections.get(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.getThreadDump();
        } else {
            JmxClient client = jmxConnections.get(id);
            if (client == null) {
                throw new IllegalArgumentException("No connection found for id: " + id);
            }
            ThreadAnalyzer analyzer = new ThreadAnalyzer(client);
            return analyzer.getThreadDump();
        }
    }

    public NmtSnapshot getNmtSnapshot(String id, boolean detail) {
        ConnectionType type = connectionTypes.get(id);
        if (type == ConnectionType.AGENT) {
            AgentClient client = agentConnections.get(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }

            // Get data from Agent
            JsonObject response = client.getNativeSummary();
            if (response == null) {
                return NmtSnapshot.builder().build(); // Empty
            }

            // Extract data from standard {success: true, data: {...}} wrapper if present
            JsonObject data = response;
            if (response.has("success") && response.has("data")) {
                data = response.getAsJsonObject("data");
            }

            // Convert NativeMemorySummary response to NmtSnapshot for consistency
            NmtSnapshot.Builder snapshotBuilder = NmtSnapshot.builder();

            // Map breakdownByCategory to NmtSnapshot categories
            if (data.has("breakdownByCategory")) {
                JsonObject breakdown = data.getAsJsonObject("breakdownByCategory");
                for (String catName : breakdown.keySet()) {
                    long committed = breakdown.get(catName).getAsLong();
                    com.memdiag.core.nmt.NmtCategory category = com.memdiag.core.nmt.NmtCategory.fromString(catName);
                    com.memdiag.core.nmt.NmtMemoryUsage usage = com.memdiag.core.nmt.NmtMemoryUsage.builder()
                        .category(category)
                        .reserved(committed)
                        .committed(committed)
                        .build();
                    snapshotBuilder.addUsage(usage);
                }
            }

            return snapshotBuilder.build();
        } else {
            JmxClient client = jmxConnections.get(id);
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
    }

    // ========== Agent-specific API wrappers ==========

    public JsonObject getAgentStatus(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getAgentStatus();
    }

    public JsonObject getAgentConfig(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getAgentConfig();
    }

    public JsonObject getAgentMetrics(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getAgentMetrics();
    }

    public JsonObject getNativeStatus(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getNativeStatus();
    }

    public JsonObject getNativeSummary(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getNativeSummary();
    }

    public JsonObject getNativeRegions(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getNativeRegions();
    }

    public JsonObject getNativeDiagnosis(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getNativeDiagnosis();
    }

    public JsonObject getAllocationsRecent(String id, int limit) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getAllocationsRecent(limit);
    }

    public JsonObject getAllocationsStats(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getAllocationsStats();
    }

    public JsonObject getAllocationsTop(String id, int limit) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getAllocationsTop(limit);
    }

    public JsonObject getAllocationsRate(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getAllocationsRate();
    }

    public JsonObject getAllocationsSummary(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getAllocationsSummary();
    }

    public JsonObject getMethodsStats(String id, int limit) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getMethodsStats(limit);
    }

    public JsonObject getMethodsSlow(String id, int limit, int thresholdMs) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getMethodsSlow(limit, thresholdMs);
    }

    public JsonObject getInstrumentationStatus(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getInstrumentationStatus();
    }

    public JsonObject enableAllocationTracking(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.enableAllocationTracking();
    }

    public JsonObject disableAllocationTracking(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.disableAllocationTracking();
    }

    public JsonObject enableMethodMonitoring(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.enableMethodMonitoring();
    }

    public JsonObject disableMethodMonitoring(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.disableMethodMonitoring();
    }

    public JsonObject getJvmtiStatus(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.getJvmtiStatus();
    }

    public boolean detachAgent(String id) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        return client.detach();
    }

    public Map<String, String> getConnections() {
        Map<String, String> result = new ConcurrentHashMap<>();
        for (String id : connectionTypes.keySet()) {
            ConnectionType type = connectionTypes.get(id);
            result.put(id, type.name().toLowerCase() + ":connected");
        }
        return result;
    }

    // ========== GC Roots API ==========

    public com.memdiag.core.heap.GcRootStats getGcRootStats(String id) {
        ConnectionType type = connectionTypes.get(id);
        if (type == ConnectionType.AGENT) {
            AgentClient client = agentConnections.get(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.getGcRootStats();
        } else {
            // JMX mode - not supported yet
            throw new UnsupportedOperationException("GC Roots analysis requires Agent mode");
        }
    }

    public boolean startGcRootTracking(String id) {
        ConnectionType type = connectionTypes.get(id);
        if (type == ConnectionType.AGENT) {
            AgentClient client = agentConnections.get(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.startGcRootTracking();
        } else {
            throw new UnsupportedOperationException("GC Roots tracking requires Agent mode");
        }
    }

    public boolean stopGcRootTracking(String id) {
        ConnectionType type = connectionTypes.get(id);
        if (type == ConnectionType.AGENT) {
            AgentClient client = agentConnections.get(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.stopGcRootTracking();
        } else {
            throw new UnsupportedOperationException("GC Roots tracking requires Agent mode");
        }
    }

    // ========== Snapshot Management ==========

    private SnapshotManager getSnapshotManager(String id) {
        return snapshotManagers.computeIfAbsent(id, k -> new SnapshotManager());
    }

    public Snapshot createSnapshot(String connectionId, String name) {
        // Collect data
        HeapHistogram histogram = getHistogram(connectionId, 1000);
        ThreadDump threadDump = getThreads(connectionId);

        // Create snapshot
        String snapshotId = name != null ? name : UUID.randomUUID().toString().substring(0, 8);
        Snapshot snapshot = new Snapshot.Builder()
            .setId(snapshotId)
            .setTimestamp(Instant.now())
            .setHeapHistogram(histogram)
            .setThreadDump(threadDump)
            .build();

        // Save snapshot
        SnapshotManager manager = getSnapshotManager(connectionId);
        manager.saveSnapshot(snapshot);

        return snapshot;
    }

    public List<SnapshotManager.SnapshotInfo> listSnapshots(String connectionId) {
        SnapshotManager manager = getSnapshotManager(connectionId);
        return manager.listSnapshots();
    }

    public Snapshot loadSnapshot(String connectionId, String snapshotId) {
        SnapshotManager manager = getSnapshotManager(connectionId);
        return manager.loadSnapshot(snapshotId);
    }

    public boolean deleteSnapshot(String connectionId, String snapshotId) {
        SnapshotManager manager = getSnapshotManager(connectionId);
        SnapshotManager.SnapshotInfo info = null;
        for (SnapshotManager.SnapshotInfo i : manager.listSnapshots()) {
            if (i.id != null && i.id.equals(snapshotId)) {
                info = i;
                break;
            }
            if (i.filename.equals(snapshotId) || i.filename.equals(snapshotId + ".snapshot")) {
                info = i;
                break;
            }
        }
        if (info != null) {
            return manager.deleteSnapshot(info.path);
        }
        return false;
    }
}
