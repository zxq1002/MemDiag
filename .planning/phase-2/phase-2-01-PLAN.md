---
phase: phase-2-tech-debt
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - memdiag-web/src/test/java/com/memdiag/web/controller/AbstractControllerTest.java
  - memdiag-web/src/main/java/com/memdiag/web/config/MemDiagProperties.java
  - memdiag-web/src/main/java/com/memdiag/web/config/MemDiagConfiguration.java
  - memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
  - memdiag-web/src/main/java/com/memdiag/web/controller/RealtimeController.java
  - memdiag-web/src/main/resources/application.properties
autonomous: true
requirements: [R-DEBT-003, R-TEST-001]
user_setup: []

must_haves:
  truths:
    - "Test suite directory exists and is recognized by Maven"
    - "Base test class for MockMvc is available"
    - "Hardcoded configuration values are replaced with property injections"
    - "Properties can be overridden via application.properties"
  artifacts:
    - path: "memdiag-web/src/test/java/com/memdiag/web/controller/AbstractControllerTest.java"
      provides: "MockMvc base setup for web tests"
    - path: "memdiag-web/src/main/java/com/memdiag/web/config/MemDiagProperties.java"
      provides: "Type-safe configuration properties"
    - path: "memdiag-web/src/main/java/com/memdiag/web/config/MemDiagConfiguration.java"
      provides: "EnableConfigurationProperties binding"
  key_links:
    - from: "AnalysisService.java"
      to: "MemDiagProperties"
      via: "Constructor injection"
    - from: "RealtimeController.java"
      to: "MemDiagProperties"
      via: "Constructor injection"
---

<objective>
Initialize the test foundation for the web module and externalize all hardcoded configuration values.

Purpose: Provide a safety net for refactoring and eliminate "magic numbers" in the codebase.
Output: A working test suite scaffold and a central configuration system.
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
@memdiag-web/src/main/java/com/memdiag/web/controller/RealtimeController.java
</context>

<tasks>

<task type="auto">
  <name>Task 1: Initialize Test Suite Foundation</name>
  <files>
    memdiag-web/src/test/java/com/memdiag/web/controller/AbstractControllerTest.java
  </files>
  <action>
    Create the directory structure `memdiag-web/src/test/java/com/memdiag/web/controller/`.
    Implement `AbstractControllerTest` using `@SpringBootTest` and `MockMvc`.
    This class will provide the base MockMvc setup for all controller tests.
    Ensure it uses JUnit 5 and AssertJ as per RESEARCH.md.
  </action>
  <verify>
    <automated>mvn test-compile -pl memdiag-web</automated>
  </verify>
  <done>Base test class exists and compiles correctly.</done>
</task>

<task type="auto">
  <name>Task 2: Externalize Configuration Properties</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/config/MemDiagProperties.java
    memdiag-web/src/main/java/com/memdiag/web/config/MemDiagConfiguration.java
    memdiag-web/src/main/resources/application.properties
  </files>
  <action>
    1. Create `MemDiagProperties.java` using `@ConfigurationProperties(prefix = "memdiag")`.
       Define fields for:
       - `agentPort` (default 6789)
       - `realtimeRate` (default 5000)
       - `defaultHistogramLimit` (default 10)
       - `snapshotHistogramLimit` (default 1000)
    2. Create `MemDiagConfiguration.java` with `@Configuration` and `@EnableConfigurationProperties(MemDiagProperties.class)`.
    3. Update `application.properties` with these defaults for clarity.
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>Configuration system is in place and type-safe.</done>
</task>

<task type="auto">
  <name>Task 3: Refactor Services and Controllers to use Properties</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
    memdiag-web/src/main/java/com/memdiag/web/controller/RealtimeController.java
  </files>
  <action>
    1. Inject `MemDiagProperties` into `AnalysisService` via constructor.
    2. Replace hardcoded `6789` in `connectAgent` with `properties.getAgentPort()`.
    3. Replace hardcoded `1000` in `createSnapshot` with `properties.getSnapshotHistogramLimit()`.
    4. Inject `MemDiagProperties` into `RealtimeController` via constructor.
    5. Replace hardcoded `5000` in `@Scheduled(fixedRate = 5000)` with `${memdiag.realtime-rate:5000}`.
    6. Replace hardcoded `10` in `sendRealtimeUpdates` call to `getHistogram` with `properties.getDefaultHistogramLimit()`.
    7. Per R-DEBT-001, verify all dependencies in these classes use constructor injection.
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>Hardcoded values removed; code uses central configuration.</done>
</task>

</tasks>

<verification>
Check for any remaining hardcoded values in `memdiag-web` using:
`grep -r "6789\\|5000\\|1000" memdiag-web/src/main/java`
</verification>

<success_criteria>
- Test foundation is ready for Wave 2.
- No hardcoded ports, rates, or limits in `AnalysisService` or `RealtimeController`.
- All injection in refactored classes is constructor-based.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-2-tech-debt/phase-2-01-SUMMARY.md`
</output>
