# MemDiag

**JVM 内存诊断工具** - 专业的 Java 堆内/堆外内存分析与诊断工具。

---

## 目录

- [产品简介](#产品简介)
- [功能特性](#功能特性)
- [快速入门](#快速入门)
- [完整命令参考](#完整命令参考)
  - [主命令 - memdiag](#主命令---memdiag)
  - [histogram - 堆内存分析](#histogram---堆内存分析)
  - [threads - 线程分析](#threads---线程分析)
  - [diagnose - 自动诊断](#diagnose---自动诊断)
  - [native - 堆外内存分析](#native---堆外内存分析)
  - [snapshot - 快照管理](#snapshot---快照管理)
  - [diff - 堆对比分析](#diff---堆对比分析)
  - [report - 报告生成](#report---报告生成)
  - [nmt - NMT 分析](#nmt---nmt-分析)
  - [agent - Agent 管理](#agent---agent-管理)
  - [allocations - 分配统计](#allocations---分配统计)
  - [methods - 方法监控](#methods---方法监控)
- [架构与实现原理](#架构与实现原理)
- [高级配置](#高级配置)
- [故障排除](#故障排除)
- [开发文档](#开发文档)

---

## 产品简介

MemDiag 是一款功能全面的 JVM 内存诊断工具，专为生产环境设计：

- **零侵入** - 通过 JMX 动态附着，无需重启目标应用
- **多维度** - 支持堆内、堆外、线程等全方位分析
- **智能化** - 内置诊断引擎，自动识别常见内存问题
- **生产级** - 包含资源限流、环境预检等企业级特性
- **字节码插桩** - Agent 模式下提供实时分配追踪和方法监控
- **JVMTI 集成** - 自动检测并加载原生库，实现深层数据采集

---

## 功能特性

| 功能模块 | 说明 |
|---------|------|
| **堆内存分析** | 堆直方图、类分布统计、大对象识别 |
| **线程分析** | 线程状态、堆栈跟踪、死锁检测 |
| **自动诊断** | 内存泄漏检测、配置问题识别、优化建议 |
| **堆外内存** | /proc 解析、内存区域映射、库映射分析 |
| **Java Agent** | 启动时加载/动态 attach、HTTP API 服务 |
| **字节码插桩** | ByteBuffer 分配追踪、方法执行监控 |
| **数据采集** | 环形缓冲区、统计聚合、定时快照 |
| **JVMTI 集成** | 自动检测平台、优雅降级机制 |
| **分配统计** | 实时分配速率、Top N 分配类型、趋势分析 |
| **方法监控** | 方法执行时间、调用次数、慢方法识别 |
| **快照管理** | 堆快照保存、加载、对比 |
| **环境预检** | PID 验证、权限检查、JDK 兼容性检测 |

---

## 快速入门

### 系统要求

- **Java**: 11 或更高版本（JDK，非 JRE）
- **操作系统**: Linux/macOS/Windows（堆外内存功能仅限 Linux）
- **内存**: 至少 512MB 可用内存

### 第一步：获取程序

#### 方式一：从源码构建

```bash
# 克隆项目
git clone https://github.com/zxq1002/MemDiag.git
cd MemDiag

# 构建（跳过测试以加快速度）
mvn clean package -DskipTests
```

构建完成后，可执行文件位于：
```
memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar
```

#### 方式二：使用发布版本

下载预构建的 jar 文件，放置在任意位置。

### 第二步：创建便捷启动脚本（推荐）

```bash
# 创建 memdiag 启动脚本
cat > /usr/local/bin/memdiag << 'EOF'
#!/bin/bash
java -jar /path/to/memdiag-cli-1.0.0-SNAPSHOT.jar "$@"
EOF

# 添加执行权限
chmod +x /usr/local/bin/memdiag
```

或者使用 alias：

```bash
# 添加到 ~/.bashrc 或 ~/.zshrc
echo 'alias memdiag="java -jar /path/to/memdiag-cli-1.0.0-SNAPSHOT.jar"' >> ~/.bashrc
source ~/.bashrc
```

### 第三步：验证安装

```bash
# 查看版本和帮助
memdiag -h

# 应该看到类似输出：
# Usage: memdiag [-hV] [COMMAND]
# JVM Memory Diagnosis Tool
# Commands:
#   histogram    Show heap histogram
#   threads      Show thread analysis
#   diagnose     Run diagnosis and show issues
#   native       Native memory analysis (Linux only)
#   snapshot     Manage heap snapshots
#   diff         Compare heap snapshots
#   report       Generate complete diagnosis report
#   nmt          Native Memory Tracking analysis
#   gc-roots     GC Root analysis (Java 11+)
#   agent        Interact with a running MemDiag agent
#   allocations  Show allocation statistics and trends
#   methods      Show method monitoring statistics
```

### 第四步：第一个诊断

```bash
# 分析当前 JVM（快速测试）
memdiag histogram

# 分析指定进程
jps -l                  # 找到目标 JVM 的 PID
memdiag diagnose 12345
```

---

## 完整命令参考

### 主命令 - memdiag

**用法**: `memdiag [OPTIONS] [COMMAND]`

MemDiag 采用**子命令**模式，结构清晰。

#### 通用选项（所有子命令支持）

| 选项 | 说明 | 示例 |
|-----|------|------|
| `-h, --help` | 显示帮助信息 | `memdiag -h` |
| `-V, --version` | 显示版本信息 | `memdiag -V` |
| `-a, --agent <host:port>` | 连接到远程 Agent | `--agent localhost:6789` |

#### 位置参数

| 参数 | 说明 | 示例 |
|-----|------|------|
| `[<pid>]` | 指定目标 JVM 进程 ID（位置参数） | `memdiag histogram 12345` |

---

### histogram - 堆内存分析

**子命令**: `histogram`

分析堆内存中对象的分布情况，识别大对象和内存热点。

#### 功能说明

- 显示按大小排序的类直方图
- 统计对象数量和占用内存
- 支持限制输出行数，避免信息过载

#### ⚠️ 对目标进程的影响

此命令会通过 JMX 获取堆直方图：
- 可能触发 Full GC（取决于 JVM 实现）
- 需要进入 Safe Point，会**短暂暂停所有应用线程**
- 已配置 500ms 超时保护，避免过长停顿

#### 选项

| 选项 | 说明 | 默认值 |
|-----|------|--------|
| `-h, --help` | 显示帮助信息 | - |
| `-l, --limit <n>` | 限制输出行数 | 20 |

#### 使用场景示例

**场景 1: 应用响应变慢，怀疑内存问题**
```bash
# 先查看堆内存总体分布，看哪些类占用最多
memdiag histogram --pid 12345

# 如果发现某个类特别大，增加显示行数看更多细节
memdiag histogram --pid 12345 -l 50
```

**场景 2: 发现内存持续增长，需要建立基线**
```bash
# 在应用启动后保存当前堆状态作为基线
memdiag snapshot --save --id=baseline --pid 12345

# 运行一段时间后，再次查看堆分布
memdiag histogram --pid 12345

# 对比看哪些类增长最多
memdiag diff --baseline=baseline --pid 12345
```

**场景 3: OOM 发生后，快速分析现场**
```bash
# OOM 后立即查看堆直方图（注意：目标 JVM 可能已经不稳定）
memdiag histogram --pid 12345 -l 100

# 保存快照供后续离线分析
memdiag snapshot --save --id=post-oom --pid 12345
```

#### 命令使用示例

```bash
# 基本用法: 分析当前 JVM（默认前 20 行）
memdiag histogram

# 指定输出行数
memdiag histogram -l 50
memdiag histogram --limit 100

# 分析指定进程（使用 --pid）
memdiag histogram --pid 12345
memdiag histogram --pid 12345 -l 20

# 分析指定进程（使用位置参数）
memdiag histogram 12345
memdiag histogram 12345 -l 50
```

#### 输出说明

```
CLASS NAME                                  OBJECTS    SHALLOW HEAP
-------------------------------------------------------------------------
[B (java.base@17.0.15)                      15,856      53,295,536
java.lang.String (java.base@17.0.15)        15,011         360,264
java.lang.Class (java.base@17.0.15)          2,532         308,152
[Ljava.lang.Object; (java.base@17.0.15)      2,318         158,496
...rrentHashMap$Node (java.base@17.0.15)     4,007         128,224
-------------------------------------------------------------------------
Total                                         39,724      54,250,672
```

字段说明：
- **CLASS NAME**: 类名
- **OBJECTS**: 对象数量
- **SHALLOW HEAP**: 浅层大小（不包含引用对象）

#### 实现原理

`HistogramCommand` 通过 `JmxHeapAnalyzer` 调用 JMX 的 `com.sun.management:type=HotSpotDiagnostic` MBean 的 `dumpHeap()` 方法，或直接使用 `getHeapHistogram()` 操作获取堆直方图数据。数据经 `HeapHistogram` 类处理后按大小排序显示。

---

### threads - 线程分析

**子命令**: `threads`

分析 JVM 中所有线程的状态和堆栈。

#### 功能说明

- 显示所有线程的状态（RUNNABLE, BLOCKED, WAITING 等）
- 可选显示完整堆栈跟踪
- 识别死锁和阻塞点

#### 选项

| 选项 | 说明 | 默认值 |
|-----|------|--------|
| `-h, --help` | 显示帮助信息 | - |
| `-l, --limit <n>` | 限制显示线程数 | 20 |
| `-s, --stacks` | 显示完整堆栈跟踪 | false |

#### 使用场景示例

**场景 1: 应用卡死，无响应**
```bash
# 先查看线程状态概览，看是否有大量 BLOCKED 或 WAITING 线程
memdiag threads --pid 12345

# 如果发现有阻塞线程，查看完整堆栈定位死锁或阻塞点
memdiag threads --pid 12345 -s
```

**场景 2: CPU 使用率过高**
```bash
# 查看线程状态，看是否有过多 RUNNABLE 线程
memdiag threads --pid 12345

# 查看 RUNNABLE 线程的堆栈，找出消耗 CPU 的代码
memdiag threads --pid 12345 -s | grep -A 20 "RUNNABLE"
```

**场景 3: 偶发超时，需要诊断**
```bash
# 定时采样线程状态，对比分析
memdiag threads --pid 12345 > threads_$(date +%H%M%S).txt
sleep 10
memdiag threads --pid 12345 > threads_$(date +%H%M%S).txt

# 对比看哪些线程状态发生了变化
diff threads_*.txt
```

#### 命令使用示例

```bash
# 基本用法: 显示线程概览
memdiag threads

# 显示完整堆栈跟踪
memdiag threads -s
memdiag threads --stacks

# 分析指定进程的线程
memdiag threads --pid 12345
memdiag threads --pid 12345 -s

# 位置参数形式
memdiag threads 12345
memdiag threads 12345 -s -l 10
```

#### 输出说明

```
THREAD ANALYSIS
==========================================================================
Total: 12, Peak: 12, Daemon: 11, Started: 12

THREAD STATES:
  RUNNABLE:       6
  BLOCKED:        0
  WAITING:        1
  TIMED_WAITING:  5

TOP THREADS:
--------------------------------------------------------------------------
TID      NAME                           STATE              BLOCKED     WAITED
--------------------------------------------------------------------------
19       JMX server connection timeo... TIMED_WAITING           20         21
18       JMX server connection timeo... TIMED_WAITING            2          3
17       RMI Scheduler(0)               TIMED_WAITING            0          1
16       RMI TCP Connection(2)-192.1... RUNNABLE                 0          1
15       RMI TCP Accept-0               RUNNABLE                 0          0
```

线程状态说明：
- **RUNNABLE**: 正在执行或可执行
- **BLOCKED**: 等待监视器锁
- **WAITING**: 无限期等待另一个线程
- **TIMED_WAITING**: 有时限的等待
- **TERMINATED**: 已终止

#### 实现原理

`ThreadsCommand` 通过 `ThreadAnalyzer` 使用 JMX 的 `java.lang:type=Threading` MBean 获取线程信息。它调用 `getAllThreadIds()` 获取所有线程 ID，然后使用 `getThreadInfo()` 获取每个线程的详细信息（包括状态、堆栈、阻塞计数等）。

---

### diagnose - 自动诊断

**子命令**: `diagnose`

运行完整的内存诊断，自动识别问题并给出建议。

#### 功能说明

- 综合堆内存、线程、GC 等多维度分析
- 自动检测内存泄漏风险
- 识别配置问题
- 提供具体的优化建议

#### 选项

| 选项 | 说明 |
|-----|------|
| `-h, --help` | 显示帮助信息 |

#### 使用场景示例

**场景 1: 首次排查问题，快速了解整体状况**
```bash
# 运行完整诊断，获取全面的问题分析
memdiag diagnose --pid 12345

# 根据诊断结果的建议，决定下一步使用哪个命令深入分析
```

**场景 2: 定期健康检查**
```bash
# 每天/每小时运行一次诊断，建立健康基线
memdiag diagnose --pid 12345 > health_check_$(date +%Y%m%d_%H%M).log

# 对比历史诊断结果，看是否有新问题出现
diff health_check_*.log
```

**场景 3: 问题复现后，先运行诊断再深入**
```bash
# 问题发生后，先用 diagnose 快速定位问题方向
memdiag diagnose --pid 12345

# 然后根据诊断结果选择对应的深入分析命令
# 例如：诊断提示堆内存问题 → 用 histogram/diff 进一步分析
# 例如：诊断提示线程问题 → 用 threads -s 查看堆栈
```

#### 命令使用示例

```bash
# 诊断当前 JVM
memdiag diagnose

# 诊断指定进程
memdiag diagnose --pid 12345
memdiag diagnose 12345
```

#### 输出说明

```
DIAGNOSIS REPORT
==========================================================================
Generated at: 2026-03-26T16:40:03.149267918Z
Heap: 60,717,360 bytes used / 136,314,880 bytes committed
Threads: 13 active

SUMMARY:
Analysis complete: 0 critical, 0 warning, 0 info issues found. Heap: 55,312,816 bytes used, 100 classes. Threads: 13 active.

✅ No issues found!
```

诊断报告包含：
- **CRITICAL ISSUES**: 严重问题（红色）
- **WARNING ISSUES**: 警告信息（黄色）
- **INFO ISSUES**: 信息提示（绿色）
- **RECOMMENDATIONS**: 优化建议

#### 内置诊断规则

| 规则类型 | 阈值 | 严重程度 | 说明 |
|---------|------|----------|------|
| **LARGE_CLASS** | 单个类占用 > 100MB | WARNING | 检测占用过大内存的类 |
| **MANY_INSTANCES** | 单个类实例数 > 100,000 | INFO | 检测实例数过多的类 |
| **MANY_BLOCKED_THREADS** | BLOCKED 线程 > 10 个 | CRITICAL | 检测大量阻塞线程（可能死锁） |
| **LARGE_COLLECTION** | 集合类实例数 > 50,000 | INFO | 检测大量使用的集合（List/Map/Set） |
| **HEAP_LEAK_SUSPECT** | 复合条件 | WARNING | 堆内存泄漏嫌疑检测（高实例数+大内存、堆占比>20%、缓存/集合异常） |

#### 实现原理

`DiagnoseCommand` 使用 `DiagnosisEngine` 整合多个分析器：
1. **堆内存分析**: 检查大对象、可疑增长、Histogram 异常
2. **线程分析**: 检查死锁、阻塞线程、线程数过多
3. **GC 分析**: 检查 GC 频率、GC 时间过长
4. **配置检查**: 验证堆大小、GC 策略等配置

诊断引擎根据预定义的规则判断问题级别，并提供相应的建议。

##### 规则可扩展性

诊断引擎采用可扩展的插件架构：

**内置规则（默认启用）：**
- `LARGE_CLASS` - 检测 > 100MB 的类
- `MANY_INSTANCES` - 检测 > 100,000 实例的类
- `MANY_BLOCKED_THREADS` - 检测 > 10 个阻塞线程
- `LARGE_COLLECTION` - 检测 > 50,000 实例的集合类
- `HEAP_LEAK_SUSPECT` - 堆内存泄漏嫌疑检测

**HEAP_LEAK_SUSPECT 检测指标：**
- 高实例数（>50,000）+ 大内存占用（>50MB）
- 单个类占用堆内存 > 20%
- 缓存/缓冲类名 + 异常实例数
- 大集合类（List/Map/Set）+ 异常实例数

**添加自定义规则：**

方式一：编程方式注册
```java
// 1. 实现 DiagnosisRule 接口
public class MyCustomRule implements DiagnosisRule {
    @Override
    public String getId() { return "MY_RULE"; }

    @Override
    public List<Issue> evaluate(DiagnosisContext context) {
        // 自定义检测逻辑
    }
}

// 2. 注册到引擎
RuleRegistry registry = new RuleRegistry();
registry.registerDefaultRules();  // 保留内置规则
registry.register(new MyCustomRule());

// 3. 创建带自定义规则的引擎
DiagnosisEngine engine = new DiagnosisEngine(
    jmxClient, heapAnalyzer, threadAnalyzer, registry);
```

方式二：ServiceLoader 自动发现
1. 实现 `DiagnosisRule` 接口
2. 在 `META-INF/services/com.memdiag.core.diagnose.DiagnosisRule` 中添加实现类全名
3. 调用 `registry.discoverRules()` 自动加载

**规则上下文数据：**
- `context.getHeapHistogram()` - 堆直方图
- `context.getThreadDump()` - 线程转储
- `context.getTotalHeapUsed()` - 已用堆内存
- `context.getTotalHeapCommitted()` - 已提交堆内存

---

### native - 堆外内存分析

**子命令**: `native`

分析 JVM 的堆外内存使用情况（Linux 专用）。

#### 功能说明

- 通过 `/proc` 文件系统分析堆外内存
- 显示虚拟内存和物理内存使用
- 识别内存区域和加载的库
- 检测堆外内存泄漏

**注意**：本命令在整体的两种使用模式（JMX 模式 / Agent 模式）下都能工作：
- **JMX 模式（默认）**：CLI 直接读取本地 `/proc`（仅同一机器时可用）
- **Agent 模式（`--agent`）**：Agent 在目标 JVM 读取 `/proc`，通过 HTTP 返回（支持跨机器）

#### 在两种使用模式下的差异

| 特性 | JMX 模式（默认） | Agent 模式（`--agent`） |
|------|-----------------|---------------------|
| **数据来源** | CLI 直接读本地 `/proc` | Agent 读 `/proc` + HTTP |
| **跨机器** | ❌ 仅同一机器 | ✅ 支持远程 |
| **状态保持** | ❌ 每次独立 | ✅ Agent 保持状态 |
| **对目标影响** | 无影响 | Agent 运行时轻微影响 |

#### ⚠️ 对目标进程的影响

| 选项 | 影响 |
|-----|------|
| `--status`/`--summary`/`--regions`/`--diagnose` | **无影响** - 只读 /proc 文件系统或 Agent API |
| `--attach` | **轻微影响** - 动态 attach Java Agent，短暂 Safe Point |
| `--detach` | **轻微影响** - 停止 Agent HTTP 服务 |

#### 选项

| 选项 | 说明 | 默认值 |
|-----|------|--------|
| `-h, --help` | 显示帮助信息 | - |
| `--status` | 检查堆外分析是否可用 | - |
| `--summary` | 显示内存摘要 | - |
| `--regions` | 显示内存区域分布 | - |
| `--diagnose` | 运行堆外泄漏诊断 | - |
| `--attach` | 动态 attach MemDiag Agent | - |
| `--detach` |  detach MemDiag Agent | - |
| `-l, --limit <n>` | 限制输出行数 | 20 |
| `--agent-host` | Agent HTTP 服务主机 | localhost |
| `--agent-port` | Agent HTTP 服务端口 | 6789 |
| `--agent-jar` | memdiag-agent.jar 路径（用于动态 attach） | - |

#### 使用 Agent 模式（推荐）

**方式一：启动时加载（推荐用于生产环境）**
```bash
# 在目标 JVM 启动时加载 Agent
java -javaagent:memdiag-agent.jar=port=6789 -jar your-app.jar

# 然后通过 --agent 选项连接
memdiag native --status --agent=localhost:6789
memdiag native --summary --agent=localhost:6789
```

**方式二：动态 attach（推荐用于测试/调试）**
```bash
# 动态 attach 到运行中的 JVM
memdiag native --attach --agent-jar /path/to/memdiag-agent.jar --pid 12345

# 后续命令通过 --agent 选项连接到 Agent（推荐）
memdiag native --status --agent=localhost:6789
memdiag native --summary --agent=localhost:6789
memdiag native --regions --agent=localhost:6789
memdiag native --diagnose --agent=localhost:6789

# detach Agent
memdiag native --detach --agent=localhost:6789
```

**重要提示**：
- Attach 成功后，Agent 会在目标 JVM 内启动 HTTP 服务（默认端口 6789）
- 建议使用 `--agent=localhost:6789` 选项明确指定连接 Agent
- 所有其他命令（histogram、threads、diagnose 等）也支持 `--agent` 选项

**Agent 模式优势**：
- Agent 运行在目标 JVM 内部，数据更准确
- 支持跨机器调用
- 支持跨 CLI 调用保持状态
- 通过 HTTP API 通信，更稳定

#### 使用 JMX 模式（默认，无需 Agent）

如果没有加载 Agent，CLI 会直接读取本地 `/proc`（仅限同一机器）：

```bash
# 直接解析 /proc 文件系统（无需 Agent）
memdiag native --status --pid 12345
memdiag native --summary --pid 12345
memdiag native --regions --pid 12345
memdiag native --diagnose --pid 12345
```

#### 使用场景示例

**场景 1: 堆内存正常，但 RSS 持续增长（堆外泄漏嫌疑）**
```bash
# 先检查堆外内存分析是否可用
memdiag native --status --pid 12345

# 查看堆外内存摘要，看虚拟内存和物理内存使用情况
memdiag native --summary --pid 12345

# 查看详细的内存区域分布，定位哪个区域在增长
memdiag native --regions --pid 12345 -l 50

# 运行堆外泄漏诊断，获取具体建议
memdiag native --diagnose --pid 12345
```

**场景 2: 需要持续监控，不想每次都 attach**
```bash
# 方式一：启动目标 JVM 时就加载 Agent（推荐生产环境）
java -javaagent:memdiag-agent.jar=port=6789 -jar your-app.jar

# 然后所有命令都通过 Agent 模式使用
memdiag native --summary --agent=localhost:6789
memdiag histogram --agent=localhost:6789
memdiag threads --agent=localhost:6789
```

**场景 3: 问题已经发生，需要动态 attach Agent**
```bash
# 动态 attach 到运行中的 JVM
memdiag native --attach --agent-jar /path/to/memdiag-agent.jar --pid 12345

# 确认 Agent 已启动
memdiag native --status --agent=localhost:6789

# 使用 Agent 模式进行深入分析
memdiag allocations --summary --agent=localhost:6789
memdiag methods --agent=localhost:6789
```

#### 命令使用示例

```bash
# === 基础分析 ===

# 检查状态和可用模式
memdiag native --status

# 显示内存摘要
memdiag native --summary
memdiag native --summary --pid 12345

# 显示内存区域
memdiag native --regions
memdiag native --regions 12345

# 运行堆外诊断
memdiag native --diagnose

# === Agent 控制 ===

# 动态 attach Agent
memdiag native --attach --agent-jar /path/to/memdiag-agent.jar --pid 12345

# detach Agent
memdiag native --detach --pid 12345
```

#### 输出示例 - 状态

```
NATIVE MEMORY ANALYSIS STATUS
==========================================================================
Available: ✅ Yes
Platform: Linux
Mode: Agent Mode (HTTP API)
Agent Connected: ✅ Yes
Agent Endpoint: localhost:6789

USAGE OPTIONS:
  ✅ With --agent (recommended) - Connect to running memdiag-agent.jar
     - Start target JVM with: java -javaagent:memdiag-agent.jar=port=6789 ...
     - Or attach dynamically: memdiag native --attach --agent-jar <path> --pid <pid>
     - Use with: memdiag native --status --agent=localhost:6789

  ✅ Without --agent (default) - Direct local /proc filesystem access
     - No agent required
     - Limited to same machine
     - Use with: memdiag native --status --pid 12345
```

#### 输出示例 - 内存摘要

```
NATIVE MEMORY SUMMARY
==========================================================================
Total Virtual:    4,802,461,696 bytes (4,579.98 MB)
Total Resident:     162,054,144 bytes (154.55 MB)
```

#### 输出示例 - 内存区域

```
MEMORY REGIONS
==========================================================================
START              END                PERMS            SIZE          RSS FILE
--------------------------------------------------------------------------
00000000c0000000-00000000c9900000 rw-p      160,432,128   93,519,872 [anonymous]
00000000c9900000-00000000ffc00000 ---p      909,115,392            0 [anonymous]
00000000ffc00000-00000000ffc75000 rw-p          479,232      479,232 /lib/libc-2.31.so
...
--------------------------------------------------------------------------
Total: 127 regions
```

#### 实现原理

`NativeCommand` 根据是否使用 `--agent` 选项选择不同实现：
- **使用 `--agent`**：`AgentNativeAnalyzer` 通过 HTTP 与 `memdiag-agent.jar` 通信
- **不使用 `--agent`**：`ProcFileSystemNativeAnalyzer` 直接解析 `/proc/<pid>/maps` 和 `/proc/<pid>/smaps`（无侵入，仅同一机器可用）

Agent 内部未来可选加载 JVMTI 原生库进行增强

---

### snapshot - 快照管理

**子命令**: `snapshot`

管理堆快照：保存、加载、列出、删除。

#### 功能说明

- 保存当前堆状态到持久化存储
- 加载历史快照进行分析
- 列出所有可用快照
- 删除不再需要的快照

#### ⚠️ 对目标进程的影响（仅 --save）

使用 `--save` 保存快照时：
- 需要获取堆直方图，影响同 `histogram` 命令
- 可能触发 Full GC
- 需要进入 Safe Point，会**短暂暂停所有应用线程**
- 已配置 500ms 超时保护

注：`--list`、`--load`、`--delete` 仅操作本地文件，对目标进程无影响。

#### 选项

| 选项 | 说明 | 默认值 |
|-----|------|--------|
| `-h, --help` | 显示帮助信息 | - |
| `--save` | 保存新快照 | - |
| `--load <id>` | 加载并显示快照（ID 或文件名） | - |
| `--list` | 列出所有保存的快照 | - |
| `--delete <id>` | 删除快照（ID 或文件名） | - |
| `--id <id>` | 自定义快照 ID（保存时使用） | - |
| `-l, --limit <n>` | 限制类显示行数 | 20 |

#### 使用场景示例

**场景 1: 内存泄漏调查 - 建立时间序列对比**
```bash
# 应用刚启动时，保存初始基线
memdiag snapshot --save --id=after-start --pid 12345

# 运行 1 小时后，保存第二张快照
memdiag snapshot --save --id=after-1h --pid 12345

# 运行 6 小时后，保存第三张快照
memdiag snapshot --save --id=after-6h --pid 12345

# 对比初始和 6 小时后的状态，看哪些类持续增长
memdiag diff --baseline=after-start --current=after-6h

# 对比 1 小时和 6 小时，看增长是否加速
memdiag diff --baseline=after-1h --current=after-6h
```

**场景 2: 问题复现 - 保存前后对比**
```bash
# 问题复现前，保存基准快照
memdiag snapshot --save --id=before-repro --pid 12345

# 执行复现步骤...
# 例如：触发某个 API 调用、执行某个批处理任务

# 问题复现后，立即保存当前快照
memdiag snapshot --save --id=after-repro --pid 12345

# 对比两个快照，找出内存变化
memdiag diff --baseline=before-repro --current=after-repro
```

**场景 3: 发布验证 - 版本对比**
```bash
# 旧版本运行一段时间后保存快照
memdiag snapshot --save --id=v1.0 --pid 12345

# 升级到新版本，同样运行时间后保存快照
memdiag snapshot --save --id=v2.0 --pid 12345

# 对比两个版本的内存使用，看是否有回归
memdiag diff --baseline=v1.0 --current=v2.0
```

**场景 4: 离线分析 - 保存现场后慢慢分析**
```bash
# 问题发生时，立即保存快照（避免目标 JVM 重启丢失数据）
memdiag snapshot --save --id=incident-$(date +%Y%m%d-%H%M) --pid 12345

# 列出所有快照，确认已保存
memdiag snapshot --list

# 可以安全地重启目标 JVM 了

# 之后随时加载快照进行离线分析
memdiag snapshot --load=incident-20260329-1030
memdiag diff --baseline=earlier-snapshot --current=incident-20260329-1030
```

#### 命令使用示例

```bash
# === 基本操作 ===

# 保存快照（自动生成 ID）
memdiag snapshot --save --pid 12345

# 保存快照并指定 ID
memdiag snapshot --save --id=baseline --pid 12345

# 列出所有快照
memdiag snapshot --list

# 加载快照（使用 ID）
memdiag snapshot --load=baseline --limit=50

# 加载快照（使用完整文件名）
memdiag snapshot --load=snapshot-test1-20260326-164306.snapshot

# 删除快照
memdiag snapshot --delete=baseline

# === 完整工作流 ===

# 步骤 1: 保存基准快照
memdiag snapshot --save --id=before --pid 12345

# 步骤 2: 执行一些操作...

# 步骤 3: 保存当前快照
memdiag snapshot --save --id=after --pid 12345

# 步骤 4: 对比两个快照
memdiag diff --baseline=before --current=after
```

#### 输出示例 - 保存快照

```
CREATING SNAPSHOT
==========================================================================
✅ Snapshot created successfully!

Snapshot ID: test1
Saved to: /root/.memdiag/snapshots/snapshot-test1-20260326-164403.snapshot

Snapshot contains:
  - 984 classes
  - 67,979 objects
  - 55,473,072 bytes
  - 14 threads

To load this snapshot later:
  memdiag snapshot --load test1
```

#### 输出示例 - 列出快照

```
AVAILABLE SNAPSHOTS
==========================================================================
ID           TIMESTAMP                         SIZE  FILENAME
--------------------------------------------------------------------------
test2        2026-03-26 16:44:05            90.3 KB  snapshot-test2-20260326-164405.snapshot
test1        2026-03-26 16:44:03            89.6 KB  snapshot-test1-20260326-164403.snapshot
--------------------------------------------------------------------------
Total: 2 snapshot(s)

To load a snapshot:
  memdiag snapshot --load <ID>

To delete a snapshot:
  memdiag snapshot --delete <ID>
```

#### 实现原理

`SnapshotCommand` 使用 `SnapshotManager` 进行快照管理：
1. **保存**: 捕获当前堆直方图和线程转储，创建 `Snapshot` 对象，使用 Java 序列化保存到 `~/.memdiag/snapshots/` 目录
2. **加载**: 从文件反序列化 `Snapshot` 对象并显示其内容
3. **文件命名**: `snapshot-{id}-{timestamp}.snapshot`，例如 `snapshot-test1-20260326-164403.snapshot`

快照包含：堆直方图、线程转储、时间戳、ID。

---

### diff - 堆对比分析

**子命令**: `diff`

对比两个堆快照，识别内存变化。

#### 功能说明

- 对比基准快照和当前快照
- 识别增长和减少的类
- 计算增长率
- 显示新增和消失的类

#### 选项

| 选项 | 说明 | 默认值 |
|-----|------|--------|
| `-h, --help` | 显示帮助信息 | - |
| `--baseline <id>` | 基准快照（必需，ID 或文件名） | - |
| `--current <id>` | 当前快照（可选，默认实时堆） | - |
| `--growing <n>` | 显示 Top N 增长类 | 10 |
| `--shrinking <n>` | 显示 Top N 减少类 | 5 |
| `--growth-rate <n>` | 显示 Top N 增长率类 | 5 |
| `--all` | 显示所有变化（不只是 Top N） | false |

#### 使用场景示例

**场景 1: 内存泄漏调查 - 定位增长的类**
```bash
# 对比两个时间点的快照，看哪些类增长最多
memdiag diff --baseline=morning --current=afternoon --growing=30

# 重点关注增长率高的类（GROWTH 列）
# 某个类持续高增长 → 泄漏嫌疑大
```

**场景 2: 怀疑某个操作导致内存泄漏**
```bash
# 操作前保存快照
memdiag snapshot --save --id=before-op --pid 12345

# 执行可疑操作（比如：批量导入数据、触发某个定时任务）

# 操作后立即保存快照
memdiag snapshot --save --id=after-op --pid 12345

# 对比看哪些类因为这个操作增长了
memdiag diff --baseline=before-op --current=after-op --all
```

**场景 3: 发布后内存监控 - 对比版本**
```bash
# 旧版本运行稳定后保存快照
memdiag snapshot --save --id=v1-stable --pid 12345

# 升级到新版本，同样负载运行相同时间
memdiag snapshot --save --id=v2-stable --pid 12345

# 对比看内存使用是否有明显增加
memdiag diff --baseline=v1-stable --current=v2-stable

# 如果新版本某些类异常增长 → 可能有回归
```

**场景 4: 持续监控 - 与基准快照实时对比**
```bash
# 建立一个健康的基准快照
memdiag snapshot --save --id=healthy-baseline --pid 12345

# 后续随时与基准对比，看是否偏离健康状态
memdiag diff --baseline=healthy-baseline --pid 12345

# 如果发现异常增长，再保存当前快照做进一步分析
memdiag snapshot --save --id=current-abnormal --pid 12345
```

#### 命令使用示例

```bash
# === 基本对比 ===

# 对比两个已保存的快照
memdiag diff --baseline=before --current=after

# 对比基准快照与当前实时堆
memdiag diff --baseline=before --pid 12345

# 自定义显示数量
memdiag diff --baseline=before --current=after --growing=20 --shrinking=10

# 显示所有变化
memdiag diff --baseline=before --current=after --all

# === 完整泄漏检测工作流 ===

# 步骤 1: 保存基准快照
memdiag snapshot --save --id=time0 --pid 12345

# 步骤 2: 等待一段时间...
sleep 60

# 步骤 3: 对比并查看增长
memdiag diff --baseline=time0 --pid 12345 --growing=20

# 步骤 4: 保存当前快照以便后续对比
memdiag snapshot --save --id=time1 --pid 12345
```

#### 输出示例

```
HEAP DIFF ANALYSIS
==========================================================================
Baseline: [test1] 2026-03-26T16:44:03.452912271Z - 67,979 objs, 52.9 MB
Current:  [test2] 2026-03-26T16:44:05.734735617Z - 68,185 objs, 82.9 MB

SUMMARY
--------------------------------------------------------------------------
Total object delta: +206
Total byte delta:   +30.0 MB
Changed classes:    988

GROWING CLASSES (top 5)
--------------------------------------------------------------------------
CLASS NAME                                     OBJ DELTA      BYTE DELTA     GROWTH
--------------------------------------------------------------------------
[B (java.base@17.0.15)                               +28        +30.0 MB       0.2%
...ConcurrentHashMap (java.base@17.0.15)             +21         +1.3 KB       6.4%
...flect.Constructor (java.base@17.0.15)              +8          +576 B       3.0%
java.lang.String (java.base@17.0.15)                 +23          +552 B       0.1%
java.lang.Class (java.base@17.0.15)                   +4          +448 B       0.2%

SHRINKING CLASSES (top 1)
--------------------------------------------------------------------------
CLASS NAME                                     OBJ DELTA      BYTE DELTA     GROWTH
--------------------------------------------------------------------------
...ethodAccessorImpl (java.base@17.0.15)              -4          -128 B     -14.8%

NEW CLASSES (4)
--------------------------------------------------------------------------
CLASS NAME                                       OBJECTS           BYTES
--------------------------------------------------------------------------
...dMethodAccessor37 (java.base@17.0.15)               1              16
...dMethodAccessor36 (java.base@17.0.15)               1              16
...dMethodAccessor35 (java.base@17.0.15)               1              16
...dMethodAccessor34 (java.base@17.0.15)               1              16

TOP BY GROWTH RATE (top 5)
--------------------------------------------------------------------------
CLASS NAME                                     OBJ DELTA      BYTE DELTA     GROWTH
--------------------------------------------------------------------------
...dMethodAccessor37 (java.base@17.0.15)              +1           +16 B        new
...dMethodAccessor36 (java.base@17.0.15)              +1           +16 B        new
...dMethodAccessor35 (java.base@17.0.15)              +1           +16 B        new
...dMethodAccessor34 (java.base@17.0.15)              +1           +16 B        new
...ctDelegator (java.management@17.0.15)              +1           +16 B      25.0%
```

#### 实现原理

`DiffCommand` 使用 `HeapDiff` 计算两个快照的差异：
1. **加载快照**: 从文件加载基准快照（通过 `SnapshotManager`）
2. **获取当前状态**: 如果未指定 `--current`，则实时从目标 JVM 获取堆直方图
3. **计算差异**: `HeapDiff.compute()` 逐个类对比：
   - 对象数增量
   - 字节数增量
   - 增长率
   - 识别新增类（基线不存在）
   - 识别消失类（当前不存在）
4. **排序显示**: 按字节增量排序增长类、按增长率排序等

---

### report - 报告生成

**子命令**: `report`

生成完整的诊断报告，支持多种格式。

#### 功能说明

- 整合所有分析结果
- 支持文本、HTML、JSON 格式
- 可输出到文件或标准输出

#### 选项

| 选项 | 说明 | 默认值 |
|-----|------|--------|
| `-h, --help` | 显示帮助信息 | - |
| `-f, --format <format>` | 输出格式: text, html, json | text |
| `-o, --output <file>` | 输出文件路径（默认 stdout） | - |
| `-l, --limit <n>` | 类显示限制 | 50 |

#### 使用场景示例

**场景 1: 问题上报 - 生成完整报告分享给团队**
```bash
# 生成 HTML 格式的完整报告，方便在浏览器中查看
memdiag report --format=html --output=mem-report-$(date +%Y%m%d-%H%M).html --pid 12345

# 或者生成 JSON 格式，便于自动化分析
memdiag report --format=json --output=mem-report-$(date +%Y%m%d-%H%M).json --pid 12345
```

**场景 2: 定期健康检查 - 保存历史报告**
```bash
# 每天生成一份报告，建立健康基线
memdiag report --format=html --output=daily-report-$(date +%Y%m%d).html --pid 12345

# 对比多天的报告，看趋势变化
open daily-report-20260328.html
open daily-report-20260329.html
```

**场景 3: 问题现场记录 - OOM 后立即保存**
```bash
# OOM 发生后，立即生成完整报告保存现场
memdiag report --format=html --output=oom-report-$(date +%Y%m%d-%H%M).html --pid 12345

# 同时保存快照，方便后续深入分析
memdiag snapshot --save --id=oom-$(date +%Y%m%d-%H%M) --pid 12345
```

**场景 4: 快速查看 - 文本报告直接输出**
```bash
# 直接在终端查看文本报告，快速获取整体状况
memdiag report --pid 12345

# 或者输出到文件
memdiag report --pid 12345 > report-$(date +%Y%m%d-%H%M).txt
```

#### 命令使用示例

```bash
# 生成文本报告到 stdout
memdiag report --pid 12345
memdiag report --format=text --pid 12345

# 生成 HTML 报告到文件
memdiag report --format=html --output=report.html --pid 12345

# 生成 JSON 报告
memdiag report --format=json --output=report.json --pid 12345

# 使用位置参数
memdiag report 12345 --format=html --output=report.html
```

#### 实现原理

`ReportCommand` 使用格式化器生成报告：
- **TextFormatter**: 纯文本格式，适合控制台
- **HtmlFormatter**: HTML 格式，带样式，适合浏览器查看
- **JsonFormatter**: JSON 格式，适合机器处理

报告包含：堆直方图、线程分析、诊断结果等完整信息。

---

### nmt - NMT 分析

**子命令**: `nmt`

使用 JVM Native Memory Tracking 进行分析（需启用 NMT）。

#### 功能说明

- 解析 NMT 输出
- 按类别显示内存使用
- 支持对比 NMT 快照

#### 前置条件

目标 JVM 必须启动时添加 `-XX:NativeMemoryTracking=summary` 或 `-XX:NativeMemoryTracking=detail` 参数。

#### 选项

| 选项 | 说明 |
|-----|------|
| `-h, --help` | 显示帮助信息 |

#### 使用示例

```bash
# 目标 JVM 启动示例（启用 NMT）
java -XX:NativeMemoryTracking=summary -jar myapp.jar

# 分析 NMT
memdiag nmt --pid 12345
```

#### 实现原理

`NmtCommand` 使用 `JmxNmtAnalyzer` 通过 JMX 调用 `com.sun.management:type=DiagnosticCommand` 的 `vmNativeMemory` 命令获取 NMT 数据，然后用 `NmtParser` 解析输出。

---

### agent - Agent 管理

**子命令**: `agent`

管理和交互运行中的 MemDiag Agent。

#### 功能说明

- 查看 Agent 状态、配置、指标
- 查看分配统计和趋势
- 查看方法监控统计
- 启用/禁用插桩功能
- 查看 JVMTI 状态

#### 子命令

| 子命令 | 说明 |
|--------|------|
| `status` | 查看 Agent 状态 |
| `config` | 查看/更新 Agent 配置 |
| `metrics` | 查看 Agent 指标 |
| `allocations` | 查看分配统计（支持 --recent/--stats/--top/--rate/--summary） |
| `methods` | 查看方法监控统计（支持 --stats/--slow） |
| `enable` | 启用插桩功能（allocation/methods） |
| `disable` | 禁用插桩功能（allocation/methods） |
| `jvmti` | 查看 JVMTI 状态 |

#### 使用场景示例

**场景 1: 刚 attach Agent，确认是否工作正常**
```bash
# 查看 Agent 状态，确认连接正常
memdiag agent status --agent=localhost:6789

# 查看 Agent 配置
memdiag agent config --agent=localhost:6789

# 查看 Agent 自身指标
memdiag agent metrics --agent=localhost:6789
```

**场景 2: 需要深入监控，启用更详细的采集**
```bash
# 启用分配追踪（如果尚未启用）
memdiag agent enable allocation --agent=localhost:6789

# 启用方法监控
memdiag agent enable methods --agent=localhost:6789

# 确认已启用
memdiag agent status --agent=localhost:6789
```

**场景 3: 生产环境问题调查期间**
```bash
# 问题发生时，先查看 Agent 收集的分配统计
memdiag agent allocations --summary --agent=localhost:6789

# 再查看方法监控，看哪些方法执行慢
memdiag agent methods --stats --agent=localhost:6789

# 如果 JVMTI 已加载，也可以查看 JVMTI 状态
memdiag agent jvmti --agent=localhost:6789
```

#### 命令使用示例

```bash
# 查看 Agent 状态
memdiag agent status --agent=localhost:6789

# 查看 Agent 配置
memdiag agent config --agent=localhost:6789

# 查看 Agent 指标
memdiag agent metrics --agent=localhost:6789

# 查看分配统计
memdiag agent allocations --summary --agent=localhost:6789

# 查看方法统计
memdiag agent methods --stats --agent=localhost:6789

# 启用分配追踪
memdiag agent enable allocation --agent=localhost:6789

# 启用方法监控
memdiag agent enable methods --agent=localhost:6789

# 禁用功能
memdiag agent disable allocation --agent=localhost:6789
memdiag agent disable methods --agent=localhost:6789

# 查看 JVMTI 状态
memdiag agent jvmti --agent=localhost:6789
```

---

### allocations - 分配统计

**子命令**: `allocations`

显示内存分配统计和趋势（需要 Agent 模式）。

#### 功能说明

- 显示总分配量和分配次数
- 显示当前分配速率
- 显示分配趋势（增长/下降/稳定）
- 显示 Top N 分配类型

#### 选项

| 选项 | 说明 | 默认值 |
|-----|------|--------|
| `-h, --help` | 显示帮助信息 | - |
| `-l, --limit <n>` | 限制 Top 类型显示 | 10 |

#### 使用场景示例

**场景 1: 内存使用持续增长，需要了解分配热点**
```bash
# 查看分配摘要，了解总体分配情况
memdiag allocations --summary --agent=localhost:6789

# 查看 Top 分配类型，看哪些类型分配最多
memdiag allocations --agent=localhost:6789 -l 30

# 重点关注 ByteBuffer、byte[] 等堆外相关类型
```

**场景 2: 怀疑 ByteBuffer 泄漏，监控分配趋势**
```bash
# 定期采样，记录分配速率
memdiag allocations --summary --agent=localhost:6789 > alloc-$(date +%H%M).log
sleep 300
memdiag allocations --summary --agent=localhost:6789 > alloc-$(date +%H%M).log

# 对比看分配速率是否异常
diff alloc-*.log
```

**场景 3: 性能优化，识别高分配的类型**
```bash
# 查看 Top 20 分配类型，找出优化目标
memdiag allocations --agent=localhost:6789 -l 20

# 对于高分配类型，考虑：
# - 对象池化
# - 减少不必要的分配
# - 使用更高效的数据结构
```

#### 命令使用示例

```bash
# 连接 Agent 查看分配统计（Top 10）
memdiag allocations --agent localhost:6789

# 查看分配摘要
memdiag allocations --summary --agent localhost:6789

# 指定限制数量（Top 20）
memdiag allocations --agent localhost:6789 -l 20
memdiag allocations --agent localhost:6789 --limit 30
```

#### 输出说明

```
Allocation Summary
==================
Total Allocated:  1,234,567,890 bytes (1,177.50 MB)
Total Count:      45,678 allocations
Current Rate:     10,234 bytes/sec (0.01 MB/sec)
Trend:            INCREASING

Top Types by Size:
TYPE NAME                                          TOTAL SIZE        COUNT
-------------------------------------------------------------------------------
java.nio.HeapByteBuffer                          567,890,123       12,345
[B                                                345,678,901       23,456
...
```

---

### methods - 方法监控

**子命令**: `methods`

显示方法监控统计（需要 Agent 模式）。

#### 功能说明

- 显示监控的方法总数
- 显示按总时间排序的 Top N 方法
- 显示按调用次数排序的 Top N 方法
- 显示方法执行时间统计（总时间、平均时间、最大时间）

#### 选项

| 选项 | 说明 | 默认值 |
|-----|------|--------|
| `-h, --help` | 显示帮助信息 | - |
| `-l, --limit <n>` | 限制方法列表 | 20 |
| `-s, --sort <type>` | 排序方式: time, count | time |

#### 使用场景示例

**场景 1: 应用响应慢，需要找出慢方法**
```bash
# 查看按总时间排序的 Top 20 方法，找出耗时最多的
memdiag methods --agent localhost:6789 -l 20 -s time

# 重点关注：
# - TOTAL(ms) 列：总耗时
# - AVG(ms) 列：平均耗时
# - MAX(ms) 列：最大耗时（可能有偶发慢请求）
```

**场景 2: 高频调用方法，需要优化调用次数**
```bash
# 查看按调用次数排序的方法
memdiag methods --agent localhost:6789 -s count -l 30

# 对于调用次数异常高的方法：
# - 考虑缓存结果
# - 检查是否有重复调用
# - 考虑批量处理
```

**场景 3: 性能回归分析，版本对比**
```bash
# 旧版本运行时，保存方法统计
memdiag methods --agent localhost:6789 -l 50 > methods-v1.txt

# 新版本运行时，同样负载下保存统计
memdiag methods --agent localhost:6789 -l 50 > methods-v2.txt

# 对比看哪些方法耗时增加了
diff methods-v1.txt methods-v2.txt
```

**场景 4: 定位偶发超时，看最大耗时**
```bash
# 查看方法统计，重点关注 MAX(ms) 列
memdiag methods --agent localhost:6789 -l 30

# 如果某个方法 MAX(ms) 远大于 AVG(ms)，说明有偶发慢请求
# 需要进一步查看该方法的代码，定位最坏情况
```

#### 命令使用示例

```bash
# 连接 Agent 查看方法统计（按总时间排序，Top 20）
memdiag methods --agent localhost:6789

# 按调用次数排序
memdiag methods --agent localhost:6789 -s count

# 指定限制数量
memdiag methods --agent localhost:6789 -l 30
memdiag methods --agent localhost:6789 --limit 30

# 组合使用
memdiag methods --agent localhost:6789 -s count -l 50
```

#### 输出说明

```
Method Monitoring Statistics
============================
Total Methods Monitored: 128

METHOD                                             COUNT    TOTAL(ms)     AVG(ms)     MAX(ms)
---------------------------------------------------------------------------------------------------------
com.example.MyService#processData                1,234     12,345.67       10.00       123.45
com.example.MyService#validate                   5,678      8,765.43        1.54        45.67
...
```

---

## 架构与实现原理

### 两种使用模式

MemDiag 提供**两种使用模式**，通过 `--agent` 选项切换：

| 特性 | JMX 模式（默认） | Agent 模式（`--agent`） |
|------|-----------------|---------------------|
| **触发方式** | 不指定 `--agent` | 指定 `--agent host:port` |
| **技术基础** | JMX Attach API | Java Agent + HTTP API |
| **目标 JVM 要求** | 任何 HotSpot JVM | 需要加载 `memdiag-agent.jar` |
| **预先配置** | 无需配置 | 启动时 `-javaagent:` 或动态 attach |
| **网络暴露** | 无（本地进程通信） | 默认 localhost:6789 |
| **跨机器分析** | ❌ 仅本地 | ✅ 支持 |
| **状态保持** | ❌ 每次重新连接 | ✅ Agent 保持状态 |
| **堆内存/线程分析** | ✅ | ✅ |
| **堆外内存分析** | ✅（同一机器）| ✅（远程也可）|
| **字节码插桩** | ❌ | ✅ ByteBuffer 分配追踪 + 方法监控 |
| **JVMTI 集成** | ❌ | ✅ 自动加载 + 优雅降级 |
| **分配统计** | ❌ | ✅ 实时速率 + Top N + 趋势分析 |
| **方法监控** | ❌ | ✅ 执行时间 + 调用次数 + 慢方法识别 |

---

### 关键概念澄清

#### 1. JMX 模式工作原理

JMX 模式**不依赖 Agent 注入**，也无需目标 JVM 预先配置：

```
memdiag-cli (JDK)
    │
    │ 1. VirtualMachine.attach(pid)
    │    ↓ (使用 tools.jar 的 Attach API)
    │ 2. 动态加载 Management Agent（如未启动）
    │    ↓
    │ 3. 获取本地 JMX 连接器地址
    │    ↓
    │ 4. 通过 JMX 协议通信（本地，不暴露网络）
    ↓
目标 JVM (任何 HotSpot JVM)
    ├─ java.lang:type=Memory
    ├─ java.lang:type=Threading
    └─ com.sun.management:type=HotSpotDiagnostic
```

**JMX 模式要求**：
- ✅ CLI 运行在 JDK（非 JRE）
- ✅ 与目标 JVM 同一用户
- ✅ 目标 JVM 是 HotSpot（大多数 JVM 都是）

---

#### 2. ProcFS 不是独立模式

`/proc` 文件系统是**数据来源**，不是独立模式：
- **JMX 模式**：CLI 直接读取本地 `/proc`（仅同一机器时可用）
- **Agent 模式**：Agent 在目标 JVM 本地读取 `/proc`，通过 HTTP 返回给 CLI

---

#### 3. JVMTI 的角色

- JVMTI 是 JVM 提供的原生层工具接口
- 当前版本：**框架已就绪，Agent 模式支持自动加载**
- 使用方式：
  - **JMX 模式**：不使用 JVMTI，仅通过 `/proc` 文件系统进行只读分析
  - **Agent 模式**：Java Agent 在**目标 JVM 内**尝试自动加载 JVMTI 库，支持优雅降级（如库不可用则仅使用 Java 层字节码插桩功能）
- **注意 `native --attach` 挂载的是 Java Agent**：不是 JVMTI Agent，挂载后可使用 Agent 模式的所有功能

---

#### 4. Agent 模式当前能力（v1.0）

| 能力 | 状态 |
|-----|------|
| HTTP API 服务（端口 6789） | ✅ |
| 堆直方图、线程分析、诊断报告 | ✅ |
| ProcFS 堆外内存分析 | ✅ |
| ByteBuffer 分配追踪（字节码插桩） | ✅ |
| 方法监控 API 框架 | ✅ |
| 分配统计（速率、Top N、趋势） | ✅ |
| JVMTI 自动检测与优雅降级 | ✅ |

---

### 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        memdiag-cli                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ HistogramCmd │  │  ThreadsCmd  │  │  DiagnoseCmd │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  NativeCmd   │  │ SnapshotCmd  │  │   DiffCmd    │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│                                                               │
│  使用模式选择：                                                │
│  ├─ 不指定 --agent  →  JMX 模式                            │
│  └─ 指定 --agent    →  Agent 模式                           │
└─────────────────────────────────────────────────────────────────┘
                              │
         ┌────────────────────┴────────────────────┐
         ▼                                         ▼
┌─────────────────────────┐           ┌─────────────────────────┐
│      JMX 模式          │           │     Agent 模式          │
│                         │           │                         │
│  memdiag-core          │           │  memdiag-core          │
│  ┌─────────────────┐  │           │  ┌─────────────────┐  │
│  │  JmxClient     │  │           │  │  AgentClient    │  │
│  │  (Attach API)  │  │           │  │  (HTTP)         │  │
│  └─────────────────┘  │           │  └─────────────────┘  │
└─────────────────────────┘           └─────────────────────────┘
         │                                         │
         ▼                                         ▼
┌──────────────────┐                    ┌─────────────────────────┐
│   目标 JVM       │                    │  memdiag-agent.jar    │
│  (HotSpot JVM)  │                    │  (在目标 JVM 内运行)  │
│                  │                    └─────────────────────────┘
│  内置 MBeans:    │                               │
│  - Memory        │                               ▼
│  - Threading     │                    ┌──────────────────┐
│  - HotSpotDiagnostic│               │    目标 JVM      │
└──────────────────┘                    └──────────────────┘
```

### 核心模块说明

#### 1. memdiag-cli - 命令行接口

使用 Picocli 框架实现子命令模式。每个子命令继承 `BaseCommand`，负责：
- 参数解析
- 调用 core 模块的分析器
- 结果格式化输出

#### 2. memdiag-core - 核心分析库

**JmxClient**: JMX 连接器，支持动态附着到目标 JVM
  - 自动发现 `tools.jar`
  - 支持 PID 附着和当前 JVM 附着

**JmxHeapAnalyzer**: 堆内存分析
  - 使用 `HotSpotDiagnostic` MBean
  - 支持超时保护（500ms）
  - ResourceLimiter 防止 Safe Point 停顿过长

**ThreadAnalyzer**: 线程分析
  - 使用 `Threading` MBean
  - 获取线程状态、堆栈、阻塞计数

**DiagnosisEngine**: 诊断引擎
  - 整合多个分析器
  - 规则匹配识别问题
  - 生成建议

**HeapDiff / Snapshot**: 快照与对比
  - Snapshot: 包含堆直方图和线程转储的不可变对象
  - SnapshotManager: 序列化存储管理
  - HeapDiff: 差异计算与分析

**EnvironmentPrecheck**: 环境预检
  - PID 存在性检查
  - JDK 检测（非 JRE）
  - 权限检查
  - Linux ptrace_scope 检测

#### 3. memdiag-native - 原生内存分析

**ProcFileSystemNativeAnalyzer**: /proc 文件系统解析
  - 解析 `/proc/<pid>/maps` 获取内存区域
  - 解析 `/proc/<pid>/smaps` 获取详细统计

**JVMTI Agent**: C++ 实现的 JVMTI Agent
  - 动态挂载/卸载
  - 字节码插桩与恢复
  - 分配事件追踪

#### 4. memdiag-agent - 远程分析 Agent

提供基于网络的远程分析能力，避免在生产服务器安装工具。

---

## libmemdiag-agent.so 原生库使用说明

### 什么是 libmemdiag-agent.so

`libmemdiag-agent.so` 是 MemDiag 的 JVMTI（JVM Tool Interface）原生代理库，使用 C++ 实现，提供以下高级功能：

| 功能 | 需要 libmemdiag-agent.so | 说明 |
|-----|-------------------------|------|
| `native --status` | ❌ 不需要 | 仅解析 /proc 文件系统 |
| `native --summary` | ❌ 不需要 | 仅解析 /proc 文件系统 |
| `native --regions` | ❌ 不需要 | 仅解析 /proc 文件系统 |
| `native --diagnose` | ❌ 不需要 | 仅解析 /proc 文件系统 |
| `native --attach` | ✅ **必需** | 动态挂载 JVMTI Agent |
| `native --detach` | ✅ **必需** | 卸载 JVMTI Agent |
| `native --start-trace` | ✅ **必需** | 启动堆外分配追踪 |
| `native --stop-trace` | ✅ **必需** | 停止堆外分配追踪 |
| `native --allocation-sites` | ✅ **必需** | 显示分配点统计 |

### 什么时候需要自行编译

**预编译版本已包含在发布包中**，以下情况需要自行编译：

1. **使用的 Linux 发行版与预编译版本不兼容**
   - 预编译版本在 Ubuntu 20.04 (gcc 10) 上构建
   - 如果使用其他发行版（CentOS, Debian, Alpine 等）可能需要重新编译

2. **修改了 C++ 源代码**
   - `memdiag-native/src/main/c/` 目录下的任何文件修改

3. **需要自定义编译选项**
   - 调整优化级别
   - 添加调试符号
   - 自定义平台特定选项

4. **在不同架构上运行**
   - 预编译版本仅支持 x86_64 (amd64)
   - ARM64 等其他架构需要重新编译

### 多架构支持

MemDiag 原生库支持**多架构共存**，`NativeLoader` 会自动根据当前系统架构选择合适的库：

| 架构 | 库文件名 | 状态 |
|-----|---------|------|
| **x86_64 / amd64** | `libmemdiag-agent-x86_64.so`<br>`libmemdiag-agent-amd64.so`<br>`libmemdiag-agent.so` | ✅ 预编译包含 |
| **ARM64 / aarch64** | `libmemdiag-agent-arm64.so`<br>`libmemdiag-agent-aarch64.so` | ⚠️ 需自行编译 |

加载优先级（从高到低）：
1. 架构特定名称（`-x86_64.so` / `-arm64.so`）
2. 通用名称（`libmemdiag-agent.so`）
3. 系统库路径（`System.loadLibrary("memdiag-agent")`）

### 编译方法

#### 方法一：使用 Docker 编译 x86_64 版本（推荐）

项目提供了完整的 Docker 编译脚本，无需在本地安装编译工具链：

```bash
# 使用项目提供的编译脚本
bash demo/build-final.sh
```

该脚本会：
1. 启动 gcc:10 容器（兼容大多数 Linux 发行版）
2. 安装 JDK 11
3. 编译 `libmemdiag-agent.so`
4. 自动复制到以下位置（包含架构特定名称）：
   - `memdiag-native/src/main/resources/libmemdiag-agent.so`
   - `memdiag-native/src/main/resources/libmemdiag-agent-x86_64.so`
   - `memdiag-native/src/main/resources/libmemdiag-agent-amd64.so`
   - `memdiag-cli/src/main/resources/libmemdiag-agent.so`
   - `memdiag-cli/src/main/resources/libmemdiag-agent-x86_64.so`
   - `memdiag-cli/src/main/resources/libmemdiag-agent-amd64.so`

#### 方法二：使用 Docker 编译 ARM64 版本

如果需要在 ARM64 设备上使用 JVMTI 功能：

```bash
# 编译 ARM64 版本
bash demo/build-arm64.sh
```

该脚本会：
1. 启动 `linux/arm64` 平台的 gcc:10 容器
2. 编译 `libmemdiag-agent-arm64.so`
3. 自动复制到 resources 目录（同时创建 `-arm64.so` 和 `-aarch64.so` 两个命名）

#### 方法二：手动在 Linux 环境编译

如果您有 Linux 环境，可以手动编译：

```bash
# 进入项目根目录
cd MemDiag

# 创建输出目录
mkdir -p memdiag-native/target/native

# 查找 JAVA_HOME
export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")

# 编译
g++ -std=c++17 -fPIC -shared \
    -I"$JAVA_HOME/include" \
    -I"$JAVA_HOME/include/linux" \
    -I"memdiag-native/src/main/c" \
    -I"memdiag-native/src/main/c/jvmti" \
    -I"memdiag-native/src/main/c/shared" \
    -I"memdiag-native/src/main/c/linux" \
    -o memdiag-native/target/native/libmemdiag-agent.so \
    memdiag-native/src/main/c/jvmti/agent.cpp \
    memdiag-native/src/main/c/jvmti/class_transformer.cpp \
    memdiag-native/src/main/c/jvmti/allocation_tracker.cpp \
    memdiag-native/src/main/c/linux/proc_parser.cpp \
    memdiag-native/src/main/c/shared/symbol_cache.cpp \
    -lpthread -ldl

# 复制到 resources 目录
cp memdiag-native/target/native/libmemdiag-agent.so memdiag-native/src/main/resources/
cp memdiag-native/target/native/libmemdiag-agent.so memdiag-cli/src/main/resources/
```

### 编译注意事项

#### 1. JDK 版本要求
- 编译时需要 JDK 11 或更高版本（不是 JRE）
- 确保 `$JAVA_HOME/include` 和 `$JAVA_HOME/include/linux` 目录存在

#### 2. GCC 版本要求
- 需要 GCC 7 或更高版本（支持 C++17）
- 推荐使用 GCC 10 以获得最佳兼容性

#### 3. 平台兼容性
- 编译的 `.so` 文件只能在相同架构的 Linux 上运行
- x86_64 编译的不能在 ARM64 上运行，反之亦然
- libc 版本需要兼容（编译环境的 libc 版本 ≤ 运行环境的 libc 版本）

#### 4. 运行时加载
- MemDiag 会按以下顺序查找库：
  1. Classpath 中的 `libmemdiag-agent.so`
  2. `java.library.path` 指定的路径
  3. 系统库路径（`/usr/lib`, `/usr/local/lib` 等）

#### 5. 验证编译结果

编译完成后，验证库文件是否可用：

```bash
# 检查文件是否存在
ls -lh memdiag-cli/src/main/resources/libmemdiag-agent.so

# 检查文件类型（应显示 ELF 64-bit LSB shared object）
file memdiag-cli/src/main/resources/libmemdiag-agent.so

# 检查依赖库
ldd memdiag-cli/src/main/resources/libmemdiag-agent.so
```

### 故障排除

#### 问题：编译时找不到 jni.h
```
fatal error: jni.h: No such file or directory
```
**解决方案**：确认 `JAVA_HOME` 环境变量正确指向 JDK 目录，且包含 `include` 子目录。

#### 问题：运行时提示 "JVMTI agent is not available"
**解决方案**：
1. 确认 `libmemdiag-agent.so` 在 classpath 中
2. 检查文件权限（需要可读权限）
3. 确认平台架构匹配

#### 问题：编译的库在目标机器上无法加载
```
java.lang.UnsatisfiedLinkError: /path/to/libmemdiag-agent.so: /lib/x86_64-linux-gnu/libc.so.6: version `GLIBC_2.32' not found
```
**解决方案**：在与目标机器 GLIBC 版本相同或更低的环境中重新编译。使用项目提供的 Docker 编译脚本（gcc:10 基于 Debian Buster，GLIBC 2.28，兼容性较好）。

---

## 高级配置

### 配置文件

MemDiag 使用 `memdiag.properties` 配置文件，位于：
`memdiag-core/src/main/resources/memdiag.properties`

#### 默认配置

```properties
# 原生内存分配追踪采样率（事件缓冲数量）
memdiag.native.sampling-rate=100000

# JMX 堆直方图分析超时（毫秒）
memdiag.jmx.heap-histogram-timeout-ms=500

# 最大 Safe Point 停顿时间（毫秒）
memdiag.jmx.max-safe-point-time-ms=500

# 分析操作内存限制（0 = 使用最大堆的 80%）
memdiag.analysis.memory-limit-bytes=0
```

### 系统属性覆盖

可以在启动时通过系统属性覆盖配置：

```bash
# 示例: 增加采样率
java -Dmemdiag.native.sampling-rate=200000 \
     -jar memdiag-cli-1.0.0-SNAPSHOT.jar \
     native --start-trace --pid 12345

# 示例: 调整超时时间
java -Dmemdiag.jmx.heap-histogram-timeout-ms=1000 \
     -jar memdiag-cli-1.0.0-SNAPSHOT.jar \
     histogram --pid 12345
```

---

## 故障排除

### 问题 1: 无法附着到目标进程

**症状**:
```
Failed to attach to PID <pid>
```

**解决方案**:

1. **检查是否使用 JDK**
   ```bash
   java -version
   # 确保输出包含 "JDK"
   ```

2. **验证 PID 是否正确**
   ```bash
   jps -l
   ps -p <pid>
   ```

3. **检查用户权限**
   - 确保以同一用户运行 MemDiag 和目标 JVM
   - Linux 下检查 ptrace_scope:
     ```bash
     cat /proc/sys/kernel/yama/ptrace_scope
     # 如果值为 1-3，临时调整:
     echo 0 | sudo tee /proc/sys/kernel/yama/ptrace_scope
     ```

4. **等待目标 JVM 完全启动**
   - 避免在 JVM 启动初期附着

### 问题 2: 堆外分析不可用

**症状**:
```
Native memory analysis is not available on this platform
```

**解决方案**:

堆外内存分析功能仅限 Linux：
- macOS/Windows: 只能使用堆内存分析功能
- Linux: 需要确保 /proc 文件系统已挂载

### 问题 3: 命令找不到

**症状**:
```
command not found: memdiag
```

**解决方案**:

检查 alias 或启动脚本配置：
```bash
# 验证 alias
alias | grep memdiag

# 验证 jar 文件存在
ls -lh /path/to/memdiag-cli-1.0.0-SNAPSHOT.jar
```

### 问题 4: 快照找不到

**症状**:
```
❌ Snapshot not found: test1
```

**解决方案**:

1. 检查快照是否存在：
   ```bash
   memdiag snapshot --list
   ```

2. 使用完整 ID 或文件名：
   ```bash
   # 使用完整文件名
   memdiag snapshot --load=snapshot-test1-20260326-164306.snapshot
   ```

3. 快照存储位置：`~/.memdiag/snapshots/`

---

## 生产环境使用建议

### 功能影响总结

| 功能模块 | 子命令/选项 | 对目标进程影响 | 推荐使用场景 |
|---------|-----------|--------------|------------|
| **堆内存分析** | `histogram` | ⚠️ 中等 - 可能触发 GC，短暂 Safe Point 停顿 | 非高峰时段分析 |
| | `snapshot --save` | ⚠️ 中等 - 同 histogram | 保存基准快照 |
| **线程分析** | `threads` | ✅ 低 - 只读 JMX | 任何时间 |
| **自动诊断** | `diagnose` | ✅ 低 - 只读 JMX | 任何时间 |
| **堆外基础分析** | `native --status/--summary/--regions` | ✅ 无 - 只读 /proc | 任何时间 |
| **Java Agent 挂载** | `native --attach` | ⚠️ 中等 - 短暂 Safe Point 停顿 | 测试环境先验证 |
| **Java Agent 卸载** | `native --detach` | ✅ 低 - 轻微影响 | 分析完成后 |
| **JVMTI 分配追踪** | `native --start-trace` | 🔴 高 - 每次分配触发回调，增加 CPU | 仅必要时使用 |
| | `native --stop-trace` | ✅ 低 - 释放缓冲区 | 追踪完成后 |
| | `native --allocation-sites` | ✅ 无 - 只读已捕获数据 | 任何时间 |
| **快照加载/对比** | `snapshot --load/list/delete`, `diff` | ✅ 无 - 本地文件操作 | 任何时间 |
| **报告生成** | `report` | ✅ 低 - 只读 JMX | 任何时间 |
| **NMT 分析** | `nmt` | ✅ 低 - 只读 JMX | 目标 JVM 已启用 NMT |
| **Agent 状态管理** | `agent status/config/metrics` | ✅ 低 - HTTP API 只读 | Agent 模式下任何时间 |
| **分配统计** | `allocations` | ✅ 低 - Agent 内存中统计，不中断目标 | Agent 模式下任何时间 |
| **方法监控** | `methods` | ⚠️ 中等 - 字节码插桩有开销 | Agent 模式下非高峰时段 |

### 使用建议

1. **优先使用非侵入式功能**
   - 先用 `histogram`, `threads`, `diagnose`, `native --summary` 定位问题
   - 堆直方图分析已配置 500ms 超时，避免过长停顿

2. **Agent 模式安全建议**
   - Agent HTTP 服务默认绑定 `localhost`，不要暴露到公网
   - 如需远程访问，建议使用 SSH 隧道或 VPN
   - 生产环境使用前务必在测试环境验证

3. **谨慎使用侵入性功能**
   - `native --attach`、`methods` 建议在测试环境先验证
   - JVMTI 分配追踪（`native --start-trace`）会增加 CPU 开销，仅在必要时启用
   - 字节码插桩采样率可配置，避免 100% 拦截

4. **使用快照进行对比**
   - 先用 `snapshot --save` 保存基准快照
   - 问题复现后再保存当前快照
   - 用 `diff` 离线对比，避免对生产环境持续影响

5. **Agent 模式下的分配统计**
   - `allocations` 命令使用 Agent 内存中的统计数据，对目标进程影响很小
   - 可实时查看分配速率、Top N 分配类型，适合持续监控

6. **配置调优**
   - 可通过 `memdiag.jmx.heap-histogram-timeout-ms` 调整超时时间
   - 可通过 `memdiag.native.sampling-rate` 调整 JVMTI 追踪采样率
   - Agent 配置可通过 `agent config` 查看和调整

---

## 开发文档

### 项目结构

```
MemDiag/
├── memdiag-core/          # 核心分析库
│   ├── src/main/java/     # 核心实现
│   │   ├── com/memdiag/core/
│   │   │   ├── heap/      # 堆内存分析
│   │   │   ├── thread/    # 线程分析
│   │   │   ├── diagnose/  # 诊断引擎
│   │   │   ├── diff/      # 快照与对比
│   │   │   ├── nativeapi/ # 原生内存 API
│   │   │   ├── nmt/       # NMT 分析
│   │   │   ├── output/    # 报告格式化
│   │   │   ├── config/    # 配置
│   │   │   └── util/      # 工具类
│   └── src/test/java/     # 单元测试
├── memdiag-cli/           # 命令行工具
│   └── src/main/java/     # CLI 实现
│       └── com/memdiag/cli/
│           ├── commands/   # 子命令实现
│           └── client/     # Agent 客户端
├── memdiag-agent/         # Java Agent
├── memdiag-native/        # 原生内存分析
│   ├── src/main/java/     # Java 绑定
│   └── src/main/c/        # JVMTI C++ 实现
├── memdiag-web/           # Web 界面
├── memdiag-ui/            # UI 前端
├── scripts/               # 辅助脚本
│   ├── quick-validate.sh  # 快速验证
│   ├── uat-blackbox.sh    # UAT 黑盒测试
│   └── native-smoke-test.sh  # Native 冒烟测试
├── demo/                  # Demo 相关
└── docs/                  # 文档
    ├── Gemini_Code_Review.md  # 代码审核报告
    ├── Gemini_UAT.md          # UAT 验收报告
    └── superpowers/           # 设计文档和计划
```

### 构建与测试

```bash
# 完整构建（含测试）
mvn clean package

# 跳过测试快速构建
mvn clean package -DskipTests

# 只运行 core 模块测试
mvn test -pl memdiag-core

# 运行验证脚本
./scripts/quick-validate.sh
./scripts/uat-blackbox.sh
```

### 技术栈

- **Java 11+**: 主要开发语言
- **Maven**: 构建工具
- **JUnit 5**: 单元测试框架
- **AssertJ**: 断言库
- **Picocli**: 命令行解析
- **Gson**: JSON 处理
- **JVMTI**: 原生内存分析（C++）

### 更多文档

- `docs/superpowers/specs/` - 设计规格文档
- `docs/superpowers/plans/` - 实施计划
- `docs/Gemini_Code_Review.md` - 代码审核报告
- `docs/Gemini_UAT.md` - UAT 验收报告

---

## 许可证

本项目仅供学习和研究使用。

## 贡献

欢迎提交 Issue 和 Pull Request！

---

## 快速参考卡

### 常用命令速查

```bash
# === 堆内存分析 ===
memdiag histogram                    # 当前 JVM
memdiag histogram -l 50              # 前 50 行
memdiag histogram 12345              # 指定进程

# === 线程分析 ===
memdiag threads                      # 线程概览
memdiag threads -s                   # 含堆栈
memdiag threads 12345                # 指定进程

# === 自动诊断 ===
memdiag diagnose                     # 当前 JVM
memdiag diagnose 12345              # 指定进程

# === 堆外内存（Linux）===
memdiag native --status              # 检查可用性
memdiag native --summary             # 内存摘要
memdiag native --regions             # 内存区域
memdiag native --diagnose            # 堆外诊断

# === 原生 Agent 控制 ===
memdiag native --attach 12345        # 挂载
memdiag native --detach 12345        # 卸载

# === 分配追踪 ===
memdiag native --start-trace 12345   # 启动追踪
memdiag native --stop-trace 12345    # 停止追踪
memdiag native --allocation-sites 12345  # 查看分配点

# === 快照管理 ===
memdiag snapshot --save 12345        # 保存快照
memdiag snapshot --save --id=test1 12345  # 指定 ID
memdiag snapshot --list               # 列出快照
memdiag snapshot --load=test1         # 加载快照
memdiag snapshot --delete=test1       # 删除快照

# === 堆对比 ===
memdiag diff --baseline=test1 --current=test2  # 对比两个快照
memdiag diff --baseline=test1 12345  # 对比快照与实时堆

# === 报告生成 ===
memdiag report 12345                 # 文本报告
memdiag report --format=html --output=report.html 12345  # HTML 报告

# === Agent 管理（需要 --agent）===
memdiag agent status --agent=localhost:6789    # 查看 Agent 状态
memdiag agent config --agent=localhost:6789    # 查看 Agent 配置
memdiag agent metrics --agent=localhost:6789   # 查看 Agent 指标

# === 分配统计（需要 --agent）===
memdiag allocations --summary --agent=localhost:6789  # 分配摘要
memdiag allocations --stats --agent=localhost:6789    # 分配统计
memdiag allocations --top=20 --agent=localhost:6789   # Top 20 分配类型

# === 方法监控（需要 --agent）===
memdiag methods --limit=20 --agent=localhost:6789     # 方法统计（按时间）
memdiag methods --sort=count --agent=localhost:6789    # 方法统计（按调用次数）
```

---

**祝您诊断愉快！** 🚀
