# AWM 架构设计文档

> 版本: 1.0.0 | 更新日期: 2026-06-11

## 1. 系统架构总览

### 1.1 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                     AWM Desktop (Electron)                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │
│  │ 通讯录   │ │  群聊    │ │  仪表盘  │ │  设置    │       │
│  │ (Agent)  │ │  (Chat)  │ │(Dashboard)│ │(Settings)│       │
│  └─────┬────┘ └─────┬────┘ └─────┬────┘ └─────┬────┘       │
│        └────────────┴────────────┴────────────┘             │
│                         │ REST API + SSE                     │
└─────────────────────────┼───────────────────────────────────┘
                          │
┌─────────────────────────┼───────────────────────────────────┐
│                    AWM Server (Spring Boot)                  │
│                         │                                     │
│  ┌──────────────────────┼──────────────────────────────┐   │
│  │                  awm-web (Controller)                 │   │
│  │  AgentCtrl  ChatCtrl  TaskCtrl  McpCtrl  DashCtrl   │   │
│  └──────────────────────┼──────────────────────────────┘   │
│                         │                                     │
│  ┌──────────────────────┼──────────────────────────────┐   │
│  │              awm-service (业务逻辑)                  │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐            │   │
│  │  │  Agent   │ │  Chat    │ │  Task    │            │   │
│  │  │  Service │ │  Service │ │  Service │            │   │
│  │  └──────────┘ └──────────┘ └──────────┘            │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐            │   │
│  │  │   MCP    │ │  Memory  │ │Knowledge │            │   │
│  │  │  Service │ │  Service │ │  Service │            │   │
│  │  └──────────┘ └──────────┘ └──────────┘            │   │
│  └──────────────────────┼──────────────────────────────┘   │
│                         │                                     │
│  ┌──────────────────────┼──────────────────────────────┐   │
│  │              awm-ai-engine (AI 能力)                 │   │
│  │  LlmClient  McpClient  EmbeddingClient  PromptBuilder│   │
│  └──────────────────────┼──────────────────────────────┘   │
│                         │                                     │
│  ┌──────────────────────┼──────────────────────────────┐   │
│  │    awm-dal (数据访问)  │  awm-model (数据模型)       │   │
│  │    12 Mapper 接口     │  12 Entity + 8 DTO + 8 VO   │   │
│  └──────────────────────┼──────────────────────────────┘   │
│                         │                                     │
│  ┌──────────────────────┼──────────────────────────────┐   │
│  │              awm-common (通用基础)                     │   │
│  │  Result  PageResult  BizException  UuidUtils         │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────┼───────────────────────────────────┐
│               基础设施层 (Docker)                            │
│  ┌──────────┐ ┌──────┐ ┌────────┐ ┌──────┐                │
│  │PostgreSQL│ │Redis │ │ Qdrant │ │MinIO │                │
│  │  :5432   │ │:6379 │ │ :6333  │ │:9000 │                │
│  └──────────┘ └──────┘ └────────┘ └──────┘                │
└─────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────┼───────────────────────────────────┐
│               外部服务                                       │
│  ┌──────────────────┐  ┌──────────────────────┐            │
│  │ LLM API          │  │  MCP Servers          │            │
│  │ (DeepSeek/OpenAI)│  │  (第三方工具服务)       │            │
│  └──────────────────┘  └──────────────────────┘            │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 分层架构

| 层次 | 模块 | 职责 | 对外依赖 |
|------|------|------|----------|
| **启动层** | awm-app | Spring Boot 启动入口，Flyway 迁移 | 所有模块 |
| **展示层** | awm-web | REST Controller + CORS/SSE/WebSocket 配置 | awm-service, awm-model, awm-common |
| **服务层** | awm-service | 核心业务逻辑（6 个子模块） | awm-dal, awm-ai-engine, awm-model, awm-common |
| **AI 引擎层** | awm-ai-engine | LLM 调用、MCP 协议、Embedding、Prompt 构建 | awm-model (仅 Entity) |
| **数据访问层** | awm-dal | MyBatis-Plus Mapper 接口 | awm-model |
| **模型层** | awm-model | Entity、DTO、VO 定义 | 无 |
| **通用层** | awm-common | 统一响应、异常处理、工具类 | 无 |

## 2. Maven 模块依赖关系

