# Phase 2: Tech Debt Reduction - Validation Strategy

## Overview
This document defines the validation architecture for Phase 2 (Tech Debt Reduction). It maps research findings to specific test requirements and ensures that refactoring does not introduce regressions in the `memdiag-web` module.

## Research Baseline (from RESEARCH.md)
- **Problem:** `memdiag-web` completely lacks a `src/test` directory.
- **Risk:** High potential for functional regression during `AnalysisService` split and configuration externalization.
- **Goal:** Initialize test foundation and achieve 80%+ coverage on the web layer.

## Validation Architecture

### 1. Test Framework
- **Framework:** JUnit 5 + Spring Boot Test + MockMvc
- **Mocking:** Mockito (`@MockBean`)
- **Assertions:** AssertJ
- **Build Tool:** Maven (`mvn test -pl memdiag-web`)

### 2. Wave 0 Gaps (MANDATORY Foundations)
Before any business logic refactoring, the following must exist:
- [ ] `memdiag-web/src/test/java/com/memdiag/web/controller/AbstractControllerTest.java` - Base class with `MockMvc` setup.
- [ ] `pom.xml` verification - Ensure `spring-boot-starter-test` is present in `memdiag-web`.

### 3. Verification Gaps by Requirement

| Requirement | Truth to Verify | Validation Method |
|-------------|-----------------|-------------------|
| **R-DEBT-001** (Constructor Injection) | All `@Autowired` fields removed; all dependencies injected via constructor. | `grep -r "@Autowired" memdiag-web/src/main/java` (should find only on methods/ctors if needed, preferably none on fields). |
| **R-DEBT-003** (Externalize Config) | Port 6789, rates, and limits are configurable via `application.properties`. | Integration test verifying `MemDiagProperties` values are correctly bound from properties file. |
| **R-TEST-001** (Web Test Suite) | `memdiag-web` has 80%+ coverage and tests run in CI. | `mvn jacoco:report -pl memdiag-web` (if Jacoco is configured) or `mvn test -pl memdiag-web`. |

## Execution Checklist for Plans

### Wave 1: Foundation (Plan 01)
- [ ] Create `AbstractControllerTest.java`.
- [ ] Verify `mvn test-compile -pl memdiag-web` passes.

### Wave 2+: Implementation (Plans 02-03)
- [ ] Every new service split from `AnalysisService` must have a corresponding unit test.
- [ ] Every controller must have an integration test extending `AbstractControllerTest`.

## Continuous Verification Command
```bash
# Run all tests in the web module
mvn test -pl memdiag-web
```
