# MemDiag Agent Optimization Plan

## 1. Introduction
This document outlines the optimization plan to fix infrastructure gaps, provide CLI visibility, and implement substantive JVMTI integration.

## 2. Infrastructure Fixes

### 2.1 StatsAggregator Scheduling
Currently, `StatsAggregator.takeSnapshot()` is only called on-demand during API requests. This prevents accurate trend analysis.
- **Change**: Introduce a `ScheduledExecutorService` in `AgentContext` or `DataCollector`.
- **Implementation**: Schedule a task to run every 1000ms that calls `statsAggregator.takeSnapshot()`.

### 2.2 AllocationTransformer Logic Correction
Fixed the contradiction between exclusion list and target classes.
- **Change**: Modify `shouldTransform` to explicitly allow `java/nio/ByteBuffer`.
- **Change**: Ensure `InstrumentManager` retransforms `java/nio/ByteBuffer` upon activation.

## 3. CLI Visibility (Closing the Loop)

### 3.1 New CLI Commands
Implement new commands in `memdiag-cli` to consume the v1 API endpoints.
- **`memdiag allocations`**:
    - Show real-time allocation rate (bytes/sec).
    - Show top 10 allocated types by size and count.
    - Show allocation trend (Increasing/Decreasing/Stable).
- **`memdiag methods`**:
    - Show top 20 methods by total execution time.
    - Show invocation counts and average duration.
- **`memdiag agent` enhancement**:
    - Add `status` sub-command to show Agent state, uptime, and instrumentation status.
    - Add `enable/disable` sub-commands for allocation tracking and method monitoring.

## 4. JVMTI Integration (Bridging the Gap)

### 4.1 JNI Event Bridge
Complete the "hollow" JVMTI implementation.
- **Java Side**: Implement `com.memdiag.agent.jvmti.JVMTIEventBridge` with native methods.
- **Native Side**:
    - Implement `onSampledObjectAlloc` in `agent.cpp`.
    - Use JNI to call back into `JVMTIEventBridge.recordNativeAllocation`.
- **Linking**: Ensure `AgentJVMTILoader.initializeJVMTI()` correctly sets up these callbacks.

## 5. Method Monitoring Enhancements

### 5.1 Package Filtering
- **Change**: Support `includePackages` configuration via Agent arguments.
- **Usage**: `-javaagent:agent.jar=includePackages=com.example.app`.
- **Impact**: Only instrument methods within specified packages, drastically reducing overhead in large applications.

## 6. Execution Phases

### Phase 1: Infrastructure & Correctness
- Fix `AllocationTransformer` filtering.
- Implement `StatsAggregator` scheduler.
- Support `includePackages` config.

### Phase 2: CLI Commands
- Implement `AllocationsCommand`.
- Implement `MethodsCommand`.
- Update `AgentCommand`.

### Phase 3: JVMTI JNI Bridge
- Implement native callbacks and JNI bridge.
