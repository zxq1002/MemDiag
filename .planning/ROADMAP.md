# MemDiag Roadmap

**Date:** 2026-04-03
**Version:** 1.1
**Focus:** Bug Fixes + Web UI Enhancement

---

## Overview

This roadmap defines the phase structure for addressing known issues and enhancing the Web UI.

---

## Phase 1: Security Hardening

**Goal:** Fix high-priority security issues

**Duration:** 1 phase
**Priority:** High

**Tasks:**
1. Restrict CORS configuration to localhost
2. Add input validation for PID and agent addresses
3. Secure error messages (no stack traces in responses)

**Deliverables:**
- CORS properly configured
- Input validation in place
- Error messages sanitized

**Acceptance Criteria:**
- [x] CORS no longer allows all origins
- [x] Invalid inputs return 400 with clear messages
- [x] No exception details exposed to clients

---

## Phase 2: Tech Debt Reduction

**Goal:** Address technical debt and improve code quality

**Duration:** 1 phase
**Priority:** Medium

**Plans:** 3 plans
- [ ] phase-2-01-PLAN.md — Test Suite & Properties initialization
- [ ] phase-2-02-PLAN.md — Core Refactoring (Connection/JMX)
- [ ] phase-2-03-PLAN.md — Specialized Services & API Cleanup

**Deliverables:**
- Constructor injection used everywhere
- Single-responsibility services
- Configuration externalized
- 80%+ coverage on web layer

**Acceptance Criteria:**
- [ ] No @Autowired on fields
- [ ] Each service < 200 lines
- [ ] No hardcoded ports/timeouts
- [ ] ApiController contract verified with MockMvc

---

## Phase 3: Web UI Foundation

**Goal:** Improve UI foundation and connection management

**Duration:** 1 phase
**Priority:** High

**Tasks:**
1. Add connection status indicator
2. Improve error handling in UI
3. Add loading states for async operations

**Deliverables:**
- Real-time connection status display
- Better user feedback on errors
- Loading indicators

**Acceptance Criteria:**
- [ ] Connection status clearly visible
- [ ] Errors displayed to user in friendly way
- [ ] Loading states shown during operations

---

## Phase 4: Histogram View Enhancement

**Goal:** Improve heap histogram visualization and interaction

**Duration:** 1 phase
**Priority:** Medium

**Tasks:**
1. Add sortable columns
2. Add search/filter by class name
3. Add CSV/JSON export

**Deliverables:**
- Interactive histogram table
- Search/filter capabilities
- Data export functionality

**Acceptance Criteria:**
- [ ] Columns sortable by click
- [ ] Search filters results in real-time
- [ ] Export produces valid CSV/JSON

---

## Phase 5: Diagnosis & Threads Views

**Goal:** Enhance diagnosis results and thread analysis views

**Duration:** 1 phase
**Priority:** Medium

**Tasks:**
1. Severity-based coloring for diagnosis issues
2. Expandable sections for recommendations
3. Thread stack trace viewing
4. Thread state filtering

**Deliverables:**
- Enhanced diagnosis display
- Full thread stack trace view
- Thread filtering capabilities

**Acceptance Criteria:**
- [ ] Issues colored by severity
- [ ] Can see full thread stacks
- [ ] Can filter threads by state

---

## Phase 6: Snapshot Comparison & NMT

**Goal:** Add snapshot diff visualization and improve NMT view

**Duration:** 1 phase
**Priority:** Low

**Tasks:**
1. Side-by-side snapshot comparison
2. Growth/shrinkage indicators
3. NMT pie chart visualization
4. NMT timeline view

**Deliverables:**
- Snapshot comparison UI
- Enhanced NMT visualization

**Acceptance Criteria:**
- [ ] Can compare two snapshots side-by-side
- [ ] NMT data visualized with charts

---

## Phase 7: Testing & Validation (Refined)

**Goal:** Validate all changes and finalize test suite

**Duration:** 1 phase
**Priority:** High

**Tasks:**
1. End-to-end integration tests
2. Performance validation
3. Final documentation update

**Deliverables:**
- Full validation report
- Completed documentation

**Acceptance Criteria:**
- [ ] All features work together correctly
- [ ] Performance meets NFRs

---

## Milestone: Bug Fixes + Web UI Complete

Upon completion of all phases:
- All high-priority security issues fixed
- Tech debt reduced
- Web UI significantly enhanced
- Proper test coverage in place

---

## Dependencies

- Phase 1 must complete before Phase 2 (some refactoring depends on secure foundations)
- Phase 3 must complete before Phases 4-6 (UI foundation needed)
- Phase 7 runs after all feature phases complete

---

*Roadmap updated: 2026-04-03*
