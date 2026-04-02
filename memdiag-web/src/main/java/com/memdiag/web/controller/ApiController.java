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
import com.memdiag.web.validation.AddressValidator;
import com.memdiag.web.validation.PidValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private static final Gson gson = new Gson();

    private static final String ERROR_HISTOGRAM = "Failed to retrieve histogram";
    private static final String ERROR_DIAGNOSIS = "Failed to perform diagnosis";
    private static final String ERROR_THREADS = "Failed to retrieve threads";
    private static final String ERROR_NMT = "Failed to retrieve NMT data";
    private static final String ERROR_CONNECTION = "Connection failed";
    private static final String ERROR_GENERIC = "An error occurred while processing your request";
    private static final String ERROR_AGENT_STATUS = "Failed to retrieve agent status";
    private static final String ERROR_AGENT_CONFIG = "Failed to retrieve agent configuration";
    private static final String ERROR_AGENT_METRICS = "Failed to retrieve agent metrics";
    private static final String ERROR_AGENT_DETACH = "Failed to detach agent";
    private static final String ERROR_NATIVE_STATUS = "Failed to retrieve native status";
    private static final String ERROR_NATIVE_SUMMARY = "Failed to retrieve native summary";
    private static final String ERROR_NATIVE_REGIONS = "Failed to retrieve native regions";
    private static final String ERROR_NATIVE_DIAGNOSIS = "Failed to perform native diagnosis";
    private static final String ERROR_ALLOCATIONS_RECENT = "Failed to retrieve recent allocations";
    private static final String ERROR_ALLOCATIONS_STATS = "Failed to retrieve allocation statistics";
    private static final String ERROR_ALLOCATIONS_TOP = "Failed to retrieve top allocations";
    private static final String ERROR_ALLOCATIONS_RATE = "Failed to retrieve allocation rate";
    private static final String ERROR_ALLOCATIONS_SUMMARY = "Failed to retrieve allocation summary";
    private static final String ERROR_METHODS_STATS = "Failed to retrieve method statistics";
    private static final String ERROR_METHODS_SLOW = "Failed to retrieve slow methods";
    private static final String ERROR_INSTRUMENTATION_STATUS = "Failed to retrieve instrumentation status";
    private static final String ERROR_INSTRUMENTATION_ENABLE = "Failed to enable instrumentation";
    private static final String ERROR_INSTRUMENTATION_DISABLE = "Failed to disable instrumentation";
    private static final String ERROR_JVMTI_STATUS = "Failed to retrieve JVMTI status";
    private static final String ERROR_GC_ROOTS_STATS = "Failed to retrieve GC roots statistics";
    private static final String ERROR_GC_ROOTS_TRACK = "Failed to control GC roots tracking";
    private static final String ERROR_SNAPSHOT_CREATE = "Failed to create snapshot";
    private static final String ERROR_SNAPSHOT_LIST = "Failed to list snapshots";
    private static final String ERROR_SNAPSHOT_DELETE = "Failed to delete snapshot";

    private final AnalysisService analysisService;

    public ApiController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    private ResponseEntity<String> validationError(String message) {
        return ResponseEntity.badRequest().body(gson.toJson(errorResponse(message)));
    }

    private boolean isConnectionId(String id) {
        return !PidValidator.isValid(id);
    }

    // ========== Connection Management ==========

    @GetMapping("/connections")
    public ResponseEntity<String> getConnections() {
        try {
            return ResponseEntity.ok(gson.toJson(analysisService.getConnections()));
        } catch (Exception e) {
            logger.error("Error getting connections", e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GENERIC)));
        }
    }

    @PostMapping("/connections/{id}")
    public ResponseEntity<String> connect(
            @PathVariable String id,
            @RequestParam(required = false) String target) {
        if (target != null && !target.isEmpty()) {
            if (!AddressValidator.isValid(target)) {
                logger.warn("Invalid target address: {}", target);
                return validationError(AddressValidator.getErrorMessage(target));
            }
        }

        try {
            boolean success = analysisService.connect(id, target);
            JsonObject result = new JsonObject();
            if (success) {
                result.addProperty("status", "connected");
            } else {
                result.addProperty("error", "Failed to connect");
            }
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error connecting to target: {}", target, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_CONNECTION)));
        }
    }

    @DeleteMapping("/connections/{id}")
    public ResponseEntity<String> disconnect(@PathVariable String id) {
        try {
            analysisService.disconnect(id);
            JsonObject result = new JsonObject();
            result.addProperty("status", "disconnected");
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error disconnecting connection: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GENERIC)));
        }
    }

    // ========== Core Analysis (dual mode: JMX or Agent) ==========

    @GetMapping({"/histogram/{id}", "/connections/{id}/histogram"})
    public ResponseEntity<String> getHistogram(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

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
            logger.error("Error getting histogram for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_HISTOGRAM)));
        }
    }

    @GetMapping({"/diagnose/{id}", "/connections/{id}/diagnose"})
    public ResponseEntity<String> diagnose(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            DiagnosisResult result = analysisService.diagnose(id);
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
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
            data.add("issues", issuesArray);
            data.addProperty("summary", result.getSummary());

            response.add("data", data);
            return ResponseEntity.ok(gson.toJson(response));
        } catch (Exception e) {
            logger.error("Error performing diagnosis for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_DIAGNOSIS)));
        }
    }

    @GetMapping({"/threads/{id}", "/connections/{id}/threads"})
    public ResponseEntity<String> getThreads(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            ThreadDump dump = analysisService.getThreads(id);
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            com.google.gson.JsonArray threadsArray = new com.google.gson.JsonArray();

            for (ThreadStats thread : dump.getThreadStats()) {
                JsonObject threadObj = new JsonObject();
                threadObj.addProperty("id", thread.getThreadId());
                threadObj.addProperty("name", thread.getThreadName());
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
            data.add("threads", threadsArray);
            data.addProperty("timestamp", dump.getTimestamp() != null ? dump.getTimestamp().toString() : null);

            response.add("data", data);
            return ResponseEntity.ok(gson.toJson(response));
        } catch (Exception e) {
            logger.error("Error getting threads for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_THREADS)));
        }
    }

    @GetMapping({"/nmt/{id}", "/connections/{id}/nmt"})
    public ResponseEntity<String> getNmt(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean detail) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

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

            result.add("data", data);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting NMT data for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_NMT)));
        }
    }

    // ========== Agent API (Agent mode only) ==========

    @GetMapping({"/agent/status/{id}", "/connections/{id}/agent/status"})
    public ResponseEntity<String> getAgentStatus(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject status = analysisService.getAgentStatus(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", status);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting agent status for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_AGENT_STATUS)));
        }
    }

    @GetMapping({"/agent/config/{id}", "/connections/{id}/agent/config"})
    public ResponseEntity<String> getAgentConfig(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject config = analysisService.getAgentConfig(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", config);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting agent config for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_AGENT_CONFIG)));
        }
    }

    @GetMapping({"/agent/metrics/{id}", "/connections/{id}/agent/metrics"})
    public ResponseEntity<String> getAgentMetrics(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject metrics = analysisService.getAgentMetrics(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", metrics);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting agent metrics for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_AGENT_METRICS)));
        }
    }

    @PostMapping({"/agent/detach/{id}", "/connections/{id}/agent/detach"})
    public ResponseEntity<String> detachAgent(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

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
            logger.error("Error detaching agent for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_AGENT_DETACH)));
        }
    }

    // ========== Native Memory API (Agent mode only) ==========

    @GetMapping({"/native/status/{id}", "/connections/{id}/native/status"})
    public ResponseEntity<String> getNativeStatus(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject status = analysisService.getNativeStatus(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", status);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting native status for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_NATIVE_STATUS)));
        }
    }

    @GetMapping({"/native/summary/{id}", "/connections/{id}/native/summary"})
    public ResponseEntity<String> getNativeSummary(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject summary = analysisService.getNativeSummary(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", summary);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting native summary for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_NATIVE_SUMMARY)));
        }
    }

    @GetMapping({"/native/regions/{id}", "/connections/{id}/native/regions"})
    public ResponseEntity<String> getNativeRegions(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject regions = analysisService.getNativeRegions(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", regions);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting native regions for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_NATIVE_REGIONS)));
        }
    }

    @GetMapping({"/native/diagnose/{id}", "/connections/{id}/native/diagnose"})
    public ResponseEntity<String> getNativeDiagnosis(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject diagnosis = analysisService.getNativeDiagnosis(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", diagnosis);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting native diagnosis for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_NATIVE_DIAGNOSIS)));
        }
    }

    // ========== Allocation Tracking API (Agent mode only) ==========

    @GetMapping({"/allocations/recent/{id}", "/connections/{id}/allocations/recent"})
    public ResponseEntity<String> getAllocationsRecent(
            @PathVariable String id,
            @RequestParam(defaultValue = "100") int limit) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject allocations = analysisService.getAllocationsRecent(id, limit);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", allocations);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting recent allocations for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_ALLOCATIONS_RECENT)));
        }
    }

    @GetMapping({"/allocations/stats/{id}", "/connections/{id}/allocations/stats"})
    public ResponseEntity<String> getAllocationsStats(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject stats = analysisService.getAllocationsStats(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", stats);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting allocation stats for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_ALLOCATIONS_STATS)));
        }
    }

    @GetMapping({"/allocations/top/{id}", "/connections/{id}/allocations/top"})
    public ResponseEntity<String> getAllocationsTop(
            @PathVariable String id,
            @RequestParam(defaultValue = "10") int limit) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject top = analysisService.getAllocationsTop(id, limit);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", top);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting top allocations for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_ALLOCATIONS_TOP)));
        }
    }

    @GetMapping({"/allocations/rate/{id}", "/connections/{id}/allocations/rate"})
    public ResponseEntity<String> getAllocationsRate(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject rate = analysisService.getAllocationsRate(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", rate);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting allocation rate for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_ALLOCATIONS_RATE)));
        }
    }

    @GetMapping({"/allocations/summary/{id}", "/connections/{id}/allocations/summary"})
    public ResponseEntity<String> getAllocationsSummary(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject summary = analysisService.getAllocationsSummary(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", summary);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting allocation summary for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_ALLOCATIONS_SUMMARY)));
        }
    }

    // ========== Method Monitoring API (Agent mode only) ==========

    @GetMapping({"/methods/stats/{id}", "/connections/{id}/methods/stats"})
    public ResponseEntity<String> getMethodsStats(
            @PathVariable String id,
            @RequestParam(defaultValue = "20") int limit) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject stats = analysisService.getMethodsStats(id, limit);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", stats);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting method stats for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_METHODS_STATS)));
        }
    }

    @GetMapping({"/methods/slow/{id}", "/connections/{id}/methods/slow"})
    public ResponseEntity<String> getMethodsSlow(
            @PathVariable String id,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "100") int thresholdMs) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject slow = analysisService.getMethodsSlow(id, limit, thresholdMs);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", slow);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting slow methods for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_METHODS_SLOW)));
        }
    }

    // ========== Instrumentation Control API (Agent mode only) ==========

    @GetMapping({"/instrumentation/status/{id}", "/connections/{id}/instrumentation/status"})
    public ResponseEntity<String> getInstrumentationStatus(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject status = analysisService.getInstrumentationStatus(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", status);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting instrumentation status for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_INSTRUMENTATION_STATUS)));
        }
    }

    @PostMapping({"/instrumentation/allocation/enable/{id}", "/connections/{id}/instrumentation/allocation/enable"})
    public ResponseEntity<String> enableAllocationTracking(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject resultData = analysisService.enableAllocationTracking(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", resultData);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error enabling allocation tracking for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_INSTRUMENTATION_ENABLE)));
        }
    }

    @PostMapping({"/instrumentation/allocation/disable/{id}", "/connections/{id}/instrumentation/allocation/disable"})
    public ResponseEntity<String> disableAllocationTracking(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject resultData = analysisService.disableAllocationTracking(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", resultData);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error disabling allocation tracking for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_INSTRUMENTATION_DISABLE)));
        }
    }

    @PostMapping({"/instrumentation/methods/enable/{id}", "/connections/{id}/instrumentation/methods/enable"})
    public ResponseEntity<String> enableMethodMonitoring(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject resultData = analysisService.enableMethodMonitoring(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", resultData);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error enabling method monitoring for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_INSTRUMENTATION_ENABLE)));
        }
    }

    @PostMapping({"/instrumentation/methods/disable/{id}", "/connections/{id}/instrumentation/methods/disable"})
    public ResponseEntity<String> disableMethodMonitoring(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject resultData = analysisService.disableMethodMonitoring(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", resultData);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error disabling method monitoring for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_INSTRUMENTATION_DISABLE)));
        }
    }

    // ========== JVMTI API (Agent mode only) ==========

    @GetMapping({"/jvmt/status/{id}", "/connections/{id}/jvmt/status"})
    public ResponseEntity<String> getJvmtiStatus(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            JsonObject status = analysisService.getJvmtiStatus(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            result.add("data", status);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting JVMTI status for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_JVMTI_STATUS)));
        }
    }

    // ========== GC Roots API ==========

    @GetMapping({"/gc-roots/stats/{id}", "/connections/{id}/gc-roots/stats"})
    public ResponseEntity<String> getGcRootStats(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            com.memdiag.core.heap.GcRootStats stats = analysisService.getGcRootStats(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("totalRoots", stats.getTotalRoots());

            JsonObject countsByType = new JsonObject();
            for (com.memdiag.core.heap.GcRootType type : com.memdiag.core.heap.GcRootType.values()) {
                countsByType.addProperty(type.name(), stats.getCount(type));
            }
            data.add("countsByType", countsByType);

            result.add("data", data);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error getting GC roots stats for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GC_ROOTS_STATS)));
        }
    }

    @PostMapping({"/gc-roots/track/start/{id}", "/connections/{id}/gc-roots/track/start"})
    public ResponseEntity<String> startGcRootTracking(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            boolean success = analysisService.startGcRootTracking(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("success", success);
            result.add("data", data);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error starting GC roots tracking for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GC_ROOTS_TRACK)));
        }
    }

    @PostMapping({"/gc-roots/track/stop/{id}", "/connections/{id}/gc-roots/track/stop"})
    public ResponseEntity<String> stopGcRootTracking(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            boolean success = analysisService.stopGcRootTracking(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("success", success);
            result.add("data", data);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error stopping GC roots tracking for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_GC_ROOTS_TRACK)));
        }
    }

    // ========== Snapshot Management ==========

    @PostMapping({"/snapshot/{id}", "/connections/{id}/snapshot"})
    public ResponseEntity<String> createSnapshot(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            String name = body != null ? body.get("name") : null;
            com.memdiag.core.diff.Snapshot snapshot = analysisService.createSnapshot(id, name);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("id", snapshot.getId());
            data.addProperty("createdAt", snapshot.getTimestamp().toString());
            data.addProperty("name", snapshot.getId());
            data.addProperty("size", 0);
            result.add("data", data);

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error creating snapshot for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_SNAPSHOT_CREATE)));
        }
    }

    @GetMapping({"/snapshots/{id}", "/connections/{id}/snapshots"})
    public ResponseEntity<String> listSnapshots(@PathVariable String id) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            java.util.List<com.memdiag.core.diff.SnapshotManager.SnapshotInfo> snapshots = analysisService.listSnapshots(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            com.google.gson.JsonArray dataArray = new com.google.gson.JsonArray();
            for (com.memdiag.core.diff.SnapshotManager.SnapshotInfo info : snapshots) {
                JsonObject item = new JsonObject();
                item.addProperty("id", info.id != null ? info.id : info.filename);
                item.addProperty("name", info.filename);
                item.addProperty("createdAt", info.lastModified.toString());
                item.addProperty("size", info.size);
                dataArray.add(item);
            }
            result.add("data", dataArray);

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error listing snapshots for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_SNAPSHOT_LIST)));
        }
    }

    @DeleteMapping({"/snapshots/{id}/{snapshotId}", "/connections/{id}/snapshots/{snapshotId}"})
    public ResponseEntity<String> deleteSnapshot(
            @PathVariable String id,
            @PathVariable String snapshotId) {
        if (!isConnectionId(id) && !PidValidator.isValid(id)) {
            logger.warn("Invalid PID: {}", id);
            return validationError(PidValidator.getErrorMessage(id));
        }

        try {
            boolean deleted = analysisService.deleteSnapshot(id, snapshotId);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("deleted", deleted);
            result.add("data", data);

            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            logger.error("Error deleting snapshot for id: {}", id, e);
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(ERROR_SNAPSHOT_DELETE)));
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
