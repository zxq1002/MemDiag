# MemDiag Agent Optimization & Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the MemDiag feature loop by fixing infrastructure gaps, implementing CLI commands for visibility, and establishing a real JNI bridge for JVMTI events.

**Architecture:** 
1. **Scheduler**: Add a background thread to AgentContext for periodic stat snapshots.
2. **Filtering**: Fix ByteBuffer exclusion and add user-defined package inclusion.
3. **CLI**: Add `allocations` and `methods` commands to `memdiag-cli`.
4. **JNI**: Implement native methods in C++ and Java to relay JVMTI allocation events.

**Tech Stack:** Java, ASM, JNI, C++, PicoCLI (for CLI).

---

### Task 1: Infrastructure - Scheduler & Filtering

**Files:**
- Modify: `memdiag-agent/src/main/java/com/memdiag/agent/AgentContext.java`
- Modify: `memdiag-agent/src/main/java/com/memdiag/agent/MemDiagAgent.java`
- Modify: `memdiag-agent/src/main/java/com/memdiag/agent/AgentConfig.java`
- Modify: `memdiag-agent/src/main/java/com/memdiag/agent/instrument/AllocationTransformer.java`
- Modify: `memdiag-agent/src/main/java/com/memdiag/agent/instrument/MethodMonitorTransformer.java`

- [ ] **Step 1: Add Scheduler to AgentContext**
Add a `ScheduledExecutorService` to handle periodic tasks like statistics aggregation.

- [ ] **Step 2: Start Snapshot Task in MemDiagAgent**
Initialize the scheduler and start the `takeSnapshot` task.

- [ ] **Step 3: Support `includePackages` in AgentConfig**
Add parsing for `includePackages` and a helper method to check inclusion.

- [ ] **Step 4: Fix AllocationTransformer shouldTransform**
Explicitly allow `java/nio/ByteBuffer` even if system packages are excluded.

- [ ] **Step 5: Apply includePackages in MethodMonitorTransformer**
Update `shouldTransform` to honor the include list if provided.

- [ ] **Step 6: Commit**

---

### Task 2: CLI - Allocations & Methods Commands

**Files:**
- Create: `memdiag-cli/src/main/java/com/memdiag/cli/commands/AllocationsCommand.java`
- Create: `memdiag-cli/src/main/java/com/memdiag/cli/commands/MethodsCommand.java`
- Modify: `memdiag-cli/src/main/java/com/memdiag/cli/MemDiagCli.java`

- [ ] **Step 1: Implement AllocationsCommand**
Create a command to show allocation stats and trends.

- [ ] **Step 2: Implement MethodsCommand**
Create a command to show method monitoring stats.

- [ ] **Step 3: Register commands in MemDiagCli**
Add the new commands to the PicoCLI entry point.

- [ ] **Step 4: Commit**

---

### Task 3: JVMTI - JNI Bridge Implementation

**Files:**
- Modify: `memdiag-agent/src/main/java/com/memdiag/agent/jvmti/AgentJVMTILoader.java`
- Create: `memdiag-agent/src/main/java/com/memdiag/agent/jvmti/JVMTIEventBridge.java`
- Modify: `memdiag-native/src/main/c/jvmti/agent.cpp`

- [ ] **Step 1: Create JVMTIEventBridge Java class**
Add native method declarations.

- [ ] **Step 2: Implement JNI Callback in agent.cpp**
Implement the C++ side of the bridge.

- [ ] **Step 3: Initialize bridge in AgentJVMTILoader**
Call `registerCallbacks` after the library is loaded.

- [ ] **Step 4: Commit**
