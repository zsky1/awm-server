# AWM API 接口文档

> 后端服务端口: 8080 | 基础路径: http://localhost:8080

## 通用说明

### 请求格式

- Content-Type: `application/json`
- 分页参数: `page`（从 1 开始）、`size`（默认 20）

### 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 状态码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 500 | 服务器内部错误 |

---

## 1. Agent 管理

### 1.1 分页查询 Agent 列表

```
GET /api/agents?page=1&size=20&keyword=&departmentId=&lifecycleStatus=
```

**Query 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，默认 1 |
| size | int | 否 | 每页条数，默认 20 |
| keyword | string | 否 | 搜索关键词（匹配名称/职位） |
| departmentId | string | 否 | 部门 ID 筛选 |
| lifecycleStatus | string | 否 | 生命周期状态（draft/active/archived） |

**响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": "xxx",
        "name": "数据分析师小张",
        "avatar": "https://...",
        "position": "高级数据分析师",
        "departmentId": "dept-tech",
        "lifecycleStatus": "active",
        "runtimeStatus": "idle",
        "createdAt": "2026-06-11 10:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 20
  }
}
```

### 1.2 获取 Agent 详情

```
GET /api/agents/{id}
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "id": "xxx",
    "name": "数据分析师小张",
    "avatar": "https://...",
    "position": "高级数据分析师",
    "departmentId": "dept-tech",
    "personaPrompt": "你是一位严谨的数据分析师...",
    "lifecycleStatus": "active",
    "runtimeStatus": "idle",
    "createdAt": "2026-06-11 10:00:00"
  }
}
```

### 1.3 创建 Agent

```
POST /api/agents
```

**请求体**：
```json
{
  "name": "数据分析师小张",
  "avatar": "https://...",
  "position": "高级数据分析师",
  "departmentId": "dept-tech",
  "supervisorId": null,
  "personaPrompt": "你是一位严谨的数据分析师..."
}
```

### 1.4 更新 Agent

```
PUT /api/agents/{id}
```

**请求体**：同创建，所有字段可选（部分更新）。

### 1.5 删除 Agent（软删除）

```
DELETE /api/agents/{id}
```

> 将 lifecycleStatus 设为 archived

### 1.6 更新 Agent 状态

```
PUT /api/agents/{id}/status?status=idle
```

| status 值 | 说明 |
|-----------|------|
| offline | 离线 |
| idle | 空闲 |
| busy | 忙碌 |
| error | 异常 |

### 1.7 获取 Agent 配置

```
GET /api/agents/{id}/config
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "skills": [{ "tag": "数据分析", "description": "..." }],
    "rules": [{ "content": "必须先查询数据再给出结论" }],
    "memory": { "short_term": { "window_size": 10 } }
  }
}
```

### 1.8 更新 Agent 配置

```
PUT /api/agents/{id}/config
```

**请求体**：完整的 config JSON 对象。

---

## 2. 部门管理

### 2.1 获取部门树

```
GET /api/departments/tree
```

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": "dept-tech",
      "name": "技术部",
      "parentId": null,
      "sortOrder": 1,
      "children": [
        { "id": "dept-tech-fe", "name": "前端组", "parentId": "dept-tech", "sortOrder": 1, "children": [] }
      ]
    }
  ]
}
```

### 2.2 创建部门

```
POST /api/departments
```

**请求体**：
```json
{
  "name": "前端组",
  "parentId": "dept-tech",
  "sortOrder": 1
}
```

### 2.3 更新部门

```
PUT /api/departments/{id}
```

### 2.4 删除部门

```
DELETE /api/departments/{id}
```

---

## 3. MCP 服务管理

### 3.1 查询 MCP 服务列表

```
GET /api/mcp/servers
```

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": "xxx",
      "name": "天气查询服务",
      "endpoint": "http://weather-mcp:8080",
      "description": "提供全球天气查询能力",
      "healthStatus": "healthy",
      "lastCheckAt": "2026-06-11 10:00:00"
    }
  ]
}
```

### 3.2 注册 MCP 服务

```
POST /api/mcp/servers
```

**请求体**：
```json
{
  "name": "天气查询服务",
  "endpoint": "http://weather-mcp:8080",
  "description": "提供全球天气查询能力"
}
```

### 3.3 测试连通性

```
POST /api/mcp/servers/{id}/test
```

### 3.4 发现工具

```
GET /api/mcp/servers/{id}/tools
```

### 3.5 绑定到 Agent

```
POST /api/mcp/servers/{id}/bind/{agentId}
```

### 3.6 解绑

```
DELETE /api/mcp/servers/{id}/bind/{agentId}
```

---

## 4. 群聊协作

### 4.1 查询群列表

```
GET /api/chat/groups
```

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": "xxx",
      "name": "数据分析群",
      "managerId": "agent-001",
      "memberCount": 5,
      "createdAt": "2026-06-11 10:00:00"
    }
  ]
}
```

