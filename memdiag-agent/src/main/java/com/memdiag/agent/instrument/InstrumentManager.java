package com.memdiag.agent.instrument;

import com.memdiag.agent.AgentConfig;
import com.memdiag.agent.collect.DataCollector;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Manages bytecode instrumentation for the MemDiag Agent.
 * <p>
 * Responsible for:
 * <ul>
 *   <li>Registering and unregistering ClassFileTransformers</li>
 *   <li>Tracking transformed classes</li>
 *   <li>Supporting runtime re-transformation</li>
 *   <li>Restoring original bytecode on detach</li>
 * </ul>
 */
public class InstrumentManager {

    private final Instrumentation instrumentation;
    private final AgentConfig config;
    private final DataCollector dataCollector;

    // Registered transformers
    private final List<ClassFileTransformer> transformers = new ArrayList<>();

    // Track transformed classes (for restoration)
    private final Set<Class<?>> transformedClasses = new CopyOnWriteArraySet<>();

    // Original bytecode storage (className -> byte[])
    private final Map<String, byte[]> originalBytecode = new ConcurrentHashMap<>();

    // Transformers
    private volatile AllocationTransformer allocationTransformer;
    private volatile MethodMonitorTransformer methodMonitorTransformer;

    // State
    private volatile boolean initialized = false;
    private volatile boolean allocationTrackingEnabled = false;
    private volatile boolean methodMonitoringEnabled = false;

    public InstrumentManager(Instrumentation instrumentation, AgentConfig config, DataCollector dataCollector) {
        this.instrumentation = instrumentation;
        this.config = config;
        this.dataCollector = dataCollector;
    }

