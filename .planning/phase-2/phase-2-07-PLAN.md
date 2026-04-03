---
phase: phase-2-tech-debt
plan: 07
type: execute
wave: 5
depends_on: ["phase-2-04", "phase-2-05", "phase-2-06"]
files_modified:
  - memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java
  - memdiag-web/src/test/java/com/memdiag/web/controller/ApiControllerIntegrationTest.java
autonomous: true
requirements: [R-DEBT-001, R-DEBT-002, R-TEST-002]
user_setup: []

must_haves:
  truths:
    - "ApiController uses specialized services via constructor injection"
    - "API contract is maintained and verified with tests"
  artifacts:
    - path: "memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java"
      provides: "REST API endpoints delegating to specialized services"
    - path: "memdiag-web/src/test/java/com/memdiag/web/controller/ApiControllerIntegrationTest.java"
      provides: "Integration test for ApiController with 80%+ coverage"
  key_links:
    - from: "ApiController.java"
      to: "ConnectionManager"
      via: "Constructor injection"
    - from: "ApiController.java"
      to: "JmxAnalysisService"
      via: "Constructor injection"
    - from: "ApiController.java"
      to: "AgentApiService"
      via: "Constructor injection"
    - from: "ApiController.java"
      to: "SnapshotService"
      via: "Constructor injection"
    - from: "ApiController.java"
      to: "GcRootsService"
      via: "Constructor injection"
---

<objective>
Refactor ApiController to use specialized services directly and verify the API contract with integration tests.

Purpose: Decouple the web layer from the monolithic AnalysisService and ensure API stability.
Output: Refactored ApiController and a comprehensive integration test suite.
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
@memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java
</context>

<tasks>

<task type="auto">
  <name>Task 1: Refactor ApiController</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java
  </files>
  <action>
    Refactor `ApiController` to use the new specialized services instead of the monolithic `AnalysisService`.
    Dependencies to inject via constructor:
    - `ConnectionManager`
    - `JmxAnalysisService`
    - `AgentApiService`
    - `SnapshotService`
    - `GcRootsService`
    - `MemDiagProperties`
    Update all endpoints to call the appropriate service method.
    Ensure ALL `@Autowired` fields are removed in favor of constructor injection (R-DEBT-001).
    Crucially, maintain the existing API contract (response format, HTTP codes).
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>ApiController is refactored to use granular services with constructor injection.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Integration Tests for ApiController</name>
  <files>
    memdiag-web/src/test/java/com/memdiag/web/controller/ApiControllerIntegrationTest.java
  </files>
  <behavior>
    - GET /api/v1/connections returns list of connections.
    - POST /api/v1/connections/{id} connects correctly.
    - GET /api/v1/histogram/{id} returns histogram data.
    - Snapshot creation and listing works.
    - GC Roots tracking endpoints respond correctly.
    - Target: 80%+ line coverage for ApiController.
  </behavior>
  <action>
    Create integration tests for `ApiController` using `MockMvc`.
    Extend `AbstractControllerTest`.
    Mock the service layer to verify controller behavior and API contract.
    Verify that all endpoints return the expected JSON structure and status codes.
  </action>
  <verify>
    <automated>mvn test -Dtest=ApiControllerIntegrationTest -pl memdiag-web</automated>
  </verify>
  <done>ApiController contract is verified with tests achieving 80%+ coverage.</done>
</task>

</tasks>

<verification>
`mvn test -Dtest=ApiControllerIntegrationTest -pl memdiag-web` passes.
Run all tests in web module: `mvn test -pl memdiag-web`.
</verification>

<success_criteria>
- `ApiController` is fully test-covered.
- Functional parity maintained across all endpoints.
- Constructor injection is strictly followed.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-2-tech-debt/phase-2-07-SUMMARY.md`
</output>
