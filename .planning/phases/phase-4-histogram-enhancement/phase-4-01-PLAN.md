---
phase: phase-4-histogram-enhancement
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - memdiag-ui/vitest.config.js
  - memdiag-ui/src/composables/useHistogram.js
  - memdiag-ui/src/composables/__tests__/useHistogram.test.js
autonomous: true
requirements: [R-UI-002]
must_haves:
  truths:
    - "Histogram fetching and formatting logic is isolated from the UI component"
    - "Composable logic is verified by automated tests"
  artifacts:
    - path: "memdiag-ui/src/composables/useHistogram.js"
      provides: "Stateless histogram management logic"
---

<objective>
Extract histogram data management logic into a reusable Composable and establish the testing infrastructure for frontend logic.
Purpose: Decouple business logic from the UI and ensure reliability via unit tests.
Output: `useHistogram.js` and its corresponding test suite.
</objective>

<tasks>

<task type="auto">
  <name>Task 1: Setup Vitest Configuration</name>
  <files>memdiag-ui/vitest.config.js</files>
  <action>
    Create `vitest.config.js` in `memdiag-ui` root. 
    Configure it to use `jsdom` environment and handle Vue components if needed (though this plan focuses on composables).
  </action>
  <verify>
    <automated>cd memdiag-ui && npx vitest run --passWithNoTests</automated>
  </verify>
  <done>Vitest is configured and runnable.</done>
</task>

<task type="auto">
  <name>Task 2: Implement useHistogram Composable</name>
  <files>memdiag-ui/src/composables/useHistogram.js</files>
  <action>
    Create `useHistogram` composable.
    - Expose `loadHistogram(connectionId, limit)` function.
    - Handle `isLoading`, `error`, and `histogram` state.
    - Include helper functions like `formatBytes` and `formatNumber` (extracted from Histogram.vue).
    - Map API response fields consistently.
  </action>
  <verify>
    <automated>Check file content for reactive state and exported methods.</automated>
  </verify>
  <done>Composable logic is implemented and exported.</done>
</task>

<task type="auto">
  <name>Task 3: Create useHistogram unit tests</name>
  <files>memdiag-ui/src/composables/__tests__/useHistogram.test.js</files>
  <action>
    Write tests for `useHistogram`:
    - Verify `formatBytes` with various inputs.
    - Mock Axios and verify `loadHistogram` updates state correctly on success and failure.
  </action>
  <verify>
    <automated>cd memdiag-ui && npx vitest run src/composables/__tests__/useHistogram.test.js</automated>
  </verify>
  <done>Tests pass and cover core formatting and fetching logic.</done>
</task>

</tasks>

<verification>
Ensure all unit tests pass and the composable is ready for integration.
</verification>

<success_criteria>
Logic is successfully extracted and verified with >80% coverage.
</success_criteria>
