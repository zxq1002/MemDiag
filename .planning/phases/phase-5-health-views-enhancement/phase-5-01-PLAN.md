---
phase: phase-5-health-views-enhancement
plan: 01
type: execute
wave: 1
depends_on: []
files_modified: [
  "memdiag-ui/src/composables/useDiagnosis.js",
  "memdiag-ui/src/composables/__tests__/useDiagnosis.test.js",
  "memdiag-ui/src/composables/useThreads.js",
  "memdiag-ui/src/composables/__tests__/useThreads.test.js"
]
autonomous: true
requirements: [R-TEST-001, R-UI-003, R-UI-005]

must_haves:
  truths:
    - "Diagnosis logic is available as a reusable composable"
    - "Threads logic is available as a reusable composable"
    - "New composables have 80%+ unit test coverage"
  artifacts:
    - path: "memdiag-ui/src/composables/useDiagnosis.js"
      provides: "Diagnosis fetching and processing logic"
    - path: "memdiag-ui/src/composables/useThreads.js"
      provides: "Thread data fetching and filtering logic"
  key_links:
    - from: "memdiag-ui/src/composables/useDiagnosis.js"
      to: "Axios GET /api/v1/diagnose"
    - from: "memdiag-ui/src/composables/useThreads.js"
      to: "Axios GET /api/v1/threads"
---

<objective>
Extract diagnosis and thread analysis logic into dedicated composables and ensure they are properly tested.

Purpose: Decouple business logic from UI components, enabling reuse and unit testing.
Output: Working composables `useDiagnosis.js` and `useThreads.js` with comprehensive Vitest suites.
</objective>

<execution_context>
@$HOME/.gemini/get-shit-done/workflows/execute-plan.md
@$HOME/.gemini/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@memdiag-ui/src/views/Diagnosis.vue
@memdiag-ui/src/views/Threads.vue
@memdiag-ui/src/composables/useHistogram.js
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Create and test useDiagnosis.js</name>
  <files>
    memdiag-ui/src/composables/useDiagnosis.js,
    memdiag-ui/src/composables/__tests__/useDiagnosis.test.js
  </files>
  <behavior>
    - loadDiagnosis(connId) calls axios.get and populates results
    - diagnosisData computed property correctly extracts data
    - issues computed property extracts issues array
    - getSeverityInfo(severity) returns correct colors/icons (ShieldAlert, AlertTriangle, Info)
    - isLoading reflects fetching state
  </behavior>
  <action>
    Extract logic from `Diagnosis.vue` into `useDiagnosis.js`.
    Implement the composable pattern seen in `useHistogram.js`.
    Write Vitest tests in `__tests__/useDiagnosis.test.js`.
  </action>
  <verify>
    <automated>npm test -- src/composables/__tests__/useDiagnosis.test.js</automated>
  </verify>
  <done>Composable created and tests pass</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Create and test useThreads.js</name>
  <files>
    memdiag-ui/src/composables/useThreads.js,
    memdiag-ui/src/composables/__tests__/useThreads.test.js
  </files>
  <behavior>
    - loadThreads(connId) calls axios.get and populates threads
    - stateCounts computed property correctly tallies thread states
    - getStateSeverity(state) returns correct PrimeVue severity strings
    - threads computed property extracts threadStats array
  </behavior>
  <action>
    Extract logic from `Threads.vue` into `useThreads.js`.
    Ensure state calculation logic is moved here.
    Write Vitest tests in `__tests__/useThreads.test.js`.
  </action>
  <verify>
    <automated>npm test -- src/composables/__tests__/useThreads.test.js</automated>
  </verify>
  <done>Composable created and tests pass</done>
</task>

</tasks>

<verification>
Check that the new files follow project conventions and tests are green.
</verification>

<success_criteria>
- useDiagnosis.js and useThreads.js exist in memdiag-ui/src/composables/
- Unit tests exist and pass for both
- Business logic (fetching, computing stats) is successfully extracted
</success_criteria>

<output>
After completion, create `.planning/phases/phase-5-health-views-enhancement/phase-5-01-SUMMARY.md`
</output>
