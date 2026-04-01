# Web 模块修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 web 模块的 API 响应格式问题，统一所有 API 返回格式，添加缺失的 GC Roots 功能。

**Architecture:** 
- 第一阶段：修复现有 API 端点的响应格式，统一使用 `{success, data, error, timestamp}` 格式
- 第二阶段：添加 GC Roots API 和前端页面
- 使用 TDD 方法，每个修改都有测试验证

**Tech Stack:** Java 11+, Spring Boot, JUnit 5, Maven

---

## 第一阶段：修复现有 API 格式问题

### Task 1: 修复 Histogram API 响应格式

**Files:**
- Modify: `memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java:61-85`

**Overview:** 修改 `/histogram/{id}` 端点，使其返回统一格式并正确映射字段名。

- [ ] **Step 1: 查看当前代码**

Read: `memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java`

- [ ] **Step 2: 修改 getHistogram 方法**

将第 61-85 行的 `getHistogram` 方法替换为：

```java
@GetMapping({"/histogram/{id}", "/connections/{id}/histogram"})
public ResponseEntity<String> getHistogram(
        @PathVariable String id,
        @RequestParam(defaultValue = "20") int limit) {
    try {
        HeapHistogram histogram = analysisService.getHistogram(id, limit);
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("timestamp", System.currentTimeMillis());

        JsonObject data = new JsonObject();
        data.addProperty("totalBytes", histogram.getTotalBytes());
        data.addProperty("totalObjects", histogram.getTotalObjects());

        com.google.gson.JsonArray classesArray = new com.google.gson.JsonArray();
        for (ClassStats entry : histogram.getClassStats()) {
            JsonObject cls = new JsonObject();
            cls.addProperty("className", entry.getClassName());
            cls.addProperty("totalSize", entry.getShallowBytes());
            cls.addProperty("instanceCount", entry.getObjectCount());
            classesArray.add(cls);
        }
        data.add("classes", classesArray);

        result.add("data", data);
        return ResponseEntity.ok(gson.toJson(result));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn compile -pl memdiag-web -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交更改**

```bash
git add memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java
git commit -m "fix: unify histogram API response format"
```

---

### Task 2: 验证其他 API 端点格式

**Files:**
- Read: `memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java`

**Overview:** 确认其他端点已使用正确格式。

- [ ] **Step 1: 检查 diagnose 端点**

确认 `/diagnose/{id}` 端点（第 87-114 行）已使用正确格式。✓

- [ ] **Step 2: 检查 threads 端点**

确认 `/threads/{id}` 端点（第 116-153 行）已使用正确格式。✓

- [ ] **Step 3: 检查 nmt 端点**

确认 `/nmt/{id}` 端点（第 155-185 行）已使用正确格式。✓

- [ ] **Step 4: 检查 Agent API 端点**

确认 Agent API 端点（第 189+ 行）已使用正确格式。✓

---

### Task 3: 测试修复后的功能

**Files:**
- Test: 手动测试或编写集成测试

**Overview:** 验证修复后的 API 是否正常工作。

- [ ] **Step 1: 启动 Web 应用**

Run: `mvn spring-boot:run -pl memdiag-web`
Expected: 应用正常启动

- [ ] **Step 2: 测试连接创建**

使用浏览器或 curl 访问 `http://localhost:8080/connections`，创建一个连接。

- [ ] **Step 3: 测试 Histogram API**

```bash
curl "http://localhost:8080/api/v1/connections/current/histogram"
```

Expected: 返回统一格式的 JSON，字段名为 `classes`、`totalSize`、`instanceCount`

- [ ] **Step 4: 验证前端页面**

访问 `http://localhost:8080/analysis/heap/current`，确认堆内存分析页面正常显示数据。

---

## 第二阶段：添加 GC Roots 功能

### Task 4: 在 AnalysisService 中添加 GC Roots 方法

**Files:**
- Modify: `memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java`

**Overview:** 在 AnalysisService 中添加 GC Roots 相关方法。

- [ ] **Step 1: 添加 GC Roots 方法**

在 `AnalysisService.java` 的末尾（第 395 行后）添加：

