---
phase: phase-5-health-views-enhancement
plan: 03
type: execute
wave: 2
depends_on: ["phase-5-01"]
files_modified: [
  "memdiag-ui/src/components/StackTraceViewer.vue",
  "memdiag-ui/src/components/threads/ThreadsSummary.vue",
  "memdiag-ui/src/components/threads/ThreadsTable.vue",
  "memdiag-ui/src/views/Threads.vue"
]
autonomous: false
requirements: [R-UI-005]

must_haves:
  truths:
    - "Thread stack traces are syntax-highlighted (colorized className, methodName, line numbers)"
    - "Threads can be filtered by state (RUNNABLE, BLOCKED, etc.)"
    - "Search and filtering work together in real-time"
  artifacts:
    - path: "memdiag-ui/src/components/StackTraceViewer.vue"
      provides: "Syntax-highlighted stack trace renderer"
    - path: "memdiag-ui/src/components/threads/ThreadsTable.vue"
      provides: "Threads table with expandable stack trace"
  key_links:
    - from: "memdiag-ui/src/views/Threads.vue"
      to: "memdiag-ui/src/composables/useThreads.js"
    - from: "memdiag-ui/src/components/threads/ThreadsTable.vue"
      to: "memdiag-ui/src/components/StackTraceViewer.vue"
---

<objective>
Refactor the Threads view with sub-components, add state-based filtering, and implement a dedicated syntax-highlighted StackTraceViewer.

Purpose: Provide a professional tool for thread analysis, making stack traces readable and filtering efficient.
Output: Refactored Threads.vue using new components and enhanced filtering capabilities.
</objective>

<execution_context>
@$HOME/.gemini/get-shit-done/workflows/execute-plan.md
@$HOME/.gemini/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@memdiag-ui/src/views/Threads.vue
@memdiag-ui/src/composables/useThreads.js
</context>

<tasks>

<task type="auto">
  <name>Task 1: Create StackTraceViewer.vue</name>
  <files>memdiag-ui/src/components/StackTraceViewer.vue</files>
  <action>
    Create a component that:
    - Takes `stackTrace` (Array) as a prop.
    - Renders each frame with regex-based syntax highlighting:
      - Class names in a specific color.
      - Method names in a different color.
      - File names and line numbers in contrasting colors.
    - Uses a dark background and mono-space font.
  </action>
  <verify>
    Component exists and uses regex for highlighting.
  </verify>
  <done>StackTraceViewer created</done>
</task>

<task type="auto">
  <name>Task 2: Componentize Threads view and refactor</name>
  <files>
    memdiag-ui/src/components/threads/ThreadsSummary.vue,
    memdiag-ui/src/components/threads/ThreadsTable.vue,
    memdiag-ui/src/views/Threads.vue
  </files>
  <action>
    - Extract `ThreadsSummary.vue` and `ThreadsTable.vue`.
    - Refactor `Threads.vue` to use `useThreads.js` and new components.
    - Add state filtering (Dropdown/SelectButton) and name search.
  </action>
  <verify>
    Threads.vue imports sub-components and filtering is functional.
  </verify>
  <done>Threads.vue refactored</done>
</task>

<task type="checkpoint:human-verify">
  <what-built>Refactored Threads view with state filtering and syntax-highlighted stack traces</what-built>
  <how-to-verify>
    1. Open Threads view.
    2. Expand a row and check stack trace highlighting.
    3. Verify state filtering (RUNNABLE, BLOCKED, etc.) works.
  </how-to-verify>
  <resume-signal>approved</resume-signal>
</task>

</tasks>

<verification>
Check for UI responsiveness and correct data flow.
</verification>

<success_criteria>
- Stack traces are readable.
- Thread filtering is functional.
- Architecture is clean.
</success_criteria>

<output>
After completion, create `.planning/phases/phase-5-health-views-enhancement/phase-5-03-SUMMARY.md`
</output>
