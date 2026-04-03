---
phase: phase-2-tech-debt
plan: 02
type: execute
wave: 2
depends_on: ["phase-2-01"]
files_modified:
  - memdiag-web/src/main/java/com/memdiag/web/service/ConnectionManager.java
  - memdiag-web/src/test/java/com/memdiag/web/service/ConnectionManagerTest.java
  - memdiag-web/src/main/java/com/memdiag/web/service/JmxAnalysisService.java
autonomous: true
requirements: [R-DEBT-001, R-DEBT-002, R-TEST-001]
user_setup: []

must_haves:
  truths:
    - "Connection lifecycle is managed by ConnectionManager"
    - "JmxAnalysisService contains core JMX logic"
    - "New services use constructor injection"
  artifacts:
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/ConnectionManager.java"
      provides: "Thread-safe connection storage and lifecycle management"
    - path: "memdiag-web/src/test/java/com/memdiag/web/service/ConnectionManagerTest.java"
      provides: "Unit test for ConnectionManager with 80%+ coverage"
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/JmxAnalysisService.java"
      provides: "JMX-specific heap, thread, and NMT analysis"
  key_links:
    - from: "ConnectionManager.java"
      to: "MemDiagProperties"
      via: "Constructor injection"
---

<objective>
Extract connection management and JMX analysis logic from the monolithic AnalysisService.

Purpose: Reduce AnalysisService size and improve maintainability by following the Single Responsibility Principle.
Output: Two new focused services and a unit test for ConnectionManager.
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
@.planning/phase-2/VALIDATION.md
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
  <name>Task 2: Unit Test ConnectionManager</name>
  <files>
    memdiag-web/src/test/java/com/memdiag/web/service/ConnectionManagerTest.java
  </files>
  <behavior>
    - Can register and retrieve a JMX connection.
    - Can register and retrieve an Agent connection.
    - Can disconnect a connection.
    - Returns correct connection type for a given ID.
    - Target: 80%+ line coverage.
  </behavior>
  <action>
    Create a unit test for `ConnectionManager` using JUnit 5 and Mockito.
    Mock `JmxClient`, `AgentClient`, and `MemDiagProperties`.
    Verify all core connection management scenarios.
  </action>
  <verify>
    <automated>mvn test -Dtest=ConnectionManagerTest -pl memdiag-web</automated>
  </verify>
  <done>ConnectionManager is verified with tests achieving 80%+ coverage.</done>
</task>

<task type="auto">
  <name>Task 3: Create JmxAnalysisService</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/service/JmxAnalysisService.java
  </files>
  <action>
    Create `JmxAnalysisService` by extracting JMX-specific logic from `AnalysisService`.
    Methods to implement:
    - `getHistogram(JmxClient client, int limit)`: Uses `JmxHeapAnalyzer`.
    - `getThreadDump(JmxClient client)`: Uses `ThreadAnalyzer`.
    - `getNmtSnapshot(JmxClient client, boolean detail)`: Uses `JmxNmtAnalyzer`.
    - `diagnose(DiagnosisEngine engine)`: Delegates to engine.
    This service should be stateless and take required dependencies as parameters or via constructor injection if they are persistent.
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>JmxAnalysisService exists with extracted JMX logic.</done>
</task>

</tasks>

<verification>
`mvn test -Dtest=ConnectionManagerTest -pl memdiag-web` passes.
Check for constructor injection in `ConnectionManager.java`.
</verification>

<success_criteria>
- `ConnectionManager` and `JmxAnalysisService` are implemented.
- `ConnectionManager` has 80%+ test coverage.
- Constructor injection is strictly used in new classes.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-2-tech-debt/phase-2-02-SUMMARY.md`
</output>
