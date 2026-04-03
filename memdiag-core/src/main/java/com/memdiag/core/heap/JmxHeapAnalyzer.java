package com.memdiag.core.heap;

import com.memdiag.core.exception.AnalysisException;
import com.memdiag.core.util.JmxClient;
import com.memdiag.core.util.ResourceLimiter;

import javax.management.ObjectName;
import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JmxHeapAnalyzer implements HeapAnalyzer {
    private final JmxClient jmxClient;
    private final ResourceLimiter resourceLimiter;

    // Matches: "  1:          1000         64000  java.lang.String"
    // Groups: 1=rank, 2=instances, 3=bytes, 4=classname
    private static final Pattern LINE_PATTERN = Pattern.compile("^\\s*(\\d+):\\s+(\\d+)\\s+(\\d+)\\s+(.+)$");

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
            while ((line = reader.readLine()) != null) {
                Matcher matcher = LINE_PATTERN.matcher(line);
                if (matcher.find()) {
                    try {
                        long count = Long.parseLong(matcher.group(2));
                        long bytes = Long.parseLong(matcher.group(3));
                        String className = matcher.group(4).trim();

                        histogram.add(new ClassStats(className, count, bytes));
                    } catch (NumberFormatException ignored) {
                        // Skip invalid numeric values
                    }
                }
            }
        } catch (Exception e) {
            throw new AnalysisException("Failed to parse class histogram", e);
        }

        return histogram;
    }
}
