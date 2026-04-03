---
phase: phase-5-health-views-enhancement
plan: 02
type: execute
wave: 2
depends_on: ["phase-5-01"]
files_modified: [
  "memdiag-ui/src/components/diagnosis/DiagnosisSummary.vue",
  "memdiag-ui/src/components/diagnosis/DiagnosisIssueList.vue",
  "memdiag-ui/src/components/diagnosis/DiagnosisIssueDetail.vue",
  "memdiag-ui/src/views/Diagnosis.vue"
]
autonomous: false
requirements: [R-UI-003]

must_haves:
  truths:
    - "Diagnosis issues can be filtered by severity using buttons (All, Critical, Warning, Info)"
    - "Summary cards reflect the data correctly"
    - "The view is componentized"
  artifacts:
    - path: "memdiag-ui/src/components/diagnosis/DiagnosisSummary.vue"
      provides: "High-level health stats cards"
    - path: "memdiag-ui/src/components/diagnosis/DiagnosisIssueList.vue"
      provides: "Filtered issues accordion list"
  key_links:
    - from: "memdiag-ui/src/views/Diagnosis.vue"
      to: "memdiag-ui/src/composables/useDiagnosis.js"
    - from: "memdiag-ui/src/views/Diagnosis.vue"
      to: "memdiag-ui/src/components/diagnosis/DiagnosisSummary.vue"
---

<objective>
Componentize the Diagnosis view and implement severity filtering using PrimeVue SelectButton.

Purpose: Professionalize the UI, improve maintainability, and provide better user experience through filtering.
Output: Refactored Diagnosis.vue using sub-components and a severity filter.
</objective>

<execution_context>
@$HOME/.gemini/get-shit-done/workflows/execute-plan.md
@$HOME/.gemini/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@memdiag-ui/src/views/Diagnosis.vue
@memdiag-ui/src/composables/useDiagnosis.js
</context>

<tasks>

<task type="auto">
  <name>Task 1: Componentize Diagnosis view</name>
  <files>
    memdiag-ui/src/components/diagnosis/DiagnosisSummary.vue,
    memdiag-ui/src/components/diagnosis/DiagnosisIssueList.vue,
    memdiag-ui/src/components/diagnosis/DiagnosisIssueDetail.vue
  </files>
  <action>
    Extract components from `Diagnosis.vue`.
    - `DiagnosisSummary.vue`: Takes heap and thread stats as props.
    - `DiagnosisIssueDetail.vue`: Takes a single issue object as prop.
    - `DiagnosisIssueList.vue`: Takes the issues array, manages filtering, and uses `DiagnosisIssueDetail.vue`.
  </action>
  <verify>
    Files exist in `memdiag-ui/src/components/diagnosis/`.
  </verify>
  <done>Components created</done>
</task>

<task type="auto">
  <name>Task 2: Refactor Diagnosis.vue and implement Severity Filter</name>
  <files>memdiag-ui/src/views/Diagnosis.vue</files>
  <action>
    - Import and use `useDiagnosis.js`.
    - Replace raw HTML/JSX with new components.
    - Add a `SelectButton` for severity filtering above the issue list.
    - Options: [ {label: 'All', value: null}, {label: 'Critical', value: 'CRITICAL'}, {label: 'Warning', value: 'WARNING'}, {label: 'Info', value: 'INFO'} ].
    - Pass filtered issues to `DiagnosisIssueList.vue`.
  </action>
  <verify>
    Diagnosis.vue imports components and useDiagnosis.
  </verify>
  <done>Diagnosis.vue updated and filtering functional</done>
</task>

<task type="checkpoint:human-verify">
  <what-built>Refactored Diagnosis view with filtering</what-built>
  <how-to-verify>
    1. Start dev server: `npm run dev` in memdiag-ui.
    2. Open browser at `/diagnosis`.
    3. Run a diagnosis and verify:
       - Summary cards show correct data.
       - Severity buttons (Critical/Warning/Info) filter the list correctly.
       - "All" button shows everything.
       - Clicking an issue expands to show details.
  </how-to-verify>
  <resume-signal>approved</resume-signal>
</task>

</tasks>

<verification>
Verify filtering behavior and component modularity.
</verification>

<success_criteria>
- Diagnosis.vue is decoupled and cleaner.
- Severity filtering works as expected.
- All diagnosis data points are correctly displayed.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-5-health-views-enhancement/phase-5-02-SUMMARY.md`
</output>
