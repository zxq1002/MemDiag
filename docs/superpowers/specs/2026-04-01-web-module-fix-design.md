---
name: Web 模块修复设计
description: 统一 API 响应格式、修复字段名不一致、添加缺失功能
type: spec
---

# Web 模块修复设计

## 概述

本文档描述了 MemDiag Web 模块的修复方案，包括统一 API 响应格式、修复字段名不一致问题、以及添加缺失的功能。

## 问题分析

### 1. API 响应格式不统一

**当前问题：**
- `/histogram/{id}` 端点返回直接的数据对象，没有 `success` 包装
- 其他端点（如 `/diagnose/{id}`, `/threads/{id}`）使用了 `{success, data, error}` 格式

**heap.html 前端期望：**
```json
{
  "success": true,
  "data": {
    "classes": [
      {
        "className": "java.lang.String",
        "totalSize": 1024,
        "instanceCount": 100
      }
    ]
  }
}
```

**ApiController 实际返回：**
```json
{
  "totalBytes": 1024,
  "totalObjects": 100,
  "classStats": [
    {
      "className": "java.lang.String",
      "shallowBytes": 1024,
      "objectCount": 100
    }
  ]
}
```

### 2. 字段名不匹配

| 前端期望 | 后端返回 | 说明 |
|----------|----------|------|
| `totalSize` | `shallowBytes` | 类的总大小 |
| `instanceCount` | `objectCount` | 实例数量 |
| `classes` | `classStats` | 类统计数组 |

### 3. 缺失的功能

- GC Roots API（stats, tracking start/stop, paths）
- Agent 配置更新 API（PUT config）
- Agent detach API
- GC Roots 前端页面

## 设计方案

### 1. 统一 API 响应格式规范

所有 API 端点将采用以下标准格式：

**成功响应：**
```json
{
  "success": true,
  "data": { /* 具体数据 */ },
  "error": null,
  "timestamp": 1234567890
}
```

**错误响应：**
```json
{
  "success": false,
  "data": null,
  "error": "错误描述",
  "timestamp": 1234567890
}
```

### 2. 字段名统一

将以下字段名进行统一映射：

| 原字段名 | 新字段名 | 位置 |
|----------|----------|------|
| `shallowBytes` | `totalSize` | ClassStats |
| `objectCount` | `instanceCount` | ClassStats |
| `classStats` | `classes` | HeapHistogram 响应 |

### 3. 修复内容

#### 3.1 修复 ApiController

##### 修改 `/histogram/{id}` 端点

**当前代码：**
```java
@GetMapping({"/histogram/{id}", "/connections/{id}/histogram"})
public ResponseEntity<String> getHistogram(...) {
    HeapHistogram histogram = analysisService.getHistogram(id, limit);
    JsonObject result = new JsonObject();
    result.addProperty("totalBytes", histogram.getTotalBytes());
    result.addProperty("totalObjects", histogram.getTotalObjects());
    // ... 添加 classStats
    return ResponseEntity.ok(gson.toJson(result));
}
```

**修复后代码：**
```java
@GetMapping({"/histogram/{id}", "/connections/{id}/histogram"})
public ResponseEntity<String> getHistogram(...) {
    try {
        HeapHistogram histogram = analysisService.getHistogram(id, limit);
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("timestamp", System.currentTimeMillis());

        JsonObject data = new JsonObject();
        data.addProperty("totalBytes", histogram.getTotalBytes());
        data.addProperty("totalObjects", histogram.getTotalObjects());

        JsonArray classesArray = new JsonArray();
        for (ClassStats entry : histogram.getClassStats()) {
            JsonObject cls = new JsonObject();
            cls.addProperty("className", entry.getClassName());
            cls.addProperty("totalSize", entry.getShallowBytes());  // 字段重命名
            cls.addProperty("instanceCount", entry.getObjectCount());  // 字段重命名
            classesArray.add(cls);
        }
        data.add("classes", classesArray);  // 字段重命名

        result.add("data", data);
        return ResponseEntity.ok(gson.toJson(result));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(gson.toJson(errorResponse(e.getMessage())));
    }
}
```

#### 3.2 验证其他端点

确认以下端点已经使用正确格式：
- `/diagnose/{id}` ✓
- `/threads/{id}` ✓
- `/nmt/{id}` ✓
- Agent API ✓
- Native Memory API ✓
- Allocations API ✓
- Methods API ✓

### 4. 新增功能（修复完成后）

#### 4.1 GC Roots API

**新增端点：**

| 端点 | 方法 | 说明 |
|------|------|------|
| `/gc-roots/stats/{id}` | GET | 获取 GC Roots 统计 |
| `/gc-roots/track/start/{id}` | POST | 启动 GC Roots 追踪 |
| `/gc-roots/track/stop/{id}` | POST | 停止 GC Roots 追踪 |

**AnalysisService 新增方法：**
```java
public GcRootStats getGcRootStats(String id);
public boolean startGcRootTracking(String id);
public boolean stopGcRootTracking(String id);
```

#### 4.2 Agent 配置更新 API

**新增端点：**
```
PUT /api/v1/connections/{id}/agent/config
```

**请求体：**
```json
{
  "allocationEnabled": true,
  "methodMonitorEnabled": false
}
```

#### 4.3 Agent Detach API

**新增端点：**
```
POST /api/v1/connections/{id}/agent/detach
```

#### 4.4 GC Roots 前端页面

创建新页面 `templates/gc-roots.html`，包含：
- GC Roots 统计显示
- 追踪启动/停止控制
- GC Root 类型分布图表
- 类筛选功能

#### 4.5 侧边栏更新

在 `layout.html` 的"高级"部分添加：
```html
<a th:href="@{/gc-roots/{id}(id=${connectionId} ?: 'current')}"
   class="nav-item" th:classappend="${activePage == 'gc-roots'} ? 'active' : ''">
    <i class="bi bi-diagram-3"></i> GC Roots
</a>
```

## 实施顺序

### 第一阶段：修复现有问题
1. 统一 ApiController 响应格式（histogram 端点）
2. 修复字段名映射
3. 测试所有现有功能

### 第二阶段：添加缺失功能
1. 添加 GC Roots API（AnalysisService + ApiController）
2. 添加 GC Roots 前端页面
3. 添加 Agent 配置更新 API
4. 添加 Agent Detach API
5. 更新侧边栏导航

## 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 前端页面需要更新字段名 | 中 | 高 | 先确认前端期望的格式 |
| 现有功能可能被破坏 | 高 | 中 | 逐步修改，充分测试 |
| GC Roots 功能复杂 | 高 | 中 | 先实现基础统计功能 |

## 验证标准

- [ ] 所有现有 API 端点返回统一格式
- [ ] 堆内存分析页面正常显示数据
- [ ] 线程分析页面正常显示数据
- [ ] 诊断报告页面正常显示数据
- [ ] 堆外内存页面正常显示数据
- [ ] 分配追踪页面正常显示数据
- [ ] 方法监控页面正常显示数据
- [ ] Agent 管理页面正常显示数据
- [ ] GC Roots 页面可访问并显示数据
