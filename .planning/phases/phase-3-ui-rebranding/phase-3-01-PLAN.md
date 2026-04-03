---
phase: phase-3-ui-rebranding
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - memdiag-ui/package.json
  - memdiag-ui/src/main.js
  - memdiag-ui/src/style.css
  - memdiag-ui/vite.config.js
  - memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java
autonomous: true
requirements: [R-UI-001]
must_haves:
  truths:
    - "Application boots with PrimeVue and Tailwind CSS 4 without errors"
    - "Backend API accepts 'pid' or 'target' interchangeably for connection"
  artifacts:
    - path: "memdiag-ui/package.json"
      provides: "New dependencies"
    - path: "memdiag-ui/src/style.css"
      provides: "Tailwind and PrimeUI plugin as per Research"
  key_links:
    - from: "memdiag-ui/src/main.js"
      to: "PrimeVue and Pinia"
      via: "app.use()"
---

<objective>
Initialize the modern frontend stack (Pinia, PrimeVue 4, Tailwind 4) and fix the API parameter mismatch between the frontend and backend.
Purpose: Lay the foundation for the UI rebranding and ensure the frontend can connect to the backend properly.
Output: Configured frontend build and patched backend controller.
</objective>

<context>
@.planning/phases/phase-3-ui-rebranding/03-RESEARCH.md
</context>

<tasks>

<task type="auto">
  <name>Task 1: Install and configure modern stack</name>
  <files>memdiag-ui/package.json, memdiag-ui/src/main.js, memdiag-ui/src/style.css, memdiag-ui/vite.config.js</files>
  <action>
    Install `pinia`, `primevue`, `@primeuix/themes`, `@vueuse/core`, `lucide-vue-next`.
    Install dev dependencies `tailwindcss`, `@tailwindcss/vite`, `vitest`, `@vue/test-utils`, `jsdom`.
    Configure `vite.config.js` with `@tailwindcss/vite`.
    Update `style.css` to import Tailwind and PrimeUI plugin as per Research.
    Update `main.js` to `app.use(createPinia())` and `app.use(PrimeVue)`.
  </action>
  <verify>
    <automated>cd memdiag-ui && npm install && npm run build</automated>
  </verify>
  <done>Build completes successfully with new dependencies configured.</done>
</task>

<task type="auto">
  <name>Task 2: Fix API Parameter Mismatch</name>
  <files>memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java</files>
  <action>
    Modify the connection endpoints in `ApiController.java` to accept `pid` as an alias or update it to consistently use `target` to match the UI. If possible, add metadata to `/api/v1/connections` responses (e.g., connected agent version or status).
  </action>
  <verify>
    <automated>mvn -f memdiag-web/pom.xml test -Dtest=ApiControllerIntegrationTest</automated>
  </verify>
  <done>API successfully handles connection requests without parameter mismatch errors.</done>
</task>

</tasks>

<verification>
Ensure the frontend builds and the backend tests pass.
</verification>

<success_criteria>
Stack is initialized and the API parameter mismatch is resolved.
</success_criteria>
