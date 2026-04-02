package com.memdiag.web.controller;

import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.web.service.AnalysisService;
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
    private final AnalysisService analysisService;

    public RealtimeController(SimpMessagingTemplate messagingTemplate, AnalysisService analysisService) {
        this.messagingTemplate = messagingTemplate;
        this.analysisService = analysisService;
    }

    @Scheduled(fixedRate = 5000)
    public void sendRealtimeUpdates() {
        Map<String, String> connections = analysisService.getConnections();

        for (String id : connections.keySet()) {
            try {
                HeapHistogram histogram = analysisService.getHistogram(id, 10);
                messagingTemplate.convertAndSend("/topic/histogram/" + id, histogram);
            } catch (Exception e) {
                logger.warn("Error sending update for connection: {}", id, e);
            }
        }
    }
}
