# MemDiag 内存诊断工具 - 设计文档

**日期**: 2026-03-24
**版本**: 1.1
**状态**: 已采纳 Gemini 审查意见

---

## 审查意见采纳记录

| 审查项 | 优先级 | 采纳情况 |
|--------|--------|----------|
| Agent 冲突防御 | Critical | ✅ 已采纳 |
| Safe Point 监控 | Critical | ✅ 已采纳 |
| 原生层背压策略 | Critical | ✅ 已采纳 |
| Retained Size 按需计算 | Important | ✅ 已采纳 |
| 差异分析复合键 | Important | ✅ 已采纳 |
| 符号解析缓存 | Technical | ✅ 已采纳 |
| Thread Stack/Code Cache 监控 | Technical | ✅ 已采纳 |

---

## 1. 项目概述

MemDiag 是一个 JVM 内存诊断工具，用于分析内存对象占用分布、定位内存泄露问题、给出诊断建议。

### 1.1 设计目标

- **生产环境安全**: 严格控制资源使用，不阻塞业务线程
- **多模式使用**: 支持 CLI 直接分析、Agent 挂载、Web UI 三种模式
- **分层架构**: 核心功能优先实现，堆外分析作为可选扩展
- **低侵入性**: 默认只读模式，可动态 attach/detach

### 1.2 核心功能

- 堆内存直方图分析
- GC Root 引用链追踪
- 多次快照差异分析
- 实时内存指标监控
- 自动诊断建议生成
- 线程分析
- **堆外内存分析（默认不启用，用户可选择启用）**

---

## 2. 系统架构

### 2.1 模块结构

```
memdiag/
├── memdiag-agent/              # Java Agent 模块
│   ├── src/main/java/
│   │   └── com/memdiag/agent/
│   │       ├── MemDiagAgent.java
│   │       ├── AgentPremain.java
│   │       └── AgentAttach.java
│   └── pom.xml
├── memdiag-core/               # 核心分析库
│   ├── src/main/java/
│   │   └── com/memdiag/core/
│   │       ├── heap/           # 堆内存分析
│   │       │   ├── HeapHistogram.java
│   │       │   ├── ClassStats.java
│   │       │   └── GcRootAnalyzer.java
│   │       ├── native/         # 堆外内存分析（接口层）
│   │       │   ├── NativeMemoryAnalyzer.java
│   │       │   └── NoOpNativeAnalyzer.java
│   │       ├── diff/           # 差异分析
│   │       │   ├── Snapshot.java
│   │       │   └── HeapDiff.java
│   │       ├── diagnose/       # 诊断引擎
│   │       │   ├── DiagnosisEngine.java
│   │       │   ├── Issue.java
│   │       │   └── Recommendation.java
│   │       ├── thread/         # 线程分析
│   │       │   └── ThreadAnalyzer.java
│   │       └── util/           # 工具类
│   │           ├── JmxClient.java
│   │           ├── Sampler.java
│   │           └── ResourceLimiter.java
│   └── pom.xml
├── memdiag-cli/                # 命令行工具
│   ├── src/main/java/
│   │   └── com/memdiag/cli/
│   │       ├── MemDiagCli.java
│   │       ├── commands/
│   │       │   ├── HistogramCommand.java
│   │       │   ├── GcRootsCommand.java
│   │       │   ├── DiagnoseCommand.java
│   │       │   └── ReportCommand.java
│   │       └── output/
│   │           ├── TextFormatter.java
│   │           ├── HtmlFormatter.java
│   │           └── JsonFormatter.java
│   └── pom.xml
├── memdiag-web/                # Web 后端服务（可选）
│   ├── src/main/java/
│   │   └── com/memdiag/web/
│   │       ├── MemDiagWebApp.java
│   │       ├── controller/
│   │       │   ├── ApiController.java
│   │       │   └── WebSocketController.java
│   │       └── service/
│   │           └── AnalysisService.java
│   └── pom.xml
├── memdiag-ui/                 # Vue 3 前端（可选）
│   ├── src/
│   │   ├── components/
│   │   │   ├── HeapChart.vue
│   │   │   ├── GcRootTree.vue
│   │   │   └── DiagnosisPanel.vue
│   │   └── views/
│   │       └── Dashboard.vue
│   └── package.json
├── memdiag-native/             # 原生扩展（Linux 优先）
│   ├── src/main/c/
│   │   ├── jvmti/
│   │   │   ├── agent.cpp           # JVMTI Agent 入口
│   │   │   ├── allocation_tracker.cpp
│   │   │   ├── stack_collector.cpp
│   │   │   └── probes.cpp
│   │   ├── linux/
│   │   │   ├── proc_parser.cpp     # /proc/<pid>/smaps 解析
│   │   │   ├── maps_parser.cpp
│   │   │   └── symbol_resolver.cpp
│   │   └── shared/
│   │       ├── ring_buffer.cpp     # 无锁环形缓冲区
│   │       └── protocol.cpp        # JNI 通信协议
│   ├── src/main/java/
│   │   └── com/memdiag/nativeimpl/
│   │       ├── JVMTINativeAnalyzer.java
│   │       ├── NativeLoader.java
│   │       └── ProcFSNativeAnalyzer.java
│   ├── libmemdiag-agent.so         # 编译输出（Linux）
│   └── pom.xml
└── pom.xml                     # 父 POM
```

