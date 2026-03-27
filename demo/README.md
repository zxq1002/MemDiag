# MemDiag Simulator - 全场景验证 Demo 使用说明

本 Demo 旨在提供一个标准化的 **Linux 运行环境**，用于验证 MemDiag 工具在处理各类极端内存场景时的准确性与安全性。

## 目录

1. [模拟器核心功能](#1-模拟器核心功能)
2. [环境准备与启动](#2-环境准备与启动)
3. [实战验证指南](#3-实战验证指南)
4. [完整测试套件](#4-完整测试套件)
5. [关键技术细节](#5-关键技术细节)
6. [故障排查](#6-故障排查)

---

## 1. 模拟器核心功能

`MemDiagDemo.java` 是一个高度可配置的 Java 程序，支持以下四种模拟模式：

| 模式 (`mode`) | 说明 | 模拟故障 |
| :--- | :--- | :--- |
| `heap-leak` | 持续分配 `byte[]` 对象并持有引用。 | **堆内存缓慢增长溢出 (OOM)** |
| `heap-high` | 一次性分配大块堆内存并保持。 | **堆内存不足 (非溢出高水位)** |
| `native-leak` | 通过 `sun.misc.Unsafe` 持续申请原生内存。 | **堆外内存溢出 (C 层泄露)** |
| `native-high` | 分配大块 `DirectByteBuffer` 并保持。 | **堆外内存占用过高** |

---

## 2. 环境准备与启动

### 2.1 一键启动 (Docker)
我们提供了 `demo/start-uat.sh` 脚本，支持通过命令行参数直接指定模拟场景：

```bash
# 基本用法
# ./demo/start-uat.sh [mode] [limit_mb] [rate_mb_per_sec]

# 示例 1: 默认启动 (heap-leak, 500MB, 10MB/s)
bash demo/start-uat.sh

# 示例 2: 模拟堆外内存泄露 (800MB 上限, 20MB/s 增长)
bash demo/start-uat.sh native-leak 800 20

# 示例 3: 模拟堆内存高水位 (非泄露, 900MB)
bash demo/start-uat.sh heap-high 900
```

---

## 3. 实战验证指南

启动容器后，打开另一个终端进入容器：
```bash
docker exec -it memdiag-uat bash
```

### 场景 A：验证堆内存泄露 (`heap-leak`)
1. **执行快照 1**：`memdiag snapshot <PID> --save --id=s1`
2. **等待 10 秒**。
3. **执行快照 2**：`memdiag snapshot <PID> --save --id=s2`
4. **对比分析**：`memdiag diff --baseline=s1 --current=s2`
   *   **验收标准**：必须看到 `byte[]` 类的增长率处于 Top 1。

### 场景 B：验证堆外内存监控 (`native-leak`)
1. **查看概览**：`memdiag native <PID> --summary`
   *   **验收标准**：`Total Resident` (RSS) 应远大于堆内存大小。
2. **定位泄露点**：
   *   挂载：`memdiag native <PID> --attach`
   *   追踪：`memdiag native <PID> --start-trace`
   *   查看：`memdiag native <PID> --allocation-sites`
   *   **验收标准**：应能识别出 `Unsafe.allocateMemory` 产生的分配点。

### 场景 C：验证自动诊断建议 (`heap-high`)
1. **运行诊断**：`memdiag diagnose <PID>`
   *   **验收标准**：系统应输出诊断报告，显示执行了 5 个规则。

---

## 4. 完整测试套件

### 4.1 自动化测试

提供完整的自动化测试套件，一键验证所有功能：

```bash
# 运行完整测试套件
bash demo/start-test-suite.sh
```

测试套件会自动：
1. 构建项目
2. 构建 Docker 测试镜像
3. 启动容器并执行所有测试
4. 输出测试结果统计

### 4.2 测试覆盖范围

根据 `docs/TEST_PLAN.md`，测试覆盖以下命令：

| 优先级 | 命令 | 选项 | 状态 |
|--------|------|------|------|
| P0 | memdiag | -h, --help | ✅ |
| P0 | memdiag | -V, --version | ✅ |
| P0 | histogram | (默认), -l, --limit, --pid | ✅ |
| P0 | threads | (默认), -s, --stacks, -l | ✅ |
| P0 | diagnose | (默认), --pid | ✅ |
| P0 | snapshot | --save, --load, --list, --delete | ✅ |
| P0 | diff | --baseline, --current, --growing, etc. | ✅ |
| P1 | native | --status, --summary, --regions | ✅ |
| P1 | report | --format, --output | ✅ |

### 4.3 手动运行测试

如果需要在容器内手动运行测试：

```bash
# 1. 启动测试容器（后台模式）
docker run -d --name memdiag-test --cap-add=SYS_PTRACE memdiag-test tail -f /dev/null

# 2. 进入容器
docker exec -it memdiag-test bash

# 3. 运行测试
/app/test-full-suite.sh
```

---

## 5. 关键技术细节 (专家提示)

1.  **权限要求**：Docker 启动时必须带有 `--cap-add=SYS_PTRACE` 参数，否则 JVM 的 Attach API 将无法连接到目标进程。
2.  **资源限制**：模拟器默认在 Docker 内受到 `JAVA_OPTS="-Xmx1G"` 的限制。如果模拟 `limit` 超过 1024MB，程序将触发真实的 `java.lang.OutOfMemoryError`。
3.  **安全验证**：在执行 `native --detach` 后，观察模拟器日志，确保其仍在正常输出（证明字节码插桩已安全剥离）。
4.  **诊断规则**：当前内置 5 个诊断规则，包括新增的 `HEAP_LEAK_SUSPECT` 堆内存泄漏嫌疑检测。
5.  **JVMTI 功能**：`native --attach`、`--start-trace` 等高级功能需要 `libmemdiag-agent.so` 原生库。该库需要在 Linux 环境下编译（可使用 Docker）。基础功能（`--status`、`--summary`、`--regions`、`--diagnose`）无需原生库即可使用。
6.  **GC Root 分析**：`gc-roots` 命令目前支持基本统计功能。完整的引用链分析需要 JVMTI 原生库支持。
7.  **Agent 模式**：已支持 Java Agent 模式，可通过 `-javaagent:memdiag-agent.jar` 启动时挂载，或动态挂载到运行中的 JVM。
8.  **Web UI**：已提供完整的 Web 界面，包含 Spring Boot 后端和 Vue 3 前端，支持实时图表展示。

---

## 6. 故障排查

*   **无法 Attach**：检查 PID 是否正确。容器内使用 `ps -ef` 或 `jps -l` 确认。
*   **Native 模块加载失败**：确保已安装 `g++` 和 `cmake`（Dockerfile 已包含）。
*   **找不到 JAR 包**：确保在运行 `start-uat.sh` 之前已经执行过 `mvn clean package`。
*   **测试脚本权限**：确保 `test-full-suite.sh` 和 `start-test-suite.sh` 有执行权限（`chmod +x`）。

## 7. 相关文件

| 文件 | 说明 |
|------|------|
| `docs/TEST_PLAN.md` | 完整测试计划文档 |
| `demo/test-full-suite.sh` | 容器内测试执行脚本 |
| `demo/start-test-suite.sh` | 测试套件启动脚本 |
| `demo/Dockerfile.test` | 测试环境 Dockerfile |
