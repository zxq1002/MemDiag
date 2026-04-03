---
phase: phase-2-tech-debt
plan: 08
type: execute
wave: 6
depends_on: ["phase-2-07"]
files_modified:
  - memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
autonomous: true
requirements: [R-DEBT-002]
user_setup: []

must_haves:
  truths:
    - "Monolithic AnalysisService is removed or fully delegated"
  artifacts:
    - path: "memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java"
      provides: "Facade service for backward compatibility (if needed) or removed"
---

<objective>
Final cleanup of the service layer and removal or deprecation of the monolithic AnalysisService.

Purpose: Eliminate the monolithic service and finalize the clean architecture.
Output: Clean codebase with no monolithic remnants.
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
  <name>Task 1: Final Cleanup and AnalysisService Removal</name>
  <files>
    memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
  </files>
  <action>
    Check for any remaining consumers of `AnalysisService`.
    If no consumers remain, delete `AnalysisService.java`.
    If it must remain as a facade for external modules not yet refactored, ensure it delegates all methods to the new specialized services.
    Verify that no business logic remains in `AnalysisService`.
  </action>
  <verify>
    <automated>mvn compile -pl memdiag-web</automated>
  </verify>
  <done>Monolithic AnalysisService is removed or fully delegated.</done>
</task>

</tasks>

<verification>
Run all tests in web module: `mvn test -pl memdiag-web`.
Verify module compiles: `mvn compile -pl memdiag-web`.
</verification>

<success_criteria>
- No monolithic `AnalysisService` exists or contains business logic.
- Total lines in `AnalysisService.java` (if it remains) < 100.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-2-tech-debt/phase-2-08-SUMMARY.md`
</output>
