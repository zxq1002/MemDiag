---
phase: phase-4-histogram-enhancement
plan: 03
type: execute
wave: 3
depends_on: ["phase-4-02"]
files_modified:
  - memdiag-ui/src/components/histogram/HistogramTable.vue
  - memdiag-ui/src/components/histogram/__tests__/HistogramTable.test.js
autonomous: true
requirements: [R-UI-002]
must_haves:
  truths:
    - "Users can filter classes by numeric count and size thresholds"
    - "Histogram data can be exported as CSV or JSON files"
    - "Table provides immediate visual feedback on relative object sizes using bars"
  artifacts:
    - path: "memdiag-ui/src/components/histogram/HistogramTable.vue"
      provides: "Enhanced interactive table with filters, export, and visual cues"
    - path: "memdiag-ui/src/components/histogram/__tests__/HistogramTable.test.js"
      provides: "Verification of table features"
  key_links:
    - from: "memdiag-ui/src/components/histogram/HistogramTable.vue"
      to: "PrimeVue DataTable"
      via: "filterDisplay='menu' and exportCSV()"
---

<objective>
Enhance the Histogram Table with advanced analysis tools including numeric filtering, multi-format data export, and relative size visual indicators. This transforms a static table into a powerful diagnostic tool for identifying memory leaks and hotspots.
</objective>

<execution_context>
@$HOME/.gemini/get-shit-done/workflows/execute-plan.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/phases/phase-4-histogram-enhancement/04-RESEARCH.md
@memdiag-ui/src/components/histogram/HistogramTable.vue
</context>

<tasks>

<task type="auto">
  <name>Task 1: Implement Advanced Filtering & Data Export</name>
  <files>memdiag-ui/src/components/histogram/HistogramTable.vue</files>
  <action>
    - Configure PrimeVue DataTable with `filterDisplay="menu"`.
    - Add numeric filters for `objectCount` and `shallowBytes` using PrimeVue's `FilterMatchMode.GREATER_THAN_OR_EQUAL_TO` and `InputNumber` components in the filter template.
    - Implement `exportCSV` using PrimeVue's built-in `dt.value.exportCSV()`.
    - Implement `exportJSON` using a custom Blob-based download utility as described in RESEARCH.md.
    - Update the UI to include buttons for these actions.
  </action>
  <verify>
    <automated>grep "exportCSV" memdiag-ui/src/components/histogram/HistogramTable.vue && grep "InputNumber" memdiag-ui/src/components/histogram/HistogramTable.vue</automated>
  </verify>
  <done>Table supports numeric filtering and multi-format export.</done>
</task>

<task type="auto">
  <name>Task 2: Implement Relative Size Data Bars</name>
  <files>memdiag-ui/src/components/histogram/HistogramTable.vue</files>
  <action>
    - Add a computed property to find the maximum `shallowBytes` in the current dataset.
    - Update the `shallowBytes` Column template to include a horizontal "data bar" that represents the value relative to the maximum.
    - Use Tailwind classes for styling: `bg-indigo-500` for the bar, `h-1.5` for height, and `rounded-full` for shape.
  </action>
  <verify>
    <automated>grep "style" memdiag-ui/src/components/histogram/HistogramTable.vue | grep "width"</automated>
  </verify>
  <done>Shallow Size column includes visual relative bars.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Create Unit Tests for HistogramTable</name>
  <files>memdiag-ui/src/components/histogram/__tests__/HistogramTable.test.js</files>
  <behavior>
    - Should render data bars with correct widths based on values
    - Should trigger CSV export when button is clicked
    - Should correctly format JSON for export
    - Should filter rows based on numeric input
  </behavior>
  <action>
    Write component tests using Vitest and @vue/test-utils. Mock PrimeVue's DataTable export methods if necessary, or verify that the correct internal methods are called.
  </action>
  <verify>
    <automated>cd memdiag-ui && npm run test -- HistogramTable</automated>
  </verify>
  <done>Enhanced table features are verified by automated tests.</done>
</task>

</tasks>

<verification>
Run all UI tests: `cd memdiag-ui && npm run test`.
Manual verification of export functionality in the browser.
</verification>

<success_criteria>
- Advanced filtering works for class name (contains) and numeric values (>=).
- Export buttons produce valid .csv and .json files.
- Data bars accurately reflect the magnitude of shallow size.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-4-histogram-enhancement/phase-4-03-SUMMARY.md`
</output>
