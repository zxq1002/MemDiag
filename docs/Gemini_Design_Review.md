# MemDiag 设计文档审查报告

**审查日期**: 2026-03-24
**审查专家**: Gemini Architect
**关联文档**: `docs/superpowers/specs/2026-03-24-memdiag-design.md`
**最终结论**: **准予通过 (Approved with Minor Adjustments)**。在实现计划阶段落实下述意见后，可立即开始编码。

---

## 1. 总体评价

该设计展现了成熟的 JVM 诊断工具设计思路。
- **优点**: 模块化解耦彻底（CLI/Core/Agent/Native 分离）；诊断策略分层合理（零侵入 -> 采样 -> 全量）；生产环境安全意识强（资源限制、非阻塞设计）。
- **亮点**: 堆外内存分析方案非常详尽，尤其是动态 `RetransformClasses` 的应用，解决了“无需重启即可深度分析堆外分配”的痛点。

---

## 2. 深度审查意见

### 2.1 架构与稳定性 (Critical)
1. **Agent 冲突防御**: 动态 attach 模式下，JVMTI Agent 与 Java Agent 可能同时触发字节码转换。建议在 `MemDiagAgent` 初始化时增加全局状态锁，确保字节码修改的原子性，防止 JVM 崩溃。
2. **Safe Point 影响**: `findGcRoots` 和 `getHistogram` 操作在某些 JVM 配置下会触发全局 Safe Point。建议在 `ResourceLimiter` 中增加对 Safe Point 时长的监控，若 STW 时间超过阈值（如 500ms），应立即中止后续分析。
3. **原生层背压 (Backpressure)**: 无锁 Ring Buffer 在高频分配场景下易溢出。建议明确：溢出时是阻塞生产线程（严重影响业务）还是丢弃数据并计数（推荐）。

### 2.2 核心分析引擎 (Important)
1. **Retained Size 算法**: 明确 `retained size` 的计算时机。建议 Core 模块默认仅提供 `shallow size`，复杂的引用链和 `retained size` 分析应通过可选参数触发，并由 `ResourceLimiter` 严密监控其占用的临时内存。
2. **差异分析的基准漂移**: 多次快照对比时，若类加载器发生了回收，类 ID 可能会漂移。建议在 `Snapshot` 模型中引入 `ClassName + ClassLoaderHash` 作为复合键。

### 2.3 堆外分析扩展 (Technical)
1. **符号解析性能**: 在 Linux 生产环境频繁调用 `dladdr` 或解析 `smaps` 可能带来 I/O 尖峰。建议实现简单的符号缓存，并支持在 CLI 端进行异步符号化。
2. **堆外内存分类**: 建议增加对 `Thread Stack` 和 `Code Cache` 的专门监控规则，这两类内存在大规模应用中常被忽视。

---

## 3. 实现计划阶段建议清单

在进入 `writing-plans` 阶段时，请确保包含以下任务：
- [ ] **错误处理规范**: 定义 `MemDiagException` 体系，区分“系统环境不支持”、“资源超限”与“代码逻辑错误”。
- [ ] **自动化测试矩阵**: 至少包含 JDK 8 和 JDK 17 的兼容性测试。
- [ ] **原生模块构建流水线**: 确保 `memdiag-native` 的编译产物（.so）能自动打包进 JAR，并在运行时自解压加载。
- [ ] **性能基准测试**: 建立 Agent 挂载前后的 Baseline 对比，量化 CPU/内存增量。

---

## 4. 结论

**该设计在逻辑和架构上是自洽且先进的。** 虽然堆外分析部分的实现复杂度较高（涉及 JNI 和 JVMTI），但设计方案中提供的“分层退避策略”极大地降低了实施风险。

**结论: 批准进入实现计划阶段。**
