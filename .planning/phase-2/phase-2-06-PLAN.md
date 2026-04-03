---
phase: phase-2-tech-debt
plan: 06
type: execute
wave: 4
depends_on: ["phase-2-03"]
files_modified:
  - memdiag-web/src/main/java/com/memdiag/web/service/GcRootsService.java
  - memdiag-web/src/test/java/com/memdiag/web/service/GcRootsServiceTest.java
autonomous: true
requirements: [R-DEBT-001, R-DEBT-002, R-TEST-001]
user_setup: []

must_haves:
  truths:
    - "GC Roots tracking logic is isolated in GcRootsService"
    - "GcRootsService is verified with unit tests"
  artifacts:
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/GcRootsService.java"
      provides: "GC Roots tracking and statistics retrieval orchestration"
    - path: "memdiag-web/src/test/java/com/memdiag/web/service/GcRootsServiceTest.java"
      provides: "Unit test for GcRootsService with 80%+ coverage"
  key_links:
    - from: "GcRootsService.java"
      to: "ConnectionManager"
      via: "Constructor injection"
---

<objective>
Extract GC Roots analysis and tracking logic from AnalysisService and verify with unit tests.

Purpose: Isolate GC Roots orchestration and ensure reliable data collection.
Output: Tested GcRootsService.
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
  <name>Task 1: Create GcRootsService</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/service/GcRootsService.java
  </files>
  <action>
    Create `@Service` class `GcRootsService`.
    Extract all GC Roots-related methods from `AnalysisService`:
    - `getGcRootStats`, `startGcRootTracking`, `stopGcRootTracking`.
    Use constructor injection for `ConnectionManager`.
    Ensure proper error handling for JMX vs Agent modes (GC Roots requires Agent mode).
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>GcRootsService is implemented with constructor injection.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Unit Test GcRootsService</name>
  <files>
    memdiag-web/src/test/java/com/memdiag/web/service/GcRootsServiceTest.java
  </files>
  <behavior>
    - Can retrieve GC Root stats for Agent connection.
    - Throws exception for JMX connection.
    - Successfully starts/stops tracking for Agent connection.
    - Target: 80%+ line coverage.
  </behavior>
  <action>
    Create a unit test for `GcRootsService` using JUnit 5 and Mockito.
    Mock `ConnectionManager` and `AgentClient`.
    Verify correct handling of different connection types and proxying to AgentClient.
  </action>
  <verify>
    <automated>mvn test -Dtest=GcRootsServiceTest -pl memdiag-web</automated>
  </verify>
  <done>GcRootsService is verified with tests achieving 80%+ coverage.</done>
</task>

</tasks>

<verification>
`mvn test -Dtest=GcRootsServiceTest -pl memdiag-web` passes.
</verification>

<success_criteria>
- `GcRootsService` is fully implemented and tested.
- GC Roots logic is successfully isolated.
- Parity maintained for all GC Roots-related features.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-2-tech-debt/phase-2-06-SUMMARY.md`
</output>
