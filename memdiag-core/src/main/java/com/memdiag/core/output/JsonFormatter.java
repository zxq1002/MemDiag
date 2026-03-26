package com.memdiag.core.output;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Recommendation;
import com.memdiag.core.diagnose.Severity;
import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.StackFrame;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.thread.ThreadStats;

import java.time.Instant;
import java.util.List;

/**
 * JSON 报告格式化器
 */
public class JsonFormatter implements ReportFormatter {

    private static final int DEFAULT_LIMIT = 100;

    private final Gson gson;
    private final int limit;

    public JsonFormatter() {
        this(DEFAULT_LIMIT, true);
    }

    public JsonFormatter(int limit, boolean prettyPrint) {
        this.limit = limit;
        GsonBuilder builder = new GsonBuilder();
        if (prettyPrint) {
            builder.setPrettyPrinting();
        }
        this.gson = builder.create();
    }

    @Override
    public String format(HeapHistogram histogram, ThreadDump threadDump, DiagnosisResult diagnosis) {
        JsonObject root = new JsonObject();
        root.addProperty("generatedAt", Instant.now().toString());
        root.addProperty("format", "json");
        root.addProperty("version", "1.0");

        if (diagnosis != null) {
            root.add("diagnosis", toJson(diagnosis));
        }

        if (histogram != null) {
            root.add("heapHistogram", toJson(histogram));
        }

        if (threadDump != null) {
            root.add("threadDump", toJson(threadDump));
        }

        return gson.toJson(root);
    }

    private JsonObject toJson(DiagnosisResult diagnosis) {
        JsonObject obj = new JsonObject();
        obj.addProperty("timestamp", diagnosis.getTimestamp().toString());
        obj.addProperty("summary", diagnosis.getSummary());
        obj.addProperty("totalHeapUsed", diagnosis.getTotalHeapUsed());
        obj.addProperty("totalHeapCommitted", diagnosis.getTotalHeapCommitted());
        obj.addProperty("threadCount", diagnosis.getThreadCount());
        obj.addProperty("hasCriticalIssues", diagnosis.hasCriticalIssues());
        obj.addProperty("hasWarningIssues", diagnosis.hasWarningIssues());

        JsonArray issuesArray = new JsonArray();
        for (Issue issue : diagnosis.getIssues()) {
            issuesArray.add(toJson(issue));
        }
        obj.add("issues", issuesArray);

        return obj;
    }

    private JsonObject toJson(Issue issue) {
        JsonObject obj = new JsonObject();
        obj.addProperty("severity", issue.getSeverity().name());
        obj.addProperty("type", issue.getType());
        obj.addProperty("title", issue.getTitle());
        obj.addProperty("description", issue.getDescription());

        if (issue.getAffectedClassName() != null) {
            obj.addProperty("affectedClassName", issue.getAffectedClassName());
        }
        if (issue.getAffectedObjectCount() != null) {
            obj.addProperty("affectedObjectCount", issue.getAffectedObjectCount());
        }
        if (issue.getAffectedBytes() != null) {
            obj.addProperty("affectedBytes", issue.getAffectedBytes());
        }

        JsonArray recommendationsArray = new JsonArray();
        for (Recommendation rec : issue.getRecommendations()) {
            JsonObject recObj = new JsonObject();
            recObj.addProperty("priority", rec.getPriority());
            recObj.addProperty("title", rec.getTitle());
            if (rec.getDescription() != null) {
                recObj.addProperty("description", rec.getDescription());
            }
            if (rec.getAction() != null) {
                recObj.addProperty("action", rec.getAction());
            }
            recommendationsArray.add(recObj);
        }
        obj.add("recommendations", recommendationsArray);

        return obj;
    }

    private JsonObject toJson(HeapHistogram histogram) {
        JsonObject obj = new JsonObject();
        obj.addProperty("totalObjects", histogram.getTotalObjects());
        obj.addProperty("totalBytes", histogram.getTotalBytes());

        JsonArray classesArray = new JsonArray();
        List<ClassStats> topClasses = histogram.getTopByShallowBytes(limit);
        for (ClassStats stats : topClasses) {
            classesArray.add(toJson(stats));
        }
        obj.add("topClasses", classesArray);

        return obj;
    }

    private JsonObject toJson(ClassStats stats) {
        JsonObject obj = new JsonObject();
        obj.addProperty("className", stats.getClassName());
        obj.addProperty("objectCount", stats.getObjectCount());
        obj.addProperty("shallowBytes", stats.getShallowBytes());
        return obj;
    }

    private JsonObject toJson(ThreadDump threadDump) {
        JsonObject obj = new JsonObject();
        if (threadDump.getTimestamp() != null) {
            obj.addProperty("timestamp", threadDump.getTimestamp().toString());
        }
        obj.addProperty("threadCount", threadDump.getThreadCount());

        JsonArray threadsArray = new JsonArray();
        for (ThreadDump.ThreadInfo info : threadDump.getThreadInfos().values()) {
            threadsArray.add(toJson(info));
        }
        obj.add("threads", threadsArray);

        return obj;
    }

    private JsonObject toJson(ThreadDump.ThreadInfo info) {
        JsonObject obj = new JsonObject();
        obj.add("stats", toJson(info.getStats()));

        JsonArray stackArray = new JsonArray();
        for (StackFrame frame : info.getStackTrace()) {
            stackArray.add(toJson(frame));
        }
        obj.add("stackTrace", stackArray);

        JsonArray lockedMonitorsArray = new JsonArray();
        for (Long monitorId : info.getLockedMonitorIds()) {
            lockedMonitorsArray.add(monitorId);
        }
        obj.add("lockedMonitorIds", lockedMonitorsArray);

        if (info.getBlockedOnMonitorId() != null) {
            obj.addProperty("blockedOnMonitorId", info.getBlockedOnMonitorId());
        }
        if (info.getBlockedOnLockOwnerName() != null) {
            obj.addProperty("blockedOnLockOwnerName", info.getBlockedOnLockOwnerName());
        }
        if (info.getBlockedOnLockOwnerId() != null) {
            obj.addProperty("blockedOnLockOwnerId", info.getBlockedOnLockOwnerId());
        }

        return obj;
    }

    private JsonObject toJson(ThreadStats stats) {
        JsonObject obj = new JsonObject();
        obj.addProperty("threadId", stats.getThreadId());
        obj.addProperty("name", stats.getThreadName());
        if (stats.getState() != null) {
            obj.addProperty("state", stats.getState().name());
        }
        obj.addProperty("blockedCount", stats.getBlockedCount());
        obj.addProperty("waitedCount", stats.getWaitedCount());
        if (stats.getBlockedTime() >= 0) {
            obj.addProperty("blockedTime", stats.getBlockedTime());
        }
        if (stats.getWaitedTime() >= 0) {
            obj.addProperty("waitedTime", stats.getWaitedTime());
        }
        return obj;
    }

    private JsonObject toJson(StackFrame frame) {
        JsonObject obj = new JsonObject();
        obj.addProperty("className", frame.getClassName());
        obj.addProperty("methodName", frame.getMethodName());
        obj.addProperty("fileName", frame.getFileName());
        obj.addProperty("lineNumber", frame.getLineNumber());
        obj.addProperty("nativeMethod", frame.isNativeMethod());
        return obj;
    }

    @Override
    public String getFormatName() {
        return "json";
    }
}
