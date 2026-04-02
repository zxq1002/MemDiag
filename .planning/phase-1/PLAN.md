# Phase 1: Security Hardening

**Phase:** 1
**Goal:** Fix high-priority security issues
**Priority:** High
**Created:** 2026-04-02

---

## Overview

This phase addresses three critical security issues identified in the codebase:
1. Restrict CORS configuration (currently open to all origins)
2. Add input validation for PID and agent addresses
3. Secure error messages (no stack traces in responses)

---

## Context

### Current State

**CORS Configuration:**
- File: `memdiag-web/src/main/resources/application.properties`
- Current: `spring.mvc.cors.allowed-origins=*`
- Also: `@CrossOrigin(origins = "*")` on `ApiController`

**Input Validation:**
- PID and agent address inputs are not validated
- No `@Validated` or validation annotations on controllers
- Risk of path traversal or injection attacks

**Error Messages:**
- Exception messages returned directly to API clients
- Example: `return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())))`
- Risk of exposing internal system details

---

## Tasks

### Task 1: Restrict CORS Configuration

**Description:** Update CORS configuration to only allow localhost and specific trusted domains.

**Files to Modify:**
1. `memdiag-web/src/main/resources/application.properties`
2. `memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java`

**Steps:**

- [ ] **Step 1.1: Update application.properties**
  - Replace `spring.mvc.cors.allowed-origins=*` with `http://localhost:8080,http://127.0.0.1:8080`
  - Add `spring.mvc.cors.allow-credentials=true`
  - Keep methods and headers configuration

- [ ] **Step 1.2: Remove @CrossOrigin annotation**
  - Remove `@CrossOrigin(origins = "*")` from `ApiController` class
  - Let Spring Boot's global CORS configuration handle it

- [ ] **Step 1.3: Add CORS configuration properties class** (optional, for flexibility)
  - Create `CorsProperties.java` with `allowedOrigins`, `allowedMethods`, `allowedHeaders`
  - Use `@ConfigurationProperties(prefix = "memdiag.cors")`
  - Make configurable via properties file

- [ ] **Step 1.4: Create CorsConfig.java** (if more control needed)
  - Alternatively, create a proper `WebMvcConfigurer` bean
  - Use `CorsConfigurationSource` for fine-grained control
  - Support comma-separated origin list

**Verification:**
- [ ] CORS headers only allow localhost origins
- [ ] Configuration can be changed without recompilation
- [ ] Existing functionality still works from localhost

---

### Task 2: Add Input Validation

**Description:** Validate PID format (numeric only) and agent address format.

**Files to Modify:**
1. `memdiag-web/pom.xml` (add validation dependencies)
2. `memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java`
3. Create validation utility class

**Steps:**

- [ ] **Step 2.1: Add validation dependencies**
  - Add `spring-boot-starter-validation` to `memdiag-web/pom.xml`
  - Includes Hibernate Validator

- [ ] **Step 2.2: Create PidValidator utility**
  - Create `memdiag-web/src/main/java/com/memdiag/web/validation/PidValidator.java`
  - Validate PID format: numeric only, 1-65535 range
  - Return `boolean isValid(String pid)`
  - Provide `String getErrorMessage()` for validation failures

- [ ] **Step 2.3: Create AddressValidator utility**
  - Create `memdiag-web/src/main/java/com/memdiag/web/validation/AddressValidator.java`
  - Validate agent address format (host:port)
  - Validate hostname/IP format
  - Validate port range (1-65535)
  - Prevent malicious addresses

- [ ] **Step 2.4: Add @Validated to ApiController**
  - Add `@Validated` annotation to `ApiController` class

- [ ] **Step 2.5: Add validation to PID path variables**
  - For all endpoints with `@PathVariable String id`:
    - Validate before use in `AnalysisService`
    - Use `PidValidator` to check format
    - Return 400 Bad Request with clear message if invalid
  - Affected endpoints:
    - `/api/v1/connections/{id}` (POST, DELETE)
    - `/api/v1/histogram/{id}`
    - `/api/v1/diagnose/{id}`
    - `/api/v1/threads/{id}`
    - `/api/v1/nmt/{id}`
    - All other `/{id}` endpoints

- [ ] **Step 2.6: Add validation to target parameter**
  - For `connect()` endpoint's `@RequestParam String target`:
    - Validate using `AddressValidator`
    - Return 400 Bad Request if invalid

