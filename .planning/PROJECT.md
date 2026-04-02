# MemDiag Project

**Project Name:** MemDiag
**Project Type:** Brownfield (existing codebase)
**Start Date:** 2026-04-02
**Current Version:** 1.0.0-SNAPSHOT

---

## Overview

MemDiag is a JVM Memory Diagnosis Tool for analyzing memory object distribution, locating memory leaks, and providing diagnostic recommendations.

**Key Characteristics:**
- Multi-module Maven project (Java 11+)
- Dual-mode operation: JMX mode (local) and Agent mode (remote)
- Plugin-based diagnosis with extensible rule engine
- Three user interfaces: CLI, Web UI, and Agent API

---

## Current State

### Modules Implemented
- ✅ `memdiag-core` - Core analysis library
- ✅ `memdiag-cli` - Command line interface
- ✅ `memdiag-agent` - Java Agent for instrumentation
- ✅ `memdiag-web` - Spring Boot web backend
- ✅ `memdiag-ui` - Vue 3 frontend
- ✅ `memdiag-native` - JVMTI native agent (C++)

### Features Implemented
- Heap histogram analysis (JMX-based)
- Thread analysis and dump capture
- Diagnostic engine with multiple rules
- Snapshot management and diff analysis
- WebSocket real-time monitoring
- Basic NMT (Native Memory Tracking) support

---

## Project Goals (GSD)

**Primary Focus:**
1. **Fix Known Issues** - Address security concerns, tech debt, and quality issues identified in codebase mapping
2. **Web UI Enhancement** - Improve the frontend user experience and feature completeness

---

## Technology Stack

- **Language:** Java 11+, JavaScript/Vue 3, C++17 (native)
- **Build:** Maven 3.x, Vite
- **Frameworks:** Spring Boot 2.7.18, Vue 3.4.0, Picocli 4.7.5
- **Testing:** JUnit 5.10.0, AssertJ 3.24.2

See `.planning/codebase/STACK.md` for full details.

---

## Key Concerns (from codebase mapping)

**Security High Priority:**
- CORS open to all origins
- No authentication/authorization on API endpoints

**Tech Debt High Priority:**
- Incomplete AgentNativeAnalyzer implementation
- Missing tests for web layer
- Spring Boot 2.7.18 (EOL) - should upgrade

See `.planning/codebase/CONCERNS.md` for full details.

---

## Related Documents

- Design Specification: `docs/superpowers/specs/2026-03-24-memdiag-design.md`
- Implementation Plan: `docs/superpowers/plans/2026-03-24-memdiag-phase0-1-plan.md`
- Codebase Mapping: `.planning/codebase/`

---

*Initialized: 2026-04-02*
