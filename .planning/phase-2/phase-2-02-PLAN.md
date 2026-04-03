---
phase: phase-2-tech-debt
plan: 02
type: execute
wave: 2
depends_on: ["phase-2-01"]
files_modified:
  - memdiag-web/src/main/java/com/memdiag/web/service/ConnectionManager.java
  - memdiag-web/src/main/java/com/memdiag/web/service/JmxAnalysisService.java
  - memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
  - memdiag-web/src/test/java/com/memdiag/web/service/ConnectionManagerTest.java
autonomous: true
requirements: [R-DEBT-002, R-TEST-001]
user_setup: []

must_haves:
  truths:
    - "Connection lifecycle is managed by ConnectionManager"
    - "JMX analysis logic is isolated in JmxAnalysisService"
    - "AnalysisService delegates core tasks to specialized services"
  artifacts:
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/ConnectionManager.java"
      provides: "Thread-safe connection storage and lifecycle management"
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/JmxAnalysisService.java"
      provides: "JMX-specific heap, thread, and NMT analysis"
  key_links:
    - from: "AnalysisService.java"
      to: "ConnectionManager"
      via: "Constructor injection"
    - from: "AnalysisService.java"
      to: "JmxAnalysisService"
      via: "Constructor injection"
---

<objective>
Extract connection management and JMX analysis from the monolithic AnalysisService.

Purpose: Reduce AnalysisService size and improve maintainability by following the Single Responsibility Principle.
Output: Two new focused services and a partially refactored AnalysisService.
</objective>

<execution_context>
@$HOME/.gemini/get-shit-done/workflows/execute-plan.md
@$HOME/.gemini/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phase-2/RESEARCH.md
@memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
</context>

<tasks>

<task type="auto">
  <name>Task 1: Create ConnectionManager Service</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/service/ConnectionManager.java
  </files>
  <action>
    Extract connection maps and lifecycle methods from `AnalysisService` to a new `@Service` class `ConnectionManager`.
    - Maps: `jmxConnections`, `agentConnections`, `heapAnalyzers`, `diagnosisEngines`, `connectionTypes`, `snapshotManagers`.
    - Methods: `connect`, `disconnect`, `getConnectionType`, `getJmxClient`, `getAgentClient`, `getConnections`, `getSnapshotManager`.
    Ensure thread safety using `ConcurrentHashMap`.
    Per R-DEBT-001, use constructor injection for `MemDiagProperties`.
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>Connection management is isolated and thread-safe.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Test ConnectionManager</name>
  <files>
    memdiag-web/src/test/java/com/memdiag/web/service/ConnectionManagerTest.java
  </files>
  <behavior>
    - Can register and retrieve a JMX connection.
    - Can register and retrieve an Agent connection.
    - Can disconnect a connection.
    - Returns correct connection type for a given ID.
  </behavior>
  <action>
    Create a unit test for `ConnectionManager` using JUnit 5 and Mockito.
    Mock `JmxClient` and `AgentClient` as needed.
  </action>
  <verify>
    <automated>mvn test -Dtest=ConnectionManagerTest -pl memdiag-web</automated>
  </verify>
  <done>ConnectionManager is verified with tests.</done>
</task>

<task type="auto">
  <name>Task 3: Create JmxAnalysisService and Refactor AnalysisService</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/service/JmxAnalysisService.java
    memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
  </files>
  <action>
    1. Create `JmxAnalysisService` with methods: `getHistogram`, `getThreadDump`, `getNmtSnapshot`, `diagnose`.
       These methods should take a `JmxClient` (and other required context) and perform analysis.
    2. Refactor `AnalysisService`:
       - Inject `ConnectionManager` and `JmxAnalysisService` via constructor.
       - Delegate connection/lifecycle calls to `ConnectionManager`.
       - Delegate JMX analysis calls to `JmxAnalysisService`.
       - Keep Agent analysis and Snapshot/GC Roots logic for Wave 3.
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>AnalysisService is reduced in size and delegates core tasks.</done>
</task>

</tasks>

<verification>
Verify that `AnalysisService` still passes existing integration tests (if any) or basic startup.
Check size of `AnalysisService.java` - target below 400 lines.
</verification>

<success_criteria>
- `ConnectionManager` and `JmxAnalysisService` are implemented and tested.
- `AnalysisService` is smaller and cleaner.
- Constructor injection is strictly used in new and refactored classes.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-2-tech-debt/phase-2-02-SUMMARY.md`
</output>
