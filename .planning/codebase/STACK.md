# Technology Stack

**Analysis Date:** 2026-04-02

## Languages

**Primary:**
- Java 11 - Backend core, CLI, Agent, and Web service
- JavaScript/TypeScript - Frontend UI

**Secondary:**
- C++17 - Native JVMTI agent for Linux

## Runtime

**Environment:**
- OpenJDK / Oracle JDK 11+
- Node.js (for frontend development)

**Package Manager:**
- Maven 3.x - Java dependencies
- npm - Node.js/frontend dependencies
- Lockfiles: `pom.xml` (Maven), `package.json` (npm)

## Frameworks

**Core:**
- Spring Boot 2.7.18 - Web backend framework
  - Location: `memdiag-web/pom.xml`
- Vue 3.4.0 - Frontend SPA framework
  - Location: `memdiag-ui/package.json`

**Testing:**
- JUnit 5.10.0 - Unit and integration testing
- AssertJ 3.24.2 - Fluent assertions
- Spring Boot Test - Integration testing for web layer

**Build/Dev:**
- Vite 5.0.0 - Frontend build tool and dev server
- Maven Shade Plugin 3.5.0 - Creates executable JARs
- Spring Boot Maven Plugin - Spring Boot application packaging

## Key Dependencies

**Critical:**
- Picocli 4.7.5 - Command line interface parsing
  - Location: `memdiag-cli/pom.xml`
- ASM 9.5 - Bytecode instrumentation for Java Agent
  - Location: `memdiag-agent/pom.xml`
- Gson 2.10.1 - JSON serialization/deserialization
  - Location: Parent `pom.xml`

**Infrastructure:**
- Spring Boot WebSocket - Real-time communication
- Thymeleaf - Server-side template rendering
- Spring Boot Actuator - Application monitoring and management

**Frontend:**
- Vue Router 4.2.0 - Client-side routing
- Axios 1.6.0 - HTTP client
- ECharts 5.4.0 - Data visualization charts
- SockJS Client 1.6.0 + WebStomp Client 1.2.6 - WebSocket communication

## Configuration

**Environment:**
- Spring Boot `application.properties` for backend configuration
- No external environment variables required by default

**Build:**
- Parent `pom.xml` - Multi-module Maven configuration
- `vite.config.js` - Frontend build configuration
- Module-specific `pom.xml` files in each submodule

## Platform Requirements

**Development:**
- JDK 11+
- Node.js 16+ (for frontend)
- Maven 3.6+

**Production:**
- JRE 11+ for running backend/CLI/Agent
- Linux for native JVMTI agent support (optional)
- Docker support via `UAT.Dockerfile`

---

*Stack analysis: 2026-04-02*