- [ ] **Step 2.7: Create consistent validation error response**
  - Update `errorResponse()` method to include validation details
  - Use consistent format: `{success: false, error: "message", timestamp: ...}`

**Verification:**
- [ ] Invalid PID returns 400 with clear message
- [ ] Invalid agent address returns 400 with clear message
- [ ] Valid inputs continue to work
- [ ] No numeric-only restriction for connection IDs (they can be user-defined labels)

---

### Task 3: Secure Error Messages

**Description:** Remove exception details from API responses, log detailed errors on server.

**Files to Modify:**
1. `memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java`
2. Create `GlobalExceptionHandler.java` (optional)
3. Add SLF4J logging

**Steps:**

- [ ] **Step 3.1: Add SLF4J logger to ApiController**
  - Add `private static final Logger logger = LoggerFactory.getLogger(ApiController.class);`
  - Import `org.slf4j.Logger` and `org.slf4j.LoggerFactory`

- [ ] **Step 3.2: Create generic error messages**
  - Define constant error messages:
    - "Failed to retrieve histogram"
    - "Failed to perform diagnosis"
    - "Failed to retrieve threads"
    - "Connection failed"
    - "An error occurred while processing your request"

- [ ] **Step 3.3: Update each catch block**
  - Replace `e.getMessage()` with generic message in API response
  - Log full exception at ERROR level: `logger.error("Error processing request", e);`
  - Update all 20+ catch blocks in `ApiController`

- [ ] **Step 3.4: Create GlobalExceptionHandler** (optional, recommended)
  - Create `memdiag-web/src/main/java/com/memdiag/web/exception/GlobalExceptionHandler.java`
  - Annotate with `@RestControllerAdvice`
  - Handle generic `Exception.class`
  - Handle `IllegalArgumentException.class` specifically
  - Return consistent error response format
  - Log all exceptions with full stack trace

- [ ] **Step 3.5: Update RealtimeController logging**
  - Add logger to `RealtimeController`
  - Log exceptions in `sendRealtimeUpdates()` instead of silently ignoring
  - Use `logger.warn("Error sending update for connection " + id, e);`

**Verification:**
- [ ] No exception messages in API responses
- [ ] All errors logged with full stack trace on server
- [ ] Generic error messages returned to clients
- [ ] Error response format remains consistent

---

## Dependencies

- No dependencies on other phases - this phase can be executed independently
- All tasks are independent of each other but should be completed together for full security hardening

---

## Acceptance Criteria

**Security:**
- [ ] CORS no longer allows all origins
- [ ] Invalid inputs return 400 with clear messages
- [ ] No exception details exposed to clients

**Functionality:**
- [ ] All existing API endpoints continue to work with valid inputs
- [ ] Web UI still functions from localhost
- [ ] Error responses maintain consistent JSON format

**Maintainability:**
- [ ] CORS configuration externalized via properties
- [ ] Validation logic in reusable utility classes
- [ ] Proper logging in place for debugging

---

## Test Plan

**Unit Tests:**
- [ ] Test `PidValidator` with valid and invalid PIDs
- [ ] Test `AddressValidator` with valid and invalid addresses

**Integration Tests:**
- [ ] Test valid PID returns 200 OK
- [ ] Test invalid PID returns 400 Bad Request
- [ ] Test CORS headers only allow localhost
- [ ] Test error response doesn't contain exception message

**Manual Verification:**
- [ ] Test Web UI still works from `http://localhost:8080`
- [ ] Verify no stack traces in browser dev tools Network tab
- [ ] Verify errors are logged in server logs

---

## Files to Create

1. `memdiag-web/src/main/java/com/memdiag/web/validation/PidValidator.java`
2. `memdiag-web/src/main/java/com/memdiag/web/validation/AddressValidator.java`
3. `memdiag-web/src/main/java/com/memdiag/web/exception/GlobalExceptionHandler.java` (optional)

## Files to Modify

1. `memdiag-web/src/main/resources/application.properties`
2. `memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java`
3. `memdiag-web/src/main/java/com/memdiag/web/controller/RealtimeController.java`
4. `memdiag-web/pom.xml` (add validation starter if needed)

---

## Estimated Effort

- Task 1: 30-45 minutes
- Task 2: 1-1.5 hours
- Task 3: 45-60 minutes
- Testing: 30-45 minutes

**Total:** ~3-4 hours

---

## Next Phase

After this phase completes: Phase 2: Tech Debt Reduction

---

*Plan created: 2026-04-02*
