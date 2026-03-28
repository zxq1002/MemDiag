package com.memdiag.agent.instrument;

import com.memdiag.agent.AgentConfig;
import com.memdiag.agent.collect.AllocationEvent;
import com.memdiag.agent.collect.DataCollector;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ClassFileTransformer for tracking memory allocations.
 * <p>
 * Currently provides a framework for allocation tracking.
 * Full ASM-based bytecode instrumentation will be added in a future phase.
 * <p>
 * For now, this class:
 * <ul>
 *   <li>Defines the target classes for instrumentation</li>
 *   <li>Provides a callback mechanism for recording allocations</li>
 *   <li>Serves as a placeholder for future bytecode transformation</li>
 * </ul>
 */
public class AllocationTransformer implements ClassFileTransformer {

    private final AgentConfig config;
    private final DataCollector dataCollector;

    // Counter for sampling
    private final AtomicLong allocationCounter = new AtomicLong(0);

    // Target classes for instrumentation
    private static final List<String> TARGET_CLASSES = List.of(
        "java/nio/ByteBuffer",
        "java/lang/Thread"
    );

    /**
     * Creates a new AllocationTransformer.
     *
     * @param config        Agent configuration
     * @param dataCollector Data collector to record allocations
     */
    public AllocationTransformer(AgentConfig config, DataCollector dataCollector) {
        this.config = config;
        this.dataCollector = dataCollector;
    }

    /**
     * Get the list of target class names (in internal format).
     *
     * @return List of target class names
     */
    public List<String> getTargetClasses() {
        return new ArrayList<>(TARGET_CLASSES);
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
        for (String target : TARGET_CLASSES) {
            if (className.equals(target) || className.startsWith(target + "$")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

        // For now, just check if we should transform this class
        // Full ASM-based transformation will be added in a future phase
        if (shouldTransform(className)) {
            System.out.println("[MemDiag] Would transform: " + className);
            // Return null to use original bytecode (no transformation yet)
        }

        // Return null to indicate no transformation
        return null;
    }

    /**
     * Record an allocation event (called from instrumented code).
     * <p>
     * This method is designed to be called from the instrumented bytecode.
     *
     * @param size     Size of the allocation in bytes
     * @param typeName Type name of the allocated object
     */
    public void recordAllocation(long size, String typeName) {
        long counter = allocationCounter.incrementAndGet();

        // Apply sampling
        if (!config.shouldSample(counter)) {
            return;
        }

        AllocationEvent event = new AllocationEvent(
            size,
            parseType(typeName),
            typeName,
            computeStackTraceHash(),
            Thread.currentThread().getId()
        );

        dataCollector.recordAllocation(event);
    }

    /**
     * Record a DirectByteBuffer allocation.
     *
     * @param capacity Capacity of the buffer in bytes
     */
    public void recordDirectByteBufferAllocation(int capacity) {
        recordAllocation(capacity, "java.nio.DirectByteBuffer");
    }

    /**
     * Record a heap ByteBuffer allocation.
     *
     * @param capacity Capacity of the buffer in bytes
     */
    public void recordHeapByteBufferAllocation(int capacity) {
        recordAllocation(capacity, "java.nio.HeapByteBuffer");
    }

    /**
     * Record a byte array allocation.
     *
     * @param length Length of the array
     */
    public void recordByteArrayAllocation(int length) {
        recordAllocation(length * 1L, "byte[]");
    }

    /**
     * Record an int array allocation.
     *
     * @param length Length of the array
     */
    public void recordIntArrayAllocation(int length) {
        recordAllocation(length * 4L, "int[]");
    }

    /**
     * Record a long array allocation.
     *
     * @param length Length of the array
     */
    public void recordLongArrayAllocation(int length) {
        recordAllocation(length * 8L, "long[]");
    }

    /**
     * Record an object array allocation.
     *
     * @param length     Length of the array
     * @param componentType Component type name
     */
    public void recordObjectArrayAllocation(int length, String componentType) {
        recordAllocation(length * 8L, componentType + "[]");
    }

    private AllocationEvent.AllocationType parseType(String typeName) {
        if (typeName == null) {
            return AllocationEvent.AllocationType.OTHER;
        }
        if (typeName.contains("byte[]")) {
            return AllocationEvent.AllocationType.BYTE_ARRAY;
        }
        if (typeName.contains("int[]")) {
            return AllocationEvent.AllocationType.INT_ARRAY;
        }
        if (typeName.contains("long[]")) {
            return AllocationEvent.AllocationType.LONG_ARRAY;
        }
        if (typeName.contains("DirectByteBuffer")) {
            return AllocationEvent.AllocationType.DIRECT_BYTE_BUFFER;
        }
        if (typeName.contains("ByteBuffer")) {
            return AllocationEvent.AllocationType.HEAP_BYTE_BUFFER;
        }
        if (typeName.contains("[]")) {
            return AllocationEvent.AllocationType.OBJECT_ARRAY;
        }
        return AllocationEvent.AllocationType.OTHER;
    }

    private int computeStackTraceHash() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        int hash = 0;
        // Skip first few elements (this method, recordAllocation, etc.)
        for (int i = 3; i < Math.min(stack.length, 10); i++) {
            hash = 31 * hash + stack[i].hashCode();
        }
        return hash;
    }

    /**
     * Get the allocation counter.
     *
     * @return Current allocation counter value
     */
    public long getAllocationCounter() {
        return allocationCounter.get();
    }

    /**
     * Reset the allocation counter.
     */
    public void resetCounter() {
        allocationCounter.set(0);
    }
}
