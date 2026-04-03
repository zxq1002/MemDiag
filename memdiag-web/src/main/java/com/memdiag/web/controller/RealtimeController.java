package com.memdiag.web.controller;

import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.util.JmxClient;
import com.memdiag.web.config.MemDiagProperties;
import com.memdiag.web.service.AgentApiService;
import com.memdiag.web.service.ConnectionManager;
import com.memdiag.web.service.JmxAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class RealtimeController {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ConnectionManager connectionManager;
    private final JmxAnalysisService jmxAnalysisService;
    private final AgentApiService agentApiService;
    private final MemDiagProperties properties;

    public RealtimeController(SimpMessagingTemplate messagingTemplate,
                              ConnectionManager connectionManager,
                              JmxAnalysisService jmxAnalysisService,
                              AgentApiService agentApiService,
                              MemDiagProperties properties) {
        this.messagingTemplate = messagingTemplate;
        this.connectionManager = connectionManager;
        this.jmxAnalysisService = jmxAnalysisService;
        this.agentApiService = agentApiService;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${memdiag.realtime-rate:5000}")
    public void sendRealtimeUpdates() {
        Map<String, String> connections = connectionManager.getConnections();

        for (String id : connections.keySet()) {
            try {
                HeapHistogram histogram;
                ConnectionManager.ConnectionType type = connectionManager.getConnectionType(id);
                int limit = properties.getDefaultHistogramLimit();

                if (type == ConnectionManager.ConnectionType.AGENT) {
                    histogram = agentApiService.getHistogram(id, limit);
                } else {
                    JmxClient client = connectionManager.getJmxClient(id);
                    if (client == null) continue;
                    histogram = jmxAnalysisService.getHistogram(client, limit);
                }

                messagingTemplate.convertAndSend("/topic/histogram/" + id, histogram);
            } catch (Exception e) {
                logger.warn("Error sending update for connection: {}", id, e);
            }
        }
    }
}
