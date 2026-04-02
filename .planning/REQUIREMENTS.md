# MemDiag Requirements

**Date:** 2026-04-02
**Version:** 1.0
**Scope:** Bug Fixes + Web UI Enhancement

---

## 1. Overview

This document defines the requirements for the current phase of MemDiag development, focusing on fixing known issues and enhancing the Web UI.

---

## 2. Bug Fix Requirements

### 2.1 Security Fixes (High Priority)

**R-SEC-001: Restrict CORS Configuration**
- **Description:** Currently allows all origins (`*`), which is insecure
- **Requirements:**
  - Restrict allowed origins to `localhost` and specific trusted domains
  - Remove `@CrossOrigin(origins = "*")` from controllers
  - Make CORS configuration externalized via properties
- **Acceptance Criteria:**
  - Only allowed origins can access the API
  - Configuration can be changed without recompilation

**R-SEC-002: Add Input Validation**
- **Description:** PID and agent address inputs are not validated
- **Requirements:**
  - Validate PID format (numeric only, valid range)
  - Validate agent address format
  - Add `@Validated` and validation annotations to controllers
- **Acceptance Criteria:**
  - Invalid inputs return 400 Bad Request
  - Clear error messages for validation failures

**R-SEC-003: Secure Error Messages**
- **Description:** Exception details are exposed to API clients
- **Requirements:**
  - Return generic error messages in API responses
  - Log detailed errors on server-side only
  - Create consistent error response format
- **Acceptance Criteria:**
  - No stack traces in API responses
  - All errors logged with full context

### 2.2 Tech Debt Fixes (Medium Priority)

**R-DEBT-001: Constructor Injection**
- **Description:** Spring beans use field injection instead of constructor injection
- **Requirements:**
  - Refactor `ApiController` to use constructor injection
  - Refactor `RealtimeController` to use constructor injection
- **Acceptance Criteria:**
  - No `@Autowired` on fields
  - All dependencies injected via constructor

**R-DEBT-002: Split AnalysisService**
- **Description:** `AnalysisService` is 500+ lines with multiple responsibilities
- **Requirements:**
  - Split into focused services:
    - Connection management
    - JMX analysis
    - Agent communication
    - Snapshot management
- **Acceptance Criteria:**
  - Each service < 200 lines
  - Single responsibility principle followed

**R-DEBT-003: Externalize Configuration**
- **Description:** Ports, timeouts, and limits are hardcoded
- **Requirements:**
  - Move hardcoded port (6789) to configuration
  - Move fixed rate (5000ms) to configuration
  - Add configuration properties file with defaults
- **Acceptance Criteria:**
  - No hardcoded configuration values
  - All configurable via `application.properties`

---

## 3. Web UI Enhancement Requirements

### 3.1 UI/UX Improvements

**R-UI-001: Connection Status Indicator**
- **Description:** Show real-time connection status to target JVM
- **Requirements:**
  - Visual indicator (green/red/yellow) showing connection state
  - Display connection details (PID/address, connection mode)
  - Auto-reconnect with exponential backoff
- **Acceptance Criteria:**
  - User always knows if connected
  - Clear feedback on connection failures

**R-UI-002: Enhanced Histogram View**
- **Description:** Improve heap histogram visualization
- **Requirements:**
  - Sortable columns (class name, count, size)
  - Search/filter by class name
  - Toggle between object count and shallow size views
  - Export histogram data as CSV/JSON
- **Acceptance Criteria:**
  - Easy to find and analyze specific classes
  - Data can be exported for offline analysis

**R-UI-003: Diagnosis Results Display**
- **Description:** Better visualization of diagnostic issues
- **Requirements:**
  - Severity-based coloring (CRITICAL/WARNING/INFO)
  - Expandable sections for details and recommendations
  - Filter by severity level
  - Actionable recommendations with clear steps
- **Acceptance Criteria:**
  - Issues prioritized by severity
  - Recommendations are clear and actionable

**R-UI-004: Snapshot Comparison**
- **Description:** Visual diff between two heap snapshots
- **Requirements:**
  - Side-by-side comparison view
  - Growth/shrinkage indicators
  - Percentage change display
  - Sort by growth rate
- **Acceptance Criteria:**
  - Easy to identify growing classes
  - Clear visualization of changes

### 3.2 Feature Completeness

**R-UI-005: Thread View Enhancement**
- **Description:** Add thread stack trace viewing
- **Requirements:**
  - Thread list with state indicators
  - Click to view full stack trace
  - Filter by thread state (RUNNABLE, WAITING, etc.)
  - Search by thread name
- **Acceptance Criteria:**
  - Can view complete thread stacks
  - Easy to filter and find threads

**R-UI-006: NMT Visualization**
- **Description:** Better Native Memory Tracking visualization
- **Requirements:**
  - Pie chart for memory category breakdown
  - Timeline view showing memory changes
  - Detailed breakdown by category
- **Acceptance Criteria:**
  - NMT data is easily understandable
  - Trends visible over time

---

## 4. Testing Requirements

**R-TEST-001: Web Layer Unit Tests**
- **Description:** Add unit tests for controllers and services
- **Requirements:**
  - Unit tests for `ApiController`
  - Unit tests for split services
  - Mock external dependencies
- **Acceptance Criteria:**
  - 80%+ coverage on web layer

**R-TEST-002: API Contract Tests**
- **Description:** Verify API endpoints behave consistently
- **Requirements:**
  - Integration tests for all API endpoints
  - Verify request/response formats
  - Error scenario tests
- **Acceptance Criteria:**
  - All API endpoints covered
  - Breaking changes caught by tests

---

## 5. Non-Functional Requirements

**R-NFR-001: Performance**
- Page load time < 2 seconds
- API response time < 500ms (non-analysis endpoints)
- Real-time updates at 5s intervals

**R-NFR-002: Browser Support**
- Latest Chrome, Firefox, Safari, Edge
- Responsive design for desktop (mobile optional)

**R-NFR-003: Accessibility**
- Keyboard navigation support
- Semantic HTML
- ARIA labels where needed

---

## 6. Out of Scope

- Authentication/authorization (deferred to later phase)
- Spring Boot 3.x upgrade (deferred)
- Mobile-responsive design
- New diagnostic rules
- Native memory tracking deep dive

---

*Requirements document: 2026-04-02*
