package com.memdiag.agent.instrument;

import com.memdiag.agent.AgentConfig;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ClassFileTransformer for monitoring method invocations.
 * <p>
 * Currently provides a framework for method monitoring.
 * Full ASM-based bytecode instrumentation will be added in a future phase.
 */
public class MethodMonitorTransformer implements ClassFileTransformer {

    private final AgentConfig config;

    // Method statistics
    private final Map<String, MethodStats> methodStats = new ConcurrentHashMap<>();

    /**
     * Creates a new MethodMonitorTransformer.
     *
     * @param config Agent configuration
     */
    public MethodMonitorTransformer(AgentConfig config) {
        this.config = config;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

        // Full ASM-based transformation will be added in a future phase
        // For now, just return null to use original bytecode
        return null;
    }

    /**
     * Record a method entry (called from instrumented code).
     *
     * @param className  Class name
     * @param methodName Method name
     * @param descriptor Method descriptor
     */
    public void recordMethodEntry(String className, String methodName, String descriptor) {
        String key = buildMethodKey(className, methodName, descriptor);
        methodStats.computeIfAbsent(key, k -> new MethodStats(className, methodName, descriptor))
                .recordEntry();
    }

    /**
     * Record a method exit (called from instrumented code).
     *
     * @param className     Class name
     * @param methodName    Method name
     * @param descriptor    Method descriptor
     * @param durationNanos Duration in nanoseconds
     */
    public void recordMethodExit(String className, String methodName, String descriptor, long durationNanos) {
        String key = buildMethodKey(className, methodName, descriptor);
        methodStats.computeIfAbsent(key, k -> new MethodStats(className, methodName, descriptor))
                .recordExit(durationNanos);
    }

    /**
     * Record an exception thrown from a method (called from instrumented code).
     *
     * @param className     Class name
     * @param methodName    Method name
     * @param descriptor    Method descriptor
     * @param exceptionType Type of exception thrown
     */
    public void recordException(String className, String methodName, String descriptor, String exceptionType) {
        String key = buildMethodKey(className, methodName, descriptor);
        methodStats.computeIfAbsent(key, k -> new MethodStats(className, methodName, descriptor))
                .recordException(exceptionType);
    }

    private String buildMethodKey(String className, String methodName, String descriptor) {
        return className + "#" + methodName + descriptor;
    }

    /**
     * Get statistics for a specific method.
     *
     * @param className  Class name
     * @param methodName Method name
     * @param descriptor Method descriptor
     * @return Method stats, or null if not found
     */
    public MethodStats getMethodStats(String className, String methodName, String descriptor) {
        return methodStats.get(buildMethodKey(className, methodName, descriptor));
    }

    /**
     * Get all method statistics.
     *
     * @return List of all method stats
     */
    public List<MethodStats> getAllMethodStats() {
        return new ArrayList<>(methodStats.values());
    }

    /**
     * Get top N methods by total time.
     *
     * @param limit Maximum number of methods to return
     * @return List of top methods
     */
    public List<MethodStats> getTopMethodsByTotalTime(int limit) {
        List<MethodStats> stats = new ArrayList<>(methodStats.values());
        stats.sort((a, b) -> Long.compare(b.getTotalTimeNanos(), a.getTotalTimeNanos()));
        return stats.subList(0, Math.min(limit, stats.size()));
    }

    /**
     * Get top N methods by invocation count.
     *
     * @param limit Maximum number of methods to return
     * @return List of top methods
     */
    public List<MethodStats> getTopMethodsByCount(int limit) {
        List<MethodStats> stats = new ArrayList<>(methodStats.values());
        stats.sort((a, b) -> Long.compare(b.getInvocationCount(), a.getInvocationCount()));
        return stats.subList(0, Math.min(limit, stats.size()));
    }

    /**
     * Clear all statistics.
     */
    public void clear() {
        methodStats.clear();
    }

    /**
     * Convert statistics to a map for JSON serialization.
     *
     * @param limit Maximum number of methods to include
     * @return Map of statistics
     */
    public Map<String, Object> toMap(int limit) {
        return Map.of(
            "totalMethods", methodStats.size(),
            "topByTotalTime", convertStatsToMapList(getTopMethodsByTotalTime(limit)),
            "topByCount", convertStatsToMapList(getTopMethodsByCount(limit))
        );
    }

    private List<Map<String, Object>> convertStatsToMapList(List<MethodStats> stats) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MethodStats stat : stats) {
            result.add(stat.toMap());
        }
        return result;
    }

    /**
     * Statistics for a single method.
     */
    public static class MethodStats {
        private final String className;
        private final String methodName;
        private final String descriptor;

        private final AtomicLong invocationCount = new AtomicLong(0);
        private final AtomicLong totalTimeNanos = new AtomicLong(0);
        private final AtomicLong maxTimeNanos = new AtomicLong(0);
        private final AtomicLong exceptionCount = new AtomicLong(0);

        public MethodStats(String className, String methodName, String descriptor) {
            this.className = className;
            this.methodName = methodName;
            this.descriptor = descriptor;
        }

        public void recordEntry() {
            invocationCount.incrementAndGet();
        }

        public void recordExit(long durationNanos) {
            totalTimeNanos.addAndGet(durationNanos);

            // Update max time
            long currentMax;
            do {
                currentMax = maxTimeNanos.get();
                if (durationNanos <= currentMax) {
                    break;
                }
            } while (!maxTimeNanos.compareAndSet(currentMax, durationNanos));
        }

        public void recordException(String exceptionType) {
            exceptionCount.incrementAndGet();
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getDescriptor() {
            return descriptor;
        }

        public long getInvocationCount() {
            return invocationCount.get();
        }

        public long getTotalTimeNanos() {
            return totalTimeNanos.get();
        }

        public long getMaxTimeNanos() {
            return maxTimeNanos.get();
        }

        public long getExceptionCount() {
            return exceptionCount.get();
        }

        public double getAverageTimeNanos() {
            long count = invocationCount.get();
            if (count == 0) {
                return 0;
            }
            return (double) totalTimeNanos.get() / count;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("className", className);
            map.put("methodName", methodName);
            map.put("descriptor", descriptor);
            map.put("invocationCount", invocationCount.get());
            map.put("totalTimeNanos", totalTimeNanos.get());
            map.put("totalTimeMs", totalTimeNanos.get() / 1_000_000.0);
            map.put("maxTimeNanos", maxTimeNanos.get());
            map.put("maxTimeMs", maxTimeNanos.get() / 1_000_000.0);
            map.put("averageTimeNanos", getAverageTimeNanos());
            map.put("averageTimeMs", getAverageTimeNanos() / 1_000_000.0);
            map.put("exceptionCount", exceptionCount.get());
            return map;
        }

        @Override
        public String toString() {
            return className + "#" + methodName +
                   " [count=" + invocationCount.get() +
                   ", total=" + (totalTimeNanos.get() / 1_000_000) + "ms" +
                   ", avg=" + String.format("%.2f", getAverageTimeNanos() / 1_000_000) + "ms" +
                   "]";
        }
    }
}
