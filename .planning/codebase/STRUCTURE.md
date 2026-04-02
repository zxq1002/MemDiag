# Codebase Structure

**Analysis Date:** 2026-04-02

## Directory Layout

```
MemDiag/
├── .planning/               # Planning and analysis artifacts
├── .vscode/                 # VS Code configuration
├── demo/                    # Demo scripts and examples
├── docs/                    # Documentation (design docs, UAT reports)
├── memdiag-agent/           # Java Agent module
├── memdiag-cli/             # Command Line Interface module
├── memdiag-core/            # Core analysis library
├── memdiag-native/          # JVMTI native agent (C++)
├── memdiag-ui/              # Frontend UI
├── memdiag-web/             # Web backend (Spring Boot)
├── scripts/                 # Build and test scripts
├── pom.xml                  # Parent Maven POM
└── README.md                # Main documentation
```

## Directory Purposes

**memdiag-core/:**
- Purpose: Core analysis library - all diagnostic capabilities
- Contains: JMX client, heap/thread analyzers, diagnosis engine, data models
- Key files: `src/main/java/com/memdiag/core/util/JmxClient.java`, `src/main/java/com/memdiag/core/diagnose/DiagnosisEngine.java`

**memdiag-cli/:**
- Purpose: Command line tool with subcommands
- Contains: Picocli commands, output formatting, agent client
- Key files: `src/main/java/com/memdiag/cli/MemDiagCli.java`, `src/main/java/com/memdiag/cli/commands/`

**memdiag-agent/:**
- Purpose: Java Agent for dynamic attachment and instrumentation
- Contains: Premain/Agentmain, HTTP server, ASM transformers, data collectors
- Key files: `src/main/java/com/memdiag/agent/MemDiagAgent.java`, `src/main/java/com/memdiag/agent/AgentServer.java`

**memdiag-native/:**
- Purpose: JVMTI native agent for deep JVM integration
- Contains: C++ JVMTI implementation, /proc parser, build scripts
- Key files: `src/main/c/jvmti/agent.cpp`, `src/main/c/linux/proc_parser.cpp`

**memdiag-web/:**
- Purpose: Spring Boot web backend for REST API and WebSocket
- Contains: Controllers, services, WebSocket config
- Key files: `src/main/java/com/memdiag/web/MemDiagWebApp.java`, `src/main/java/com/memdiag/web/controller/ApiController.java`

**memdiag-ui/:**
- Purpose: Frontend web interface
- Contains: UI assets, templates, frontend code

**docs/:**
- Purpose: Project documentation
- Contains: Design specs, implementation plans, code review reports

**scripts/:**
- Purpose: Build and test automation
- Contains: Quick validation, UAT tests, native smoke tests

## Key File Locations

**Entry Points:**
- `memdiag-cli/src/main/java/com/memdiag/cli/MemDiagCli.java`: CLI main class
- `memdiag-agent/src/main/java/com/memdiag/agent/MemDiagAgent.java`: Agent premain/agentmain
- `memdiag-web/src/main/java/com/memdiag/web/MemDiagWebApp.java`: Spring Boot application

**Configuration:**
- `pom.xml`: Parent Maven configuration
- `memdiag-*/pom.xml`: Module-specific Maven configuration
- `memdiag-web/src/main/resources/application.properties`: Spring Boot config

**Core Logic:**
- `memdiag-core/src/main/java/com/memdiag/core/util/JmxClient.java`: JMX attachment and communication
- `memdiag-core/src/main/java/com/memdiag/core/heap/JmxHeapAnalyzer.java`: Heap memory analysis
- `memdiag-core/src/main/java/com/memdiag/core/thread/ThreadAnalyzer.java`: Thread analysis
- `memdiag-core/src/main/java/com/memdiag/core/diagnose/DiagnosisEngine.java`: Automatic diagnosis
- `memdiag-core/src/main/java/com/memdiag/core/diff/SnapshotManager.java`: Snapshot management
- `memdiag-agent/src/main/java/com/memdiag/agent/AgentServer.java`: Agent HTTP API
- `memdiag-agent/src/main/java/com/memdiag/agent/instrument/InstrumentManager.java`: Bytecode instrumentation

**Testing:**
- `memdiag-core/src/test/java/`: Unit and integration tests
- `scripts/quick-validate.sh`: Quick validation script
- `scripts/uat-blackbox.sh`: UAT blackbox tests

## Naming Conventions

**Files:**
- PascalCase for class names: `JmxClient.java`, `DiagnosisEngine.java`
- Subcommand pattern: `[CommandName]Command.java` (e.g., `HistogramCommand.java`)
- Implementation pattern: `[Interface][Implementation].java` (e.g., `ProcFileSystemNativeAnalyzer.java`)

**Directories:**
- Lowercase with hyphens for module names: `memdiag-core`, `memdiag-agent`
- Lowercase for package names: `com.memdiag.core.heap`, `com.memdiag.agent.instrument`
- Feature-based organization within packages

## Where to Add New Code

**New CLI Command:**
- Primary code: `memdiag-cli/src/main/java/com/memdiag/cli/commands/[NewCommand]Command.java`
- Core logic (if non-trivial): `memdiag-core/src/main/java/com/memdiag/core/[feature]/`
- Tests: `memdiag-cli/src/test/java/` or `memdiag-core/src/test/java/`

**New Analysis Feature:**
- Implementation: `memdiag-core/src/main/java/com/memdiag/core/[feature]/`
- Tests: `memdiag-core/src/test/java/com/memdiag/core/[feature]/`

**New Diagnosis Rule:**
- Rule class: `memdiag-core/src/main/java/com/memdiag/core/diagnose/rules/[RuleName]Rule.java`
- Register in: `RuleRegistry.withDefaults()` (if built-in)

**New Agent API Endpoint:**
- Handler: Add inner class in `memdiag-agent/src/main/java/com/memdiag/agent/AgentServer.java`
- Register: In `AgentServer.start()` method

**New Web API Endpoint:**
- Controller: `memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java`
- Service logic: `memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java`

**Utilities:**
- Shared helpers: `memdiag-core/src/main/java/com/memdiag/core/util/`

## Special Directories

**memdiag-native/src/main/c/:**
- Purpose: C++ JVMTI agent implementation
- Generated: Compiled .so files copied to resources
- Committed: Source code only, compiled binaries in releases

**~/.memdiag/snapshots/:**
- Purpose: Stored heap snapshots
- Generated: Yes, at runtime
- Committed: No (user-specific data)

**memdiag-ui/:**
- Purpose: Frontend web interface
- Note: Separate frontend build process

**target/ (in each module):**
- Purpose: Maven build output
- Generated: Yes
- Committed: No (in .gitignore)

---

*Structure analysis: 2026-04-02*
