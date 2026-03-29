package com.memdiag.web.controller;

import com.google.gson.JsonObject;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.nmt.NmtSnapshot;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.web.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class ApiController {

    @Autowired
    private AnalysisService analysisService;

    // ========== Connection Management ==========

    @GetMapping("/connections")
    public ResponseEntity<Map<String, String>> getConnections() {
        return ResponseEntity.ok(analysisService.getConnections());
    }

    @PostMapping("/connections/{id}")
    public ResponseEntity<Map<String, String>> connect(
            @PathVariable String id,
            @RequestParam(required = false) String target) {
        boolean success = analysisService.connect(id, target);
        if (success) {
            return ResponseEntity.ok(Map.of("status", "connected"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to connect"));
        }
    }

    @DeleteMapping("/connections/{id}")
    public ResponseEntity<Map<String, String>> disconnect(@PathVariable String id) {
        analysisService.disconnect(id);
        return ResponseEntity.ok(Map.of("status", "disconnected"));
    }

    // ========== Core Analysis (dual mode: JMX or Agent) ==========

    @GetMapping("/histogram/{id}")
    public ResponseEntity<HeapHistogram> getHistogram(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            HeapHistogram histogram = analysisService.getHistogram(id, limit);
            return ResponseEntity.ok(histogram);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/diagnose/{id}")
    public ResponseEntity<DiagnosisResult> diagnose(@PathVariable String id) {
        try {
            DiagnosisResult result = analysisService.diagnose(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/threads/{id}")
    public ResponseEntity<ThreadDump> getThreads(@PathVariable String id) {
        try {
            ThreadDump dump = analysisService.getThreads(id);
            return ResponseEntity.ok(dump);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/nmt/{id}")
    public ResponseEntity<NmtSnapshot> getNmt(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean detail) {
        try {
            NmtSnapshot snapshot = analysisService.getNmtSnapshot(id, detail);
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ========== Agent API (Agent mode only) ==========

    @GetMapping("/agent/status/{id}")
    public ResponseEntity<JsonObject> getAgentStatus(@PathVariable String id) {
        try {
            JsonObject status = analysisService.getAgentStatus(id);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @GetMapping("/agent/config/{id}")
    public ResponseEntity<JsonObject> getAgentConfig(@PathVariable String id) {
        try {
            JsonObject config = analysisService.getAgentConfig(id);
            return ResponseEntity.ok(config);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @GetMapping("/agent/metrics/{id}")
    public ResponseEntity<JsonObject> getAgentMetrics(@PathVariable String id) {
        try {
            JsonObject metrics = analysisService.getAgentMetrics(id);
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @PostMapping("/agent/detach/{id}")
    public ResponseEntity<Map<String, Object>> detachAgent(@PathVariable String id) {
        try {
            boolean success = analysisService.detachAgent(id);
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "Detach request sent" : "Failed to send detach request"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========== Native Memory API (Agent mode only) ==========

    @GetMapping("/native/status/{id}")
    public ResponseEntity<JsonObject> getNativeStatus(@PathVariable String id) {
        try {
            JsonObject status = analysisService.getNativeStatus(id);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @GetMapping("/native/summary/{id}")
    public ResponseEntity<JsonObject> getNativeSummary(@PathVariable String id) {
        try {
            JsonObject summary = analysisService.getNativeSummary(id);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @GetMapping("/native/regions/{id}")
    public ResponseEntity<JsonObject> getNativeRegions(@PathVariable String id) {
        try {
            JsonObject regions = analysisService.getNativeRegions(id);
            return ResponseEntity.ok(regions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @GetMapping("/native/diagnose/{id}")
    public ResponseEntity<JsonObject> getNativeDiagnosis(@PathVariable String id) {
        try {
            JsonObject diagnosis = analysisService.getNativeDiagnosis(id);
            return ResponseEntity.ok(diagnosis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    // ========== Allocation Tracking API (Agent mode only) ==========

    @GetMapping("/allocations/recent/{id}")
    public ResponseEntity<JsonObject> getAllocationsRecent(
            @PathVariable String id,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            JsonObject allocations = analysisService.getAllocationsRecent(id, limit);
            return ResponseEntity.ok(allocations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @GetMapping("/allocations/stats/{id}")
    public ResponseEntity<JsonObject> getAllocationsStats(@PathVariable String id) {
        try {
            JsonObject stats = analysisService.getAllocationsStats(id);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @GetMapping("/allocations/top/{id}")
    public ResponseEntity<JsonObject> getAllocationsTop(
            @PathVariable String id,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            JsonObject top = analysisService.getAllocationsTop(id, limit);
            return ResponseEntity.ok(top);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @GetMapping("/allocations/rate/{id}")
    public ResponseEntity<JsonObject> getAllocationsRate(@PathVariable String id) {
        try {
            JsonObject rate = analysisService.getAllocationsRate(id);
            return ResponseEntity.ok(rate);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @GetMapping("/allocations/summary/{id}")
    public ResponseEntity<JsonObject> getAllocationsSummary(@PathVariable String id) {
        try {
            JsonObject summary = analysisService.getAllocationsSummary(id);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    // ========== Method Monitoring API (Agent mode only) ==========

    @GetMapping("/methods/stats/{id}")
    public ResponseEntity<JsonObject> getMethodsStats(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            JsonObject stats = analysisService.getMethodsStats(id, limit);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @GetMapping("/methods/slow/{id}")
    public ResponseEntity<JsonObject> getMethodsSlow(
            @PathVariable String id,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "100") int thresholdMs) {
        try {
            JsonObject slow = analysisService.getMethodsSlow(id, limit, thresholdMs);
            return ResponseEntity.ok(slow);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    // ========== Instrumentation Control API (Agent mode only) ==========

    @GetMapping("/instrumentation/status/{id}")
    public ResponseEntity<JsonObject> getInstrumentationStatus(@PathVariable String id) {
        try {
            JsonObject status = analysisService.getInstrumentationStatus(id);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @PostMapping("/instrumentation/allocation/enable/{id}")
    public ResponseEntity<JsonObject> enableAllocationTracking(@PathVariable String id) {
        try {
            JsonObject result = analysisService.enableAllocationTracking(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @PostMapping("/instrumentation/allocation/disable/{id}")
    public ResponseEntity<JsonObject> disableAllocationTracking(@PathVariable String id) {
        try {
            JsonObject result = analysisService.disableAllocationTracking(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @PostMapping("/instrumentation/methods/enable/{id}")
    public ResponseEntity<JsonObject> enableMethodMonitoring(@PathVariable String id) {
        try {
            JsonObject result = analysisService.enableMethodMonitoring(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    @PostMapping("/instrumentation/methods/disable/{id}")
    public ResponseEntity<JsonObject> disableMethodMonitoring(@PathVariable String id) {
        try {
            JsonObject result = analysisService.disableMethodMonitoring(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    // ========== JVMTI API (Agent mode only) ==========

    @GetMapping("/jvmti/status/{id}")
    public ResponseEntity<JsonObject> getJvmtiStatus(@PathVariable String id) {
        try {
            JsonObject status = analysisService.getJvmtiStatus(id);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(errorResponse(e.getMessage()));
        }
    }

    // ========== Helper methods ==========

    private JsonObject errorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        error.addProperty("timestamp", System.currentTimeMillis());
        return error;
    }
}