```
awm-parent (pom)
  ├── awm-common          ← 无内部依赖
  ├── awm-model           ← awm-common
  ├── awm-dal             ← awm-model
  ├── awm-ai-engine       ← awm-model
  ├── awm-service (pom)
  │   ├── awm-service-agent     ← awm-dal, awm-ai-engine, awm-model, awm-common
  │   ├── awm-service-chat      ← awm-dal, awm-model, awm-common
  │   ├── awm-service-task      ← awm-dal, awm-ai-engine, awm-service-chat, awm-service-mcp, awm-service-agent, awm-model, awm-common
  │   ├── awm-service-mcp       ← awm-dal, awm-ai-engine, awm-model, awm-common
  │   ├── awm-service-memory    ← awm-dal, awm-ai-engine, awm-model, awm-common
  │   └── awm-service-knowledge ← awm-dal, awm-ai-engine, awm-model, awm-common
  ├── awm-web             ← awm-service(全部), awm-dal, awm-model, awm-common
  └── awm-app             ← awm-web, awm-service(全部), awm-dal, awm-ai-engine, awm-model, awm-common
```

> **注意**: `awm-service-task` 依赖 `awm-service-chat` 和 `awm-service-mcp`。为避免循环依赖，`ChatGroupService` 通过 `ApplicationEventPublisher` 发布 `DispatchTaskEvent`，由 `DispatchService` 监听处理。

## 3. 数据库设计

### 3.1 ER 关系图

```
┌──────────────┐       ┌───────────────────┐
│  department  │       │       agent        │
├──────────────┤       ├───────────────────┤
│ id (PK)      │◄──┐   │ id (PK)           │
│ name         │   │   │ name              │
│ parent_id    │   │   │ avatar            │
│ sort_order   │   │   │ position          │
└──────────────┘   │   │ department_id (FK)│──► department.id
                   │   │ supervisor_id (FK)│──► agent.id
                   │   │ persona_prompt    │
                   │   │ lifecycle_status  │
                   │   │ runtime_status    │
                   │   │ config (JSONB)    │
                   │   └───────┬───────────┘
                   │           │
                   │     ┌─────┴──────────────────────┐
                   │     │                            │
                   │  ┌──┴──────────────┐  ┌──────────┴────────┐
                   │  │agent_mcp_binding│  │agent_knowledge_   │
                   │  ├─────────────────┤  │     binding       │
                   │  │ id (PK)         │  ├───────────────────┤
                   │  │ agent_id (FK)   │  │ id (PK)           │
                   │  │ mcp_server_id   │  │ agent_id (FK)     │
                   │  │ enabled         │  │ kb_id (FK)        │
                   │  │ config (JSONB)  │  │ alias             │
                   │  └───────┬─────────┘  │ sync_interval     │
                   │          │             └────────┬──────────┘
                   │          │                      │
                   │          ▼                      ▼
                   │  ┌──────────────┐     ┌────────────────┐
                   │  │  mcp_server  │     │ knowledge_base │
                   │  ├──────────────┤     ├────────────────┤
                   │  │ id (PK)      │     │ id (PK)        │
                   │  │ name         │     │ name           │
                   │  │ endpoint     │     │ description    │
                   │  │ description  │     │ vector_collection│
                   │  │ tools (JSONB)│     │ index_status   │
                   │  │ health_status│     │ last_indexed_at│
                   │  │ last_check_at│     └────────────────┘
                   │  │ config (JSONB)│
                   │  └──────────────┘
                   │
                   │  ┌──────────────────┐
                   │  │    mcp_call_log   │
                   │  ├──────────────────┤
                   │  │ id (PK)           │
                   │  │ agent_id (FK)     │
                   │  │ mcp_server_id (FK) │
                   │  │ tool_name         │
                   │  │ request_params    │
                   │  │ response_data     │
                   │  │ status            │
                   │  │ duration_ms       │
                   │  └──────────────────┘
                   │
                   │  ┌──────────────┐
                   └──┤   memory     │
                      ├──────────────┤
                      │ id (PK)      │
                      │ agent_id (FK)│
                      │ type         │
                      │ content      │
                      │ summary      │
                      │ token_count  │
                      │ session_id   │
                      └──────────────┘

┌──────────────┐     ┌───────────────────┐     ┌──────────────┐
│  chat_group  │     │ chat_group_member  │     │   message    │
├──────────────┤     ├───────────────────┤     ├──────────────┤
│ id (PK)      │◄──┐│ id (PK)           │     │ id (PK)      │
│ name         │   ││ group_id (FK)     │──►  │ group_id (FK)│
│ manager_id   │   ││ agent_id (FK)     │     │ sender_type  │
│ created_by   │   ││ role              │     │ sender_id    │
│ created_at   │   ││ joined_at         │     │ sender_name  │
│ updated_at   │   │└───────────────────┘     │ content      │
└──────────────┘   │                           │ message_type │
                   └─── chat_group.id           │ metadata     │
                                               │ created_at   │
┌──────────────┐                                └──────────────┘
│    task      │
├──────────────┤
│ id (PK)      │
│ group_id (FK)│──► chat_group.id
│ title        │
│ description  │
│ assigned_    │
│  agent_id   │──► agent.id
│ status       │
│ progress     │
│ parent_task_ │
│  id         │──► task.id (自引用)
│ priority     │
└──────────────┘
```

