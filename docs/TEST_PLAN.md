# MemDiag 测试计划

根据 README.md 中的功能说明，制定以下完整测试计划。

## 测试目标

验证 memdiag-cli 所有命令行选项的有效性和功能符合预期。

## 测试环境

- **操作系统**: Linux (Docker)
- **Java**: JDK 17+
- **工具**: Docker

## 测试命令清单

| 序号 | 命令 | 子命令/选项 | 优先级 | 说明 |
|------|------|-----------|--------|------|
| 1 | memdiag | -h, --help | P0 | 主命令帮助 |
| 2 | memdiag | -V, --version | P1 | 版本信息 |
| 3 | histogram | (无选项) | P0 | 堆直方图默认输出 |
| 4 | histogram | -l, --limit | P0 | 限制输出行数 |
| 5 | histogram | --pid | P0 | 指定 PID |
| 6 | threads | (无选项) | P0 | 线程概览 |
| 7 | threads | -s, --stacks | P0 | 显示堆栈 |
| 8 | threads | -l, --limit | P1 | 限制线程数 |
| 9 | diagnose | (无选项) | P0 | 自动诊断 |
| 10 | diagnose | --pid | P0 | 诊断指定进程 |
| 11 | native | --status | P0 | 检查可用性 |
| 12 | native | --summary | P0 | 内存摘要 |
| 13 | native | --regions | P1 | 内存区域 |
| 14 | native | --diagnose | P1 | 堆外诊断 |
| 15 | native | --attach | P1 | 挂载 Agent |
| 16 | native | --detach | P1 | 卸载 Agent |
| 17 | native | --start-trace | P2 | 启动追踪 |
| 18 | native | --stop-trace | P2 | 停止追踪 |
| 19 | native | --allocation-sites | P2 | 分配点 |
| 20 | snapshot | --save | P0 | 保存快照 |
| 21 | snapshot | --load | P0 | 加载快照 |
| 22 | snapshot | --list | P0 | 列出快照 |
| 23 | snapshot | --delete | P0 | 删除快照 |
| 24 | snapshot | --id | P1 | 自定义 ID |
| 25 | diff | --baseline | P0 | 基准快照（必需） |
| 26 | diff | --current | P0 | 当前快照 |
| 27 | diff | --growing | P1 | 增长类限制 |
| 28 | diff | --shrinking | P1 | 减少类限制 |
| 29 | diff | --growth-rate | P1 | 增长率限制 |
| 30 | diff | --all | P1 | 显示所有变化 |
| 31 | report | (无选项) | P1 | 文本报告 |
| 32 | report | -f, --format | P1 | 输出格式 |
| 33 | report | -o, --output | P1 | 输出文件 |
| 34 | nmt | (无选项) | P2 | NMT 分析 |

## 测试场景

### 场景 1: 基础功能验证 (P0)
- 验证所有命令的帮助信息
- 验证当前 JVM 分析（无 PID）
- 验证简单的输出格式

### 场景 2: 堆内存分析 (P0)
- histogram 命令基本功能
- 限制输出行数
- 指定 PID 分析

### 场景 3: 线程分析 (P0)
- threads 命令基本功能
- 显示堆栈跟踪
- 限制线程数

### 场景 4: 自动诊断 (P0)
- diagnose 命令基本功能
- 验证 5 个内置规则执行
- HEAP_LEAK_SUSPECT 规则验证

### 场景 5: 快照管理 (P0)
- snapshot --save 保存
- snapshot --list 列出
- snapshot --load 加载
- snapshot --delete 删除

### 场景 6: 堆对比分析 (P0)
- diff --baseline 必需参数
- diff 对比两个快照
- 各类增长/减少显示

### 场景 7: 堆外内存分析 (P1)
- native --status/--summary
- native --regions
- native --attach/--detach

### 场景 8: 报告生成 (P1)
- report 文本格式
- report HTML 格式
- report JSON 格式

## 验收标准

对于每个测试项：
- 命令执行成功 (exit code 0)
- 输出格式符合预期
- 无异常栈信息
- 功能正确执行

## 测试执行

使用 `demo/test-full-suite.sh` 脚本执行完整测试套件。
