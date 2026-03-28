package com.memdiag.agent.instrument;

import com.memdiag.agent.AgentConfig;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.objectweb.asm.Opcodes.*;

/**
 * ClassFileTransformer for monitoring method invocations.
 * <p>
 * Uses ASM to instrument methods and record their execution times.
 */
public class MethodMonitorTransformer implements ClassFileTransformer {

    private final AgentConfig config;

    // Method statistics
    private final Map<String, MethodStats> methodStats = new ConcurrentHashMap<>();

    // Static reference for use by instrumented code
    private static volatile MethodMonitorTransformer instance;

    // Packages to include/exclude
    private final List<String> includePackages;
    private final List<String> excludePackages;

    /**
     * Creates a new MethodMonitorTransformer.
     *
     * @param config Agent configuration
     */
    public MethodMonitorTransformer(AgentConfig config) {
        this.config = config;
        this.includePackages = new ArrayList<>();
        this.excludePackages = new ArrayList<>();

        // Default exclusions
        excludePackages.add("java/");
        excludePackages.add("javax/");
        excludePackages.add("sun/");
        excludePackages.add("com/sun/");
        excludePackages.add("jdk/");
        excludePackages.add("org/objectweb/asm/");
        excludePackages.add("com/memdiag/");

        // Set the static instance
        instance = this;
    }

    /**
     * Get the singleton instance (for use by instrumented code).
     *
     * @return The MethodMonitorTransformer instance
     */
    public static MethodMonitorTransformer getInstance() {
        return instance;
    }

    /**
     * Add a package to include in instrumentation.
     *
     * @param pkg Package name in internal format (e.g., "com/example/")
     */
    public void addIncludePackage(String pkg) {
        includePackages.add(pkg);
    }

    /**
     * Add a package to exclude from instrumentation.
     *
     * @param pkg Package name in internal format (e.g., "java/")
     */
    public void addExcludePackage(String pkg) {
        excludePackages.add(pkg);
    }

    /**
     * Check if a class should be transformed.
     *
     * @param className Class name in internal format (with '/')
     * @return true if the class should be transformed
     */
    private boolean shouldTransform(String className) {
        if (className == null) {
            return false;
        }

        // Check exclusions first
        for (String exclude : excludePackages) {
            if (className.startsWith(exclude)) {
                return false;
            }
        }

        // If includes are specified, check them
        if (!includePackages.isEmpty()) {
            for (String include : includePackages) {
                if (className.startsWith(include)) {
                    return true;
                }
            }
            return false;
        }

        // By default, instrument all non-excluded classes
        return true;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (!shouldTransform(className)) return null;

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            cr.accept(new ClassVisitor(AsmUtils.getAsmApiVersion(Opcodes.V1_8), cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exc) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, sig, exc);
                    return new AdviceAdapter(AsmUtils.getAsmApiVersion(Opcodes.V1_8), mv, access, name, desc) {
                        private int startTimeId;

                        @Override
                        protected void onMethodEnter() {
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
                            startTimeId = newLocal(Type.LONG_TYPE);
                            mv.visitVarInsn(LSTORE, startTimeId);

                            mv.visitLdcInsn(className);
                            mv.visitLdcInsn(name);
                            mv.visitLdcInsn(desc);
                            mv.visitMethodInsn(INVOKESTATIC, "com/memdiag/agent/instrument/MemDiagSpy", "recordMethodEntry", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
                        }

                        @Override
                        protected void onMethodExit(int opcode) {
                            if (opcode != ATHROW) {
                                recordExit();
                            }
                        }

                        private void recordExit() {
                            mv.visitLdcInsn(className);
                            mv.visitLdcInsn(name);
                            mv.visitLdcInsn(desc);
                            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J", false);
                            mv.visitVarInsn(LLOAD, startTimeId);
                            mv.visitInsn(LSUB);
                            mv.visitMethodInsn(INVOKESTATIC, "com/memdiag/agent/instrument/MemDiagSpy", "recordMethodExit", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;J)V", false);
                        }
                    };
                }
            }, ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
        } catch (Exception e) {
            System.err.println("[MemDiag] Error transforming class for method monitoring " + className + ": " + e.getMessage());
            return null;
        }
    }

    // ========== The rest is the same as before (stats recording) ==========

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
        Map<String, Object> map = new HashMap<>();
        map.put("totalMethods", methodStats.size());
        map.put("topByTotalTime", convertStatsToMapList(getTopMethodsByTotalTime(limit)));
        map.put("topByCount", convertStatsToMapList(getTopMethodsByCount(limit)));
        return map;
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
            invocationCount.incrementAndGet();
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
