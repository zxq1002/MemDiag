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
autonomous: true
requirements: [R-UI-002]
must_haves:
  truths:
    - "Histogram view is broken down into small, focused components"
    - "Functional parity with existing Histogram view is maintained"
  artifacts:
    - path: "memdiag-ui/src/components/histogram/HistogramTable.vue"
      provides: "Refactored data table with PrimeVue features"
---

<objective>
Componentize the Histogram view to improve maintainability and prepare for advanced feature implementation.
Purpose: Reduce the size of the main view and create focused components for summary, charts, and data tables.
Output: A cleaner `Histogram.vue` orchestrating specialized child components.
</objective>

<tasks>

<task type="auto">
  <name>Task 1: Create Histogram child components</name>
  <files>memdiag-ui/src/components/histogram/HistogramSummary.vue, memdiag-ui/src/components/histogram/HistogramChart.vue, memdiag-ui/src/components/histogram/HistogramTable.vue</files>
  <action>
    Extract UI logic from `Histogram.vue` into:
    - `HistogramSummary.vue`: Displays the total objects/bytes cards.
    - `HistogramChart.vue`: Handles ECharts rendering.
    - `HistogramTable.vue`: Houses the DataTable and basic class stats listing.
    Ensure they accept props for data and loading states.
  </action>
  <verify>
    <automated>Verify component files exist and have clear prop definitions.</automated>
  </verify>
  <done>Child components are created.</done>
</task>

<task type="auto">
  <name>Task 2: Refactor Histogram View</name>
  <files>memdiag-ui/src/views/Histogram.vue</files>
  <action>
    Update `Histogram.vue` to:
    - Use the `useHistogram` composable created in Wave 1.
    - Import and use the new child components.
    - Orchestrate the data flow between the composable and children.
  </action>
  <verify>
    <automated>cd memdiag-ui && npm run build</automated>
  </verify>
  <done>Main view is refactored and uses the new architecture.</done>
</task>

</tasks>

<verification>
Ensure the Histogram page still works as expected and shows the same data as before the refactor.
</verification>

<success_criteria>
View is successfully componentized without breaking existing functionality.
</success_criteria>