    /**
     * Initialize the instrumentation manager.
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        System.out.println("[MemDiag] Initializing InstrumentManager...");

        // Check if instrumentation is supported
        if (!instrumentation.isRetransformClassesSupported()) {
            System.out.println("[MemDiag] Warning: RetransformClasses not supported by this JVM");
        }

        if (config.isInstrumentationEnabled()) {
            // Initialize allocation tracking transformer
            if (allocationTransformer == null) {
                allocationTransformer = new AllocationTransformer(config, dataCollector);
            }

            // Initialize method monitor transformer
            if (methodMonitorTransformer == null) {
                methodMonitorTransformer = new MethodMonitorTransformer(config);
            }

            System.out.println("[MemDiag] InstrumentManager initialized");
        } else {
            System.out.println("[MemDiag] Instrumentation disabled by configuration");
        }

        initialized = true;
    }

    /**
     * Enable allocation tracking.
     */
    public synchronized boolean enableAllocationTracking() {
        if (!initialized) {
            initialize();
        }

        if (allocationTrackingEnabled) {
            return true;
        }

        if (allocationTransformer == null) {
            System.err.println("[MemDiag] AllocationTransformer not initialized");
            return false;
        }

        try {
            System.out.println("[MemDiag] Enabling allocation tracking...");

            // Register the transformer
            instrumentation.addTransformer(allocationTransformer, true);
            transformers.add(allocationTransformer);

            // Retransform classes that are already loaded
            retransformTargetClasses(allocationTransformer.getTargetClasses());

            allocationTrackingEnabled = true;
            System.out.println("[MemDiag] Allocation tracking enabled");
            return true;
        } catch (Exception e) {
            System.err.println("[MemDiag] Failed to enable allocation tracking: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Disable allocation tracking.
     */
    public synchronized boolean disableAllocationTracking() {
        if (!allocationTrackingEnabled) {
            return true;
        }

        try {
            System.out.println("[MemDiag] Disabling allocation tracking...");

            if (allocationTransformer != null) {
                instrumentation.removeTransformer(allocationTransformer);
                transformers.remove(allocationTransformer);
            }

            // Restore original classes
            restoreTransformedClasses();

            allocationTrackingEnabled = false;
            System.out.println("[MemDiag] Allocation tracking disabled");
            return true;
        } catch (Exception e) {
            System.err.println("[MemDiag] Failed to disable allocation tracking: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Enable method monitoring.
     */
    public synchronized boolean enableMethodMonitoring() {
        if (!initialized) {
            initialize();
        }

        if (methodMonitoringEnabled) {
            return true;
        }

        if (methodMonitorTransformer == null) {
            System.err.println("[MemDiag] MethodMonitorTransformer not initialized");
            return false;
        }

        try {
            System.out.println("[MemDiag] Enabling method monitoring...");

            instrumentation.addTransformer(methodMonitorTransformer, true);
            transformers.add(methodMonitorTransformer);

            methodMonitoringEnabled = true;
            System.out.println("[MemDiag] Method monitoring enabled");
            return true;
        } catch (Exception e) {
            System.err.println("[MemDiag] Failed to enable method monitoring: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Disable method monitoring.
     */
    public synchronized boolean disableMethodMonitoring() {
        if (!methodMonitoringEnabled) {
            return true;
        }

        try {
            System.out.println("[MemDiag] Disabling method monitoring...");

            if (methodMonitorTransformer != null) {
                instrumentation.removeTransformer(methodMonitorTransformer);
                transformers.remove(methodMonitorTransformer);
            }

            restoreTransformedClasses();

            methodMonitoringEnabled = false;
            System.out.println("[MemDiag] Method monitoring disabled");
            return true;
        } catch (Exception e) {
            System.err.println("[MemDiag] Failed to disable method monitoring: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Retransform specific classes.
     */
    private void retransformTargetClasses(List<String> targetClassNames) {
        if (targetClassNames == null || targetClassNames.isEmpty()) {
            return;
        }

        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();
        List<Class<?>> classesToRetransform = new ArrayList<>();

        for (Class<?> clazz : allLoadedClasses) {
            String className = clazz.getName().replace('.', '/');
            for (String target : targetClassNames) {
                if (className.equals(target) || className.startsWith(target + "$")) {
                    if (instrumentation.isModifiableClass(clazz)) {
                        classesToRetransform.add(clazz);
                    }
                    break;
                }
            }
        }

        if (!classesToRetransform.isEmpty()) {
            try {
                System.out.println("[MemDiag] Retransforming " + classesToRetransform.size() + " classes...");
                instrumentation.retransformClasses(classesToRetransform.toArray(new Class<?>[0]));
                transformedClasses.addAll(classesToRetransform);
            } catch (UnmodifiableClassException e) {
                System.err.println("[MemDiag] Failed to retransform some classes: " + e.getMessage());
            }
        }
    }

    /**
     * Restore all transformed classes to their original state.
     */
    private void restoreTransformedClasses() {
        if (transformedClasses.isEmpty()) {
            return;
        }

        try {
            System.out.println("[MemDiag] Restoring " + transformedClasses.size() + " classes...");

            // Remove all transformers first
            for (ClassFileTransformer transformer : transformers) {
                instrumentation.removeTransformer(transformer);
            }
            transformers.clear();

            // Retransform to restore original bytecode
            if (!transformedClasses.isEmpty()) {
                instrumentation.retransformClasses(transformedClasses.toArray(new Class<?>[0]));
            }

            transformedClasses.clear();
            originalBytecode.clear();
        } catch (UnmodifiableClassException e) {
            System.err.println("[MemDiag] Failed to restore some classes: " + e.getMessage());
        }
    }

    /**
     * Shutdown the instrumentation manager and restore all classes.
     */
    public synchronized void shutdown() {
        System.out.println("[MemDiag] Shutting down InstrumentManager...");

        disableAllocationTracking();
        disableMethodMonitoring();

        initialized = false;
        System.out.println("[MemDiag] InstrumentManager shut down");
    }

    // ========== Getters ==========

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isAllocationTrackingEnabled() {
        return allocationTrackingEnabled;
    }

    public boolean isMethodMonitoringEnabled() {
        return methodMonitoringEnabled;
    }

    public AllocationTransformer getAllocationTransformer() {
        return allocationTransformer;
    }

    public MethodMonitorTransformer getMethodMonitorTransformer() {
        return methodMonitorTransformer;
    }

    /**
     * Convert to a map for JSON serialization.
     */
    public Map<String, Object> toMap() {
        return Map.of(
            "initialized", initialized,
            "allocationTrackingEnabled", allocationTrackingEnabled,
            "methodMonitoringEnabled", methodMonitoringEnabled,
            "transformedClassCount", transformedClasses.size(),
            "transformerCount", transformers.size()
        );
    }
}
