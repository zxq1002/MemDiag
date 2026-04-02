# Testing Patterns

**Analysis Date:** 2026-04-02

## Test Framework

**Runner:**
- Java: JUnit Jupiter 5.10.0
- Config: Maven Surefire Plugin 3.0.0 (unit tests), Maven Failsafe Plugin 3.0.0 (integration tests)

**Assertion Library:**
- Java: AssertJ 3.24.2 - Fluent assertions

**Run Commands:**
```bash
mvn test              # Run all unit tests
mvn verify            # Run integration tests
mvn -pl memdiag-core test  # Run tests in specific module
```

## Test File Organization

**Location:**
- Java: Co-located with source code in `src/test/java/`
- Follows same package structure as main source

**Naming:**
- Unit tests: `{ClassUnderTest}Test.java` (e.g., `ResourceLimiterTest.java`, `HeapDiffTest.java`)
- Integration tests: `{Feature}IntegrationTest.java` or `{Feature}IT.java`

**Structure:**
```
memdiag-core/
├── src/
│   ├── main/java/com/memdiag/core/
│   │   ├── util/ResourceLimiter.java
│   │   └── diff/HeapDiff.java
│   └── test/java/com/memdiag/core/
│       ├── util/ResourceLimiterTest.java
│       ├── diff/HeapDiffTest.java
│       ├── FunctionalTestBase.java
│       └── integration/
│           └── CoreAnalysisIntegrationTest.java
```

## Test Structure

**Suite Organization:**
```java
class ResourceLimiterTest {

    @Test
    void executeWithinTimeout() {
        ResourceLimiter limiter = new ResourceLimiter(
            64 * 1024 * 1024,
            Duration.ofSeconds(30),
            Duration.ofMillis(500)
        );

        String result = limiter.executeWithLimit(() -> "success");
        assertThat(result).isEqualTo("success");
    }
}
```

**Patterns:**
- Setup: Use `@BeforeEach` for common test data (e.g., `HeapDiffTest.setUp()`)
- No explicit teardown pattern observed
- Assertion: Fluent AssertJ style (e.g., `assertThat(result).isEqualTo("success")`)

**Integration Test Tags:**
- Use `@Tag("integration")` for integration tests (e.g., `CoreAnalysisIntegrationTest`)

## Mocking

**Framework:** Not observed in current test suite

**Patterns:**
- Testability achieved through package-private constructors (e.g., `ResourceLimiter` has a package-private constructor for injecting `MemoryMXBean`)
- Real dependencies used in tests

**What to Mock:**
- Currently no mocking used - tests use real implementations

## Fixtures and Factories

**Test Data:**
```java
private Snapshot createTestSnapshot(String id) {
    HeapHistogram histogram = new HeapHistogram();
    histogram.add(new ClassStats("java.lang.String", 1000, 64000));

    return new Snapshot.Builder()
            .setId(id)
            .setTimestamp(Instant.now())
            .setHeapHistogram(histogram)
            .build();
}
```

**Location:**
- Helper methods in test classes (e.g., `HeapDiffTest.createTestSnapshot()`)
- No separate fixture/factory classes

## Coverage

**Requirements:** Not enforced in current configuration

**View Coverage:**
- No coverage plugin configured (JaCoCo not detected)

## Test Types

**Unit Tests:**
- Scope: Individual classes and methods
- Location: `src/test/java/`
- Naming: `*Test.java`
- Examples: `ResourceLimiterTest.java`, `HeapDiffTest.java`

**Integration Tests:**
- Scope: Multiple components working together, JMX integration
- Location: `src/test/java/.../integration/`
- Naming: `*IntegrationTest.java`
- Tagged with `@Tag("integration")`
- Use real JVM for testing (attach to current JVM via JMX)
- Example: `CoreAnalysisIntegrationTest.java`

**E2E Tests:**
- Not detected

## Common Patterns

**Testing Current JVM:**
```java
@Test
void jmxClientConnectsToCurrentJvm() {
    JmxClient client = JmxClient.attachToCurrentJvm();
    assertThat(client).isNotNull();
    assertThat(client.getConnection()).isNotNull();
}
```

**Package-Private Constructors for Testing:**
- Test-only constructors with dependency injection
- Allows testing without mocking frameworks
- Example: `ResourceLimiter` has a package-private constructor accepting `MemoryMXBean`

**Error Testing:**
- Not explicitly observed in current test suite
- Exception testing would use AssertJ's exception assertions

**Testing Immutable Objects:**
- Use Builder pattern in tests
- Assert state through getter methods

---

*Testing analysis: 2026-04-02*
