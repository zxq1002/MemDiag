package com.memdiag.core.heap;

import com.memdiag.core.exception.AnalysisException;
import com.memdiag.core.util.JmxClient;
import com.memdiag.core.util.ResourceLimiter;

import javax.management.ObjectName;
import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Duration;

public class JmxHeapAnalyzer implements HeapAnalyzer {
    private final JmxClient jmxClient;
    private final ResourceLimiter resourceLimiter;

    public JmxHeapAnalyzer(JmxClient jmxClient) {
        this(jmxClient, createDefaultResourceLimiter());
    }

    public JmxHeapAnalyzer(JmxClient jmxClient, ResourceLimiter resourceLimiter) {
        this.jmxClient = jmxClient;
        this.resourceLimiter = resourceLimiter;
    }

    private static ResourceLimiter createDefaultResourceLimiter() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        return new ResourceLimiter(
            maxMemory,
            Duration.ofMillis(500),
            Duration.ofMillis(500)
        );
    }

    @Override
    public HeapHistogram getHistogram(int limit) {
        HeapHistogram full = getFullHistogram();
        HeapHistogram limited = new HeapHistogram();
        full.getTopByShallowBytes(limit).forEach(limited::add);
        return limited;
    }

    @Override
    public HeapHistogram getFullHistogram() {
        return resourceLimiter.executeWithLimit(() -> {
            try {
                ObjectName diagnosticName = new ObjectName("com.sun.management:type=DiagnosticCommand");

                // 调用 gc.class_histogram 命令
                String result = resourceLimiter.executeWithSafePointMonitor(() -> {
                    try {
                        return (String) jmxClient.getConnection().invoke(
                            diagnosticName,
                            "gcClassHistogram",
                            new Object[]{null},
                            new String[]{String[].class.getName()}
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                return parseClassHistogram(result);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof Exception) {
                    throw new AnalysisException("Failed to get heap histogram via JMX", e.getCause());
                }
                throw new AnalysisException("Failed to get heap histogram via JMX", e);
            } catch (Exception e) {
                throw new AnalysisException("Failed to get heap histogram via JMX", e);
            }
        });
    }

    private HeapHistogram parseClassHistogram(String output) {
        HeapHistogram histogram = new HeapHistogram();

        try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
            String line;
            boolean inDataSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 跳过表头和分隔线
                if (line.startsWith("num")) {
                    inDataSection = true;
                    continue;
                }
                if (line.startsWith("-") || line.startsWith("Total")) {
                    continue;
                }
                if (!inDataSection) {
                    continue;
                }
                if (line.isEmpty()) {
                    continue;
                }

                // 解析行格式: "  1:          1000         64000  java.lang.String"
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    try {
                        int idx = Integer.parseInt(parts[0].replace(":", ""));
                        long count = Long.parseLong(parts[1]);
                        long bytes = Long.parseLong(parts[2]);

                        // 类名可能包含空格（内部类等），从第3个索引开始拼接
                        StringBuilder className = new StringBuilder(parts[3]);
                        for (int i = 4; i < parts.length; i++) {
                            className.append(" ").append(parts[i]);
                        }

                        histogram.add(new ClassStats(className.toString(), count, bytes));
                    } catch (NumberFormatException ignored) {
                        // 跳过无法解析的行
                    }
                }
            }
        } catch (Exception e) {
            throw new AnalysisException("Failed to parse class histogram", e);
        }

        return histogram;
    }

}