### 3.2 核心表说明

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `agent` | AI Agent 实体 | `config`(JSONB)、`lifecycle_status`、`runtime_status` |
| `department` | 部门（树形结构） | `parent_id` 自引用 |
| `mcp_server` | MCP 工具服务注册 | `endpoint`、`tools`(JSONB)、`health_status` |
| `agent_mcp_binding` | Agent-MCP 绑定关系 | `enabled`、`config`(JSONB) |
| `agent_knowledge_binding` | Agent-知识库绑定 | `alias`、`sync_interval` |
| `chat_group` | 群聊 | `manager_id`(总管 Agent) |
| `chat_group_member` | 群聊成员 | `role`(manager/member) |
| `message` | 聊天消息 | `sender_type`、`metadata`(JSONB) |
| `task` | 任务（支持父子） | `parent_task_id`(自引用)、`status`、`progress` |
| `memory` | Agent 记忆 | `type`(short_term/long_term)、`session_id` |
| `knowledge_base` | 知识库 | `vector_collection`、`index_status`(JSONB) |
| `mcp_call_log` | MCP 工具调用日志 | `duration_ms`、`request_params`(JSONB) |

### 3.3 Agent Config JSONB 结构

```json
{
  "skills": [
    { "tag": "数据分析", "description": "擅长 SQL 和 Python 数据处理" },
    { "tag": "报告撰写", "description": "能够生成专业分析报告" }
  ],
  "rules": [
    { "content": "必须先查询数据再给出结论" },
    { "content": "报告必须包含数据来源" }
  ],
  "memory": {
    "short_term": { "window_size": 10 },
    "long_term": { "max_entries": 100 }
  }
}
```

## 4. 核心业务流程

### 4.1 任务调度流程（核心流程）

这是 AWM 最核心的业务流程：用户在群聊中 @总管，总管通过 LLM 拆解任务并分配给群成员。

```
用户发送消息 ──► ChatGroupController.sendMessage()
       │
       ▼
ChatGroupService.processUserMessage()
       │
       ├── 检查是否 @总管
       │      │
       │      ▼ 是
       │   发布 DispatchTaskEvent (Spring Event)
       │      │
       │      ▼
       │   DispatchService.onDispatchTaskEvent()
       │      │
       │      ├── 1. 获取群内总管 Agent
       │      ├── 2. 收集群成员技能信息
       │      ├── 3. PromptBuilder.buildDispatcherPrompt()
       │      ├── 4. LlmClient.chatWithTools(assign_task)
       │      ├── 5. 解析 ToolCall 结果
       │      ├── 6. 创建父 Task + 子 Task
       │      └── 7. 异步调用 executeSubTask()
       │              │
       │              ▼
       │         ConfigLoader.loadAgentRuntime()
       │              │
       │              ├── 读取 Agent 基础信息
       │              ├── 解析 config JSONB
       │              ├── 查询 MCP 工具绑定
       │              ├── 查询知识库绑定
       │              └── 拼接 System Prompt
       │              │
       │              ▼
       │         LlmClient.chatStream() 流式执行
       │              │
       │              ├── SSE 推送流式内容到前端
       │              ├── 处理 ToolCall → ToolCallProxy.executeTool()
       │              │       │
       │              │       ├── 检查 Agent-MCP 授权
       │              │       ├── McpClient.callTool() JSON-RPC 2.0
       │              │       └── 记录 McpCallLog
       │              └── 更新 Task 状态
       │
       └── 否：普通对话（待扩展）
```

### 4.2 Agent 运行时配置加载流程

```
ConfigLoader.loadAgentRuntime(agentId)
     │
     ├── Step 1: AgentMapper.selectById() → Agent 基础信息
     ├── Step 2: 解析 config JSONB → skills, rules, memory 配置
     ├── Step 3: AgentMcpBindingMapper → 绑定的 MCP 服务
     │        └── McpClient.listTools() → 可用工具列表
     ├── Step 4: AgentKnowledgeBindingMapper → 绑定的知识库
     └── Step 5: PromptBuilder.buildSystemPrompt()
              → 拼接完整 System Prompt
                   │
                   ├── 角色定义 (name + position)
                   ├── 人设 (persona_prompt)
                   ├── 技能 (skills)
                   ├── 规则 (rules)
                   └── 长期记忆摘要
```

### 4.3 MCP 工具调用流程

