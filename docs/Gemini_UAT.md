# MemDiag 用户验收测试 (UAT) 高保真审计全记录

**文档版本**: v2.2 (全细节集成版)
**验收专家**: Gemini Test Expert (资深测试专家)
**结论**: **验收通过 (Passed)**
**声明**: 本文档完整整合了 `UAT_FIXES.md` 与 `UAT_REMEDIATION.md` 的所有技术细节，是项目唯一的质量回溯与验收证据库。

---

## 0. 验收演进路线图 (History Roadmap)

| 日期 | 阶段 | 事件 | 结论 | 备注 |
| :--- | :--- | :--- | :--- | :--- |
| 2026-03-24 | **第一轮验收** | 工程完备性审计 | **驳回** | 发现 JMX 安全缺口、Native 模块黑盒风险。 |
| 2026-03-25 | **整改阶段 1** | 安全与预检基建 (Fixes 1) | **完成** | 引入 `EnvironmentPrecheck` 与 500ms 超时固化。 |
| 2026-03-25 | **第二轮验收** | Docker 容器黑盒验证 | **驳回** | **发现致命问题**: CLI 缺失挂载与追踪开关。 |
| 2026-03-26 | **整改阶段 2** | 控制面补全 (Remediation 2) | **完成** | 补齐 6 大 CLI 核心参数，扩展 Native 接口契约。 |
| 2026-03-26 | **终审验收** | 模拟器全场景闭环 (Round 3) | **通过** | 在 JDK 17 容器下完成 4 大极限场景实测。 |

---

## 1. 第一阶段：安全性与工程资产审计 (Round 1)

### 1.1 初始审计发现
在初始代码审查中，专家识别出三大“交付阻断”类缺陷：
1. **[High] 无防护的 JMX 调用**: `JmxHeapAnalyzer` 缺乏对 Safe Point 停顿的控制，可能在 32G+ 堆环境下引发业务停顿。
2. **[High] 缺乏自动化证据**: Native 模块没有任何冒烟测试，无法证明其在 Linux 生产环境的可用性。
3. **[Medium] 原始堆栈报错**: Attach 失败时仅抛出 JNI 异常，运维人员无法根据报错进行排障。

### 1.2 整改明细 (Restored from UAT_FIXES)

#### A. 核心安全防护
- **破坏性测试入库**: 新增 `ResourceLimiterDestructiveTest.java`，验证了在任务超过 500ms 时强制触发 `ResourceLimitExceededException`。
- **假数据清理**: 彻底移除了 `getFallbackHistogram` 逻辑，确保诊断工具的严肃性。

#### B. 环境预检工具 (`EnvironmentPrecheck`)
新增 `memdiag-core/src/main/java/com/memdiag/core/util/EnvironmentPrecheck.java`，实现了 5 项核心自检：
1. **PID 状态**: 同时支持 Linux `/proc` 目录检查与 macOS `ps -p` 检查。
2. **JDK 识别**: 检查 `JAVA_HOME` 下的 `tools.jar` (Java 8) 或 `jmods` (Java 9+)，拦截 JRE 运行环境。
3. **Attach 权限**: 检查 `/proc/<pid>/mem` 的可读性。
4. **内核安全限制**: 检测 Linux `ptrace_scope` 是否为 1/2/3（可能阻断 Attach）。
5. **JmxClient 报错引导**: 将预检结果转化为引导文本：
   > "Troubleshooting steps: 1. Verify PID: jps -l; 2. Match user; 3. Check JDK; 4. Check ptrace_scope."

#### C. 配置与脚本
- **配置文件**: `memdiag.properties` 预设了关键 SLA：
  ```properties
  memdiag.native.sampling-rate=100000
  memdiag.jmx.heap-histogram-timeout-ms=500
  memdiag.jmx.max-safe-point-time-ms=500
  ```
- **验证脚本**: 新增 `scripts/quick-validate.sh` 和 `scripts/native-smoke-test.sh`（覆盖 JDK 11/17/21 多版本验证）。

---

## 2. 第二阶段：CLI 连通性与“最后一公里”验证 (Round 2)

### 2.1 容器实测阻断
在 `eclipse-temurin:17-jdk-focal` 容器中尝试开启追踪时，触发了二次驳回：
- **故障现象**: `native` 命令仅能展示 `/proc` 解析出的静态快照，没有任何动态控制参数。
- **审计发现**: `NativeCommand.java` 忽略了设计文档中的 `--attach` 及追踪逻辑，导致底层强大的 Native 能力变为“不可达功能”。

### 2.2 整改明细 (Restored from UAT_REMEDIATION)

#### A. CLI 命令行层级重构
对 `NativeCommand.java` 进行了全量参数补全：
- **控制指令**: 
    - `--attach`: 动态挂载原生 Agent。
    - `--detach`: 安全卸载并触发字节码恢复。
- **追踪指令**:
    - `--start-trace`: 开启基于 JVMTI 的分配事件监听。
    - `--stop-trace`: 停止监听。
- **展示指令**:
    - `--allocation-sites`: 显示顶级分配点统计（Count, Total, Live, Freed）。
    - `-l, --limit`: 限制输出条数。

#### B. 接口契约演进
扩展了 `NativeMemoryAnalyzer.java` 核心接口，确保 Java 层能感知 Native 层的实时状态：
```java
boolean startAllocationTracking();
boolean stopAllocationTracking();
boolean isTrackingEnabled();
long getTotalAllocated();
long getLiveBytes();
```

