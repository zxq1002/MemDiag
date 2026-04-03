---
phase: phase-2-tech-debt
plan: 03
type: execute
wave: 3
depends_on: ["phase-2-02"]
files_modified:
  - memdiag-web/src/test/java/com/memdiag/web/service/JmxAnalysisServiceTest.java
  - memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
autonomous: true
requirements: [R-DEBT-001, R-DEBT-002, R-TEST-001]
user_setup: []

must_haves:
  truths:
    - "JmxAnalysisService is verified with unit tests"
    - "AnalysisService delegates connection and JMX tasks"
  artifacts:
    - path: "memdiag-web/src/test/java/com/memdiag/web/service/JmxAnalysisServiceTest.java"
      provides: "Unit test for JmxAnalysisService with 80%+ coverage"
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java"
      provides: "AnalysisService partially refactored to delegate to specialized services"
  key_links:
    - from: "AnalysisService.java"
      to: "ConnectionManager"
      via: "Constructor injection"
    - from: "AnalysisService.java"
      to: "JmxAnalysisService"
      via: "Constructor injection"
---

<objective>
Verify JmxAnalysisService and refactor AnalysisService to delegate connection and JMX-specific logic.

Purpose: Continue the monolithic service split and ensure high quality through testing.
Output: Tested JmxAnalysisService and a thinner AnalysisService.
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
@memdiag-web/src/main/java/com/memdiag/web/service/JmxAnalysisService.java
@memdiag-web/src/main/java/com/memdiag/web/service/ConnectionManager.java
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Unit Test JmxAnalysisService</name>
  <files>
    memdiag-web/src/test/java/com/memdiag/web/service/JmxAnalysisServiceTest.java
  </files>
  <behavior>
    - Correctly retrieves heap histogram from JmxClient.
    - Correctly retrieves thread dump from JmxClient.
    - Correctly retrieves NMT summary/detail snapshots.
    - Correctly delegates diagnosis to DiagnosisEngine.
    - Target: 80%+ line coverage.
  </behavior>
  <action>
    Create a unit test for `JmxAnalysisService` using JUnit 5 and Mockito.
    Mock `JmxClient`, `HeapAnalyzer`, `ThreadAnalyzer`, `JmxNmtAnalyzer`, and `DiagnosisEngine`.
    Verify all analysis methods return expected data from the underlying core components.
  </action>
  <verify>
    <automated>mvn test -Dtest=JmxAnalysisServiceTest -pl memdiag-web</automated>
  </verify>
  <done>JmxAnalysisService is verified with tests achieving 80%+ coverage.</done>
</task>

<task type="auto">
  <name>Task 2: Refactor AnalysisService Delegation</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
  </files>
  <action>
    1. Update `AnalysisService` to inject `ConnectionManager` and `JmxAnalysisService` via constructor.
    2. Replace internal connection maps with calls to `ConnectionManager`.
    3. Update `connect`, `disconnect`, `getConnectionType`, etc. to delegate to `ConnectionManager`.
    4. Update `getHistogram`, `getThreads`, `getNmtSnapshot`, and `diagnose` to delegate JMX-specific calls to `JmxAnalysisService`.
    5. Maintain Agent analysis, Snapshot, and GC Roots logic in `AnalysisService` for now (to be split in subsequent plans).
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>AnalysisService delegates core connection and JMX tasks to specialized services.</done>
</task>

</tasks>

<verification>
`mvn test -Dtest=JmxAnalysisServiceTest -pl memdiag-web` passes.
Compile entire module: `mvn compile -pl memdiag-web`.
</verification>

<success_criteria>
- `JmxAnalysisService` has 80%+ coverage.
- `AnalysisService` has constructor injection for its new dependencies.
- Parity maintained for JMX-related features.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-2-tech-debt/phase-2-03-SUMMARY.md`
</output>
