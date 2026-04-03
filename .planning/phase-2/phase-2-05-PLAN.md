---
phase: phase-2-tech-debt
plan: 05
type: execute
wave: 4
depends_on: ["phase-2-03"]
files_modified:
  - memdiag-web/src/main/java/com/memdiag/web/service/SnapshotService.java
  - memdiag-web/src/test/java/com/memdiag/web/service/SnapshotServiceTest.java
autonomous: true
requirements: [R-DEBT-001, R-DEBT-002, R-TEST-001]
user_setup: []

must_haves:
  truths:
    - "Snapshot operations are managed by SnapshotService"
    - "SnapshotService is verified with unit tests"
  artifacts:
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/SnapshotService.java"
      provides: "High-level snapshot creation and retrieval orchestration"
    - path: "memdiag-web/src/test/java/com/memdiag/web/service/SnapshotServiceTest.java"
      provides: "Unit test for SnapshotService with 80%+ coverage"
  key_links:
    - from: "SnapshotService.java"
      to: "ConnectionManager"
      via: "Constructor injection"
    - from: "SnapshotService.java"
      to: "JmxAnalysisService"
      via: "Constructor injection"
    - from: "SnapshotService.java"
      to: "AgentApiService"
      via: "Constructor injection"
---

<objective>
Extract snapshot creation and management logic from AnalysisService and verify with unit tests.

Purpose: Isolate snapshot orchestration and ensure reliable data collection.
Output: Tested SnapshotService.
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
  <name>Task 1: Create SnapshotService</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/service/SnapshotService.java
  </files>
  <action>
    Create `@Service` class `SnapshotService`.
    Extract all snapshot-related methods from `AnalysisService`:
    - `createSnapshot`, `listSnapshots`, `loadSnapshot`, `deleteSnapshot`.
    Use constructor injection for `ConnectionManager`, `JmxAnalysisService`, `AgentApiService`, and `MemDiagProperties`.
    Refactor `createSnapshot` to use `JmxAnalysisService` or `AgentApiService` for data collection.
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>SnapshotService is implemented with constructor injection.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Unit Test SnapshotService</name>
  <files>
    memdiag-web/src/test/java/com/memdiag/web/service/SnapshotServiceTest.java
  </files>
  <behavior>
    - Can create a snapshot using data from analysis services.
    - Correctly lists available snapshots from SnapshotManager.
    - Correctly loads/deletes snapshots via SnapshotManager.
    - Target: 80%+ line coverage.
  </behavior>
  <action>
    Create a unit test for `SnapshotService` using JUnit 5 and Mockito.
    Mock all injected services and `SnapshotManager`.
    Verify snapshot creation workflow and file system interactions.
  </action>
  <verify>
    <automated>mvn test -Dtest=SnapshotServiceTest -pl memdiag-web</automated>
  </verify>
  <done>SnapshotService is verified with tests achieving 80%+ coverage.</done>
</task>

</tasks>

<verification>
`mvn test -Dtest=SnapshotServiceTest -pl memdiag-web` passes.
</verification>

<success_criteria>
- `SnapshotService` is fully implemented and tested.
- Snapshot logic is successfully isolated.
- Parity maintained for all snapshot-related features.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-2-tech-debt/phase-2-05-SUMMARY.md`
</output>
