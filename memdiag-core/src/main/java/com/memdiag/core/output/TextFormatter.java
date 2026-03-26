package com.memdiag.core.output;

import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Severity;
import com.memdiag.core.heap.ClassStats;
import com.memdiag.core.heap.HeapHistogram;
import com.memdiag.core.thread.StackFrame;
import com.memdiag.core.thread.ThreadDump;
import com.memdiag.core.thread.ThreadState;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * 文本报告格式化器
 */
public class TextFormatter implements ReportFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private static final int DEFAULT_LIMIT = 20;

    private final int limit;

    public TextFormatter() {
        this(DEFAULT_LIMIT);
    }

    public TextFormatter(int limit) {
        this.limit = limit;
    }

    @Override
    public String format(HeapHistogram histogram, ThreadDump threadDump, DiagnosisResult diagnosis) {
        StringBuilder sb = new StringBuilder();

        sb.append("========================================\n");
        sb.append("     MemDiag 内存诊断报告\n");
        sb.append("========================================\n");
        sb.append("\n");
        sb.append("生成时间: ").append(DATE_FORMATTER.format(Instant.now())).append("\n");
        sb.append("\n");

        if (diagnosis != null) {
            appendDiagnosisSummary(sb, diagnosis);
            sb.append("\n");
        }

        if (histogram != null) {
            appendHeapHistogram(sb, histogram);
            sb.append("\n");
        }

        if (threadDump != null) {
            appendThreadDump(sb, threadDump);
            sb.append("\n");
        }

        if (diagnosis != null && !diagnosis.getIssues().isEmpty()) {
            appendDiagnosisDetails(sb, diagnosis);
            sb.append("\n");
        }

        sb.append("========================================\n");
        sb.append("            报告结束\n");
        sb.append("========================================\n");

        return sb.toString();
    }

    private void appendDiagnosisSummary(StringBuilder sb, DiagnosisResult diagnosis) {
        sb.append("【诊断概要】\n");
        sb.append("----------------------------------------\n");

        if (diagnosis.getSummary() != null) {
            sb.append(diagnosis.getSummary()).append("\n");
            sb.append("\n");
        }

        sb.append(String.format("堆内存使用: %s / %s%n",
            formatBytes(diagnosis.getTotalHeapUsed()),
            formatBytes(diagnosis.getTotalHeapCommitted())));
        sb.append("线程数量: ").append(diagnosis.getThreadCount()).append("\n");
        sb.append("发现问题: ").append(diagnosis.getIssues().size()).append(" 个\n");
        sb.append("  - 严重: ").append(diagnosis.getCriticalIssues().size()).append("\n");
        sb.append("  - 警告: ").append(diagnosis.getWarningIssues().size()).append("\n");
        sb.append("  - 信息: ").append(diagnosis.getInfoIssues().size()).append("\n");
    }

    private void appendHeapHistogram(StringBuilder sb, HeapHistogram histogram) {
        sb.append("【堆直方图】\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("总对象数: %,d%n", histogram.getTotalObjects()));
        sb.append(String.format("总大小: %s%n", formatBytes(histogram.getTotalBytes())));
        sb.append("\n");

        List<ClassStats> topClasses = histogram.getTopByShallowBytes(limit);
        sb.append(String.format("Top %d 类（按大小排序）:%n", Math.min(limit, topClasses.size())));
        sb.append("\n");

        String header = String.format("    %-40s %15s %15s %12s",
            "类名", "对象数", "大小", "占比");
        sb.append(header).append("\n");
        sb.append("-".repeat(header.length())).append("\n");

        long totalBytes = histogram.getTotalBytes();
        int index = 1;
        for (ClassStats stats : topClasses) {
            double percentage = totalBytes > 0 ? (stats.getShallowBytes() * 100.0 / totalBytes) : 0;
            String className = stats.getClassName();
            if (className.length() > 40) {
                className = "..." + className.substring(className.length() - 37);
            }
            sb.append(String.format("%3d. %-40s %,15d %15s %10.1f%%%n",
                index++,
                className,
                stats.getObjectCount(),
                formatBytes(stats.getShallowBytes()),
                percentage));
        }
    }

    private void appendThreadDump(StringBuilder sb, ThreadDump threadDump) {
        sb.append("【线程分析】\n");
        sb.append("----------------------------------------\n");
        sb.append("总线程数: ").append(threadDump.getThreadCount()).append("\n");
        sb.append("\n");

        for (ThreadState state : ThreadState.values()) {
            List<ThreadDump.ThreadInfo> threads = threadDump.getThreadsByState(state);
            if (!threads.isEmpty()) {
                sb.append(String.format("%s: %d 个%n", state, threads.size()));
            }
        }
        sb.append("\n");

        List<ThreadDump.ThreadInfo> blockedThreads = threadDump.getThreadsByState(ThreadState.BLOCKED);
        List<ThreadDump.ThreadInfo> waitingThreads = threadDump.getThreadsByState(ThreadState.WAITING);
        List<ThreadDump.ThreadInfo> timedWaitingThreads = threadDump.getThreadsByState(ThreadState.TIMED_WAITING);

        if (!blockedThreads.isEmpty() || !waitingThreads.isEmpty() || !timedWaitingThreads.isEmpty()) {
            sb.append("值得关注的线程:\n");
            sb.append("\n");

            int count = 0;
            for (ThreadDump.ThreadInfo info : blockedThreads) {
                if (count >= 5) break;
                appendThreadInfo(sb, info, "BLOCKED");
                count++;
            }
            for (ThreadDump.ThreadInfo info : waitingThreads) {
                if (count >= 10) break;
                appendThreadInfo(sb, info, "WAITING");
                count++;
            }
        }
    }

    private void appendThreadInfo(StringBuilder sb, ThreadDump.ThreadInfo info, String label) {
        sb.append(String.format("  [%s] %s (ID: %d)%n",
            label,
            info.getStats().getThreadName(),
            info.getStats().getThreadId()));

        if (info.getBlockedOnLockOwnerName() != null) {
            sb.append(String.format("    等待锁: 被 %s (ID: %d) 持有%n",
                info.getBlockedOnLockOwnerName(),
                info.getBlockedOnLockOwnerId()));
        }

        List<StackFrame> stackTrace = info.getStackTrace();
        if (!stackTrace.isEmpty()) {
            sb.append("    堆栈:\n");
            int frameCount = 0;
            for (StackFrame frame : stackTrace) {
                if (frameCount >= 5) {
                    sb.append("    ...\n");
                    break;
                }
                sb.append(String.format("      at %s.%s(%s:%d)%n",
                    frame.getClassName(),
                    frame.getMethodName(),
                    frame.getFileName() != null ? frame.getFileName() : "Unknown Source",
                    frame.getLineNumber()));
                frameCount++;
            }
        }
        sb.append("\n");
    }

    private void appendDiagnosisDetails(StringBuilder sb, DiagnosisResult diagnosis) {
        sb.append("【诊断详情】\n");
        sb.append("----------------------------------------\n");
        sb.append("\n");

        List<Issue> issues = diagnosis.getIssues().stream()
            .sorted(Comparator.comparing(Issue::getSeverity).reversed())
            .toList();

        for (Issue issue : issues) {
            String severityLabel = getSeverityLabel(issue.getSeverity());
            sb.append(String.format("【%s】%s%n", severityLabel, issue.getTitle()));
            sb.append("  ").append(issue.getDescription()).append("\n");

            if (!issue.getRecommendations().isEmpty()) {
                sb.append("  建议:\n");
                for (int i = 0; i < issue.getRecommendations().size(); i++) {
                    sb.append(String.format("    %d. %s%n",
                        i + 1,
                        issue.getRecommendations().get(i).getDescription()));
                }
            }
            sb.append("\n");
        }
    }

    private String getSeverityLabel(Severity severity) {
        switch (severity) {
            case CRITICAL: return "严重";
            case WARNING: return "警告";
            case INFO: return "信息";
            default: return severity.name();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    @Override
    public String getFormatName() {
        return "text";
    }
}
