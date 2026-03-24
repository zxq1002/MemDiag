# MemDiag

JVM 内存诊断工具。

## 功能特性

- 堆内存直方图分析
- 线程分析
- 自动诊断和问题检测
- Agent 挂载模式（支持 --agent 选项连接远程 Agent）
- 堆外内存分析（Linux 下支持 /proc 解析）
- PID 附着支持

## 模块结构

```
memdiag/
├── memdiag-core/     # 核心分析库
├── memdiag-cli/      # 命令行工具
└── memdiag-agent/    # Java Agent（用于动态挂载）
```

## 构建

```bash
mvn clean package
```

## 使用

### 基础命令

```bash
# 查看帮助
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar
```

### 堆直方图分析

```bash
# 查看当前 JVM 的堆直方图
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar histogram

# 查看堆直方图（限制显示前 20 行）
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar histogram -l 20

# 查看指定 PID 的 JVM 堆直方图
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar histogram <pid>
```

### 线程分析

```bash
# 查看当前 JVM 的线程状态
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar threads

# 显示线程堆栈
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar threads -s

# 查看指定 PID 的线程分析
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar threads <pid>
```

### 诊断分析

```bash
# 运行完整诊断
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar diagnose

# 查看指定 PID 的诊断报告
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar diagnose <pid>
```

### 堆外内存分析（Linux 专用）

```bash
# 检查堆外分析是否可用
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar native --status

# 显示堆外内存摘要
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar native --summary

# 显示内存区域分布
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar native --regions

# 运行堆外泄露诊断
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar native --diagnose
```

### Agent 模式

```bash
# 连接到远程 Agent（格式：host:port）
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar histogram --agent localhost:6789
java -jar memdiag-cli/target/memdiag-cli-1.0.0-SNAPSHOT.jar diagnose --agent localhost:6789
```

## 技术栈

- Java 11+
- Maven
- JUnit 5
- AssertJ
- Picocli
- Gson