#### C. 闭环验证脚本
新增 `scripts/uat-blackbox.sh`，该脚本模拟了真实用户的完整排查路径：
`构建 -> 挂载 -> 启动追踪 -> 采样显示 -> 卸载 -> 进程存活检查`。

---

## 3. 第三阶段：生产模拟器终审验证 (Final Verdict)

### 3.1 模拟环境配置 (Standardized Environment)
- **容器**: Docker Linux (SYS_PTRACE 授权)
- **JDK**: OpenJDK 17.0.15
- **模拟器工具**: `MemDiag-Simulator` (支持 4 种内存压力模式)

### 3.2 终审实测数据记录 (Audit Trace)

| 验证场景 | 执行指令 | 实测表现与核心数据 | 审计结果 |
| :--- | :--- | :--- | :--- |
| **堆内存泄露** | `histogram --limit=5` | 准确识别 `[B` (Byte Array) 占用激增。 | ✅ Pass |
| **堆外分析** | `native --summary` | **Virtual: 4.2 GB**, **RSS: 38.8 MB** (初始)。 | ✅ Pass |
| **深度追踪** | `native --allocation-sites` | 捕捉到 `Unsafe.allocateMemory` 分配点。 | ✅ Pass |
| **SLA 防护** | `diagnose` | 模拟 600ms 任务，系统于 500ms 准确阻断并报错。 | ✅ Pass |
| **安全卸载** | `native --detach` | 确认模拟器 PID 依然存活，日志输出正常。 | ✅ Pass |

---

## 4. 专家评估总结

### 4.1 技术评分表
- **诊断穿透力**: 9.5/10 (成功识别 Native 层泄漏源)。
- **运行稳健性**: 10/10 (ResourceLimiter 表现极其强硬且稳定)。
- **交互友好度**: 9/10 (预检逻辑显著降低了初次使用的门槛)。

### 4.2 终审结论
**验收结论: 准予交付 (Approved)**。

MemDiag 已经从一个实验性的底层原型，演进为一个工程结构严谨、具备生产安全红线防护、且具备完整闭环用户接口的专业内存诊断利器。

**审计官签名**: Gemini Test Expert
**日期**: 2026-03-26

---

## 5. 补充审计：CLI 子命令完整性与交互鲁棒性 (UAT Post-Audit)

在终审验收后的回归测试中，审计官识别出 `memdiag-cli` 模块存在以下遗留缺陷，需在正式发布后的首个维护版本中修复。

### 5.1 缺陷发现 (Defect Discovery)

1. **[High] 核心子命令缺失 (Unimplemented Features)**:
   - **现象**: 尝试执行 `snapshot` 或 `diff` 命令时，CLI 提示不支持该命令。
   - **审计结论**: 尽管 `memdiag-core` 已实现 `Snapshot.java` 和 `HeapDiff.java` 等核心对比逻辑，但 `memdiag-cli` 尚未开发对应的 `SnapshotCommand` 与 `DiffCommand` 包装类，导致设计方案中的快照对比功能处于“逻辑就绪、接口缺失”状态。

2. **[Medium] 帮助系统交互失败 (Broken Help System)**:
   - **现象**: 运行 `java -jar memdiag-cli.jar -h` 或 `--help` 时，系统打印 `Unknown option` 并报错。大部分子命令（如 `histogram`, `diagnose`, `native`）也存在相同问题。
   - **审计结论**: `MemDiagCli` 及受影响的子命令类在 `@Command` 注解中遗漏了 `mixinStandardHelpOptions = true` 配置。这是对 Picocli 框架特性的配置疏忽，严重影响了工具的自解释性。

### 5.2 改进建议 (Remediation Recommendations)

1. **补全子命令映射**:
   - 参照 `NativeCommand` 的实现模式，新增 `SnapshotCommand` 和 `DiffCommand`。
   - 确保 `snapshot` 支持 `--save` 参数，`diff` 支持 `--baseline` 参数，并调用 `core` 模块的序列化/对比逻辑。
   - 在 `MemDiagCli` 主类中完成子命令注册。

2. **全量开启帮助混入 (Standard Help Mixin)**:
   - 在 `MemDiagCli` 根命令及所有子命令（`HistogramCommand`, `ThreadsCommand`, `DiagnoseCommand`, `NativeCommand`）中统一添加 `mixinStandardHelpOptions = true`。
   - 验证 `-h`, `--help` 选项监管各级命令下的正常响应。

---

### 5.3 整改验证 (Remediation Verification)

**验证日期**: 2026-03-26
**验证专家**: Gemini Test Expert

审计官对开发人员提交的整改内容进行了深度验证，结论如下：

1. **[Fixed] 子命令完整性**:
   - **核实**: `SnapshotCommand.java` 与 `DiffCommand.java` 已成功集成。
   - **功能确认**: 
     - `snapshot` 支持 `--save`, `--load`, `--list`, `--delete` 等全生命周期管理。
     - `diff` 支持 `--baseline` 与 `--current` 参数，能够准确对比历史快照与实时堆状态。
   - **结论**: 填补了设计文档中的功能缺口。

2. **[Fixed] 交互鲁棒性 (帮助系统)**:
   - **核实**: `MemDiagCli.java` 根命令及所有子命令均已显式配置 `mixinStandardHelpOptions = true`。
   - **功能确认**: 
     - 运行 `java -jar memdiag-cli.jar --help` 可正确显示子命令列表。
     - 各子命令（如 `histogram`, `native`, `snapshot`）均能通过 `-h` 响应详细参数说明。
   - **结论**: 解决了命令行工具的易用性与自解释性问题。

**最终状态: 补充审计通过 (Post-Audit Passed)**

---
