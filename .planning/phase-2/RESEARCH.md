# Phase 2: Tech Debt Reduction - Research

**Researched:** 2026-04-03
**Domain:** Spring Boot Refactoring / Tech Debt
**Confidence:** HIGH

## Summary
The research phase for Tech Debt Reduction focused on auditing the `memdiag-web` module. The primary findings are:
1. **Field Injection:** A comprehensive grep audit confirmed that `@Autowired` on fields is no longer present in `memdiag-web`. Controllers and configuration classes are already using constructor injection.
2. **AnalysisService Audit:** The service is a 500-line "God Object" handling connection lifecycles, JMX analysis, Agent API proxying, Snapshot management, and GC Roots tracking. It is ripe for splitting.
3. **Configuration:** Multiple hardcoded values were identified, including port `6789`, update rates, and default data limits.
4. **Testing Gap:** The `memdiag-web` module completely lacks a `src/test` directory, representing a major risk for regressions during refactoring.

**Primary recommendation:** Initialize the test suite for `memdiag-web` before proceeding with the `AnalysisService` split to ensure functional parity.

## User Constraints (from CONTEXT.md / PLAN.md)
### Locked Decisions
- Refactor any remaining field injection to constructor injection.
- Split `AnalysisService` into focused services: `ConnectionManager`, `JmxAnalysisService`, `AgentApiService`, `SnapshotService`, `GcRootsService`.
- Externalize hardcoded configuration (Port 6789, Fixed rates).
- Achieve 80%+ coverage on the web layer.

## Standard Stack
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Spring Boot | 2.7.18 | Web Framework | Project Standard |
| JUnit 5 | 5.10.0 | Unit Testing | Industry Standard |
| AssertJ | 3.24.2 | Fluent Assertions | Preferred by team |
| Mockito | 4.5.1 | Mocking | Standard for Spring Boot 2.7 |

## Architecture Patterns
### Recommended Service Split
- **ConnectionManager:** Manages `ConcurrentHashMap` of JMX and Agent connections.
- **JmxAnalysisService:** Pure JMX-based analysis logic (Histogram, Threads, NMT).
- **AgentApiService:** Proxy/Wrapper for Agent HTTP endpoints (20+ methods).
- **SnapshotService:** Orchestrates data collection and delegates to `SnapshotManager`.
- **GcRootsService:** Handles Agent-specific GC Roots tracking.

## Don't Hand-Roll
| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Config Injection | Custom parser | `@ConfigurationProperties` | Type-safe, Spring-native, supports validation. |
| Testing Mocks | Custom mock classes | `@MockBean` / `Mockito` | Reduces boilerplate and ensures clean test isolation. |

## Common Pitfalls
### Pitfall 1: Breaking API Contract
- **What goes wrong:** Changing method signatures in `AnalysisService` breaks `ApiController` and `RealtimeController`.
- **Prevention:** Keep `AnalysisService` as a deprecated facade that delegates to new services, or refactor controllers first with comprehensive integration tests.

### Pitfall 2: Thread Safety in Connection Pools
- **What goes wrong:** Moving `ConcurrentHashMap` to a new service might introduce race conditions if the lifecycle isn't managed correctly.
- **Prevention:** Ensure `ConnectionManager` is a `@Service` singleton and connections are properly synchronized.

## Hardcoded Values Found
- `6789`: Default Agent Port (`AnalysisService.java:86`)
- `5000`: Real-time fixed rate (`RealtimeController.java:26`)
- `10`: Default limit for histograms in controllers and scheduled tasks.
- `1000`: Default limit for snapshot histograms.

## Validation Architecture
### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test |
| Quick run command | `mvn test -pl memdiag-web` |

### Wave 0 Gaps
- [ ] `memdiag-web/src/test/java` - Directory and package structure needs creation.
- [ ] `pom.xml` - Verify `spring-boot-starter-test` dependency.
- [ ] `AbstractControllerTest` - Base class for MockMvc setup.

## Sources
- **Direct Code Audit:** `AnalysisService.java`, `ApiController.java`, `RealtimeController.java`.
- **Grep Search:** Confirmed 0 occurrences of `@Autowired` in `memdiag-web`.