```
Agent 执行任务中触发 ToolCall
     │
     ▼
ToolCallProxy.executeTool(agentId, mcpServerId, toolName, params)
     │
     ├── 1. 查找工具所属 MCP Server
     ├── 2. 检查 Agent-MCP 绑定授权
     ├── 3. McpClient.callTool(endpoint, toolName, params)
     │        │
     │        └── JSON-RPC 2.0 调用:
     │            POST {endpoint}/mcp
     │            {
     │              "jsonrpc": "2.0",
     │              "method": "tools/call",
     │              "params": { "name": "...", "arguments": {...} },
     │              "id": 3
     │            }
     ├── 4. 记录 McpCallLog（含耗时）
     └── 5. 返回调用结果
```

### 4.4 SSE 实时推送流程

```
前端订阅                          后端推送
   │                                │
   ▼                                │
GET /api/chat/groups/{id}/stream    │
   │                                │
   ▼                                │
SseManager.addEmitter(groupId)      │
   │                                │
   │◄─── SSE 连接建立 ──────────────┤
   │                                │
   │    Agent 流式回复时:            │
   │                                ▼
   │               SseManager.pushEventToGroup()
   │                                │
   │◄──── event: agent_message ─────┤
   │        data: {"agentName":"...","content":"..."}
   │                                │
   │    30分钟超时或客户端断开:      │
   │                                ▼
   │               SseManager.removeEmitter()
```

## 5. API 接口设计

### 5.1 接口总览

| 模块 | 基础路径 | Controller | 主要功能 |
|------|---------|------------|----------|
| Agent 管理 | `/api/agents` | AgentController | CRUD、状态管理、配置管理 |
| 部门管理 | `/api/departments` | DepartmentController | 部门树 CRUD |
| MCP 服务 | `/api/mcp/servers` | McpServerController | 注册/发现/绑定/测试 |
| 群聊协作 | `/api/chat/groups` | ChatGroupController | 群管理、消息、SSE 流 |
| 任务管理 | `/api/tasks` | TaskController | 查询、状态更新、进度更新 |
| 记忆管理 | `/api/agents/{id}/memories` | MemoryController | 查看/清空 Agent 记忆 |
| 知识库 | `/api/knowledge-bases` | KnowledgeBaseController | 创建/上传文档/索引 |
| 仪表盘 | `/api/dashboard` | DashboardController | 统计数据 |

### 5.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

分页响应:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [ ... ],
    "total": 100,
    "page": 1,
    "size": 20
  }
}
```

## 6. 关键设计决策

### 6.1 ID 策略

- 所有表主键使用 `VARCHAR(36)`，由 MyBatis-Plus `ASSIGN_UUID` 策略自动生成
- 不使用数据库自增 ID，便于分布式扩展和数据迁移

### 6.2 Agent Config 用 JSONB

- Agent 的 skills、rules、memory 等配置存储在 `config` JSONB 字段
- 优势：灵活扩展，无需频繁改表；PostgreSQL JSONB 支持索引查询
- 运行时通过 `ConfigLoader` 解析为 Java Map

### 6.3 跨模块解耦：Spring Event

- `ChatGroupService` → `DispatchService` 存在跨模块调用需求
- 直接依赖会导致 `awm-service-chat` → `awm-service-task` 循环
- 解决：通过 `DispatchTaskEvent`（Spring ApplicationEvent）解耦
- `ChatGroupService` 发布事件，`DispatchService` 监听并处理

### 6.4 SSE 而非 WebSocket

- 聊天流式输出使用 Server-Sent Events
- 优势：单向推送（服务端→客户端）场景更简单，自动重连，HTTP 兼容
- `SseManager` 管理连接池，按 groupId 分组推送

### 6.5 MCP JSON-RPC 2.0

- 与 MCP 服务通信采用 JSON-RPC 2.0 协议
- 标准 MCP 端点为 `{endpoint}/mcp`
- 支持初始化握手 (`initialize`)、工具列表 (`tools/list`)、工具调用 (`tools/call`)

### 6.6 逻辑删除

- MyBatis-Plus 全局配置 `logic-delete-field: deleted`
- Agent 删除通过设置 `lifecycle_status = archived` 实现
- 部分表（如 message）为物理删除

## 7. 基础设施

### 7.1 Docker Compose

| 服务 | 镜像 | 端口 | 持久化 |
|------|------|------|--------|
| PostgreSQL 16 | `postgres:16` | 5432 | `awm-pgdata` volume |
| Redis 7 | `redis:7-alpine` | 6379 | 无持久化 |
| Qdrant | `qdrant/qdrant:latest` | 6333 | `awm-qdrant` volume |
| MinIO | `minio/minio:latest` | 9000/9001 | `awm-minio` volume |

### 7.2 Flyway 数据库迁移

| 版本 | 文件 | 说明 |
|------|------|------|
| V1 | `V1__init_schema.sql` | 12 张表 + 索引 |
| V2 | `V2__init_data.sql` | 初始部门数据（技术部/运维部/市场部/法务部） |
