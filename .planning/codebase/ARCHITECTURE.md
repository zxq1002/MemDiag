# Architecture

**Analysis Date:** 2026-04-02

## Pattern Overview

**Overall:** Modular Monolith with Multi-Component Architecture

**Key Characteristics:**
- **Multi-module Maven project** - Clear separation of concerns across modules
- **Dual-mode operation** - JMX mode (local) and Agent mode (remote)
- **Plugin-based diagnosis** - Extensible rule engine for issue detection
- **Layered architecture** - Presentation → Service → Core → Native layers
- **Bytecode instrumentation** - ASM-based transformation for allocation tracking

## Layers

**Command Line Interface (CLI) Layer:**
- Purpose: User-facing command line tool with subcommands
- Location: `memdiag-cli/src/main/java/com/memdiag/cli/`
- Contains: Picocli-based commands, output formatting
- Depends on: memdiag-core, memdiag-agent (optional)
- Used by: End users directly

**Web Layer:**
- Purpose: REST API and WebSocket backend for web UI
- Location: `memdiag-web/src/main/java/com/memdiag/web/`
- Contains: Spring Boot controllers, WebSocket handlers, services
- Depends on: memdiag-core, memdiag-agent
- Used by: Web frontend

**Agent Layer:**
- Purpose: Java Agent for dynamic attachment and bytecode instrumentation
- Location: `memdiag-agent/src/main/java/com/memdiag/agent/`
- Contains: Premain/Agentmain entry points, HTTP server, instrumentation managers
- Depends on: memdiag-core, memdiag-native (optional)
- Used by: CLI (--agent mode), Web layer

**Core Analysis Layer:**
- Purpose: Core analysis capabilities and data models
- Location: `memdiag-core/src/main/java/com/memdiag/core/`
- Contains: JMX client, heap/thread analyzers, diagnosis engine, snapshot management
- Depends on: JDK tools.jar (optional)
- Used by: CLI, Agent, Web layers

**Native Layer:**
- Purpose: JVMTI integration and native memory analysis
- Location: `memdiag-native/src/main/`
- Contains: C++ JVMTI agent, /proc filesystem parser
- Depends on: JNI, JVMTI
- Used by: memdiag-agent (optional)

## Data Flow

**JMX Mode Data Flow:**

1. User executes CLI command (e.g., `memdiag histogram <pid>`)
2. `BaseCommand` parses arguments and creates `JmxClient`
3. `JmxClient.attachToPid()` uses Attach API to connect to target JVM
4. Command invokes specific analyzer (e.g., `JmxHeapAnalyzer.getHistogram()`)
5. Analyzer uses JMX MBeans to retrieve data
6. Results are formatted and displayed to user

**Agent Mode Data Flow:**

1. User executes CLI command with `--agent` option
2. `AgentClient` makes HTTP request to AgentServer
3. `AgentServer` handlers process request in target JVM
4. Local analyzers retrieve data directly from target JVM
5. JSON response returned to CLI
6. CLI formats and displays results

**Diagnosis Engine Flow:**
1. `DiagnosisEngine.analyze()` is called
2. Collects data: HeapHistogram, ThreadDump, MemoryUsage
3. Builds `DiagnosisContext` with collected data
4. Iterates through registered `DiagnosisRule`s
5. Each rule evaluates context and returns `Issue`s
6. Issues are aggregated into `DiagnosisResult`
7. Result returned for display/reporting

**State Management:**
- Agent maintains state in `AgentContext` (singleton)
- Snapshots stored locally in `~/.memdiag/snapshots/`
- Allocation events stored in ring buffer (`AllocationRingBuffer`)
- No distributed state - each component operates independently

## Key Abstractions

**Analyzer Pattern:**
- Purpose: Standard interface for all analysis components
- Examples: `memdiag-core/src/main/java/com/memdiag/core/heap/HeapAnalyzer.java`, `memdiag-core/src/main/java/com/memdiag/core/thread/ThreadAnalyzer.java`
- Pattern: Simple interface with single method for data retrieval

**Diagnosis Rule Pattern:**
- Purpose: Extensible issue detection
- Examples: `memdiag-core/src/main/java/com/memdiag/core/diagnose/DiagnosisRule.java`, `memdiag-core/src/main/java/com/memdiag/core/diagnose/rules/LargeClassRule.java`
- Pattern: `evaluate(DiagnosisContext) → List<Issue>` with registry for discovery

**Native Memory Analyzer Factory:**
- Purpose: Platform-specific native memory analysis
- Examples: `memdiag-core/src/main/java/com/memdiag/core/nativeapi/NativeMemoryAnalyzerFactory.java`
- Pattern: Factory creates `ProcFileSystemNativeAnalyzer` or `NoOpNativeAnalyzer` based on platform

## Entry Points

**CLI Entry Point:**
- Location: `memdiag-cli/src/main/java/com/memdiag/cli/MemDiagCli.java`
- Triggers: User executes `java -jar memdiag-cli.jar`
- Responsibilities: Parses command line, delegates to subcommands

**Agent Premain Entry Point:**
- Location: `memdiag-agent/src/main/java/com/memdiag/agent/MemDiagAgent.java`
- Triggers: `-javaagent:memdiag-agent.jar` on JVM startup
- Responsibilities: Initializes agent, starts HTTP server, sets up instrumentation

**Agent Agentmain Entry Point:**
- Location: `memdiag-agent/src/main/java/com/memdiag/agent/MemDiagAgent.java`
- Triggers: Dynamic attach via `VirtualMachine.attach()`
- Responsibilities: Same as premain, but for already running JVMs

**Web Entry Point:**
- Location: `memdiag-web/src/main/java/com/memdiag/web/MemDiagWebApp.java`
- Triggers: Spring Boot application startup
- Responsibilities: Starts embedded Tomcat, initializes REST controllers

**JVMTI Entry Point:**
- Location: `memdiag-native/src/main/c/jvmti/agent.cpp`
- Triggers: System.loadLibrary() from Java Agent
- Responsibilities: Registers JVMTI callbacks, initializes native tracking

## Error Handling

**Strategy:** Layered exception hierarchy with contextual information

**Patterns:**
- Checked exceptions for expected error conditions
- Unchecked exceptions for programming errors
- Rich error messages with troubleshooting suggestions
- Graceful degradation (e.g., NoOpNativeAnalyzer when platform unsupported)

**Key Exception Classes:**
- `MemDiagException` - Base exception
- `AnalysisException` - Analysis failed
- `PlatformNotSupportedException` - Feature not available on this platform
- `ResourceLimitExceededException` - Safety limits exceeded

## Cross-Cutting Concerns

**Logging:**
- Approach: Standard output/error (printStackTrace for errors)
- No dedicated logging framework - keeps CLI lightweight

**Validation:**
- Environment pre-check in `EnvironmentPrecheck`
- PID validation, JDK detection, permission checks
- Platform capability detection before native operations

**Authentication:**
- JMX mode: Same-user requirement, OS-level permissions
- Agent mode: HTTP server binds to localhost by default (no auth)
- No built-in authentication - relies on network security

**Resource Limiting:**
- `ResourceLimiter` class enforces safe operation limits
- 500ms timeout for heap histogram operations
- Configurable ring buffer size for allocation tracking
- Prevents excessive memory overhead in target JVM

---

*Architecture analysis: 2026-04-02*