### 2.2 架构分层

```
┌─────────────────────────────────────────────────────────┐
│                      用户交互层                           │
│  ┌─────────────────┐  ┌─────────────┐  ┌───────────┐  │
│  │  memdiag-cli   │  │ memdiag-web │  │    UI     │  │
│  └─────────────────┘  └─────────────┘  └───────────┘  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                     Agent 通信层                         │
│           ┌───────────────────────────┐                 │
│           │    Local HTTP API         │                 │
│           │   (127.0.0.1:port)       │                 │
│           └───────────────────────────┘                 │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                      核心分析层                           │
│  ┌────────────┐ ┌────────────┐ ┌────────────────────┐ │
│  │   Heap     │ │    Diff    │ │     Diagnose       │ │
│  │  Analysis  │ │  Analysis  │ │      Engine        │ │
│  └────────────┘ └────────────┘ └────────────────────┘ │
│  ┌────────────┐ ┌────────────┐                          │
│  │   Thread   │ │   Native   │  (可选扩展接口)         │
│  │  Analysis  │ │  Analysis  │                          │
│  └────────────┘ └────────────┘                          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    JVM 接入层                            │
│  ┌──────────────────────────────────────────────────┐  │
│  │  JMX / Attach API / Instrumentation              │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## 3. 核心功能设计

### 3.1 堆内存分析

**职责**: 按类统计对象数量和占用大小

```java
public interface HeapAnalyzer {
    HeapHistogram getHistogram(int limit);
    ClassStats getClassStats(String className);
    List<GcRootPath> findGcRoots(ObjectId objectId);
}
```

**实现策略**:
- 使用 `HotSpotDiagnosticMXBean` 获取堆信息
- 可选采样策略，避免全量扫描
- 支持 `shallow size` 和 `retained size` 计算

### 3.2 差异分析

**职责**: 对比两个快照，识别内存增长

```java
public class HeapDiff {
    private final Snapshot baseline;
    private final Snapshot current;

    public List<ClassDiff> getGrowingClasses(int limit);
    public List<ClassDiff> getShrinkingClasses(int limit);
    public long getTotalGrowth();
}
```

**实现策略**:
- 快照存储轻量级统计数据（非全量对象）
- 支持按增长率和绝对增长排序

### 3.3 诊断引擎

**职责**: 基于规则生成诊断建议

```java
public class DiagnosisEngine {
    public DiagnosisResult analyze(HeapHistogram histogram, ThreadDump threads);
}

public class Issue {
    private Severity severity;  // CRITICAL, WARNING, INFO
    private String type;        // LARGE_COLLECTION, CLASSLOADER_LEAK, ...
    private String description;
    private List<Recommendation> recommendations;
}
```

**诊断规则**:
- 异常大对象检测（> 100MB）
- 集合类异常增长
- 线程局部变量堆积
- 类加载器泄露
- Finalizer 队列积压

### 3.4 线程分析

**职责**: 分析线程状态与内存关联

```java
public class ThreadAnalyzer {
    public ThreadDump getThreadDump();
    public List<ThreadStats> getThreadStats();
    public Map<ThreadId, List<ObjectId>> getThreadLocalObjects();
}
```

### 3.5 堆外内存分析（可选启用）

**设计原则**: 默认不启用，用户显式选择后才加载原生模块，仅支持 Linux 平台。

#### 3.5.1 启用方式（无需重启应用）

**重要**: 所有堆外分析功能都支持动态 attach，**无需重启目标应用**，可在故障发生时直接诊断。

```bash
# ──────────────────────────────────────────────────────────────
# 方式一：直接分析（无需挂载 Agent）
# 基于 /proc 文件系统，零侵入，随时可用
# ──────────────────────────────────────────────────────────────
java -jar memdiag-cli.jar native <pid> --summary      # 堆外概览
java -jar memdiag-cli.jar native <pid> --regions      # 内存区域分布
java -jar memdiag-cli.jar native <pid> --diagnose     # 初步诊断

# ──────────────────────────────────────────────────────────────
# 方式二：动态挂载 Agent（深度分析）
# 动态加载 JVMTI Agent，进行字节码插桩和分配追踪
# ──────────────────────────────────────────────────────────────
# 1. 动态挂载原生 Agent
java -jar memdiag-cli.jar native <pid> --attach

# 2. 启用分配追踪
java -jar memdiag-cli.jar native <pid> --start-trace --sampling-rate=0.01

# 3. 观察一段时间后，查看结果
java -jar memdiag-cli.jar native <pid> --allocation-sites --limit=20

# 4. 停止追踪并卸载 Agent
java -jar memdiag-cli.jar native <pid> --stop-trace
java -jar memdiag-cli.jar native <pid> --detach

