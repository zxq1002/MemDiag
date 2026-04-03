package com.memdiag.web.service;

import com.memdiag.core.agent.AgentClient;
import com.memdiag.core.diagnose.DiagnosisEngine;
import com.memdiag.core.diff.SnapshotManager;
import com.memdiag.core.heap.HeapAnalyzer;
import com.memdiag.core.heap.JmxHeapAnalyzer;
import com.memdiag.core.thread.ThreadAnalyzer;
import com.memdiag.core.util.JmxClient;
import com.memdiag.web.config.MemDiagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private final MemDiagProperties properties;
    private final Map<String, JmxClient> jmxConnections = new ConcurrentHashMap<>();
    private final Map<String, AgentClient> agentConnections = new ConcurrentHashMap<>();
    private final Map<String, ConnectionType> connectionTypes = new ConcurrentHashMap<>();
    private final Map<String, SnapshotManager> snapshotManagers = new ConcurrentHashMap<>();

    public enum ConnectionType {
        JMX,
        AGENT
    }

    public ConnectionManager(MemDiagProperties properties) {
        this.properties = properties;
        logger.info("ConnectionManager bean created");
    }

    @PostConstruct
    public void init() {
        logger.info("Initializing ConnectionManager - attempting to connect to current JVM...");
        try {
            ensureCurrentConnected();
        } catch (Exception e) {
            logger.error("Critical failure during ConnectionManager initialization: {}", e.getMessage(), e);
        }
    }

    private synchronized void ensureCurrentConnected() {
        if (!connectionTypes.containsKey("current")) {
            try {
                JmxClient client = JmxClient.attachToCurrentJvm();
                registerJmxConnection("current", client);
                logger.info("Successfully registered default connection: 'current'");
            } catch (Exception e) {
                logger.warn("Auto-connect to current JVM failed: {}. This is expected in some restrictive environments.", e.getMessage());
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("Cleaning up connections...");
        for (String id : jmxConnections.keySet()) {
            disconnect(id);
        }
    }

    public boolean connect(String id, String pidOrAgentAddress) {
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
            registerJmxConnection(id, client);
            return true;
        } catch (Exception e) {
            logger.error("Failed to connect to JMX PID: {}", pid, e);
            return false;
        }
    }

    void registerJmxConnection(String id, JmxClient client) {
        jmxConnections.put(id, client);
        connectionTypes.put(id, ConnectionType.JMX);
    }

    void registerAgentConnection(String id, AgentClient client) {
        agentConnections.put(id, client);
        connectionTypes.put(id, ConnectionType.AGENT);
    }

    private boolean connectAgent(String id, String agentAddress) {
        try {
            String[] parts = agentAddress.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : properties.getAgentPort();
            AgentClient client = new AgentClient(host, port);
            if (client.isReachable()) {
                registerAgentConnection(id, client);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to connect to Agent at: {}", agentAddress, e);
            return false;
        }
    }

    public void disconnect(String id) {
        jmxConnections.remove(id);
        agentConnections.remove(id);
        connectionTypes.remove(id);
        snapshotManagers.remove(id);
    }

    public ConnectionType getConnectionType(String id) {
        return connectionTypes.get(id);
    }

    public JmxClient getJmxClient(String id) {
        return jmxConnections.get(id);
    }

    public DiagnosisEngine getDiagnosisEngine(String id) {
        JmxClient client = getJmxClient(id);
        if (client == null) {
            return null;
        }
        HeapAnalyzer analyzer = new JmxHeapAnalyzer(client);
        ThreadAnalyzer threadAnalyzer = new ThreadAnalyzer(client);
        return new DiagnosisEngine(client, analyzer, threadAnalyzer);
    }

    public AgentClient getAgentClient(String id) {
        return agentConnections.get(id);
    }

    public SnapshotManager getSnapshotManager(String id) {
        return snapshotManagers.computeIfAbsent(id, k -> new SnapshotManager(java.nio.file.Paths.get(".memdiag/snapshots", k)));
    }

    public Map<String, String> getConnections() {
        // Double-check current connection on every poll if it's missing
        if (!connectionTypes.containsKey("current")) {
            ensureCurrentConnected();
        }
        
        Map<String, String> result = new ConcurrentHashMap<>();
        for (String id : connectionTypes.keySet()) {
            ConnectionType type = connectionTypes.get(id);
            result.put(id, type.name().toLowerCase() + ":connected");
        }
        return result;
    }
}
