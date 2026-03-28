package com.memdiag.agent.instrument;

import com.memdiag.agent.AgentConfig;
import com.memdiag.agent.collect.AllocationEvent;
import com.memdiag.agent.collect.DataCollector;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.objectweb.asm.Opcodes.*;

/**
 * ClassFileTransformer for tracking memory allocations.
 * <p>
 * Currently provides a framework for allocation tracking.
 * For now, allocation tracking works through manual recording via API.
 * Full ASM-based bytecode instrumentation for JDK classes will be added in a future phase.
 */
public class AllocationTransformer implements ClassFileTransformer {

    private final AgentConfig config;
    private final DataCollector dataCollector;

    // Counter for sampling
    private final AtomicLong allocationCounter = new AtomicLong(0);

    // Target classes for instrumentation (reserved for future use)
    private static final List<String> TARGET_CLASSES = List.of(
        "java/nio/ByteBuffer"
    );

    // Static reference for use by instrumented code
    private static volatile AllocationTransformer instance;

    /**
     * Creates a new AllocationTransformer.
     *
     * @param config        Agent configuration
     * @param dataCollector Data collector to record allocations
     */
    public AllocationTransformer(AgentConfig config, DataCollector dataCollector) {
        this.config = config;
        this.dataCollector = dataCollector;
        instance = this;
    }

    /**
     * Get the singleton instance.
     *
     * @return The AllocationTransformer instance
     */
    public static AllocationTransformer getInstance() {
        return instance;
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
        if (className == null) return false;

        // Exclude common system packages to improve performance and stability
        if (className.startsWith("java/") ||
            className.startsWith("javax/") ||
            className.startsWith("sun/") ||
            className.startsWith("jdk/") ||
            className.startsWith("com/sun/") ||
            className.startsWith("com/memdiag/agent/")) {
            return false;
        }

        return true;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

        if (!shouldTransform(className)) {
            return null;
        }

        try {
            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, AsmUtils.getClassWriterFlags(cr));
            AllocationClassVisitor cv = new AllocationClassVisitor(cw, className);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            return cw.toByteArray();
        } catch (Exception e) {
            System.err.println("[MemDiag] Error transforming class " + className + ": " + e.getMessage());
            return null;
        }
    }

    private class AllocationClassVisitor extends ClassVisitor {
        private final String className;
        public AllocationClassVisitor(ClassVisitor cv, String className) {
            super(AsmUtils.getAsmApiVersion(Opcodes.V1_8), cv);
            this.className = className;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new AllocationMethodVisitor(mv);
        }
    }

    private class AllocationMethodVisitor extends MethodVisitor {
        public AllocationMethodVisitor(MethodVisitor mv) {
            super(AsmUtils.getAsmApiVersion(Opcodes.V1_8), mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            if (opcode == INVOKESTATIC && owner.equals("java/nio/ByteBuffer") &&
                (name.equals("allocateDirect") || name.equals("allocate"))) {
                // After allocateDirect/allocat, stack: [ByteBuffer]
                boolean isDirect = name.equals("allocateDirect");
                mv.visitInsn(DUP);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/nio/ByteBuffer", "capacity", "()I", false);
                mv.visitInsn(I2L);
                mv.visitLdcInsn(isDirect ? "java.nio.DirectByteBuffer" : "java.nio.HeapByteBuffer");
                mv.visitMethodInsn(INVOKESTATIC, "com/memdiag/agent/instrument/MemDiagSpy", "recordAllocation", "(JLjava/lang/String;)V", false);
            }
        }
    }

    /**
     * Record an allocation event.
     * <p>
     * This method can be called manually via API or from instrumented code.
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
     * @param length         Length of the array
     * @param componentType  Component type name
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

    /**
     * Get the data collector.
     *
     * @return The data collector
     */
    public DataCollector getDataCollector() {
        return dataCollector;
    }
}
