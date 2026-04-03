package com.memdiag.web.service;

import com.memdiag.core.diff.Snapshot;
import com.memdiag.core.diff.SnapshotManager;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.util.JmxClient;
import com.memdiag.web.config.MemDiagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {

    @Mock
    private ConnectionManager connectionManager;

    @Mock
    private JmxAnalysisService jmxAnalysisService;

    @Mock
    private AgentApiService agentApiService;

    @Mock
    private MemDiagProperties properties;

    @Mock
    private SnapshotManager snapshotManager;

    @Mock
    private JmxClient jmxClient;

    private SnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        snapshotService = new SnapshotService(connectionManager, jmxAnalysisService, agentApiService, properties);
    }

    @Test
    void canCreateSnapshotForJmx() {
        when(connectionManager.getConnectionType("jmx-id")).thenReturn(ConnectionManager.ConnectionType.JMX);
        when(connectionManager.getJmxClient("jmx-id")).thenReturn(jmxClient);
        when(properties.getSnapshotHistogramLimit()).thenReturn(100);
        when(jmxAnalysisService.getHistogram(any(), anyInt())).thenReturn(new HeapHistogram());
        when(jmxAnalysisService.getThreadDump(any())).thenReturn(new ThreadDump());
        when(connectionManager.getSnapshotManager("jmx-id")).thenReturn(snapshotManager);

        Snapshot snapshot = snapshotService.createSnapshot("jmx-id", "test-snapshot");

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getId()).isEqualTo("test-snapshot");
        verify(snapshotManager).saveSnapshot(any(Snapshot.class));
    }

    @Test
    void canCreateSnapshotForAgent() {
        when(connectionManager.getConnectionType("agent-id")).thenReturn(ConnectionManager.ConnectionType.AGENT);
        when(properties.getSnapshotHistogramLimit()).thenReturn(100);
        when(agentApiService.getHistogram(anyString(), anyInt())).thenReturn(new HeapHistogram());
        when(agentApiService.getThreadDump(anyString())).thenReturn(new ThreadDump());
        when(connectionManager.getSnapshotManager("agent-id")).thenReturn(snapshotManager);

        Snapshot snapshot = snapshotService.createSnapshot("agent-id", null);

        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getId()).hasSize(8);
        verify(snapshotManager).saveSnapshot(any(Snapshot.class));
    }

    @Test
    void canListSnapshots() {
        when(connectionManager.getSnapshotManager("test-id")).thenReturn(snapshotManager);
        List<SnapshotManager.SnapshotInfo> infoList = Collections.emptyList();
        when(snapshotManager.listSnapshots()).thenReturn(infoList);

        List<SnapshotManager.SnapshotInfo> result = snapshotService.listSnapshots("test-id");

        assertThat(result).isSameAs(infoList);
    }
}
