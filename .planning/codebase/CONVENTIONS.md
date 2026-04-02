# Coding Conventions

**Analysis Date:** 2026-04-02

## Naming Patterns

**Files:**
- Java: PascalCase for class names (e.g., `ResourceLimiter.java`, `Snapshot.java`)
- Vue: PascalCase for component files (e.g., `Dashboard.vue`, `App.vue`)
- JavaScript: camelCase for utility files (e.g., `index.js`)

**Functions:**
- Java: camelCase (e.g., `executeWithLimit()`, `getLastSafePointDuration()`)
- JavaScript/Vue: camelCase (e.g., `loadConnections()`, `connect()`)

**Variables:**
- Java: camelCase (e.g., `maxMemoryBytes`, `analysisTimeout`)
- JavaScript/Vue: camelCase (e.g., `connections`, `newConnId`)
- Constants: UPPER_SNAKE_CASE in Java (e.g., `serialVersionUID`)

**Types:**
- Java: PascalCase for classes and interfaces (e.g., `ResourceLimiter`, `ReportFormatter`)
- Enums: PascalCase (e.g., `Severity`)

## Code Style

**Formatting:**
- Java: 4-space indentation, braces on same line
- JavaScript/Vue: 2-space indentation
- No explicit formatter configuration detected, but code follows consistent patterns

**Linting:**
- No ESLint/Prettier configuration detected in UI module
- No Checkstyle/SpotBugs configuration detected in Java modules

## Import Organization

**Java:**
- Grouped by package: standard library first, then external dependencies, then project imports
- Static imports separate (e.g., `import static org.assertj.core.api.Assertions.*;`)

**JavaScript:**
- Third-party imports first (e.g., `import axios from 'axios'`)
- Relative imports next (e.g., `import Dashboard from '../views/Dashboard.vue'`)

## Error Handling

**Patterns:**
- Java: Throw custom exceptions (e.g., `ResourceLimitExceededException`)
- Try-catch with specific exception handling
- Cleanup in finally blocks (e.g., `executor.shutdownNow()`)
- API endpoints wrap exceptions in JSON error responses with `success: false`

**JavaScript/Vue:**
- Try-catch with async/await
- Errors logged to console (e.g., `console.error('Failed to load connections:', e)`)

## Logging

**Framework:**
- Java: `System.err.println()` for warnings (simple approach)
- JavaScript: `console.error()` for error logging

**Patterns:**
- Warnings recorded but not thrown (e.g., safe point duration warnings)
- Error messages include context (e.g., `"Analysis timed out after " + analysisTimeout.toMillis() + "ms"`)

## Comments

**When to Comment:**
- Explain non-obvious design decisions
- Mark test-only constructors (e.g., `// 用于测试的构造函数`)
- Section markers in controllers (e.g., `// ========== Connection Management ==========`)

**JSDoc/TSDoc:**
- Java: Minimal Javadoc usage
- JavaScript: No JSDoc comments detected

**Language:**
- Comments in Chinese (preference per user config)

## Function Design

**Size:**
- Generally small, focused functions
- Some larger controller methods due to API response building

**Parameters:**
- Constructor overloads for testing (package-private constructors with additional dependencies)
- Use of `Supplier<T>` for task execution

**Return Values:**
- Immutable objects preferred
- Builder pattern for complex objects (e.g., `Snapshot.Builder`)

## Module Design

**Exports:**
- JavaScript: Default exports for Vue components
- Named exports for router configuration

**Barrel Files:**
- Not used

## Design Patterns Used

**Builder Pattern:**
- `Snapshot.Builder` - Immutable object construction
- Located at: `memdiag-core/src/main/java/com/memdiag/core/diff/Snapshot.java`

**Strategy Pattern:**
- `ReportFormatter` interface with multiple implementations (Text, JSON, HTML)
- Located at: `memdiag-core/src/main/java/com/memdiag/core/output/`

---

*Convention analysis: 2026-04-02*
