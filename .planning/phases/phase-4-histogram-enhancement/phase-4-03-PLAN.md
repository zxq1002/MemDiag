---
phase: phase-4-histogram-enhancement
plan: 03
type: execute
wave: 3
depends_on: ["phase-4-02"]
files_modified:
  - memdiag-ui/src/components/histogram/HistogramTable.vue
autonomous: true
requirements: [R-UI-002]
must_haves:
  truths:
    - "Users can filter classes by numeric thresholds (count/size)"
    - "Data can be exported to CSV and JSON formats"
    - "Relative size bars provide visual weight to large classes"
  artifacts:
    - path: "memdiag-ui/src/components/histogram/HistogramTable.vue"
      provides: "Enhanced analysis features"
---

<objective>
Implement advanced analysis features in the Histogram table, including complex filtering, data export, and visual data bars.
Purpose: Provide users with powerful tools to drill down into heap usage.
Output: An advanced, feature-rich Histogram analysis table.
</objective>

<tasks>

<task type="auto">
  <name>Task 1: Add Advanced Filtering</name>
  <files>memdiag-ui/src/components/histogram/HistogramTable.vue</files>
  <action>
    Configure PrimeVue DataTable to use `filterDisplay="menu"`.
    Add numeric filter constraints for "Objects" and "Shallow Size" columns (e.g., greater than, less than).
  </action>
  <verify>
    <automated>Check for filterDisplay and filter constraints in code.</automated>
  </verify>
  <done>Advanced filtering is implemented.</done>
</task>

<task type="auto">
  <name>Task 2: Implement Data Export</name>
  <files>memdiag-ui/src/components/histogram/HistogramTable.vue</files>
  <action>
    - Integrate `DataTable.exportCSV()` for CSV export.
    - Implement a custom JSON export function using `Blob` and `a.download` pattern.
    - Add UI buttons to trigger these exports.
  </action>
  <verify>
    <automated>Check for export methods and trigger buttons.</automated>
  </verify>
  <done>CSV and JSON export functionality is implemented.</done>
</task>

<task type="auto">
  <name>Task 3: Implement Visual Data Bars</name>
  <files>memdiag-ui/src/components/histogram/HistogramTable.vue</files>
  <action>
    Add a template to the "Shallow Size" column that renders a background bar representing the class's size relative to the largest class in the current view.
    Use Tailwind classes for styling the bar (e.g., `bg-indigo-100`).
  </action>
  <verify>
    <automated>Check for template implementation in Shallow Size column.</automated>
  </verify>
  <done>Visual data bars are implemented.</done>
</task>

</tasks>

<verification>
Ensure all new features are integrated and do not introduce UI glitches or performance regressions.
</verification>

<success_criteria>
Histogram view provides advanced filtering, data bars, and export capabilities.
</success_criteria>
