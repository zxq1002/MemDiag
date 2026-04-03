# Phase 2: Tech Debt Reduction

**Phase Goal:** Address technical debt and improve code quality by splitting `AnalysisService`, externalizing configuration, and ensuring all beans use constructor injection.

## Wave Structure

| Wave | Plan | Objective | Autonomous |
|------|------|-----------|------------|
| 1 | [phase-2-01-PLAN.md](./phase-2-01-PLAN.md) | Test Suite & Properties | yes |
| 2 | [phase-2-02-PLAN.md](./phase-2-02-PLAN.md) | Core Refactoring (Connection/JMX) | yes |
| 3 | [phase-2-03-PLAN.md](./phase-2-03-PLAN.md) | Specialized Services & API Cleanup | yes |

## Requirements Covered

- **R-DEBT-001:** Constructor Injection (Plan 01, 02, 03)
- **R-DEBT-002:** Split AnalysisService (Plan 02, 03)
- **R-DEBT-003:** Externalize Configuration (Plan 01)
- **R-TEST-001:** Web Layer Unit Tests (Plan 01, 02, 03)
- **R-TEST-002:** API Contract Tests (Plan 03)

## Execution Instructions

Run each plan in sequence:

1. `/gsd:execute-phase phase-2-tech-debt --plan 01`
2. `/gsd:execute-phase phase-2-tech-debt --plan 02`
3. `/gsd:execute-phase phase-2-tech-debt --plan 03`

Each plan contains detailed tasks, file paths, and verification commands.