# ──────────────────────────────────────────────────────────────
# 方式三：启动时挂载（用于持续监控）
# ──────────────────────────────────────────────────────────────
java -Dmemdiag.native.enabled=true \
     -javaagent:memdiag-agent.jar=port=5000 -jar app.jar
```

**能力对比（按侵入程度排序）**:

| 方式 | 需要 Agent | 重启应用 | 能力 | 适用场景 |
|------|-----------|----------|------|----------|
| 仅 /proc 解析 | ❌ | ❌ | RSS/VSS、内存区域分布 | 快速查看，故障现场保留 |
| + NMT 数据 | ❌ | ❌（需 JVM 参数） | JVM 原生内存分类 | 已加 `-XX:NativeMemoryTracking` |
| 动态挂载 JVMTI | ✅ | ❌ | 分配追踪、调用栈、字节码插桩 | 深度诊断，故障现场 |
| 启动时挂载 | ✅ | ✅ | 全功能，持续监控 | 预发环境、已知问题追踪 |

#### 3.5.2 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                   Java 层 (memdiag-core)                     │
│  ┌───────────────────────────────────────────────────────┐  │
│  │    NativeMemoryAnalyzer (接口)                         │  │
│  │      - NoOpNativeAnalyzer (默认，无操作)              │  │
│  │      - ProcFSNativeAnalyzer (解析 /proc)              │  │
│  │      - JVMTINativeAnalyzer (完整功能，需要加载 so)   │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                           ↓ JNI
┌─────────────────────────────────────────────────────────────┐
│               原生层 (libmemdiag-agent.so)                   │
│  ┌──────────────────┐  ┌─────────────────────────────────┐  │
│  │  JVMTI Agent     │  │     Linux /proc 解析器          │  │
│  │  - 字节码插桩    │  │     - smaps                     │  │
│  │  - 分配追踪      │  │     - maps                      │  │
│  │  - 栈采集        │  │     - symbols                   │  │
│  └──────────────────┘  └─────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │              无锁环形缓冲区 (Ring Buffer)              │  │
│  │         用于高效传递原生数据到 Java 层                 │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

#### 3.5.3 Java 接口设计

```java
// memdiag-core 中的接口
package com.memdiag.core.nativeapi;

public interface NativeMemoryAnalyzer {
    boolean isAvailable();
    String getPlatform();
    boolean requiresAgent();          // 是否需要挂载 Agent
    boolean isAgentAttached();        // 是否已挂载 Agent
    boolean attachAgent();            // 动态挂载 Agent
    boolean detachAgent();            // 卸载 Agent

    // ========== 基础统计 ==========
    NativeMemorySummary getSummary();

    // ========== /proc 分析（无需 JVMTI） ==========
    List<MemoryRegion> getMemoryRegions();
    List<LibraryMapping> getLibraryMappings();

    // ========== 分配追踪（需要 JVMTI） ==========
    void startAllocationTracking(AllocationFilter filter);
    void stopAllocationTracking();
    AllocationTrace getAllocationTrace();

    // ========== 栈分析 ==========
    List<NativeStackFrame> getNativeStackFrames(ThreadId threadId);
    Map<AllocationSite, Long> getAllocationSites();

    // ========== 诊断 ==========
    NativeDiagnosis analyzeNativeLeaks();
}

// 数据模型
public class NativeMemorySummary {
    private long totalResident;        // RSS
    private long totalVirtual;         // VSS
    private long directByteBufferSize;
    private long jniAllocatedSize;
    private Map<String, Long> breakdownByCategory;  // Code, GC, Compiler, Internal...
}

public class MemoryRegion {
    private long startAddress;
    private long endAddress;
    private long residentSize;
    private String permissions;        // rwxp
    private String mappingFile;        // 关联的 so 文件或匿名
    private String regionType;          // heap, stack, mmap, etc.
}

public class AllocationSite {
    private List<NativeStackFrame> stackTrace;
    private long allocationCount;
    private long totalBytesAllocated;
    private long bytesStillLive;
}

public class NativeStackFrame {
    private String libraryName;
    private String functionName;
    private String sourceFile;
    private int lineNumber;
    private long instructionAddress;
}
```

#### 3.5.4 原生层实现（Linux，支持动态 attach）

**JVMTI Agent 支持两种加载方式**：
1. `Agent_OnLoad` - 启动时加载 (`-javaagent:`)
2. `Agent_OnAttach` - 动态加载 (通过 `VirtualMachine.loadAgent()`)

**文件: memdiag-native/src/main/c/jvmti/agent.cpp**

```cpp
#include <jvmti.h>
#include <jni.h>
#include <unordered_map>
#include <atomic>
#include <mutex>
#include "ring_buffer.h"
#include "class_transformer.h"

// 全局状态
struct GlobalState {
    JavaVM* jvm;
    jvmtiEnv* jvmti;
    JNIEnv* jni;
    std::atomic<bool> initialized;
    std::atomic<bool> tracking_enabled;
    std::mutex state_mutex;
    RingBuffer<AllocationEvent>* event_buffer;
    ClassTransformer* class_transformer;
};

static GlobalState* g_state = nullptr;

