package com.memdiag.core.output;

import com.memdiag.core.diagnose.DiagnosisResult;
import com.memdiag.core.diagnose.Issue;
import com.memdiag.core.diagnose.Recommendation;
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
 * HTML 报告格式化器
 */
public class HtmlFormatter implements ReportFormatter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault());

    private static final int DEFAULT_LIMIT = 50;

    private final int limit;

    public HtmlFormatter() {
        this(DEFAULT_LIMIT);
    }

    public HtmlFormatter(int limit) {
        this.limit = limit;
    }

    @Override
    public String format(HeapHistogram histogram, ThreadDump threadDump, DiagnosisResult diagnosis) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"zh-CN\">\n");
        sb.append("<head>\n");
        sb.append("    <meta charset=\"UTF-8\">\n");
        sb.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("    <title>MemDiag 内存诊断报告</title>\n");
        sb.append("    <style>\n");
        sb.append(getCss());
        sb.append("    </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");

        sb.append("    <div class=\"container\">\n");

        sb.append("        <header class=\"header\">\n");
        sb.append("            <h1>MemDiag 内存诊断报告</h1>\n");
        sb.append("            <p class=\"subtitle\">生成时间: ").append(DATE_FORMATTER.format(Instant.now())).append("</p>\n");
        sb.append("        </header>\n");

        if (diagnosis != null) {
            appendDiagnosisSummary(sb, diagnosis);
        }

        if (histogram != null) {
            appendHeapHistogram(sb, histogram);
        }

        if (threadDump != null) {
            appendThreadDump(sb, threadDump);
        }

        if (diagnosis != null && !diagnosis.getIssues().isEmpty()) {
            appendDiagnosisDetails(sb, diagnosis);
        }

        sb.append("        <footer class=\"footer\">\n");
        sb.append("            <p>MemDiag - JVM 内存诊断工具</p>\n");
        sb.append("        </footer>\n");

        sb.append("    </div>\n");

        sb.append("    <script>\n");
        sb.append(getJavaScript());
        sb.append("    </script>\n");

        sb.append("</body>\n");
        sb.append("</html>\n");

        return sb.toString();
    }

    private String getCss() {
        return "    * {\n" +
            "        margin: 0;\n" +
            "        padding: 0;\n" +
            "        box-sizing: border-box;\n" +
            "    }\n" +
            "\n" +
            "    body {\n" +
            "        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;\n" +
            "        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
            "        min-height: 100vh;\n" +
            "        padding: 20px;\n" +
            "    }\n" +
            "\n" +
            "    .container {\n" +
            "        max-width: 1200px;\n" +
            "        margin: 0 auto;\n" +
            "        background: white;\n" +
            "        border-radius: 12px;\n" +
            "        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);\n" +
            "        overflow: hidden;\n" +
            "    }\n" +
            "\n" +
            "    .header {\n" +
            "        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
            "        color: white;\n" +
            "        padding: 30px;\n" +
            "        text-align: center;\n" +
            "    }\n" +
            "\n" +
            "    .header h1 {\n" +
            "        font-size: 28px;\n" +
            "        margin-bottom: 8px;\n" +
            "    }\n" +
            "\n" +
            "    .subtitle {\n" +
            "        opacity: 0.9;\n" +
            "        font-size: 14px;\n" +
            "    }\n" +
            "\n" +
            "    .section {\n" +
            "        padding: 25px 30px;\n" +
            "        border-bottom: 1px solid #eee;\n" +
            "    }\n" +
            "\n" +
            "    .section:last-child {\n" +
            "        border-bottom: none;\n" +
            "    }\n" +
            "\n" +
            "    .section-title {\n" +
            "        font-size: 20px;\n" +
            "        font-weight: 600;\n" +
            "        color: #333;\n" +
            "        margin-bottom: 20px;\n" +
            "        display: flex;\n" +
            "        align-items: center;\n" +
            "        gap: 10px;\n" +
            "    }\n" +
            "\n" +
            "    .section-title::before {\n" +
            "        content: '';\n" +
            "        width: 4px;\n" +
            "        height: 24px;\n" +
            "        background: #667eea;\n" +
            "        border-radius: 2px;\n" +
            "    }\n" +
            "\n" +
            "    .summary-grid {\n" +
            "        display: grid;\n" +
            "        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n" +
            "        gap: 15px;\n" +
            "        margin-bottom: 20px;\n" +
            "    }\n" +
            "\n" +
            "    .summary-card {\n" +
            "        background: #f8f9fa;\n" +
            "        padding: 20px;\n" +
            "        border-radius: 8px;\n" +
            "        border-left: 4px solid #667eea;\n" +
            "    }\n" +
            "\n" +
            "    .summary-card.critical {\n" +
            "        border-left-color: #e74c3c;\n" +
            "        background: #fff5f5;\n" +
            "    }\n" +
            "\n" +
            "    .summary-card.warning {\n" +
            "        border-left-color: #f39c12;\n" +
            "        background: #fffbf0;\n" +
            "    }\n" +
            "\n" +
            "    .summary-card.info {\n" +
            "        border-left-color: #3498db;\n" +
            "        background: #f0f7ff;\n" +
            "    }\n" +
            "\n" +
            "    .summary-card.success {\n" +
            "        border-left-color: #27ae60;\n" +
            "        background: #f0fff4;\n" +
            "    }\n" +
            "\n" +
            "    .summary-label {\n" +
            "        font-size: 13px;\n" +
            "        color: #666;\n" +
            "        margin-bottom: 5px;\n" +
            "    }\n" +
            "\n" +
            "    .summary-value {\n" +
            "        font-size: 24px;\n" +
            "        font-weight: 700;\n" +
            "        color: #333;\n" +
            "    }\n" +
            "\n" +
            "    .summary-text {\n" +
            "        background: #f8f9fa;\n" +
            "        padding: 15px;\n" +
            "        border-radius: 8px;\n" +
            "        border-left: 4px solid #667eea;\n" +
            "        margin-bottom: 15px;\n" +
            "    }\n" +
            "\n" +
            "    table {\n" +
            "        width: 100%;\n" +
            "        border-collapse: collapse;\n" +
            "        margin-top: 15px;\n" +
            "    }\n" +
            "\n" +
            "    th, td {\n" +
            "        padding: 12px 15px;\n" +
            "        text-align: left;\n" +
            "        border-bottom: 1px solid #eee;\n" +
            "    }\n" +
            "\n" +
            "    th {\n" +
            "        background: #f8f9fa;\n" +
            "        font-weight: 600;\n" +
            "        color: #333;\n" +
            "        position: sticky;\n" +
            "        top: 0;\n" +
            "    }\n" +
            "\n" +
            "    tr:hover {\n" +
            "        background: #f8f9fa;\n" +
            "    }\n" +
            "\n" +
            "    .table-container {\n" +
            "        max-height: 400px;\n" +
            "        overflow-y: auto;\n" +
            "        border-radius: 8px;\n" +
            "        border: 1px solid #eee;\n" +
            "    }\n" +
            "\n" +
            "    .severity-badge {\n" +
            "        display: inline-block;\n" +
            "        padding: 4px 12px;\n" +
            "        border-radius: 20px;\n" +
            "        font-size: 12px;\n" +
            "        font-weight: 600;\n" +
            "    }\n" +
            "\n" +
            "    .severity-critical {\n" +
            "        background: #fee;\n" +
            "        color: #c0392b;\n" +
            "    }\n" +
            "\n" +
            "    .severity-warning {\n" +
            "        background: #fff3cd;\n" +
            "        color: #856404;\n" +
            "    }\n" +
            "\n" +
            "    .severity-info {\n" +
            "        background: #d1ecf1;\n" +
            "        color: #0c5460;\n" +
            "    }\n" +
            "\n" +
            "    .issue-card {\n" +
            "        background: #fff;\n" +
            "        border: 1px solid #eee;\n" +
            "        border-radius: 8px;\n" +
            "        padding: 20px;\n" +
            "        margin-bottom: 15px;\n" +
            "    }\n" +
            "\n" +
            "    .issue-header {\n" +
            "        display: flex;\n" +
            "        justify-content: space-between;\n" +
            "        align-items: center;\n" +
            "        margin-bottom: 12px;\n" +
            "    }\n" +
            "\n" +
            "    .issue-type {\n" +
            "        font-weight: 600;\n" +
            "        color: #333;\n" +
            "    }\n" +
            "\n" +
            "    .issue-description {\n" +
            "        color: #555;\n" +
            "        margin-bottom: 12px;\n" +
            "        line-height: 1.6;\n" +
            "    }\n" +
            "\n" +
            "    .recommendations {\n" +
            "        background: #f8f9fa;\n" +
            "        padding: 15px;\n" +
            "        border-radius: 6px;\n" +
            "    }\n" +
            "\n" +
            "    .recommendations h4 {\n" +
            "        font-size: 14px;\n" +
            "        color: #666;\n" +
            "        margin-bottom: 10px;\n" +
            "    }\n" +
            "\n" +
            "    .recommendations ol {\n" +
            "        margin-left: 20px;\n" +
            "    }\n" +
            "\n" +
            "    .recommendations li {\n" +
            "        color: #555;\n" +
            "        margin-bottom: 6px;\n" +
            "    }\n" +
            "\n" +
            "    .thread-stats {\n" +
            "        display: flex;\n" +
            "        gap: 15px;\n" +
            "        flex-wrap: wrap;\n" +
            "        margin-bottom: 20px;\n" +
            "    }\n" +
            "\n" +
            "    .thread-stat {\n" +
            "        background: #f8f9fa;\n" +
            "        padding: 10px 20px;\n" +
            "        border-radius: 6px;\n" +
            "        text-align: center;\n" +
            "    }\n" +
            "\n" +
            "    .thread-stat-value {\n" +
            "        font-size: 20px;\n" +
            "        font-weight: 700;\n" +
            "        color: #667eea;\n" +
            "    }\n" +
            "\n" +
            "    .thread-stat-label {\n" +
            "        font-size: 12px;\n" +
            "        color: #666;\n" +
            "    }\n" +
            "\n" +
            "    .thread-card {\n" +
            "        background: #fff;\n" +
            "        border: 1px solid #eee;\n" +
            "        border-radius: 8px;\n" +
            "        padding: 15px;\n" +
            "        margin-bottom: 10px;\n" +
            "    }\n" +
            "\n" +
            "    .thread-header {\n" +
            "        display: flex;\n" +
            "        justify-content: space-between;\n" +
            "        align-items: center;\n" +
            "        margin-bottom: 10px;\n" +
            "        cursor: pointer;\n" +
            "    }\n" +
            "\n" +
            "    .thread-name {\n" +
            "        font-weight: 600;\n" +
            "        color: #333;\n" +
            "    }\n" +
            "\n" +
            "    .thread-id {\n" +
            "        color: #999;\n" +
            "        font-size: 13px;\n" +
            "    }\n" +
            "\n" +
            "    .stack-trace {\n" +
            "        background: #2d2d2d;\n" +
            "        color: #f8f8f2;\n" +
            "        padding: 15px;\n" +
            "        border-radius: 6px;\n" +
            "        font-family: 'Monaco', 'Menlo', monospace;\n" +
            "        font-size: 13px;\n" +
            "        overflow-x: auto;\n" +
            "        display: none;\n" +
            "        margin-top: 10px;\n" +
            "    }\n" +
            "\n" +
            "    .stack-trace.visible {\n" +
            "        display: block;\n" +
            "    }\n" +
            "\n" +
            "    .stack-frame {\n" +
            "        padding: 3px 0;\n" +
            "    }\n" +
            "\n" +
            "    .stack-frame .class-name {\n" +
            "        color: #a6e22e;\n" +
            "    }\n" +
            "\n" +
            "    .stack-frame .method-name {\n" +
            "        color: #f92672;\n" +
            "    }\n" +
            "\n" +
            "    .stack-frame .file-name {\n" +
            "        color: #66d9ef;\n" +
            "    }\n" +
            "\n" +
            "    .toggle-btn {\n" +
            "        background: #667eea;\n" +
            "        color: white;\n" +
            "        border: none;\n" +
            "        padding: 6px 12px;\n" +
            "        border-radius: 4px;\n" +
            "        cursor: pointer;\n" +
            "        font-size: 12px;\n" +
            "    }\n" +
            "\n" +
            "    .toggle-btn:hover {\n" +
            "        background: #5a6fd6;\n" +
            "    }\n" +
            "\n" +
            "    .footer {\n" +
            "        background: #f8f9fa;\n" +
            "        padding: 20px;\n" +
            "        text-align: center;\n" +
            "        color: #999;\n" +
            "        font-size: 13px;\n" +
            "    }\n" +
            "\n" +
            "    .progress-bar {\n" +
            "        height: 8px;\n" +
            "        background: #eee;\n" +
            "        border-radius: 4px;\n" +
            "        overflow: hidden;\n" +
            "        margin-top: 5px;\n" +
            "    }\n" +
            "\n" +
            "    .progress-fill {\n" +
            "        height: 100%;\n" +
            "        background: linear-gradient(90deg, #667eea, #764ba2);\n" +
            "        border-radius: 4px;\n" +
            "        transition: width 0.3s ease;\n" +
            "    }";
    }

    private String getJavaScript() {
        return "    document.addEventListener('DOMContentLoaded', function() {\n" +
            "        // Toggle stack traces\n" +
            "        document.querySelectorAll('.thread-header').forEach(function(header) {\n" +
            "            header.addEventListener('click', function() {\n" +
            "                var stackTrace = this.nextElementSibling;\n" +
            "                if (stackTrace && stackTrace.classList.contains('stack-trace')) {\n" +
            "                    stackTrace.classList.toggle('visible');\n" +
            "                }\n" +
            "            });\n" +
            "        });\n" +
            "\n" +
            "        // Tab functionality\n" +
            "        document.querySelectorAll('.tab').forEach(function(tab) {\n" +
            "            tab.addEventListener('click', function() {\n" +
            "                var tabId = this.getAttribute('data-tab');\n" +
            "\n" +
            "                // Update tab styles\n" +
            "                document.querySelectorAll('.tab').forEach(function(t) {\n" +
            "                    t.classList.remove('active');\n" +
            "                });\n" +
            "                this.classList.add('active');\n" +
            "\n" +
            "                // Update content visibility\n" +
            "                document.querySelectorAll('.tab-content').forEach(function(content) {\n" +
            "                    content.classList.remove('active');\n" +
            "                });\n" +
            "                document.getElementById('tab-' + tabId).classList.add('active');\n" +
            "            });\n" +
            "        });\n" +
            "    });";
    }

    private void appendDiagnosisSummary(StringBuilder sb, DiagnosisResult diagnosis) {
        sb.append("        <section class=\"section\">\n");
        sb.append("            <h2 class=\"section-title\">诊断概要</h2>\n");

        if (diagnosis.getSummary() != null) {
            sb.append("            <div class=\"summary-text\">\n");
            sb.append("                ").append(escapeHtml(diagnosis.getSummary())).append("\n");
            sb.append("            </div>\n");
        }

        sb.append("            <div class=\"summary-grid\">\n");
        sb.append("                <div class=\"summary-card\">\n");
        sb.append("                    <div class=\"summary-label\">堆内存使用</div>\n");
        sb.append("                    <div class=\"summary-value\">").append(formatBytes(diagnosis.getTotalHeapUsed())).append("</div>\n");
        sb.append("                    <div class=\"progress-bar\">\n");
        double heapPercent = diagnosis.getTotalHeapCommitted() > 0
            ? (diagnosis.getTotalHeapUsed() * 100.0 / diagnosis.getTotalHeapCommitted())
            : 0;
        sb.append("                        <div class=\"progress-fill\" style=\"width: ").append(String.format("%.1f", heapPercent)).append("%\"></div>\n");
        sb.append("                    </div>\n");
        sb.append("                    <div style=\"font-size: 12px; color: #999; margin-top: 5px;\">");
        sb.append("已提交 ").append(formatBytes(diagnosis.getTotalHeapCommitted()));
        sb.append("</div>\n");
        sb.append("                </div>\n");

        sb.append("                <div class=\"summary-card info\">\n");
        sb.append("                    <div class=\"summary-label\">线程数量</div>\n");
        sb.append("                    <div class=\"summary-value\">").append(diagnosis.getThreadCount()).append("</div>\n");
        sb.append("                </div>\n");

        sb.append("                <div class=\"summary-card ").append(diagnosis.hasCriticalIssues() ? "critical" : "success").append("\">\n");
        sb.append("                    <div class=\"summary-label\">严重问题</div>\n");
        sb.append("                    <div class=\"summary-value\">").append(diagnosis.getCriticalIssues().size()).append("</div>\n");
        sb.append("                </div>\n");

        sb.append("                <div class=\"summary-card ").append(diagnosis.hasWarningIssues() ? "warning" : "success").append("\">\n");
        sb.append("                    <div class=\"summary-label\">警告问题</div>\n");
        sb.append("                    <div class=\"summary-value\">").append(diagnosis.getWarningIssues().size()).append("</div>\n");
        sb.append("                </div>\n");
        sb.append("            </div>\n");

        sb.append("        </section>\n");
    }

    private void appendHeapHistogram(StringBuilder sb, HeapHistogram histogram) {
        sb.append("        <section class=\"section\">\n");
        sb.append("            <h2 class=\"section-title\">堆直方图</h2>\n");

        sb.append("            <div class=\"summary-grid\">\n");
        sb.append("                <div class=\"summary-card\">\n");
        sb.append("                    <div class=\"summary-label\">总对象数</div>\n");
        sb.append("                    <div class=\"summary-value\">").append(String.format("%,d", histogram.getTotalObjects())).append("</div>\n");
        sb.append("                </div>\n");
        sb.append("                <div class=\"summary-card\">\n");
        sb.append("                    <div class=\"summary-label\">总大小</div>\n");
        sb.append("                    <div class=\"summary-value\">").append(formatBytes(histogram.getTotalBytes())).append("</div>\n");
        sb.append("                </div>\n");
        sb.append("            </div>\n");

        List<ClassStats> topClasses = histogram.getTopByShallowBytes(limit);
        long totalBytes = histogram.getTotalBytes();

        sb.append("            <div class=\"table-container\">\n");
        sb.append("            <table>\n");
        sb.append("                <thead>\n");
        sb.append("                    <tr>\n");
        sb.append("                        <th>#</th>\n");
        sb.append("                        <th>类名</th>\n");
        sb.append("                        <th style=\"text-align: right;\">对象数</th>\n");
        sb.append("                        <th style=\"text-align: right;\">大小</th>\n");
        sb.append("                        <th style=\"text-align: right;\">占比</th>\n");
        sb.append("                    </tr>\n");
        sb.append("                </thead>\n");
        sb.append("                <tbody>\n");

        int index = 1;
        for (ClassStats stats : topClasses) {
            double percentage = totalBytes > 0 ? (stats.getShallowBytes() * 100.0 / totalBytes) : 0;
            sb.append("                    <tr>\n");
            sb.append("                        <td>").append(index++).append("</td>\n");
            sb.append("                        <td>").append(escapeHtml(stats.getClassName())).append("</td>\n");
            sb.append("                        <td style=\"text-align: right;\">").append(String.format("%,d", stats.getObjectCount())).append("</td>\n");
            sb.append("                        <td style=\"text-align: right;\">").append(formatBytes(stats.getShallowBytes())).append("</td>\n");
            sb.append("                        <td style=\"text-align: right;\">").append(String.format("%.1f%%", percentage)).append("</td>\n");
            sb.append("                    </tr>\n");
        }

        sb.append("                </tbody>\n");
        sb.append("            </table>\n");
        sb.append("            </div>\n");

        sb.append("        </section>\n");
    }

    private void appendThreadDump(StringBuilder sb, ThreadDump threadDump) {
        sb.append("        <section class=\"section\">\n");
        sb.append("            <h2 class=\"section-title\">线程分析</h2>\n");

        sb.append("            <div class=\"thread-stats\">\n");
        for (ThreadState state : ThreadState.values()) {
            List<ThreadDump.ThreadInfo> threads = threadDump.getThreadsByState(state);
            if (!threads.isEmpty()) {
                sb.append("                <div class=\"thread-stat\">\n");
                sb.append("                    <div class=\"thread-stat-value\">").append(threads.size()).append("</div>\n");
                sb.append("                    <div class=\"thread-stat-label\">").append(state).append("</div>\n");
                sb.append("                </div>\n");
            }
        }
        sb.append("            </div>\n");

        List<ThreadDump.ThreadInfo> blockedThreads = threadDump.getThreadsByState(ThreadState.BLOCKED);
        List<ThreadDump.ThreadInfo> waitingThreads = threadDump.getThreadsByState(ThreadState.WAITING);

        if (!blockedThreads.isEmpty() || !waitingThreads.isEmpty()) {
            sb.append("            <h3 style=\"margin: 20px 0 10px 0; color: #333;\">值得关注的线程</h3>\n");

            int count = 0;
            for (ThreadDump.ThreadInfo info : blockedThreads) {
                if (count >= 10) break;
                appendThreadCard(sb, info, "BLOCKED");
                count++;
            }
            for (ThreadDump.ThreadInfo info : waitingThreads) {
                if (count >= 20) break;
                appendThreadCard(sb, info, "WAITING");
                count++;
            }
        }

        sb.append("        </section>\n");
    }

    private void appendThreadCard(StringBuilder sb, ThreadDump.ThreadInfo info, String label) {
        sb.append("            <div class=\"thread-card\">\n");
        sb.append("                <div class=\"thread-header\">\n");
        sb.append("                    <div>\n");
        sb.append("                        <span class=\"severity-badge severity-").append(label.toLowerCase()).append("\">").append(label).append("</span>\n");
        sb.append("                        <span class=\"thread-name\">").append(escapeHtml(info.getStats().getThreadName())).append("</span>\n");
        sb.append("                        <span class=\"thread-id\">ID: ").append(info.getStats().getThreadId()).append("</span>\n");
        sb.append("                    </div>\n");
        if (!info.getStackTrace().isEmpty()) {
            sb.append("                    <button class=\"toggle-btn\">堆栈</button>\n");
        }
        sb.append("                </div>\n");

        if (info.getBlockedOnLockOwnerName() != null) {
            sb.append("                <div style=\"color: #e74c3c; font-size: 13px; margin-bottom: 10px;\">\n");
            sb.append("                    等待锁: 被 ").append(escapeHtml(info.getBlockedOnLockOwnerName()));
            sb.append(" (ID: ").append(info.getBlockedOnLockOwnerId()).append(") 持有\n");
            sb.append("                </div>\n");
        }

        if (!info.getStackTrace().isEmpty()) {
            sb.append("                <div class=\"stack-trace\">\n");
            for (StackFrame frame : info.getStackTrace()) {
                sb.append("                    <div class=\"stack-frame\">\n");
                sb.append("                        at <span class=\"class-name\">").append(escapeHtml(frame.getClassName())).append("</span>");
                sb.append(".<span class=\"method-name\">").append(escapeHtml(frame.getMethodName())).append("</span>");
                sb.append("(<span class=\"file-name\">").append(escapeHtml(frame.getFileName() != null ? frame.getFileName() : "Unknown Source")).append("</span>");
                if (frame.getLineNumber() > 0) {
                    sb.append(":").append(frame.getLineNumber());
                }
                sb.append(")\n");
                sb.append("                    </div>\n");
            }
            sb.append("                </div>\n");
        }

        sb.append("            </div>\n");
    }

    private void appendDiagnosisDetails(StringBuilder sb, DiagnosisResult diagnosis) {
        sb.append("        <section class=\"section\">\n");
        sb.append("            <h2 class=\"section-title\">诊断详情</h2>\n");

        List<Issue> issues = diagnosis.getIssues().stream()
            .sorted(Comparator.comparing(Issue::getSeverity).reversed())
            .toList();

        for (Issue issue : issues) {
            sb.append("            <div class=\"issue-card\">\n");
            sb.append("                <div class=\"issue-header\">\n");
            sb.append("                    <div>\n");
            sb.append("                        <span class=\"severity-badge severity-").append(issue.getSeverity().name().toLowerCase()).append("\">");
            sb.append(getSeverityLabel(issue.getSeverity())).append("</span>\n");
            sb.append("                        <span class=\"issue-type\">").append(escapeHtml(issue.getTitle())).append("</span>\n");
            sb.append("                    </div>\n");
            sb.append("                </div>\n");
            sb.append("                <div class=\"issue-description\">").append(escapeHtml(issue.getDescription())).append("</div>\n");

            if (!issue.getRecommendations().isEmpty()) {
                sb.append("                <div class=\"recommendations\">\n");
                sb.append("                    <h4>建议</h4>\n");
                sb.append("                    <ol>\n");
                for (Recommendation rec : issue.getRecommendations()) {
                    sb.append("                        <li>").append(escapeHtml(rec.getDescription())).append("</li>\n");
                }
                sb.append("                    </ol>\n");
                sb.append("                </div>\n");
            }

            sb.append("            </div>\n");
        }

        sb.append("        </section>\n");
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

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#039;");
    }

    @Override
    public String getFormatName() {
        return "html";
    }
}
