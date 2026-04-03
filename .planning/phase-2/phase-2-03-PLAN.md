---
phase: phase-2-tech-debt
plan: 03
type: execute
wave: 3
depends_on: ["phase-2-02"]
files_modified:
  - memdiag-web/src/main/java/com/memdiag/web/service/AgentApiService.java
  - memdiag-web/src/main/java/com/memdiag/web/service/SnapshotService.java
  - memdiag-web/src/main/java/com/memdiag/web/service/GcRootsService.java
  - memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java
  - memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
  - memdiag-web/src/test/java/com/memdiag/web/controller/ApiControllerIntegrationTest.java
autonomous: true
requirements: [R-DEBT-002, R-TEST-001, R-TEST-002]
user_setup: []

must_haves:
  truths:
    - "Agent API calls are isolated in AgentApiService"
    - "Snapshot operations are managed by SnapshotService"
    - "GC Roots operations are handled by GcRootsService"
    - "ApiController uses specialized services directly"
    - "API contract is maintained and verified with tests"
  artifacts:
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/AgentApiService.java"
      provides: "Agent communication layer"
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/SnapshotService.java"
      provides: "Snapshot creation and management"
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/GcRootsService.java"
      provides: "GC Roots tracking and analysis"
  key_links:
    - from: "ApiController.java"
      to: "AgentApiService"
      via: "Constructor injection"
    - from: "ApiController.java"
      to: "SnapshotService"
      via: "Constructor injection"
---

<objective>
Complete the AnalysisService split by extracting Agent, Snapshot, and GC Roots logic.
Refactor ApiController to use these specialized services directly.

Purpose: Finalize the tech debt reduction and establish a robust, testable service layer.
Output: Specialized services for all core features and a refactored API controller.
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
@memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java
</context>

<tasks>

<task type="auto">
  <name>Task 1: Create Specialized Services</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/service/AgentApiService.java
    memdiag-web/src/main/java/com/memdiag/web/service/SnapshotService.java
    memdiag-web/src/main/java/com/memdiag/web/service/GcRootsService.java
  </files>
  <action>
    Extract remaining logic from `AnalysisService`:
    1. Create `AgentApiService`: Proxy all `getAgentStatus`, `getAgentConfig`, `getNativeSummary` etc. calls to `AgentClient`.
    2. Create `SnapshotService`: Implement `createSnapshot`, `listSnapshots`, `loadSnapshot`, `deleteSnapshot`.
    3. Create `GcRootsService`: Implement `getGcRootStats`, `startGcRootTracking`, `stopGcRootTracking`.
    Use constructor injection for all dependencies (`ConnectionManager`, `JmxAnalysisService`, `MemDiagProperties`).
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>Specialized services exist and are properly injected.</done>
</task>

<task type="auto">
  <name>Task 2: Refactor ApiController to use Specialized Services</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java
  </files>
  <action>
    Refactor `ApiController` to use the new specialized services instead of the monolithic `AnalysisService`.
    Dependencies to inject: `ConnectionManager`, `JmxAnalysisService`, `AgentApiService`, `SnapshotService`, `GcRootsService`, `MemDiagProperties`.
    Update all endpoints to call the appropriate service method.
    Maintain the existing API contract (response format, HTTP codes).
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>ApiController is decoupled from AnalysisService facade.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Integration Tests for ApiController</name>
  <files>
    memdiag-web/src/test/java/com/memdiag/web/controller/ApiControllerIntegrationTest.java
  </files>
  <behavior>
    - GET /api/v1/connections returns list of connections.
    - POST /api/v1/connections/{id} connects correctly.
    - GET /api/v1/histogram/{id} returns histogram data.
    - Snapshot creation and listing works.
  </behavior>
  <action>
    Create integration tests for `ApiController` using `MockMvc`.
    Extend `AbstractControllerTest`.
    Mock the service layer to verify controller behavior and API contract.
    Target 80%+ coverage for `ApiController`.
  </action>
  <verify>
    <automated>mvn test -Dtest=ApiControllerIntegrationTest -pl memdiag-web</automated>
  </verify>
  <done>ApiController contract is verified with tests.</done>
</task>

<task type="auto">
  <name>Task 4: Final Cleanup and AnalysisService Deprecation</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
  </files>
  <action>
    If any classes (like `RealtimeController`) still use `AnalysisService`, refactor them to use specialized services.
    Once `AnalysisService` has no consumers, delete it.
    If it must remain as a facade, ensure it delegates all calls to the new services.
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>Monolithic AnalysisService is removed or fully delegated.</done>
</task>

</tasks>

<verification>
Run all tests in `memdiag-web`: `mvn test -pl memdiag-web`
Verify coverage using Jacoco or manual review.
</verification>

<success_criteria>
- No monolithic `AnalysisService` exists.
- `ApiController` is fully test-covered.
- Functional parity maintained across all endpoints.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-2-tech-debt/phase-2-03-SUMMARY.md`
</output>