// ──────────────────────────────────────────────────────────
// 启动时加载: -javaagent:libmemdiag-agent.so
// ──────────────────────────────────────────────────────────
jint JNICALL Agent_OnLoad(JavaVM* vm, char* options, void* reserved) {
    return initialize_agent(vm, options, /*is_attach=*/false);
}

// ──────────────────────────────────────────────────────────
// 动态加载: VirtualMachine.loadAgent()
// ──────────────────────────────────────────────────────────
jint JNICALL Agent_OnAttach(JavaVM* vm, char* options, void* reserved) {
    return initialize_agent(vm, options, /*is_attach=*/true);
}

// ──────────────────────────────────────────────────────────
// 统一初始化
// ──────────────────────────────────────────────────────────
jint initialize_agent(JavaVM* vm, char* options, bool is_attach) {
    std::lock_guard<std::mutex> lock(g_state->state_mutex);

    if (g_state->initialized) {
        return JNI_OK;  // 已初始化
    }

    g_state->jvm = vm;
    vm->GetEnv((void**)&g_state->jvmti, JVMTI_VERSION_1_2);
    vm->GetEnv((void**)&g_state->jni, JNI_VERSION_1_8);

    // 设置 JVMTI 回调
    jvmtiEventCallbacks callbacks;
    memset(&callbacks, 0, sizeof(callbacks));
    callbacks.ClassFileLoadHook = &OnClassFileLoadHook;
    callbacks.VMDeath = &OnVMDeath;

    g_state->jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
    g_state->jvmti->SetEventNotificationMode(JVMTI_ENABLE,
                                                JVMTI_EVENT_CLASS_FILE_LOAD_HOOK,
                                                nullptr);

    // 如果是动态 attach，需要重新转换已加载的类
    if (is_attach) {
        retransform_already_loaded_classes();
    }

    g_state->initialized = true;
    return JNI_OK;
}

// ──────────────────────────────────────────────────────────
// 动态 attach 后，对已加载的类进行字节码插桩
// ──────────────────────────────────────────────────────────
void retransform_already_loaded_classes() {
    jint class_count;
    jclass* classes;
    g_state->jvmti->GetLoadedClasses(&class_count, &classes);

    for (jint i = 0; i < class_count; i++) {
        jclass klass = classes[i];
        char* class_name;
        g_state->jvmti->GetClassSignature(klass, &class_name, nullptr);

        // 只对目标类进行转换
        if (is_target_class(class_name)) {
            g_state->jvmti->RetransformClasses(1, &klass);
        }

        g_state->jvmti->Deallocate((unsigned char*)class_name);
    }

    g_state->jvmti->Deallocate((unsigned char*)classes);
}

// ──────────────────────────────────────────────────────────
// 字节码插桩回调（动态 attach 后也会触发）
// ──────────────────────────────────────────────────────────
void JNICALL OnClassFileLoadHook(
        jvmtiEnv* jvmti, JNIEnv* jni,
        jclass class_being_redefined,
        jobject loader, const char* name,
        jobject protection_domain,
        jint class_data_len,
        const unsigned char* class_data,
        jint* new_class_data_len,
        unsigned char** new_class_data) {

    // 对 Unsafe、ByteBuffer 等类进行字节码插桩
    if (g_state->class_transformer->shouldTransform(name)) {
        g_state->class_transformer->transform(
            name, class_data, class_data_len,
            new_class_data, new_class_data_len);
    }
}

// ──────────────────────────────────────────────────────────
// 卸载 Agent
// ──────────────────────────────────────────────────────────
void JNICALL Agent_OnUnload(JavaVM* vm) {
    std::lock_guard<std::mutex> lock(g_state->state_mutex);
    if (!g_state->initialized) return;

    // 恢复被转换的类
    restore_transformed_classes();

    // 清理资源
    delete g_state->event_buffer;
    delete g_state->class_transformer;
    g_state->initialized = false;
}
```

**字节码插桩设计**：
- 在 `Unsafe.allocateMemory/freeMemory/reallocateMemory` 前后插入回调
- 在 `ByteBuffer.allocateDirect` 前后插入回调
- 动态 attach 后，通过 `RetransformClasses` 对已加载类重新转换

#### 3.5.5 故障现场保留策略

**关键设计目标**: 发生 OOM 或异常时，无需重启即可诊断。

**分层诊断策略（按侵入程度递增）**:

```
┌───────────────────────────────────────────────────────────────┐
│  阶段 1: 零侵入诊断（无需 Agent，无需重启）                    │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ - 读取 /proc/<pid>/smaps, maps, status                   │ │
│  │ - 通过 JMX 获取 NMT 数据（如果已启用）                    │ │
│  │ - 查看 DirectByteBuffer 统计                               │ │
│  └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
           ↓ (如果需要更深层数据)
┌───────────────────────────────────────────────────────────────┐
│  阶段 2: 动态挂载 Agent（无需重启）                            │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ - VirtualMachine.loadAgent() 动态加载                     │ │
│  │ - Agent_OnAttach 初始化                                    │ │
│  │ - RetransformClasses 重新转换已加载类                      │ │
│  └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
           ↓ (开始追踪)
