---
phase: phase-4-histogram-enhancement
plan: 02
type: execute
wave: 2
depends_on: ["phase-4-01"]
files_modified:
  - memdiag-ui/src/components/histogram/HistogramSummary.vue
  - memdiag-ui/src/components/histogram/HistogramChart.vue
  - memdiag-ui/src/components/histogram/HistogramTable.vue
  - memdiag-ui/src/views/Histogram.vue
autonomous: false
requirements: [R-UI-002]
must_haves:
  truths:
    - "Histogram view is divided into clear functional components"
    - "Components are driven by the useHistogram composable"
    - "UI remains visually identical to current state after refactoring"
  artifacts:
    - path: "memdiag-ui/src/components/histogram/HistogramSummary.vue"
      provides: "Total objects/bytes cards"
    - path: "memdiag-ui/src/components/histogram/HistogramChart.vue"
      provides: "ECharts memory distribution visualization"
    - path: "memdiag-ui/src/components/histogram/HistogramTable.vue"
      provides: "Main DataTable for class statistics"
    - path: "memdiag-ui/src/views/Histogram.vue"
      provides: "Coordinated view using sub-components"
  key_links:
    - from: "memdiag-ui/src/views/Histogram.vue"
      to: "memdiag-ui/src/composables/useHistogram.js"
      via: "useHistogram()"
---

<objective>
Refactor the single monolithic Histogram view into modular, reusable components according to the research recommendation. This modularity is a prerequisite for adding complex features like advanced filtering and data export without bloating the main view.
</objective>

<execution_context>
@$HOME/.gemini/get-shit-done/workflows/execute-plan.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/phases/phase-4-histogram-enhancement/04-RESEARCH.md
@memdiag-ui/src/views/Histogram.vue
@memdiag-ui/src/composables/useHistogram.js
</context>

<interfaces>
From memdiag-ui/src/composables/useHistogram.js (Planned in 01):
```javascript
export function useHistogram() {
  const histogram = ref(null);
  const isLoading = ref(false);
  const limit = ref(20);
  const classStats = computed(() => ...);
  const totalObjects = computed(() => ...);
  const totalBytes = computed(() => ...);
  const load = async (connectionId) => { ... };
  return { histogram, isLoading, limit, classStats, totalObjects, totalBytes, load };
}
```
</interfaces>

<tasks>

<task type="auto">
  <name>Task 1: Create Histogram Sub-components</name>
  <files>
    memdiag-ui/src/components/histogram/HistogramSummary.vue,
    memdiag-ui/src/components/histogram/HistogramChart.vue,
    memdiag-ui/src/components/histogram/HistogramTable.vue
  </files>
  <action>
    Extract specific templates and logic from Histogram.vue:
    - HistogramSummary.vue: Card-based summary (totalObjects, totalBytes props)
    - HistogramChart.vue: ECharts chart (classStats prop, contains resize logic)
    - HistogramTable.vue: DataTable (classStats prop, filters state)
    Ensure all icons and PrimeVue components are properly imported in each file.
  </action>
  <verify>
    <automated>test -f memdiag-ui/src/components/histogram/HistogramSummary.vue && test -f memdiag-ui/src/components/histogram/HistogramChart.vue && test -f memdiag-ui/src/components/histogram/HistogramTable.vue</automated>
  </verify>
  <done>All three sub-components are created and correctly isolated.</done>
</task>

<task type="auto">
  <name>Task 2: Refactor Histogram.vue View</name>
  <files>memdiag-ui/src/views/Histogram.vue</files>
  <action>
    Replace the monolithic logic with the useHistogram composable and the three new components.
    - Import and initialize useHistogram.
    - Replace template sections with component tags, passing required props.
    - Keep the top-level controls (connection selection, refresh button) in Histogram.vue.
    - Remove unused imports and logic from the main view.
  </action>
  <verify>
    <automated>grep "HistogramSummary" memdiag-ui/src/views/Histogram.vue && grep "useHistogram" memdiag-ui/src/views/Histogram.vue</automated>
  </verify>
  <done>Histogram.vue is a clean orchestrator view with delegated logic and presentation.</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 3: Verify Refactored View</name>
  <files>memdiag-ui/src/views/Histogram.vue</files>
  <action>Human verification of the refactored UI.</action>
  <what-built>Refactored Histogram View with sub-components</what-built>
  <how-to-verify>
    1. Start the dev server: `cd memdiag-ui && npm run dev`
    2. Open the browser and navigate to the Histogram view.
    3. Verify that the summary, chart, and table still render correctly and respond to the refresh button.
  </how-to-verify>
  <verify>User approves the UI check.</verify>
  <done>UI is functional.</done>
  <resume-signal>approved</resume-signal>
</task>

</tasks>

<verification>
Manual verification in browser to ensure no regression in UI or functionality.
</verification>

<success_criteria>
- View logic is fully decoupled into components.
- No regression in existing functionality (charting, basic table sorting, refresh).
</success_criteria>

<output>
After completion, create `.planning/phases/phase-4-histogram-enhancement/phase-4-02-SUMMARY.md`
</output>