### 4.2 创建群

```
POST /api/chat/groups
```

**请求体**：
```json
{
  "name": "数据分析群",
  "managerId": "agent-001",
  "memberIds": ["agent-001", "agent-002", "agent-003"]
}
```

> managerId 的成员角色为 "manager"，其余为 "member"。总管会自动加入成员列表。

### 4.3 获取群成员

```
GET /api/chat/groups/{groupId}/members
```

### 4.4 获取消息历史

```
GET /api/chat/groups/{groupId}/messages?page=1&size=50
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "records": [
      {
        "id": "msg-001",
        "groupId": "xxx",
        "senderType": "user",
        "senderId": "user-001",
        "senderName": "管理员",
        "content": "@总管 请分析一下最近的销售数据",
        "messageType": "text",
        "metadata": null,
        "createdAt": "2026-06-11 10:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "size": 50
  }
}
```

### 4.5 发送消息

```
POST /api/chat/groups/{groupId}/messages
```

**请求体**：
```json
{
  "content": "@总管 请分析一下最近的销售数据",
  "messageType": "text"
}
```

> 如果消息包含 @总管名称，将自动触发任务调度流程。

### 4.6 SSE 流式订阅

```
GET /api/chat/groups/{groupId}/stream
```

- Content-Type: `text/event-stream`
- 超时: 30 分钟
- 事件格式:
  ```
  event: agent_message
  data: {"agentName":"数据分析师小张","content":"根据数据分析..."}
  ```

---

## 5. 任务管理

### 5.1 查询任务列表

```
GET /api/tasks?groupId=&status=
```

**Query 参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| groupId | string | 否 | 群 ID 筛选 |
| status | string | 否 | 状态筛选（pending/in_progress/completed/failed） |

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "id": "task-001",
      "groupId": "xxx",
      "title": "分析销售数据",
      "description": "对最近一个月的销售数据进行趋势分析",
      "assignedAgentId": "agent-002",
      "status": "in_progress",
      "progress": 60,
      "parentTaskId": "task-parent",
      "priority": 5
    }
  ]
}
```

### 5.2 任务详情

```
GET /api/tasks/{id}
```

### 5.3 更新任务状态

```
PUT /api/tasks/{id}/status?status=completed
```

| status 值 | 说明 |
|-----------|------|
| pending | 待执行 |
| in_progress | 执行中 |
| completed | 已完成 |
| failed | 失败 |

### 5.4 更新任务进度

```
PUT /api/tasks/{id}/progress?progress=80
```

| 参数 | 类型 | 说明 |
|------|------|------|
| progress | int | 进度百分比（0-100） |

---

## 6. 记忆管理

### 6.1 获取 Agent 记忆

```
GET /api/agents/{agentId}/memories?type=
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 否 | 记忆类型（short_term/long_term） |

### 6.2 清空 Agent 记忆

```
DELETE /api/agents/{agentId}/memories?type=
```

---

## 7. 知识库

### 7.1 查询知识库列表

```
GET /api/knowledge-bases
```

### 7.2 创建知识库

```
POST /api/knowledge-bases?name=xxx&description=xxx
```

### 7.3 上传文档

```
POST /api/knowledge-bases/{id}/documents
```

- Content-Type: `multipart/form-data`
- 参数: `file` (MultipartFile)

### 7.4 重新索引

```
POST /api/knowledge-bases/{id}/reindex
```

### 7.5 获取索引状态

```
GET /api/knowledge-bases/{id}/status
```

---

## 8. 仪表盘

### 8.1 获取统计数据

```
GET /api/dashboard/stats
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "totalAgents": 10,
    "onlineAgents": 3,
    "busyAgents": 2,
    "errorAgents": 0,
    "offlineAgents": 5,
    "pendingTasks": 4,
    "inProgressTasks": 2,
    "completedTasks": 15,
    "failedTasks": 1,
    "totalMcpServers": 3,
    "healthyMcpServers": 2,
    "unhealthyMcpServers": 1
  }
}
```
