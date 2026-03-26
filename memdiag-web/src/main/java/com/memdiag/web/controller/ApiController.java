package com.memdiag.web.controller;

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

    @GetMapping("/connections")
    public ResponseEntity<Map<String, String>> getConnections() {
        return ResponseEntity.ok(analysisService.getConnections());
    }

    @PostMapping("/connections/{id}")
    public ResponseEntity<Map<String, String>> connect(
            @PathVariable String id,
            @RequestParam(required = false) String pid) {
        boolean success;
        if (pid != null && !pid.isEmpty()) {
            success = analysisService.connect(id, pid);
        } else {
            success = analysisService.connect(id, null);
        }
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
}
