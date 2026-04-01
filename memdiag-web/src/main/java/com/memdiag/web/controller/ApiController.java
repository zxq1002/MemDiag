package com.memdiag.web.controller;

import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Recommendation;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.nmt.NmtSnapshot;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.thread.ThreadStats;
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

    private static final Gson gson = new Gson();

    // ========== Connection Management ==========

    @GetMapping("/connections")
    public ResponseEntity<String> getConnections() {
        return ResponseEntity.ok(gson.toJson(analysisService.getConnections()));
    }

    @PostMapping("/connections/{id}")
    public ResponseEntity<String> connect(
            @PathVariable String id,
            @RequestParam(required = false) String target) {
        boolean success = analysisService.connect(id, target);
        JsonObject result = new JsonObject();
        if (success) {
            result.addProperty("status", "connected");
        } else {
            result.addProperty("error", "Failed to connect");
        }
        return ResponseEntity.ok(gson.toJson(result));
    }

    @DeleteMapping("/connections/{id}")
    public ResponseEntity<String> disconnect(@PathVariable String id) {
        analysisService.disconnect(id);
        JsonObject result = new JsonObject();
        result.addProperty("status", "disconnected");
        return ResponseEntity.ok(gson.toJson(result));
    }

    // ========== Core Analysis (dual mode: JMX or Agent) ==========

    @GetMapping({"/histogram/{id}", "/connections/{id}/histogram"})
    public ResponseEntity<String> getHistogram(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            HeapHistogram histogram = analysisService.getHistogram(id, limit);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("totalBytes", histogram.getTotalBytes());
            data.addProperty("totalObjects", histogram.getTotalObjects());

            com.google.gson.JsonArray classesArray = new com.google.gson.JsonArray();
            for (ClassStats entry : histogram.getClassStats()) {
                JsonObject cls = new JsonObject();
                cls.addProperty("className", entry.getClassName());
                cls.addProperty("totalSize", entry.getShallowBytes());
                cls.addProperty("instanceCount", entry.getObjectCount());
                classesArray.add(cls);
            }
            data.add("classes", classesArray);

            result.add("data", data);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/diagnose/{id}", "/connections/{id}/diagnose"})
    public ResponseEntity<String> diagnose(@PathVariable String id) {
        try {
            DiagnosisResult result = analysisService.diagnose(id);
            // Match Frontend expectation for Diagnosis.vue
            JsonObject response = new JsonObject();
            com.google.gson.JsonArray issuesArray = new com.google.gson.JsonArray();
            for (Issue issue : result.getIssues()) {
                JsonObject issueObj = new JsonObject();
                issueObj.addProperty("title", issue.getTitle());
                issueObj.addProperty("description", issue.getDescription());
                issueObj.addProperty("severity", issue.getSeverity().name());
                
                String recommendation = "";
                if (issue.getRecommendations() != null && !issue.getRecommendations().isEmpty()) {
                    Recommendation rec = issue.getRecommendations().get(0);
                    recommendation = rec.getDescription() != null ? rec.getDescription() : rec.getTitle();
                }
                issueObj.addProperty("recommendation", recommendation);
                issuesArray.add(issueObj);
            }
            response.add("issues", issuesArray);
            response.addProperty("summary", result.getSummary());
            return ResponseEntity.ok(gson.toJson(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/threads/{id}", "/connections/{id}/threads"})
    public ResponseEntity<String> getThreads(@PathVariable String id) {
        try {
            ThreadDump dump = analysisService.getThreads(id);
            // Match Threads.vue: expect an object with threadStats array
            JsonObject response = new JsonObject();
            com.google.gson.JsonArray threadsArray = new com.google.gson.JsonArray();
            
            for (ThreadStats thread : dump.getThreadStats()) {
                JsonObject threadObj = new JsonObject();
                threadObj.addProperty("threadId", thread.getThreadId());
                threadObj.addProperty("threadName", thread.getThreadName());
                threadObj.addProperty("state", thread.getState() != null ? thread.getState().name() : "UNKNOWN");
                threadObj.addProperty("blockedCount", thread.getBlockedCount());
                threadObj.addProperty("waitedCount", thread.getWaitedCount());
                
                com.google.gson.JsonArray stackArray = new com.google.gson.JsonArray();
                if (thread.getStackTrace() != null) {
                    for (com.memdiag.core.thread.StackFrame frame : thread.getStackTrace()) {
                        JsonObject frameObj = new JsonObject();
                        frameObj.addProperty("className", frame.getClassName());
                        frameObj.addProperty("methodName", frame.getMethodName());
                        frameObj.addProperty("fileName", frame.getFileName());
                        frameObj.addProperty("lineNumber", frame.getLineNumber());
                        frameObj.addProperty("nativeMethod", frame.isNativeMethod());
                        stackArray.add(frameObj);
                    }
                }
                threadObj.add("stackTrace", stackArray);
                threadsArray.add(threadObj);
            }
            response.add("threadStats", threadsArray);
            response.addProperty("timestamp", dump.getTimestamp() != null ? dump.getTimestamp().toString() : null);
            return ResponseEntity.ok(gson.toJson(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/nmt/{id}", "/connections/{id}/nmt"})
    public ResponseEntity<String> getNmt(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean detail) {
        try {
            NmtSnapshot snapshot = analysisService.getNmtSnapshot(id, detail);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("totalReserved", snapshot.getTotalReserved());
            data.addProperty("totalCommitted", snapshot.getTotalCommitted());

            com.google.gson.JsonArray categories = new com.google.gson.JsonArray();
            for (com.memdiag.core.nmt.NmtMemoryUsage usage : snapshot.getUsages()) {
                JsonObject catObj = new JsonObject();
                catObj.addProperty("name", usage.getCategory().getDisplayName());
                catObj.addProperty("reserved", usage.getReserved());
                catObj.addProperty("committed", usage.getCommitted());
                categories.add(catObj);
            }
            data.add("categories", categories);
            data.addProperty("raw", snapshot.toString()); // Keep raw for debugging

            result.add("data", data);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    // ========== Agent API (Agent mode only) ==========

    @GetMapping({"/agent/status/{id}", "/connections/{id}/agent/status"})
    public ResponseEntity<String> getAgentStatus(@PathVariable String id) {
        try {
            JsonObject status = analysisService.getAgentStatus(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", status);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/agent/config/{id}", "/connections/{id}/agent/config"})
    public ResponseEntity<String> getAgentConfig(@PathVariable String id) {
        try {
            JsonObject config = analysisService.getAgentConfig(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", config);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/agent/metrics/{id}", "/connections/{id}/agent/metrics"})
    public ResponseEntity<String> getAgentMetrics(@PathVariable String id) {
        try {
            JsonObject metrics = analysisService.getAgentMetrics(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", metrics);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @PostMapping({"/agent/detach/{id}", "/connections/{id}/agent/detach"})
    public ResponseEntity<String> detachAgent(@PathVariable String id) {
        try {
            boolean success = analysisService.detachAgent(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            JsonObject data = new JsonObject();
            data.addProperty("success", success);
            data.addProperty("message", success ? "Detach request sent" : "Failed to send detach request");
            result.add("data", data);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    // ========== Native Memory API (Agent mode only) ==========

    @GetMapping({"/native/status/{id}", "/connections/{id}/native/status"})
    public ResponseEntity<String> getNativeStatus(@PathVariable String id) {
        try {
            JsonObject status = analysisService.getNativeStatus(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", status);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/native/summary/{id}", "/connections/{id}/native/summary"})
    public ResponseEntity<String> getNativeSummary(@PathVariable String id) {
        try {
            JsonObject summary = analysisService.getNativeSummary(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", summary);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/native/regions/{id}", "/connections/{id}/native/regions"})
    public ResponseEntity<String> getNativeRegions(@PathVariable String id) {
        try {
            JsonObject regions = analysisService.getNativeRegions(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", regions);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/native/diagnose/{id}", "/connections/{id}/native/diagnose"})
    public ResponseEntity<String> getNativeDiagnosis(@PathVariable String id) {
        try {
            JsonObject diagnosis = analysisService.getNativeDiagnosis(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", diagnosis);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    // ========== Allocation Tracking API (Agent mode only) ==========

    @GetMapping({"/allocations/recent/{id}", "/connections/{id}/allocations/recent"})
    public ResponseEntity<String> getAllocationsRecent(
            @PathVariable String id,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            JsonObject allocations = analysisService.getAllocationsRecent(id, limit);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", allocations);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/allocations/stats/{id}", "/connections/{id}/allocations/stats"})
    public ResponseEntity<String> getAllocationsStats(@PathVariable String id) {
        try {
            JsonObject stats = analysisService.getAllocationsStats(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", stats);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/allocations/top/{id}", "/connections/{id}/allocations/top"})
    public ResponseEntity<String> getAllocationsTop(
            @PathVariable String id,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            JsonObject top = analysisService.getAllocationsTop(id, limit);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", top);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/allocations/rate/{id}", "/connections/{id}/allocations/rate"})
    public ResponseEntity<String> getAllocationsRate(@PathVariable String id) {
        try {
            JsonObject rate = analysisService.getAllocationsRate(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", rate);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/allocations/summary/{id}", "/connections/{id}/allocations/summary"})
    public ResponseEntity<String> getAllocationsSummary(@PathVariable String id) {
        try {
            JsonObject summary = analysisService.getAllocationsSummary(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", summary);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    // ========== Method Monitoring API (Agent mode only) ==========

    @GetMapping({"/methods/stats/{id}", "/connections/{id}/methods/stats"})
    public ResponseEntity<String> getMethodsStats(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            JsonObject stats = analysisService.getMethodsStats(id, limit);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", stats);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @GetMapping({"/methods/slow/{id}", "/connections/{id}/methods/slow"})
    public ResponseEntity<String> getMethodsSlow(
            @PathVariable String id,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "100") int thresholdMs) {
        try {
            JsonObject slow = analysisService.getMethodsSlow(id, limit, thresholdMs);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", slow);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    // ========== Instrumentation Control API (Agent mode only) ==========

    @GetMapping({"/instrumentation/status/{id}", "/connections/{id}/instrumentation/status"})
    public ResponseEntity<String> getInstrumentationStatus(@PathVariable String id) {
        try {
            JsonObject status = analysisService.getInstrumentationStatus(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", status);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @PostMapping({"/instrumentation/allocation/enable/{id}", "/connections/{id}/instrumentation/allocation/enable"})
    public ResponseEntity<String> enableAllocationTracking(@PathVariable String id) {
        try {
            JsonObject resultData = analysisService.enableAllocationTracking(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", resultData);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @PostMapping({"/instrumentation/allocation/disable/{id}", "/connections/{id}/instrumentation/allocation/disable"})
    public ResponseEntity<String> disableAllocationTracking(@PathVariable String id) {
        try {
            JsonObject resultData = analysisService.disableAllocationTracking(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", resultData);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @PostMapping({"/instrumentation/methods/enable/{id}", "/connections/{id}/instrumentation/methods/enable"})
    public ResponseEntity<String> enableMethodMonitoring(@PathVariable String id) {
        try {
            JsonObject resultData = analysisService.enableMethodMonitoring(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", resultData);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @PostMapping({"/instrumentation/methods/disable/{id}", "/connections/{id}/instrumentation/methods/disable"})
    public ResponseEntity<String> disableMethodMonitoring(@PathVariable String id) {
        try {
            JsonObject resultData = analysisService.disableMethodMonitoring(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", resultData);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    // ========== JVMTI API (Agent mode only) ==========

    @GetMapping({"/jvmti/status/{id}", "/connections/{id}/jvmti/status"})
    public ResponseEntity<String> getJvmtiStatus(@PathVariable String id) {
        try {
            JsonObject status = analysisService.getJvmtiStatus(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", status);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
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
