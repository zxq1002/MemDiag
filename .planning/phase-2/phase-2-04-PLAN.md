---
phase: phase-2-tech-debt
plan: 04
type: execute
wave: 4
depends_on: ["phase-2-03"]
files_modified:
  - memdiag-web/src/main/java/com/memdiag/web/service/AgentApiService.java
  - memdiag-web/src/test/java/com/memdiag/web/service/AgentApiServiceTest.java
autonomous: true
requirements: [R-DEBT-001, R-DEBT-002, R-TEST-001]
user_setup: []

must_haves:
  truths:
    - "Agent communications are isolated in AgentApiService"
    - "AgentApiService is verified with unit tests"
  artifacts:
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/AgentApiService.java"
      provides: "Proxy for AgentClient calls with consistent error handling"
    - path: "memdiag-web/src/test/java/com/memdiag/web/service/AgentApiServiceTest.java"
      provides: "Unit test for AgentApiService with 80%+ coverage"
  key_links:
    - from: "AgentApiService.java"
      to: "ConnectionManager"
      via: "Constructor injection"
---

<objective>
Extract agent-specific analysis and communication logic into AgentApiService and verify with unit tests.

Purpose: Isolate agent API interactions and ensure they work as expected.
Output: Tested AgentApiService.
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
  <name>Task 1: Create AgentApiService</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/service/AgentApiService.java
  </files>
  <action>
    Create `@Service` class `AgentApiService`.
    Extract all agent-related methods from `AnalysisService`:
    - `getAgentStatus`, `getAgentConfig`, `getAgentMetrics`.
    - `getNativeStatus`, `getNativeSummary`, `getNativeRegions`, `getNativeDiagnosis`.
    - `getAllocationsRecent`, `getAllocationsStats`, `getAllocationsTop`, etc.
    - `getInstrumentationStatus`, `enable/disableAllocationTracking`, `enable/disableMethodMonitoring`.
    - `getHistogram`, `getThreads`, `getNmtSnapshot` (agent implementation part).
    Use constructor injection for `ConnectionManager`.
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>AgentApiService is implemented with constructor injection.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Unit Test AgentApiService</name>
  <files>
    memdiag-web/src/test/java/com/memdiag/web/service/AgentApiServiceTest.java
  </files>
  <behavior>
    - Proxies status/config/metrics calls correctly to AgentClient.
    - Handles missing agent connection gracefully.
    - Correctly maps NativeMemorySummary to NmtSnapshot.
    - Forwards tracking/monitoring commands correctly.
    - Target: 80%+ line coverage.
  </behavior>
  <action>
    Create a unit test for `AgentApiService` using JUnit 5 and Mockito.
    Mock `ConnectionManager` and `AgentClient`.
    Verify all proxy methods return expected data from AgentClient.
  </action>
  <verify>
    <automated>mvn test -Dtest=AgentApiServiceTest -pl memdiag-web</automated>
  </verify>
  <done>AgentApiService is verified with tests achieving 80%+ coverage.</done>
</task>

</tasks>

<verification>
`mvn test -Dtest=AgentApiServiceTest -pl memdiag-web` passes.
</verification>

<success_criteria>
- `AgentApiService` is fully implemented and tested.
- Agent analysis logic is successfully isolated.
- Parity maintained for all agent-related features.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-2-tech-debt/phase-2-04-SUMMARY.md`
</output>
