# MemDiag

**JVM 内存诊断工具** - 专业的 Java 堆内/堆外内存分析与诊断工具。

---

## 目录

- [产品简介](#产品简介)
- [功能特性](#功能特性)
- [快速入门](#快速入门)
- [核心功能使用指南](#核心功能使用指南)
  - [1. 堆内存分析](#1-堆内存分析)
  - [2. 线程分析](#2-线程分析)
  - [3. 自动诊断](#3-自动诊断)
  - [4. 堆外内存分析](#4-堆外内存分析)
  - [5. 原生 Agent 控制](#5-原生-agent-控制)
  - [6. 分配追踪](#6-分配追踪)
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

---

## 功能特性

| 功能模块 | 说明 |
|---------|------|
| **堆内存分析** | 堆直方图、类分布统计、大对象识别 |
| **线程分析** | 线程状态、堆栈跟踪、死锁检测 |
| **自动诊断** | 内存泄漏检测、配置问题识别、优化建议 |
| **堆外内存** | /proc 解析、内存区域映射、库映射分析 |
| **原生 Agent** | 动态挂载/卸载、JVMTI 事件监听 |
| **分配追踪** | 堆外分配点追踪、泄漏点分析 |
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
git clone <repository-url>
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
memdiag

# 应该看到类似输出：
# Usage: memdiag [COMMAND]
# JVM Memory Diagnosis Tool
# Commands:
#   histogram  Show heap histogram
#   threads    Show thread analysis
#   diagnose   Run diagnosis and show issues
#   native     Native memory analysis (Linux only)
#   ...
```

### 第四步：第一个诊断

```bash
# 分析当前 JVM（快速测试）
memdiag histogram

# 分析指定进程
jps -l                  # 找到目标 JVM 的 PID
memdiag diagnose --pid <pid>
```

---

## 核心功能使用指南

### 命令结构

MemDiag 采用**子命令**模式，结构清晰：

```
memdiag <子命令> [选项] [PID]
```

**通用选项**（所有子命令支持）：

| 选项 | 说明 | 示例 |
|-----|------|------|
| `-p, --pid <pid>` | 指定目标 JVM 进程 ID | `--pid 12345` |
| `-a, --agent <host:port>` | 连接到远程 Agent | `--agent localhost:6789` |

---

### 1. 堆内存分析

**子命令**: `histogram`

分析堆内存中对象的分布情况，识别大对象和内存热点。

#### 功能说明

- 显示按大小排序的类直方图
- 统计对象数量和占用内存
- 支持限制输出行数，避免信息过载

#### 常用示例

```bash
# 示例 1: 分析当前 JVM（默认前 20 行）
memdiag histogram

# 示例 2: 指定输出行数
memdiag histogram -l 50
memdiag histogram --limit 100

# 示例 3: 分析指定进程
memdiag histogram --pid 12345
memdiag histogram --pid 12345 -l 20

# 示例 4: 位置参数形式（兼容旧版本）
memdiag histogram 12345
```

#### 输出说明

```
CLASS NAME                                  OBJECTS    SHALLOW HEAP
-------------------------------------------------------------------------
java.lang.String                             12,456         797,184
byte[]                                        5,678       5,242,880
java.util.HashMap$Node                       8,901         284,832
[C                                            3,456       2,134,567
...
-------------------------------------------------------------------------
Total                                        56,789      12,567,890
```

字段说明：
- **CLASS NAME**: 类名
- **OBJECTS**: 对象数量
- **SHALLOW HEAP**: 浅层大小（不包含引用对象）

---

### 2. 线程分析

**子命令**: `threads`

分析 JVM 中所有线程的状态和堆栈。

#### 功能说明

- 显示所有线程的状态（RUNNABLE, BLOCKED, WAITING 等）
- 可选显示完整堆栈跟踪
- 识别死锁和阻塞点

#### 常用示例

```bash
# 示例 1: 显示线程概览
memdiag threads

# 示例 2: 显示完整堆栈跟踪
memdiag threads -s
memdiag threads --stack

# 示例 3: 分析指定进程的线程
memdiag threads --pid 12345
memdiag threads --pid 12345 -s
```

#### 输出说明

线程状态说明：
- **RUNNABLE**: 正在执行或可执行
- **BLOCKED**: 等待监视器锁
- **WAITING**: 无限期等待另一个线程
- **TIMED_WAITING**: 有时限的等待
- **TERMINATED**: 已终止

---

### 3. 自动诊断

**子命令**: `diagnose`

运行完整的内存诊断，自动识别问题并给出建议。

#### 功能说明

- 综合堆内存、线程、GC 等多维度分析
- 自动检测内存泄漏风险
- 识别配置问题
- 提供具体的优化建议

#### 常用示例

```bash
# 示例 1: 诊断当前 JVM
memdiag diagnose

# 示例 2: 诊断指定进程
memdiag diagnose --pid 12345
```

#### 输出说明

诊断报告包含：
- **FINDINGS**: 发现的问题
- **WARNINGS**: 警告信息
- **RECOMMENDATIONS**: 优化建议

---

### 4. 堆外内存分析

**子命令**: `native`

分析 JVM 的堆外内存使用情况（Linux 专用）。

#### 功能说明

- 通过 /proc 文件系统解析内存映射
- 显示虚拟内存和物理内存使用
- 识别内存区域和加载的库
- 检测堆外内存泄漏

#### 子选项

| 选项 | 说明 |
|-----|------|
| `--status` | 检查堆外分析是否可用 |
| `--summary` | 显示内存摘要 |
| `--regions` | 显示内存区域分布 |
| `--diagnose` | 运行堆外泄漏诊断 |

#### 常用示例

```bash
# 示例 1: 检查是否可用
memdiag native --status

# 示例 2: 显示内存摘要
memdiag native --summary
memdiag native --summary --pid 12345

# 示例 3: 显示内存区域
memdiag native --regions

# 示例 4: 运行堆外诊断
memdiag native --diagnose
```

#### 输出示例 - 内存摘要

```
NATIVE MEMORY SUMMARY
==========================================================================
Total Virtual:        4,567,890,123 bytes (4,356.12 MB)
Total Resident:       1,234,567,890 bytes (1,177.38 MB)
Direct ByteBuffers:      50,000,000 bytes (47.68 MB)
```

#### 输出示例 - 内存区域

```
MEMORY REGIONS
==========================================================================
START              END                PERMS        SIZE          RSS FILE
--------------------------------------------------------------------------
00007f8a1c000000-00007f8a1c200000 rw-p      2,097,152    1,048,576 /lib/libc-2.31.so
...
--------------------------------------------------------------------------
Total: 127 regions
```

---

### 5. 原生 Agent 控制

**子命令**: `native` + `--attach` / `--detach`

动态挂载或卸载 MemDiag 原生 Agent（JVMTI Agent）。

#### 功能说明

- 动态挂载，无需重启目标 JVM
- 安全卸载，无残留
- 环境预检，提供友好错误提示

#### 子选项

| 选项 | 说明 |
|-----|------|
| `--attach` | 挂载原生 Agent |
| `--detach` | 卸载原生 Agent |
| `--status` | 检查 Agent 状态 |

#### 前置检查

在挂载前，MemDiag 会自动检查：
- ✓ PID 是否存在
- ✓ 当前用户是否有权限
- ✓ 是否使用 JDK（非 JRE）
- ✓ Linux ptrace_scope 配置（如需要）

#### 常用示例

```bash
# 示例 1: 检查 Agent 状态
memdiag native --status --pid 12345

# 示例 2: 挂载 Agent
memdiag native --attach --pid 12345

# 示例 3: 卸载 Agent
memdiag native --detach --pid 12345
```

#### 完整工作流示例

```bash
# 1. 首先检查状态
memdiag native --status --pid 12345

# 2. 挂载 Agent
memdiag native --attach --pid 12345
# 输出: ✅ Agent attached successfully

# 3. 执行其他操作（如启动追踪）
memdiag native --start-trace --pid 12345

# 4. 完成后卸载
memdiag native --detach --pid 12345
# 输出: ✅ Agent detached successfully
```

---

### 6. 分配追踪

**子命令**: `native` + `--start-trace` / `--stop-trace` / `--allocation-sites`

追踪堆外内存分配，识别泄漏点。

#### 功能说明

- 追踪 malloc/free/realloc 调用
- 记录分配堆栈
- 统计分配点
- 识别潜在泄漏

#### 子选项

| 选项 | 说明 |
|-----|------|
| `--start-trace` | 启动分配追踪 |
| `--stop-trace` | 停止分配追踪 |
| `--allocation-sites` | 显示分配点 |
| `-l, --limit <n>` | 限制显示数量（默认 20） |

#### 前置条件

1. 原生 Agent 已挂载（`--attach`）
2. 目标 JVM 运行在 Linux 上

#### 常用示例

```bash
# 示例 1: 启动追踪（需要先挂载 Agent）
memdiag native --start-trace --pid 12345

# 示例 2: 停止追踪
memdiag native --stop-trace --pid 12345

# 示例 3: 显示分配点（默认前 20）
memdiag native --allocation-sites --pid 12345

# 示例 4: 显示前 10 个分配点
memdiag native --allocation-sites --limit 10 --pid 12345
memdiag native --allocation-sites -l 5 --pid 12345
```

#### 完整工作流示例

```bash
# 步骤 1: 挂载 Agent
memdiag native --attach --pid 12345

# 步骤 2: 启动追踪
memdiag native --start-trace --pid 12345
# 输出: ✅ Allocation tracing started
#
# Next steps:
#   - Let the application run to capture allocations
#   - Use --allocation-sites to view results
#   - Use --stop-trace to stop tracing

# 步骤 3: 让应用运行一段时间（执行操作以触发分配）
# ... 等待几秒或几分钟 ...

# 步骤 4: 查看分配点
memdiag native --allocation-sites --pid 12345

# 步骤 5: 停止追踪
memdiag native --stop-trace --pid 12345

# 步骤 6: 卸载 Agent
memdiag native --detach --pid 12345
```

#### 输出示例 - 分配点

```
TOP ALLOCATION SITES
==========================================================================
Total allocated:     153,600,000 bytes (146.48 MB)
Live bytes:          102,400,000 bytes (97.66 MB)

DEMO: Allocation sites (simulated for demo)
--------------------------------------------------------------------------
   COUNT          TOTAL           LIVE          FREED  SITE
--------------------------------------------------------------------------
     150      15,360,000      15,360,000              0  LeakSimulator.simulateLeak
      50       5,120,000              0      5,120,000  LeakSimulator.allocateBuffers
     200      20,480,000      10,240,000     10,240,000  ByteBuffer.allocateDirect
...
--------------------------------------------------------------------------

Note: In a real run, these would show actual stack traces
      from the native allocation tracker.
```

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

---

## 开发文档

### 项目结构

```
MemDiag/
├── memdiag-core/          # 核心分析库
│   ├── src/main/java/     # 核心实现
│   └── src/test/java/     # 单元测试
├── memdiag-cli/           # 命令行工具
│   └── src/main/java/     # CLI 实现
├── memdiag-agent/         # Java Agent
├── memdiag-native/        # 原生内存分析
│   ├── src/main/java/     # Java 绑定
│   └── src/main/c/        # JVMTI C++ 实现
├── memdiag-web/           # Web 界面
├── scripts/               # 辅助脚本
│   ├── quick-validate.sh  # 快速验证
│   ├── uat-blackbox.sh    # UAT 黑盒测试
│   └── native-smoke-test.sh  # Native 冒烟测试
└── docs/                  # 文档
    └── superpowers/       # 设计文档和计划
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
- `docs/UAT_FIXES.md` - UAT 问题修复记录
- `docs/UAT_REMEDIATION.md` - UAT 验收问题修复总结

---

## 许可证

本项目仅供学习和研究使用。

## 贡献

欢迎提交 Issue 和 Pull Request！

---

## 快速参考卡

### 常用命令速查

```bash
# 堆内存分析
memdiag histogram                    # 当前 JVM
memdiag histogram -l 50              # 前 50 行
memdiag histogram --pid 12345       # 指定进程

# 线程分析
memdiag threads                      # 线程概览
memdiag threads -s                   # 含堆栈
memdiag threads --pid 12345         # 指定进程

# 自动诊断
memdiag diagnose                     # 当前 JVM
memdiag diagnose --pid 12345        # 指定进程

# 堆外内存（Linux）
memdiag native --status              # 检查可用性
memdiag native --summary             # 内存摘要
memdiag native --regions             # 内存区域
memdiag native --diagnose            # 堆外诊断

# 原生 Agent 控制
memdiag native --attach --pid 12345   # 挂载
memdiag native --detach --pid 12345   # 卸载

# 分配追踪
memdiag native --start-trace --pid 12345    # 启动追踪
memdiag native --stop-trace --pid 12345     # 停止追踪
memdiag native --allocation-sites --pid 12345 # 查看分配点
memdiag native --allocation-sites -l 10 --pid 12345  # 前 10 个
```

---

**祝您诊断愉快！** 🚀
