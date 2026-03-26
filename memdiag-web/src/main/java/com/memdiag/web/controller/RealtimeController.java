package com.memdiag.web.controller;

import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.web.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class RealtimeController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AnalysisService analysisService;

    @Scheduled(fixedRate = 5000)
    public void sendRealtimeUpdates() {
        Map<String, String> connections = analysisService.getConnections();

        for (String id : connections.keySet()) {
            try {
                HeapHistogram histogram = analysisService.getHistogram(id, 10);
                messagingTemplate.convertAndSend("/topic/histogram/" + id, histogram);
            } catch (Exception e) {
                // Ignore errors for individual connections
            }
        }
    }
}
