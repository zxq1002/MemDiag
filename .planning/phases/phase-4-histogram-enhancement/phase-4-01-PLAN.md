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
    - "Histogram logic is extracted from components into a reusable composable"
    - "Unit tests verify that data fetching and mapping work correctly"
    - "Test infrastructure (Vitest) is configured and running"
  artifacts:
    - path: "memdiag-ui/src/composables/useHistogram.js"
      provides: "Reactive histogram state and fetch logic"
    - path: "memdiag-ui/src/composables/__tests__/useHistogram.test.js"
      provides: "Automated verification of composable logic"
  key_links:
    - from: "memdiag-ui/src/composables/useHistogram.js"
      to: "/api/v1/histogram"
      via: "axios.get"
---

<objective>
Initialize the testing infrastructure and extract the core histogram data management logic into a reusable Vue composable. This improves maintainability and allows for automated testing of the business logic independent of the UI.
</objective>

<execution_context>
@$HOME/.gemini/get-shit-done/workflows/execute-plan.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/REQUIREMENTS.md
@.planning/phases/phase-4-histogram-enhancement/04-RESEARCH.md
@memdiag-ui/src/views/Histogram.vue
</context>

<interfaces>
From memdiag-ui/src/views/Histogram.vue:
```javascript
const loadHistogram = async () => {
  if (!selectedConn.value) return
  isLoading.value = true
  try {
    const response = await axios.get(`/api/v1/histogram/${selectedConn.value}`, { 
      params: { limit: limit.value } 
    })
    histogram.value = response.data
    // ...
  } finally {
    isLoading.value = false
  }
}
```
</interfaces>

<tasks>

<task type="auto">
  <name>Task 1: Initialize Vitest Infrastructure</name>
  <files>memdiag-ui/vitest.config.js</files>
  <action>
    Create a basic Vitest configuration file in the UI module. Ensure it handles Vue SFCs and JSDOM environment.
    Reference: memdiag-ui/package.json already has vitest dependency.
  </action>
  <verify>
    <automated>cd memdiag-ui && npx vitest --version</automated>
  </verify>
  <done>Vitest command runs without configuration errors.</done>
</task>

<task type="auto">
  <name>Task 2: Extract useHistogram Composable</name>
  <files>memdiag-ui/src/composables/useHistogram.js</files>
  <action>
    Extract the logic from Histogram.vue (lines 19-65 approx) into a dedicated composable.
    - state: histogram, isLoading, limit
    - computed: classStats, totalObjects, totalBytes
    - methods: load (accepts connectionId)
    Ensure it handles both { data: { classes: [] } } and { classes: [] } response formats found in the existing code.
  </action>
  <verify>
    <automated>test -f memdiag-ui/src/composables/useHistogram.js</automated>
  </verify>
  <done>Composable exported with required reactive state and methods.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Implement Unit Tests for useHistogram</name>
  <files>memdiag-ui/src/composables/__tests__/useHistogram.test.js</files>
  <behavior>
    - Should initialize with default values (isLoading=false, histogram=null)
    - Should fetch data from API and update state
    - Should correctly compute classStats, totalObjects, and totalBytes from various response formats
    - Should handle API errors gracefully
  </behavior>
  <action>
    Write unit tests using Vitest and @vue/test-utils (or just standard Vue reactivity if testing the composable in isolation). Use vi.mock('axios') to simulate API responses.
  </action>
  <verify>
    <automated>cd memdiag-ui && npm run test -- useHistogram</automated>
  </verify>
  <done>All tests pass with 100% coverage of the composable logic.</done>
</task>

</tasks>

<verification>
Run all unit tests in the UI module: `cd memdiag-ui && npm run test`.
</verification>

<success_criteria>
- `useHistogram.js` exists and is fully tested.
- `vitest.config.js` exists.
- Business logic is successfully decoupled from the `Histogram.vue` view.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-4-histogram-enhancement/phase-4-01-SUMMARY.md`
</output>