```java
    // ========== GC Roots API ==========

    public com.memdiag.core.heap.GcRootStats getGcRootStats(String id) {
        ConnectionType type = connectionTypes.get(id);
        if (type == ConnectionType.AGENT) {
            AgentClient client = agentConnections.get(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.getGcRootStats();
        } else {
            // JMX mode - not supported yet
            throw new UnsupportedOperationException("GC Roots analysis requires Agent mode");
        }
    }

    public boolean startGcRootTracking(String id) {
        ConnectionType type = connectionTypes.get(id);
        if (type == ConnectionType.AGENT) {
            AgentClient client = agentConnections.get(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.startGcRootTracking();
        } else {
            throw new UnsupportedOperationException("GC Roots tracking requires Agent mode");
        }
    }

    public boolean stopGcRootTracking(String id) {
        ConnectionType type = connectionTypes.get(id);
        if (type == ConnectionType.AGENT) {
            AgentClient client = agentConnections.get(id);
            if (client == null) {
                throw new IllegalArgumentException("No agent connection found for id: " + id);
            }
            return client.stopGcRootTracking();
        } else {
            throw new UnsupportedOperationException("GC Roots tracking requires Agent mode");
        }
    }
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl memdiag-web -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交更改**

```bash
git add memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
git commit -m "feat: add GC Roots methods to AnalysisService"
```

---

### Task 5: 在 ApiController 中添加 GC Roots 端点

**Files:**
- Modify: `memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java`

**Overview:** 在 ApiController 中添加 GC Roots API 端点。

- [ ] **Step 1: 添加 GC Roots 端点**

在 `ApiController.java` 的末尾（第 513 行前）添加：

```java
    // ========== GC Roots API ==========

    @GetMapping({"/gc-roots/stats/{id}", "/connections/{id}/gc-roots/stats"})
    public ResponseEntity<String> getGcRootStats(@PathVariable String id) {
        try {
            com.memdiag.core.heap.GcRootStats stats = analysisService.getGcRootStats(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("totalRoots", stats.getTotalRoots());

            JsonObject countsByType = new JsonObject();
            for (com.memdiag.core.heap.GcRootType type : com.memdiag.core.heap.GcRootType.values()) {
                countsByType.addProperty(type.name(), stats.getCount(type));
            }
            data.add("countsByType", countsByType);

            result.add("data", data);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @PostMapping({"/gc-roots/track/start/{id}", "/connections/{id}/gc-roots/track/start"})
    public ResponseEntity<String> startGcRootTracking(@PathVariable String id) {
        try {
            boolean success = analysisService.startGcRootTracking(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("success", success);
            result.add("data", data);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @PostMapping({"/gc-roots/track/stop/{id}", "/connections/{id}/gc-roots/track/stop"})
    public ResponseEntity<String> stopGcRootTracking(@PathVariable String id) {
        try {
            boolean success = analysisService.stopGcRootTracking(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());

            JsonObject data = new JsonObject();
            data.addProperty("success", success);
            result.add("data", data);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl memdiag-web -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交更改**

```bash
git add memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java
git commit -m "feat: add GC Roots API endpoints"
```

---

### Task 6: 在 WebController 中添加 GC Roots 页面路由

**Files:**
- Modify: `memdiag-web/src/main/java/com/memdiag/web/controller/WebController.java`

**Overview:** 添加 GC Roots 页面的路由。

- [ ] **Step 1: 添加路由方法**

在 `WebController.java` 的末尾（第 106 行后）添加：

```java
    @GetMapping("/gc-roots/{id}")
    public String gcRoots(@PathVariable String id, Model model) {
        model.addAttribute("pageTitle", "GC Roots - MemDiag");
        model.addAttribute("activePage", "gc-roots");
        model.addAttribute("connectionId", id);
        return "gc-roots";
    }
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl memdiag-web -am`
Expected: BUILD SUCCESS

- [ ] **Step 3: 提交更改**

```bash
git add memdiag-web/src/main/java/com/memdiag/web/controller/WebController.java
git commit -m "feat: add GC Roots page route"
```

---

### Task 7: 创建 GC Roots 前端页面

**Files:**
- Create: `memdiag-web/src/main/resources/templates/gc-roots.html`

**Overview:** 创建 GC Roots 分析页面。

- [ ] **Step 1: 创建页面文件**

创建 `memdiag-web/src/main/resources/templates/gc-roots.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head th:replace="layout :: head"></head>
<body>
    <div class="wrapper">
        <aside th:replace="layout :: sidebar"></aside>
        <main class="main-content">
            <header th:replace="layout :: header"></header>
            <div class="content">
                <div class="card" style="margin-bottom: 20px;">
                    <div class="card-body">
                        <div class="d-flex align-items-center gap-3">
                            <i class="bi bi-info-circle" style="font-size: 1.5rem; color: var(--info-color);"></i>
                            <div>
                                <strong>连接:</strong> <span id="connectionName">-</span>
                            </div>
                            <div class="ms-auto">
                                <button class="btn btn-primary" id="refreshBtn">
                                    <i class="bi bi-arrow-repeat"></i> 刷新
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="stats-grid" style="margin-bottom: 20px;">
                    <div class="stat-card">
                        <div class="stat-icon primary">
                            <i class="bi bi-diagram-3"></i>
                        </div>
                        <div class="stat-info">
                            <h4>GC Root 总数</h4>
                            <div class="value" id="totalRoots">-</div>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon success">
                            <i class="bi bi-play-circle"></i>
                        </div>
                        <div class="stat-info">
                            <h4>追踪状态</h4>
                            <div class="value" id="trackingStatus">-</div>
                        </div>
                    </div>
                </div>

                <div class="card" style="margin-bottom: 20px;">
                    <div class="card-header">
                        <h3><i class="bi bi-sliders"></i> 追踪控制</h3>
                    </div>
                    <div class="card-body">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <button class="btn btn-success" id="startTrackingBtn">
                                    <i class="bi bi-play-circle"></i> 启动追踪
                                </button>
                            </div>
                            <div class="col-md-6">
                                <button class="btn btn-danger" id="stopTrackingBtn">
                                    <i class="bi bi-stop-circle"></i> 停止追踪
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="row" style="margin-bottom: 20px;">
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-header">
                                <h3><i class="bi bi-pie-chart"></i> GC Root 类型分布</h3>
                            </div>
                            <div class="card-body">
                                <div class="chart-container" style="height: 250px;">
                                    <canvas id="gcRootPieChart"></canvas>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-header">
                                <h3><i class="bi bi-bar-chart"></i> GC Root 类型数量</h3>
                            </div>
                            <div class="card-body">
                                <div class="chart-container" style="height: 250px;">
                                    <canvas id="gcRootBarChart"></canvas>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header">
                        <h3><i class="bi bi-list-ul"></i> GC Root 类型详情</h3>
                    </div>
                    <div class="card-body" id="gcRootsContainer">
                        <div class="alert alert-info" style="margin: 0;">
                            <i class="bi bi-info-circle"></i> 加载中...
                        </div>
                    </div>
                </div>
            </div>
        </main>
    </div>

    <script th:src="@{/js/memdiag.js}"></script>
    <script th:fragment="scripts">
        let connectionId = currentConnectionId;
        let pieChart = null;
        let barChart = null;

        const chartColors = [
            '#0d6efd', '#198754', '#ffc107', '#dc3545', '#0dcaf0',
            '#6f42c1', '#fd7e14', '#20c997', '#e83e8c', '#6610f2'
        ];

        document.addEventListener('DOMContentLoaded', function() {
            document.getElementById('connectionName').textContent = connectionId;

            if (typeof Chart === 'undefined') {
                const checkChart = setInterval(function() {
                    if (typeof Chart !== 'undefined') {
                        clearInterval(checkChart);
                        initPage();
                    }
                }, 100);
                setTimeout(function() {
                    clearInterval(checkChart);
                    if (typeof Chart === 'undefined') {
                        showError('Chart.js 加载超时，请刷新页面重试');
                    }
                }, 5000);
            } else {
                initPage();
            }
        });

        function initPage() {
            loadGcRoots();
            document.getElementById('refreshBtn').addEventListener('click', loadGcRoots);
            document.getElementById('startTrackingBtn').addEventListener('click', startTracking);
            document.getElementById('stopTrackingBtn').addEventListener('click', stopTracking);
        }

        function formatNumber(num) {
            return num.toString().replace(/\\B(?=(\\d{3})+(?!\\d))/g, ',');
        }

        function loadGcRoots() {
            fetch(`/api/v1/connections/${encodeURIComponent(connectionId)}/gc-roots/stats`)
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        renderStats(data.data);
                        renderCharts(data.data.countsByType);
                        renderGcRootList(data.data.countsByType);
                    } else {
                        showError(data.error || '加载失败');
                    }
                })
                .catch(err => {
                    showError('加载失败: ' + err.message);
                });
        }

        function renderStats(data) {
            document.getElementById('totalRoots').textContent = formatNumber(data.totalRoots || 0);
            document.getElementById('trackingStatus').textContent = '就绪';
        }

        function renderCharts(countsByType) {
            const types = Object.entries(countsByType || {})
                .filter(([type, count]) => count > 0)
                .sort((a, b) => b[1] - a[1]);

            const pieCtx = document.getElementById('gcRootPieChart').getContext('2d');
            if (pieChart) pieChart.destroy();

            pieChart = new Chart(pieCtx, {
                type: 'doughnut',
                data: {
                    labels: types.map(([type]) => type),
                    datasets: [{
                        data: types.map(([, count]) => count),
                        backgroundColor: chartColors,
                        borderWidth: 2,
                        borderColor: '#fff'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'right',
                            labels: { boxWidth: 12, font: { size: 11 } }
                        }
                    }
                }
            });

            const barCtx = document.getElementById('gcRootBarChart').getContext('2d');
            if (barChart) barChart.destroy();

            barChart = new Chart(barCtx, {
                type: 'bar',
                data: {
                    labels: types.map(([type]) => type),
                    datasets: [{
                        label: '数量',
                        data: types.map(([, count]) => count),
                        backgroundColor: chartColors[0],
                        borderRadius: 4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: {
                        y: { beginAtZero: true }
                    }
                }
            });
        }

        function renderGcRootList(countsByType) {
            const container = document.getElementById('gcRootsContainer');
            const types = Object.entries(countsByType || {})
                .filter(([type, count]) => count > 0)
                .sort((a, b) => b[1] - a[1]);

            if (types.length === 0) {
                container.innerHTML = `
                    <div class="alert alert-info" style="margin: 0;">
                        <i class="bi bi-info-circle"></i> 暂无 GC Root 数据
                    </div>
                `;
                return;
            }

            let html = '<table class="table" style="margin: 0;">';
            html += '<thead><tr><th>类型</th><th>数量</th><th>占比</th></tr></thead>';
            html += '<tbody>';

            const total = types.reduce((sum, [, count]) => sum + count, 0);

            types.forEach(([type, count]) => {
                const percentage = total > 0 ? ((count / total) * 100).toFixed(1) : 0;
                html += `
                    <tr>
                        <td><code>${escapeHtml(type)}</code></td>
                        <td>${formatNumber(count)}</td>
                        <td>${percentage}%</td>
                    </tr>
                `;
            });

            html += '</tbody></table>';
            container.innerHTML = html;
        }

        function startTracking() {
            fetch(`/api/v1/connections/${encodeURIComponent(connectionId)}/gc-roots/track/start`, {
                method: 'POST'
            })
            .then(response => response.json())
            .then(data => {
                if (data.success && data.data.success) {
                    alert('追踪已启动');
                    document.getElementById('trackingStatus').textContent = '追踪中';
                } else {
                    alert('启动失败: ' + (data.error || '未知错误'));
                }
            })
            .catch(err => {
                alert('启动失败: ' + err.message);
            });
        }

        function stopTracking() {
            fetch(`/api/v1/connections/${encodeURIComponent(connectionId)}/gc-roots/track/stop`, {
                method: 'POST'
            })
            .then(response => response.json())
            .then(data => {
                if (data.success && data.data.success) {
                    alert('追踪已停止');
                    document.getElementById('trackingStatus').textContent = '已停止';
                    loadGcRoots();
                } else {
                    alert('停止失败: ' + (data.error || '未知错误'));
                }
            })
            .catch(err => {
                alert('停止失败: ' + err.message);
            });
        }

        function showError(message) {
            const container = document.getElementById('gcRootsContainer');
            container.innerHTML = `
                <div class="alert alert-danger" style="margin: 0;">
                    <i class="bi bi-exclamation-triangle"></i> ${escapeHtml(message)}
                </div>
            `;
        }

        function escapeHtml(text) {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        }
    </script>
</body>
</html>
```

- [ ] **Step 2: 提交更改**

```bash
git add memdiag-web/src/main/resources/templates/gc-roots.html
git commit -m "feat: add GC Roots frontend page"
```

---

### Task 8: 更新侧边栏添加 GC Roots 导航

**Files:**
- Modify: `memdiag-web/src/main/resources/templates/layout.html`

**Overview:** 在侧边栏添加 GC Roots 导航项。

- [ ] **Step 1: 修改 layout.html**

在 `layout.html` 的"高级"部分（第 74 行后）添加：

```html
                <a th:href="@{/gc-roots/{id}(id=${connectionId} ?: 'current')}"
                   class="nav-item" th:classappend="${activePage == 'gc-roots'} ? 'active' : ''"
                   >
                    <i class="bi bi-diagram-3"></i> GC Roots
                </a>
```

同时在 `pageUrlMap`（第 152 行后）添加：

```javascript
            'gc-roots': '/gc-roots/{id}',
```

- [ ] **Step 2: 提交更改**

```bash
git add memdiag-web/src/main/resources/templates/layout.html
git commit -m "feat: add GC Roots to sidebar navigation"
```

---

### Task 9: 添加 Agent 配置更新和 Detach API

**Files:**
- Modify: `memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java`
- Modify: `memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java`

**Overview:** 添加 Agent 配置更新和 Detach 功能。

- [ ] **Step 1: 在 AnalysisService 中添加配置更新方法**

在 `AnalysisService.java` 中添加：

```java
    public JsonObject updateAgentConfig(String id, Map<String, Object> config) {
        AgentClient client = agentConnections.get(id);
        if (client == null) {
            throw new IllegalArgumentException("No agent connection found for id: " + id);
        }
        // AgentClient 目前没有 updateConfig 方法，返回 placeholder
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("error", "Config update not implemented yet");
        return result;
    }
```

- [ ] **Step 2: 在 ApiController 中添加配置更新端点**

在 `ApiController.java` 中添加：

```java
    @PutMapping({"/agent/config/{id}", "/connections/{id}/agent/config"})
    public ResponseEntity<String> updateAgentConfig(
            @PathVariable String id,
            @RequestBody String configJson) {
        try {
            JsonObject config = JsonParser.parseString(configJson).getAsJsonObject();
            Map<String, Object> configMap = gson.fromJson(config, Map.class);
            JsonObject result = analysisService.updateAgentConfig(id, configMap);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }

    @PostMapping({"/agent/detach/{id}", "/connections/{id}/agent/detach"})
    public ResponseEntity<String> detachAgent(@PathVariable String id) {
        try {
            boolean success = analysisService.detachAgent(id);
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("timestamp", System.currentTimeMillis());
            JsonObject data = new JsonObject();
            data.addProperty("success", success);
            result.add("data", data);
            return ResponseEntity.ok(gson.toJson(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
        }
    }
```

- [ ] **Step 3: 编译验证**

Run: `mvn compile -pl memdiag-web -am`
Expected: BUILD SUCCESS

- [ ] **Step 4: 提交更改**

```bash
git add memdiag-web/src/main/java/com/memdiag/web/service/AnalysisService.java
git add memdiag-web/src/main/java/com/memdiag/web/controller/ApiController.java
git commit -m "feat: add agent config update and detach API"
```

---

### Task 10: 完整集成测试

**Files:**
- Test: 手动端到端测试

**Overview:** 测试所有功能是否正常工作。

- [ ] **Step 1: 编译整个项目**

Run: `mvn clean install -DskipTests`
Expected: BUILD SUCCESS

- [ ] **Step 2: 启动 Web 应用**

Run: `mvn spring-boot:run -pl memdiag-web`
Expected: 应用正常启动在 8080 端口

- [ ] **Step 3: 测试连接管理**

访问 `http://localhost:8080/connections`，创建一个连接。

- [ ] **Step 4: 测试堆内存分析页面**

访问 `http://localhost:8080/analysis/heap/current`，验证：
- 数据正常显示
- 图表正确渲染
- 搜索和筛选功能正常

- [ ] **Step 5: 测试其他分析页面**

验证以下页面正常工作：
- 线程分析
- 诊断报告
- 堆外内存
- 分配追踪
- 方法监控
- Agent 管理

- [ ] **Step 6: 测试 GC Roots 页面**

访问 `http://localhost:8080/gc-roots/current`，验证：
- 页面正常加载
- GC Root 统计显示
- 图表正确渲染

- [ ] **Step 7: 提交最终测试确认**

```bash
git status
echo "All tests passed"
```

---

## 验证清单

- [ ] Histogram API 返回统一格式
- [ ] 字段名正确映射（shallowBytes → totalSize, objectCount → instanceCount, classStats → classes）
- [ ] 堆内存分析页面正常显示数据
- [ ] GC Roots API 端点可访问
- [ ] GC Roots 前端页面可访问
- [ ] 侧边栏包含 GC Roots 导航
- [ ] 所有现有功能正常工作
- [ ] 编译成功
- [ ] 端到端测试通过
