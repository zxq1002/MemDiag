# External Integrations

**Analysis Date:** 2026-04-02

## APIs & External Services

**JVM Instrumentation:**
- JDK Attach API - Dynamic attachment to running JVMs
  - SDK/Client: Built-in JDK tools
  - Location: `memdiag-core/pom.xml` (tools.jar dependency)
- JVMTI (JVM Tool Interface) - Low-level JVM profiling
  - Implementation: C++ native agent
  - Location: `memdiag-native/src/main/c/jvmti/`

**WebSocket Communication:**
- STOMP over WebSocket - Real-time data streaming
  - Server: Spring WebSocket
  - Client: SockJS + WebStomp
  - Location: `memdiag-web/src/main/java/com/memdiag/web/config/WebSocketConfig.java`
  - Frontend: `memdiag-ui/package.json`

## Data Storage

**Databases:**
- None - In-memory analysis only

**File Storage:**
- Local filesystem - For heap dump files (hprof format)
- JVM heap dump generation via HotSpotDiagnosticMXBean

**Caching:**
- None - Analysis performed on demand

## Authentication & Identity

**Auth Provider:**
- None - Application runs locally with full access to the host system

## Monitoring & Observability

**Error Tracking:**
- None - Uses standard logging

**Logs:**
- Spring Boot standard logging (SLF4J + Logback)
- Console output for CLI operations

## CI/CD & Deployment

**Hosting:**
- Local execution - Designed to run on developer machines
- Docker support via `UAT.Dockerfile` for containerized deployment

**CI Pipeline:**
- None detected

## Environment Configuration

**Required env vars:**
- None - Works with standard Java installation

**Optional env vars:**
- JAVA_HOME - For locating JDK tools

**Secrets location:**
- Not applicable - No secrets used

## Webhooks & Callbacks

**Incoming:**
- None

**Outgoing:**
- None

## JVM Internal Integrations

**JMX Beans:**
- HotSpotDiagnosticMXBean - Heap dump generation
- MemoryMXBean - Memory usage monitoring
- GarbageCollectorMXBean - GC statistics
- ThreadMXBean - Thread analysis

**Class Transformation:**
- ASM bytecode manipulation - For method instrumentation
- Java Agent premain/agentmain - JVM startup and dynamic attachment

---

*Integration audit: 2026-04-02*
