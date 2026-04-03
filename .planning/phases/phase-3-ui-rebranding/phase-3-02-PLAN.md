---
phase: phase-3-ui-rebranding
plan: 02
type: execute
wave: 2
depends_on: [01]
files_modified:
  - memdiag-ui/src/stores/connectionStore.js
  - memdiag-ui/src/components/ConnectionIndicator.vue
  - memdiag-ui/src/components/Layout.vue
  - memdiag-ui/src/App.vue
autonomous: true
requirements: [R-UI-001]
must_haves:
  truths:
    - "User can see real-time connection status in the UI"
    - "UI provides a modern layout with Sidebar, Topbar, and Main area"
  artifacts:
    - path: "memdiag-ui/src/stores/connectionStore.js"
      provides: "Global state for WebSocket connection"
    - path: "memdiag-ui/src/components/Layout.vue"
      provides: "Application shell layout"
  key_links:
    - from: "memdiag-ui/src/components/ConnectionIndicator.vue"
      to: "memdiag-ui/src/stores/connectionStore.js"
      via: "Pinia store usage"
---

<objective>
Establish global state for connections and error handling, implement the Connection Status Indicator (R-UI-001), and create the modern layout components.
Purpose: Provide users with clear feedback on connection health and a structured navigation experience.
Output: `connectionStore.js`, `ConnectionIndicator.vue`, and a new `Layout.vue` wrapper.
</objective>

<context>
@.planning/phases/phase-3-ui-rebranding/03-RESEARCH.md
</context>

<tasks>

<task type="auto">
  <name>Task 1: Global State for Connection</name>
  <files>memdiag-ui/src/stores/connectionStore.js</files>
  <action>
    Create a Pinia store `useConnectionStore`. Use `@vueuse/core`'s `useWebSocket` to manage the WebSocket connection to the backend (`/ws` or appropriate endpoint). Ensure `autoReconnect: true` and `heartbeat: true` are configured. Expose `status` and `isOnline` properties.
  </action>
  <verify>
    <automated>cd memdiag-ui && npx vitest run --passWithNoTests</automated>
  </verify>
  <done>Store is created and properly utilizes useWebSocket.</done>
</task>

<task type="auto">
  <name>Task 2: Layout & Connection Indicator</name>
  <files>memdiag-ui/src/components/ConnectionIndicator.vue, memdiag-ui/src/components/Layout.vue, memdiag-ui/src/App.vue</files>
  <action>
    1. Create `ConnectionIndicator.vue` that reads from `useConnectionStore` and displays a colored dot (green/red) and status text based on `isOnline`.
    2. Create `Layout.vue` containing a Topbar (housing the `ConnectionIndicator`), a Sidebar (for navigation), and a Main content area. Style using Tailwind CSS.
    3. Update `App.vue` to wrap `<router-view>` within `<Layout>`.
  </action>
  <verify>
    <automated>cd memdiag-ui && npm run build</automated>
  </verify>
  <done>Layout components exist and App.vue is updated to use them.</done>
</task>

</tasks>

<verification>
Start the UI development server and verify the layout renders and the connection indicator correctly reflects the store state.
</verification>

<success_criteria>
Modern layout is in place and global connection state is visible to the user.
</success_criteria>
