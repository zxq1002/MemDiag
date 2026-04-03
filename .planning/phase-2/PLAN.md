# Phase 2: Tech Debt Reduction

**Phase Goal:** Address technical debt and improve code quality by splitting `AnalysisService`, externalizing configuration, and ensuring all beans use constructor injection.

## Wave Structure

| Wave | Plan | Objective | Autonomous |
|------|------|-----------|------------|
| 1 | [phase-2-01-PLAN.md](./phase-2-01-PLAN.md) | Test Suite & Properties | yes |
| 2 | [phase-2-02-PLAN.md](./phase-2-02-PLAN.md) | ConnectionManager Extraction & Test | yes |
| 3 | [phase-2-03-PLAN.md](./phase-2-03-PLAN.md) | JmxAnalysisService Extraction & Test | yes |
| 4 | [phase-2-04-PLAN.md](./phase-2-04-PLAN.md) | AgentApiService Extraction & Test | yes |
| 4 | [phase-2-05-PLAN.md](./phase-2-05-PLAN.md) | SnapshotService Extraction & Test | yes |
| 4 | [phase-2-06-PLAN.md](./phase-2-06-PLAN.md) | GcRootsService Extraction & Test | yes |
| 5 | [phase-2-07-PLAN.md](./phase-2-07-PLAN.md) | ApiController Refactor & Test | yes |
| 6 | [phase-2-08-PLAN.md](./phase-2-08-PLAN.md) | Final Cleanup | yes |

## Requirements Covered

- **R-DEBT-001:** Constructor Injection (All plans)
- **R-DEBT-002:** Split AnalysisService (Plans 02-08)
- **R-DEBT-003:** Externalize Configuration (Plan 01)
- **R-TEST-001:** Web Layer Unit Tests (Plans 02-06)
- **R-TEST-002:** API Contract Tests (Plan 07)

## Execution Instructions

Run each plan in sequence (Waves 4 can be run in any order after Wave 3):

1. `/gsd:execute-phase phase-2-tech-debt --plan 01`
2. `/gsd:execute-phase phase-2-tech-debt --plan 02`
3. `/gsd:execute-phase phase-2-tech-debt --plan 03`
4. `/gsd:execute-phase phase-2-tech-debt --plan 04`
5. `/gsd:execute-phase phase-2-tech-debt --plan 05`
6. `/gsd:execute-phase phase-2-tech-debt --plan 06`
7. `/gsd:execute-phase phase-2-tech-debt --plan 07`
8. `/gsd:execute-phase phase-2-tech-debt --plan 08`

Each plan contains detailed tasks, file paths, and verification commands.
