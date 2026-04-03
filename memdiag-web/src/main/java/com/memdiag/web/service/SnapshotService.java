package com.memdiag.web.service;

import com.memdiag.core.diff.Snapshot;
import com.memdiag.core.diff.SnapshotManager;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.util.JmxClient;
import com.memdiag.web.config.MemDiagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SnapshotService {
    private static final Logger logger = LoggerFactory.getLogger(SnapshotService.class);

    private final ConnectionManager connectionManager;
    private final JmxAnalysisService jmxAnalysisService;
    private final AgentApiService agentApiService;
    private final MemDiagProperties properties;

    public SnapshotService(ConnectionManager connectionManager,
                           JmxAnalysisService jmxAnalysisService,
                           AgentApiService agentApiService,
                           MemDiagProperties properties) {
        this.connectionManager = connectionManager;
        this.jmxAnalysisService = jmxAnalysisService;
        this.agentApiService = agentApiService;
        this.properties = properties;
    }

    public Snapshot createSnapshot(String connectionId, String name) {
        logger.info("Creating snapshot for connection: {} (name: {})", connectionId, name);
        
        ConnectionManager.ConnectionType type = connectionManager.getConnectionType(connectionId);
        int limit = properties.getSnapshotHistogramLimit();

        HeapHistogram histogram;
        ThreadDump threadDump;

        try {
            if (type == ConnectionManager.ConnectionType.AGENT) {
                histogram = agentApiService.getHistogram(connectionId, limit);
                threadDump = agentApiService.getThreadDump(connectionId);
            } else {
                JmxClient client = connectionManager.getJmxClient(connectionId);
                if (client == null) {
                    throw new IllegalArgumentException("No connection found for id: " + connectionId);
                }
                histogram = jmxAnalysisService.getHistogram(client, limit);
                threadDump = jmxAnalysisService.getThreadDump(client);
            }

            String snapshotId = name != null && !name.isEmpty() ? name : UUID.randomUUID().toString().substring(0, 8);
            Snapshot snapshot = new Snapshot.Builder()
                .setId(snapshotId)
                .setTimestamp(Instant.now())
                .setHeapHistogram(histogram)
                .setThreadDump(threadDump)
                .build();

            SnapshotManager manager = connectionManager.getSnapshotManager(connectionId);
            manager.saveSnapshot(snapshot);
            
            logger.info("Snapshot '{}' saved successfully for connection '{}'", snapshotId, connectionId);
            return snapshot;
        } catch (Exception e) {
            logger.error("Failed to create snapshot for connection '{}': {}", connectionId, e.getMessage(), e);
            throw new RuntimeException("Snapshot capture failed: " + e.getMessage(), e);
        }
    }

    public List<SnapshotManager.SnapshotInfo> listSnapshots(String connectionId) {
        return connectionManager.getSnapshotManager(connectionId).listSnapshots();
    }

    public Snapshot loadSnapshot(String connectionId, String snapshotId) {
        Snapshot snapshot = connectionManager.getSnapshotManager(connectionId).loadSnapshot(snapshotId);
        if (snapshot == null) {
            throw new IllegalArgumentException("Snapshot not found: " + snapshotId);
        }
        return snapshot;
    }

    public boolean deleteSnapshot(String connectionId, String snapshotId) {
        SnapshotManager manager = connectionManager.getSnapshotManager(connectionId);
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
            java.nio.file.Path path = manager.findSnapshot(info.filename);
            return path != null && manager.deleteSnapshot(path);
        }
        return false;
    }
}