┌───────────────────────────────────────────────────────────────┐
│  阶段 3: 分配追踪（观察一段时间）                               │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ - 采样记录分配调用栈                                       │ │
│  │ - 分析增长趋势                                             │ │
│  │ - 定位泄露热点                                             │ │
│  └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
           ↓ (诊断完成)
┌───────────────────────────────────────────────────────────────┐
│  阶段 4: 卸载 Agent（无残留）                                  │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │ - 恢复类字节码                                              │ │
│  │ - 清理资源                                                  │ │
│  │ - Agent_OnUnload                                           │ │
│  └─────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────┘
```

**Java 层动态 attach 实现**:

```java
// memdiag-agent 中的动态 attach 逻辑
package com.memdiag.agent;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.AgentLoadException;

public class AgentAttach {

    public static boolean attach(String pid, String agentJarPath) {
        try {
            VirtualMachine vm = VirtualMachine.attach(pid);
            try {
                // 加载 Agent (触发 Agent_OnAttach)
                vm.loadAgent(agentJarPath);
                return true;
            } finally {
                vm.detach();
            }
        } catch (AgentLoadException e) {
            // 可能已加载，检查状态
            return checkAgentStatus(pid);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean detach(String pid) {
        // 通知 Agent 清理并卸载
        // 通过 JMX 或本地 socket 发送 detach 命令
        sendDetachCommand(pid);
        return true;
    }
}
```

**文件: memdiag-native/src/main/c/linux/proc_parser.cpp**

```cpp
// 解析 /proc/<pid>/smaps
std::vector<MemoryRegion> parse_smaps(pid_t pid) {
    std::vector<MemoryRegion> regions;
    char path[64];
    snprintf(path, sizeof(path), "/proc/%d/smaps", pid);

    FILE* fp = fopen(path, "r");
    if (!fp) return regions;

    // 解析格式:
    // 7f8c1a000000-7f8c1a200000 rw-p 00000000 00:00 0
    // Size:               2048 kB
    // Rss:                2048 kB
    // ...

    fclose(fp);
    return regions;
}

// 解析 /proc/<pid>/maps 获取库映射
std::vector<LibraryMapping> parse_maps(pid_t pid);

// 使用 dladdr 和 /proc/<pid>/maps 解析符号
std::string resolve_symbol(void* addr);
```

**文件: memdiag-native/src/main/c/shared/ring_buffer.cpp**

```cpp
// 无锁环形缓冲区，用于原生->Java 数据传递
template<typename T>
class RingBuffer {
public:
    RingBuffer(size_t capacity);
    bool push(const T& item);
    bool pop(T& item);

private:
    std::atomic<size_t> write_pos;
    std::atomic<size_t> read_pos;
    std::vector<T> buffer;
};
```

#### 3.5.5 插桩策略（JVMTI）

| 目标方法 | 插桩方式 | 数据采集 |
|----------|----------|----------|
| `Unsafe.allocateMemory(long)` | 字节码插桩 | 大小、调用栈、时间戳 |
| `Unsafe.freeMemory(long)` | 字节码插桩 | 地址、时间戳 |
| `Unsafe.reallocateMemory(long, long)` | 字节码插桩 | 旧地址、新大小 |
| `ByteBuffer.allocateDirect(int)` | 字节码插桩 | 容量、调用栈 |
| `JNI NewByteArray / Release*` | JVMTI 回调 | 大小、时间 |
| `mmap/munmap` (系统调用) | 可选：eBPF 或 ptrace | 仅在高级模式 |

#### 3.5.6 堆外诊断规则

- **DirectByteBuffer 异常增长**: 追踪分配和释放不匹配
- **JNI 内存泄露**: 检测未配对的 JNI 分配/释放
- **未映射区域增长**: 分析 `/proc/smaps` 中的匿名区域
- **分配热点**: 按调用栈统计分配次数和大小
- **库泄露**: 某些原生库的已知泄露模式

#### 3.5.7 性能影响

| 模式 | CPU 开销 | 内存开销 | 适用场景 |
|------|----------|----------|----------|
| 仅解析 /proc | < 1% | < 1MB | 日常监控 |
| JVMTI 采样（1%） | ~5% | ~10MB | 疑似泄露 |
| JVMTI 全量追踪 | ~50-100% | ~100MB+ | 深度诊断 |
| + 栈采集 | ~100-200% | ~500MB+ | 定位泄露点 |

**设计**:
- 默认 1% 采样率，可配置
- 缓冲区大小限制，防止自身 OOM
- 可随时开启/关闭追踪

---

## 4. 生产环境安全设计

### 4.1 资源限制

```java
public class ResourceLimiter {
    private final long maxMemoryBytes;
    private final double cpuLimit;
    private final Duration analysisTimeout;

    public <T> T executeWithLimit(Supplier<T> task);
}
```

**配置项**:
- `memdiag.max-memory=64m` - 自身最大内存
- `memdiag.sampling-rate=0.1` - 对象采样率
- `memdiag.analysis-timeout=30s` - 单次分析超时
- `memdiag.cpu-throttle=true` - 启用 CPU 限流

### 4.2 非阻塞设计

- 所有分析任务在单独的低优先级线程执行
- 堆 dump 需要用户显式确认（STW 警告）
- 支持任务取消和超时自动中止
- 增量计算 + 结果缓存

### 4.3 安全策略

- 默认只读模式（`agent.read-only=true`）
- 不修改字节码（除非显式启用）
- 敏感数据过滤（不记录字段值）
- Agent 可动态 detach，无残留

### 4.4 增强安全设计（采纳 Gemini 审查意见）

#### 4.4.1 Agent 冲突防御

**问题**: 多个 JVMTI/Java Agent 同时进行字节码转换可能导致 JVM 崩溃。

**解决方案**:
```cpp
// 全局状态中的原子锁
struct GlobalState {
    // ...
    std::atomic<bool> bytecode_transform_in_progress;
    std::mutex class_transform_mutex;
};

// 转换前获取锁，确保原子性
void safe_retransform_class(jclass klass) {
    bool expected = false;
    if (!g_state->bytecode_transform_in_progress.compare_exchange_strong(expected, true)) {
        return;  // 另一个转换正在进行，跳过
    }

    std::lock_guard<std::mutex> lock(g_state->class_transform_mutex);
    g_state->jvmti->RetransformClasses(1, &klass);
    g_state->bytecode_transform_in_progress = false;
}
```

#### 4.4.2 Safe Point 监控

**问题**: `findGcRoots`、全量直方图等操作可能触发全局 Safe Point，导致业务 STW。

**解决方案**:
```java
public class ResourceLimiter {
    // ...
    private final Duration maxSafePointTime;
    private final AtomicLong lastSafePointDuration = new AtomicLong(0);

    public <T> T executeWithSafePointMonitor(Supplier<T> task) {
        long start = System.currentTimeMillis();
        try {
            return task.get();
        } finally {
            long duration = System.currentTimeMillis() - start;
            lastSafePointDuration.set(duration);

            if (duration > maxSafePointTime.toMillis()) {
                // 超过阈值，记录警告并中止后续分析
                abortPendingAnalyses();
            }
        }
    }
}
```

**配置项** (新增):
- `memdiag.max-safepoint-time=500ms` - Safe Point 最大容忍时间

#### 4.4.3 原生层背压策略

**问题**: 高频分配场景下，无锁 Ring Buffer 可能溢出。

**解决方案**:
```cpp
template<typename T>
class RingBuffer {
public:
    enum class PushResult {
        SUCCESS,
        OVERFLOW_DROPPED  // 溢出时丢弃数据，不阻塞
    };

    PushResult push(const T& item) {
        size_t next_write = (write_pos + 1) % capacity;
        if (next_write == read_pos.load()) {
            overflow_count.fetch_add(1, std::memory_order_relaxed);
            return PushResult::OVERFLOW_DROPPED;
        }
        buffer[write_pos] = item;
        write_pos.store(next_write, std::memory_order_release);
        return PushResult::SUCCESS;
    }

    size_t get_overflow_count() const {
        return overflow_count.load(std::memory_order_relaxed);
    }

private:
    std::atomic<size_t> write_pos;
    std::atomic<size_t> read_pos;
    std::atomic<size_t> overflow_count;
    std::vector<T> buffer;
};
```

**策略**: 溢出时丢弃数据并计数，**不阻塞业务线程**。

#### 4.4.4 Retained Size 按需计算

**问题**: Retained Size 计算需要遍历对象图，内存和时间开销大。

**解决方案**:
```java
public interface HeapAnalyzer {
    // 默认仅计算 shallow size
    HeapHistogram getHistogram(int limit);

    // 显式请求才计算 retained size（开销大）
    HeapHistogram getHistogramWithRetained(int limit, ProgressListener listener);

    // GC Root 分析也是按需触发
    List<GcRootPath> findGcRoots(ObjectId objectId, int maxDepth);
}
```

#### 4.4.5 差异分析复合键

**问题**: 类加载器回收可能导致类 ID 漂移，快照对比不准确。

**解决方案**:
```java
public class ClassKey {
    private final String className;
    private final int classLoaderHash;  // ClassLoader.identityHashCode()

    public ClassKey(String className, ClassLoader classLoader) {
        this.className = className;
        this.classLoaderHash = System.identityHashCode(classLoader);
    }

    // equals & hashCode 基于 className + classLoaderHash
}

public class Snapshot {
    private final Map<ClassKey, ClassStats> stats;
    // ...
}
```

#### 4.4.6 符号解析缓存

**问题**: 频繁调用 `dladdr` 和解析 `/proc` 带来 I/O 尖峰。

**解决方案**:
```cpp
class SymbolCache {
private:
    std::unordered_map<void*, std::string> addr_to_symbol;
    std::unordered_map<std::string, std::string> file_to_symbols;
    std::mutex cache_mutex;
    size_t max_entries;

public:
    std::string resolve(void* addr) {
        std::lock_guard<std::mutex> lock(cache_mutex);

        auto it = addr_to_symbol.find(addr);
        if (it != addr_to_symbol.end()) {
            return it->second;
        }

        // 缓存未命中，调用 dladdr
        std::string symbol = do_resolve(addr);
        if (addr_to_symbol.size() < max_entries) {
            addr_to_symbol[addr] = symbol;
        }
        return symbol;
    }
};
```

#### 4.4.7 Thread Stack 和 Code Cache 监控

**堆外内存分类扩展**:
```java
public class NativeMemorySummary {
    // ...
    private long threadStackSize;        // 线程栈总大小
    private long codeCacheSize;          // JIT 代码缓存
    private Map<String, Long> threadStackByThread;  // 按线程细分
}
```

**诊断规则补充**:
- 线程栈异常增长（单个线程 > 8MB）
- Code Cache 满或接近满（可能导致 JIT 退化）

---

## 5. 使用模式设计

### 5.1 模式一：CLI 直接分析

**适用场景**: 临时分析、快速诊断

```bash
# 基本用法
java -jar memdiag-cli.jar <pid>

# 子命令
java -jar memdiag-cli.jar histogram <pid> --limit=20
java -jar memdiag-cli.jar gc-roots <pid> --class=com.example.BigObject
java -jar memdiag-cli.jar diagnose <pid>
java -jar memdiag-cli.jar threads <pid>

# 导出报告
java -jar memdiag-cli.jar report <pid> --format=html --output=report.html
java -jar memdiag-cli.jar report <pid> --format=json --output=report.json

# 差异分析
java -jar memdiag-cli.jar snapshot <pid> --save=snapshot1.bin
# ... 一段时间后 ...
java -jar memdiag-cli.jar diff <pid> --baseline=snapshot1.bin

# ========== 堆外分析命令（Linux 专用，默认不启用） ==========
# 检查堆外分析是否可用
java -jar memdiag-cli.jar native <pid> --status

# 启用堆外分析（加载原生模块）
java -jar memdiag-cli.jar native <pid> --enable

# 查看堆外概览
java -jar memdiag-cli.jar native <pid> --summary

# 查看内存区域分布（基于 /proc/smaps）
java -jar memdiag-cli.jar native <pid> --regions

# 开始分配追踪（采样模式）
java -jar memdiag-cli.jar native <pid> --start-trace --sampling-rate=0.01

# 停止追踪并查看结果
java -jar memdiag-cli.jar native <pid> --stop-trace
java -jar memdiag-cli.jar native <pid> --allocation-sites --limit=20

# 堆外泄露诊断
java -jar memdiag-cli.jar native <pid> --diagnose
```

**实现原理**:
- 使用 `VirtualMachine.attach(pid)` 动态连接
- 通过 JMX 获取数据
- 分析完成后自动 detach

### 5.2 模式二：Agent 挂载

**适用场景**: 持续监控、生产环境常驻

```bash
# 启动时挂载
java -javaagent:memdiag-agent.jar=port=5000,bind=127.0.0.1 -jar app.jar

# 动态挂载
java -jar memdiag-agent.jar --attach <pid> --port=5000

# 查询
java -jar memdiag-cli.jar connect localhost:5000
java -jar memdiag-cli.jar histogram localhost:5000
java -jar memdiag-cli.jar diagnose localhost:5000

# 实时监控
java -jar memdiag-cli.jar monitor localhost:5000 --interval=5s

# 卸载
java -jar memdiag-cli.jar detach localhost:5000
```

**Agent HTTP API**:
```
GET  /api/v1/histogram?limit=20
GET  /api/v1/gc-roots?className=...
GET  /api/v1/threads
GET  /api/v1/diagnose
POST /api/v1/snapshot
POST /api/v1/detach

# ========== 堆外分析 API（可选） ==========
GET  /api/v1/native/status
POST /api/v1/native/enable
GET  /api/v1/native/summary
GET  /api/v1/native/regions
POST /api/v1/native/trace/start?samplingRate=0.01
POST /api/v1/native/trace/stop
GET  /api/v1/native/allocation-sites?limit=20
GET  /api/v1/native/diagnose
```

### 5.3 模式三：Web UI（可选）

**适用场景**: 可视化分析、团队共享

```bash
# 启动 Web 服务
java -jar memdiag-web.jar --port=8080

# 浏览器打开
# http://localhost:8080
```

---

## 6. 技术选型

| 组件 | 技术选择 | 说明 |
|------|----------|------|
| 语言 | Java 11+ | 兼容性好 |
| 构建 | Maven | 稳定成熟 |
| Agent | java.lang.instrument | 标准 API |
| JVM 交互 | JMX / Attach API | 无需原生库 |
| Web 后端 | Spring Boot (轻量) | 可选模块 |
| 前端 | Vue 3 + TypeScript + ECharts | 可选模块 |
| 原生扩展（后续） | C++ JVMTI | 堆外深度分析 |

---

## 7. 实施计划

### 阶段零：基础设施（采纳 Gemini 建议）
- [ ] **错误处理规范**: 定义 `MemDiagException` 体系，区分环境不支持、资源超限、逻辑错误
- [ ] **异常体系**: `PlatformNotSupportedException` / `ResourceLimitExceededException` / `AnalysisException`
- [ ] **测试矩阵**: JDK 8 / JDK 11 / JDK 17 兼容性测试
- [ ] **性能基准框架**: 建立 Agent 挂载前后的基线对比

### 阶段一：MVP（最小可行产品）
- [ ] 项目脚手架搭建
- [ ] memdiag-core 基础框架
- [ ] 堆直方图分析（JMX 实现，默认仅 shallow size）
- [ ] SafePoint 监控
- [ ] memdiag-cli 基础命令
- [ ] 文本报告输出

### 阶段二：核心功能
- [ ] GC Root 分析（按需触发）
- [ ] 差异分析（使用 ClassName + ClassLoaderHash 复合键）
- [ ] 诊断引擎
- [ ] 线程分析
- [ ] HTML/JSON 报告
- [ ] ResourceLimiter 完善

### 阶段三：Agent 模式
- [ ] Java Agent 实现
- [ ] Agent 冲突防御（全局状态锁）
- [ ] 本地 HTTP API
- [ ] CLI 连接 Agent 模式

### 阶段四：Web UI（可选）
- [ ] Web 后端服务
- [ ] Vue 3 前端
- [ ] 实时图表展示

### 阶段五：堆外分析 - 基础层（Java 接口 + /proc 解析）
- [ ] NativeMemoryAnalyzer 接口定义（attach/detach 支持）
- [ ] NoOpNativeAnalyzer 默认实现
- [ ] Linux /proc/<pid>/smaps 解析器
- [ ] Linux /proc/<pid>/maps 解析器
- [ ] NMT 数据解析（基于 JMX）
- [ ] Thread Stack / Code Cache 专门监控
- [ ] CLI 堆外命令框架

### 阶段六：堆外分析 - JVMTI 原生层（Linux）
- [ ] JVMTI Agent 框架（Agent_OnLoad + Agent_OnAttach）
- [ ] 无锁环形缓冲区（背压策略：溢出丢弃）
- [ ] Unsafe.allocateMemory/freeMemory 字节码插桩
- [ ] ByteBuffer.allocateDirect 插桩
- [ ] RetransformClasses 支持（动态 attach 后重转换）
- [ ] 分配调用栈采集
- [ ] 符号解析缓存
- [ ] 原生库编译脚本 + 自解压加载

### 阶段七：堆外诊断引擎
- [ ] 分配-释放配对检测
- [ ] DirectByteBuffer 泄露检测
- [ ] 分配热点分析
- [ ] 诊断建议生成
- [ ] 性能基准测试（量化 CPU/内存增量）

---

## 8. 配置参考

### memdiag.properties

```properties
# ==========================================
# 资源限制
# ==========================================
memdiag.max-memory=64m
memdiag.sampling-rate=0.1
memdiag.analysis-timeout=30s
memdiag.cpu-throttle=true
memdiag.max-safepoint-time=500ms

# ==========================================
# Agent 配置
# ==========================================
agent.port=5000
agent.bind=127.0.0.1
agent.read-only=true
agent.sensitive-data-filter=true

# ==========================================
# 诊断规则
# ==========================================
diagnose.large-object-threshold=100mb
diagnose.growing-class-threshold=2.0
diagnose.enable-classloader-check=true

# ==========================================
# 堆外分析配置（默认不启用）
# ==========================================
# 是否启用原生模块
memdiag.native.enabled=false

# 原生库路径（可选，默认从 classpath 加载）
memdiag.native.library.path=

# 分配追踪采样率（0.01 = 1%）
memdiag.native.sampling-rate=0.01

# 事件缓冲区大小
memdiag.native.buffer-size=100000

# 是否自动加载符号表
memdiag.native.load-symbols=true
```

---

## 附录

### A. 术语表

- **Shallow Size**: 对象自身占用的大小
- **Retained Size**: 对象被 GC 后能释放的总大小
- **GC Root**: 垃圾回收的根对象
- **Heap Histogram**: 堆直方图，按类统计对象分布
- **JVMTI**: JVM Tool Interface，JVM 原生工具接口
- **堆外内存**: JVM 堆之外的原生内存，通过 Unsafe、ByteBuffer.allocateDirect、JNI 等分配
- **RSS**: Resident Set Size，进程实际驻留在物理内存中的大小
- **VSS**: Virtual Set Size，进程虚拟地址空间大小
- **/proc/smaps**: Linux 特有的进程内存映射信息文件
- **NMT**: Native Memory Tracking，HotSpot JVM 的原生内存追踪特性

### B. 参考资料

- OpenJDK JMX 文档
- java.lang.instrument 规范
- HotSpotDiagnosticMXBean API
- JVMTI 规范: https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html
- Linux /proc 文件系统: https://www.kernel.org/doc/html/latest/filesystems/proc.html
- HotSpot NMT: https://docs.oracle.com/en/java/javase/11/vm/native-memory-tracking.html
