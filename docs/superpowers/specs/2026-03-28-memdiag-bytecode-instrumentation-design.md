# MemDiag Bytecode Instrumentation Design Spec

## 1. Introduction
This document outlines the design for the core bytecode instrumentation functionality of the MemDiag Agent. The goal is to provide efficient, low-overhead monitoring of memory allocations and method execution times using ASM.

## 2. Architecture: Bootstrap Bridge (MemDiagSpy)

### 2.1 The Problem
JDK core classes (e.g., `java.nio.ByteBuffer`) are loaded by the Bootstrap ClassLoader, which cannot see classes loaded by the System ClassLoader (where the Agent resides).

### 2.2 The Solution
We introduce a "bridge" class `com.memdiag.agent.instrument.MemDiagSpy` that will be injected into the Bootstrap ClassLoader search path.

- **Injection**: Use `Instrumentation.appendToBootstrapClassLoaderSearch(agentJar)` during agent initialization.
- **Entry Points**: `MemDiagSpy` provides static methods for instrumented code to call.
- **Anti-Recursion**: Uses a `ThreadLocal<Boolean>` to prevent infinite recursion during data collection.

```java
public class MemDiagSpy {
    private static final ThreadLocal<Boolean> IN_PROGRESS = ThreadLocal.withInitial(() -> false);
    private static volatile AllocationTransformer allocationTransformer;

    public static void init(AllocationTransformer transformer) {
        allocationTransformer = transformer;
    }

    public static void recordAllocation(long size, String type) {
        if (IN_PROGRESS.get()) return;
        IN_PROGRESS.set(true);
        try {
            if (allocationTransformer != null) {
                allocationTransformer.recordAllocation(size, type);
            }
        } finally {
            IN_PROGRESS.set(false);
        }
    }
}
```

## 3. Allocation Instrumentation

### 3.1 Array Allocations (Heap)
- **Target Instructions**: `NEWARRAY`, `ANEWARRAY`, `MULTIANEWARRAY`.
- **Instrumentation Point**: Immediately after the instruction (when the array reference is on top of the stack).
- **Logic**: 
    1. Duplicate the array reference (`DUP`).
    2. Get array length (`ARRAYLENGTH`).
    3. Call `MemDiagSpy.recordAllocation(length * elementSize, type)`.
- **Sampling**: Applied within `MemDiagSpy` or `AllocationTransformer` (default 1/100).

### 3.2 Direct Memory Allocations (Off-heap)
- **Target Method**: `java.nio.ByteBuffer.allocateDirect(int capacity)`.
- **Instrumentation Point**: In caller classes, intercepting calls to `allocateDirect`.
- **Logic**:
    1. Intercept `INVOKESTATIC java/nio/ByteBuffer.allocateDirect(I)Ljava/nio/ByteBuffer;`.
    2. Call `MemDiagSpy.recordDirectByteBufferAllocation(capacity)` after the return.
- **Tracking**: 100% tracking (no sampling) for direct memory.

## 4. Method Monitoring Instrumentation

### 4.1 Scope
- **Inclusions**: User-defined packages (e.g., `com.example.*`).
- **Exclusions**: `java.*`, `javax.*`, `sun.*`, `com.memdiag.*`, etc.

### 4.2 Logic
- **Entry**: `long startTime = System.nanoTime();` injected at the start of the method.
- **Exit**: `MemDiagSpy.recordMethodExit(className, methodName, System.nanoTime() - startTime);` injected before every `RETURN` instruction and within the `ATHROW` / Exception handler.

## 5. Performance and Sampling

- **Sampling Strategy**: Atomic counter-based sampling for heap allocations.
- **Stack Trace Optimization**: 
    - Only capture stack traces when a sample is selected.
    - Limit depth to 10 frames.
    - Cache common stack trace hashes to reduce object allocation.
- **RingBuffer**: All events are published to a lock-free `RingBuffer` (DataCollector) for asynchronous processing.

## 6. Safety and Error Handling

- **Class Version Compatibility**: Use `AsmUtils` to select the correct ASM API version.
- **Verification**: Ensure instrumented bytecode passes JVM verification (handled by ASM `COMPUTE_FRAMES`).
- **Fail-safe**: If instrumentation fails for a class, the Agent will catch the exception and return the original bytecode (no-op).
