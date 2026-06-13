# AWM 模块说明文档

> 详细说明各模块的职责、核心类、调用关系和关键实现

## 模块总览

| # | 模块 | artifactId | 核心职责 |
|---|------|-----------|----------|
| 1 | 通用层 | awm-common | 统一响应、异常处理、工具类 |
| 2 | 模型层 | awm-model | Entity、DTO、VO 定义 |
| 3 | 数据访问层 | awm-dal | MyBatis-Plus Mapper 接口 |
| 4 | AI 引擎层 | awm-ai-engine | LLM/MCP/Embedding 客户端 |
| 5 | 服务层 - Agent | awm-service-agent | Agent & 部门管理 |
| 6 | 服务层 - Chat | awm-service-chat | 群聊 & 消息 & SSE |
| 7 | 服务层 - Task | awm-service-task | 任务 & 调度 |
| 8 | 服务层 - MCP | awm-service-mcp | MCP 工具管理 & 健康检查 |
| 9 | 服务层 - Memory | awm-service-memory | 记忆管理 |
| 10 | 服务层 - Knowledge | awm-service-knowledge | 知识库 & RAG |
| 11 | 展示层 | awm-web | REST Controller & 配置 |
| 12 | 启动层 | awm-app | Spring Boot 入口 & Flyway |

---

## 1. awm-common — 通用基础

```
com.awm.common
├── result/
│   ├── Result.java          # 统一 API 响应封装
│   └── PageResult.java      # 分页响应封装
├── exception/
│   ├── BizException.java    # 业务异常
│   └── GlobalExceptionHandler.java  # 全局异常处理 (@RestControllerAdvice)
├── constant/
│   └── Constants.java       # 全局常量
└── util/
    ├── UuidUtils.java       # UUID 生成工具
    └── JsonUtils.java       # JSON 序列化工具
```

### 核心类说明

**Result<T>** — 所有 API 的统一返回格式：
```java
Result.success(data)     → {"code":200, "message":"success", "data":{...}}
Result.success()         → {"code":200, "message":"success", "data":null}
Result.error("msg")      → {"code":500, "message":"msg", "data":null}
```

**PageResult<T>** — 分页查询返回：
```java
PageResult.of(records, total, page, size)
// → { records: [...], total: 100, page: 1, size: 20 }
```

**BizException** — 业务异常，携带错误码和消息，由 `GlobalExceptionHandler` 统一捕获转换为 `Result.error()`。

---

## 2. awm-model — 数据模型

```
com.awm.model
├── entity/         # 12 个数据库实体
│   ├── Agent.java              # AI Agent（config 为 JSONB）
│   ├── Department.java         # 部门（树形 parent_id 自引用）
│   ├── McpServer.java          # MCP 服务注册
│   ├── AgentMcpBinding.java    # Agent-MCP 绑定
│   ├── AgentKnowledgeBinding.java # Agent-知识库绑定
│   ├── ChatGroup.java          # 群聊
│   ├── ChatGroupMember.java    # 群聊成员
│   ├── Message.java             # 消息
│   ├── Task.java                # 任务（支持父子关系）
│   ├── Memory.java              # 记忆
│   ├── KnowledgeBase.java       # 知识库
│   └── McpCallLog.java          # MCP 调用日志
├── dto/            # 8 个请求 DTO
│   ├── AgentCreateDTO.java      # 创建 Agent
│   ├── AgentUpdateDTO.java      # 更新 Agent
│   ├── AgentConfigDTO.java      # 更新 Agent 配置
│   ├── DeptCreateDTO.java       # 创建部门
│   ├── McpServerCreateDTO.java  # 注册 MCP 服务
│   ├── GroupCreateDTO.java      # 创建群
│   ├── MessageSendDTO.java      # 发送消息
│   └── TaskUpdateDTO.java       # 更新任务
└── vo/             # 8 个响应 VO
    ├── AgentVO.java             # Agent 列表项
    ├── AgentDetailVO.java       # Agent 详情
    ├── DeptTreeVO.java          # 部门树节点
    ├── McpServerVO.java         # MCP 服务信息
    ├── GroupVO.java             # 群信息
    ├── MessageVO.java           # 消息信息
    ├── TaskVO.java              # 任务信息
    └── DashboardVO.java         # 仪表盘统计数据
```

### 设计约定

- Entity 主键统一使用 `String id`（MyBatis-Plus `ASSIGN_UUID`）
- Entity 使用 `@Data` (Lombok)，VO/DTO 同样使用 `@Data`
- JSONB 字段在 Entity 中映射为 `String`，运行时用 Jackson 解析
- 时间字段统一 `LocalDateTime`，由数据库 `DEFAULT NOW()` 生成

---

## 3. awm-dal — 数据访问层

```
com.awm.dal
├── config/
│   └── MyBatisPlusConfig.java  # 分页插件 + 自动填充
└── mapper/         # 12 个 Mapper 接口
    ├── AgentMapper.java
    ├── DepartmentMapper.java
    ├── McpServerMapper.java
    ├── AgentMcpBindingMapper.java
    ├── AgentKnowledgeBindingMapper.java
    ├── ChatGroupMapper.java
    ├── ChatGroupMemberMapper.java
    ├── MessageMapper.java
    ├── TaskMapper.java
    ├── MemoryMapper.java
    ├── KnowledgeBaseMapper.java
    └── McpCallLogMapper.java
```

- 所有 Mapper 继承 `BaseMapper<T>`（MyBatis-Plus）
- 分页查询通过 `MybatisPlusInterceptor` 自动处理
- 无自定义 XML Mapper，全部使用 LambdaQueryWrapper

---

## 4. awm-ai-engine — AI 引擎

```
com.awm.ai
├── config/
│   └── AiEngineConfig.java      # @ConfigurationProperties(prefix="ai")
├── client/
│   ├── LlmClient.java            # LLM 调用接口
│   ├── OpenAiLlmClient.java      # OpenAI 兼容实现（OkHttp + SSE）
│   ├── McpClient.java            # MCP JSON-RPC 2.0 客户端
│   └── EmbeddingClient.java      # Embedding 向量化客户端
├── model/
│   ├── ChatRequest.java          # 对话请求（systemPrompt + messages + params）
│   ├── ChatResponse.java         # 对话响应（content + toolCalls + usage）
│   ├── ChatChunk.java            # 流式分块（content + toolCall + done）
│   ├── MessageItem.java          # 消息条目（role + content）
│   └── ToolDefinition.java       # 工具定义（name + description + parameters）
└── prompt/
    └── PromptBuilder.java        # System Prompt 构建器
```

### 核心类详解

#### OpenAiLlmClient

实现 `LlmClient` 接口，支持 OpenAI 兼容 API（DeepSeek/通义千问/OpenAI 等）。

| 方法 | 说明 |
|------|------|
| `chat(request)` | 同步对话，返回完整响应 |
| `chatStream(request)` | 流式对话，返回 `Flux<ChatChunk>` |
| `chatWithTools(request, tools)` | 带 Function Calling 的对话 |

**SSE 流式解析**：OkHttp 异步回调 → BufferedReader 逐行读取 → 解析 `data:` 前缀 → 转为 `ChatChunk` → Reactor Flux 推送。

#### McpClient

MCP（Model Context Protocol）客户端，基于 JSON-RPC 2.0 协议。

| 方法 | JSON-RPC Method | 说明 |
|------|----------------|------|
| `initialize(endpoint)` | `initialize` | 初始化握手 |
| `listTools(endpoint)` | `tools/list` | 获取工具列表 |
| `callTool(endpoint, name, args)` | `tools/call` | 调用工具 |

#### PromptBuilder

构建 Agent 的 System Prompt，按固定模板拼接：

```
你是 {name}，{position}。

## 人设
{persona_prompt}

## 你擅长以下技能
- {tag}: {description}

## 请严格遵守以下规则
- {content}

## 你之前的关键经验
{memory_summary}
```

以及总管调度的专用 Prompt：
```
你是群聊「{groupName}」的任务总管。

## 群成员及其技能
- {name} ({position}) [skill1, skill2]

## 用户任务
{userTask}

请将以上任务拆解为子任务，并分配给最合适的群成员。
```

---

## 5. awm-service-agent — Agent & 部门管理

```
com.awm.service.agent
├── AgentService.java       # Agent CRUD + 配置管理
├── DepartmentService.java  # 部门树 CRUD
└── ConfigLoader.java       # Agent 运行时配置加载器
```

### AgentService

| 方法 | 说明 |
|------|------|
| `listAgents(page, size, keyword, deptId, status)` | 分页查询，支持关键词/部门/状态筛选 |
| `getAgentById(id)` | 获取详情 |
| `createAgent(dto)` | 创建 Agent（初始 active + offline） |
| `updateAgent(id, dto)` | 更新基础信息 |
| `deleteAgent(id)` | 软删除（lifecycleStatus→archived） |
| `updateStatus(id, status)` | 更新运行时状态 |
| `getConfig(id)` | 获取 JSONB 配置 |
| `updateConfig(id, configDTO)` | 更新 JSONB 配置 |
| `searchBySkill(skillTag)` | JSONB 查询技能标签 |

### ConfigLoader ⭐

**核心类**，负责在 Agent 运行时加载完整配置，是"Agent 即员工"理念的关键实现。

```java
AgentRuntime loadAgentRuntime(agentId)
    → AgentRuntime(agent, config, systemPrompt, tools, knowledgeBaseIds)
```

加载步骤：
1. 查询 Agent 基础信息 + personaPrompt
2. 解析 config JSONB → skills、rules、memory 配置
3. 查询 agent_mcp_binding → 获取绑定的 MCP 服务 → McpClient.listTools()
4. 查询 agent_knowledge_binding → 获取知识库列表
5. PromptBuilder.buildSystemPrompt() → 拼接完整 System Prompt

---

## 6. awm-service-chat — 群聊 & 消息

```
com.awm.service.chat
├── ChatGroupService.java     # 群聊管理 + 消息处理
├── MessageService.java        # 消息 CRUD
├── SseManager.java            # SSE 连接管理器
└── DispatchTaskEvent.java     # 任务调度事件（Spring Event）
```

### ChatGroupService

| 方法 | 说明 |
|------|------|
| `listGroups()` | 查询群列表 |
| `createGroup(dto)` | 创建群 + 添加成员 + 指定总管 |
| `getGroupMembers(groupId)` | 获取群成员列表 |
| `getMessages(groupId, page, size)` | 分页查询消息历史 |
| `sendMessage(groupId, dto)` | 发送消息 + 触发 Agent 处理 |
| `processUserMessage(groupId, message)` | 检查 @总管 → 发布事件 |

### SseManager

管理 SSE 长连接，核心数据结构：

```java
Map<String, List<SseEmitter>> groupEmitters  // groupId → SSE 连接列表
```

| 方法 | 说明 |
|------|------|
| `addEmitter(groupId)` | 创建 SSE 连接（30分钟超时） |
| `removeEmitter(groupId, emitter)` | 移除连接 |
| `pushToGroup(groupId, data)` | 推送消息到群 |
| `pushEventToGroup(groupId, eventName, data)` | 推送命名事件 |

### DispatchTaskEvent

跨模块解耦的关键。`ChatGroupService` 发布事件，`DispatchService` 监听处理，避免循环依赖。

---

## 7. awm-service-task — 任务 & 调度

```
com.awm.service.task
├── TaskService.java        # 任务 CRUD
└── DispatchService.java    # 总管调度核心逻辑 ⭐
```

### DispatchService ⭐

**最核心的业务类**，实现"总管调度"机制：

| 方法 | 说明 |
|------|------|
| `onDispatchTaskEvent(event)` | 监听 DispatchTaskEvent |
| `dispatchTask(groupId, userMessage)` | 总管调度主流程 |
| `executeSubTask(taskId, agentId)` | Agent 执行子任务 |

**dispatchTask 流程**：
1. 获取群内总管 Agent
2. 收集群成员技能（解析 config JSONB）
3. `PromptBuilder.buildDispatcherPrompt()` 构建调度 Prompt
4. `LlmClient.chatWithTools(assign_task)` 让总管拆解任务
5. 解析 ToolCall → 创建父 Task + 子 Task
6. 异步调用 `executeSubTask()`

**executeSubTask 流程**：
1. `ConfigLoader.loadAgentRuntime()` 加载配置
2. 构建 ChatRequest（System Prompt + 任务描述）
3. `LlmClient.chatStream()` 流式执行
4. SSE 推送流式内容到群聊
5. 处理 ToolCall → `ToolCallProxy.executeTool()`
6. 更新 Task 状态（completed/failed）

---

## 8. awm-service-mcp — MCP 工具管理

```
com.awm.service.mcp
├── McpServerService.java    # MCP 服务 CRUD + 工具发现
├── ToolCallProxy.java       # 工具调用代理 ⭐
└── McpHealthChecker.java    # 健康检查（定时任务）
```

### ToolCallProxy

MCP 工具调用的安全代理，职责：

1. **授权检查**：验证 Agent 是否绑定了该 MCP Server
2. **调用执行**：`McpClient.callTool()` 发起 JSON-RPC 2.0 调用
3. **日志记录**：记录 `mcp_call_log`（含请求参数、响应、耗时、状态）

### McpHealthChecker

定时健康检查（`@Scheduled(fixedRate = 5min)`）：
- 对每个 MCP Server 执行 `McpClient.initialize()` 握手
- 更新 `mcp_server.health_status`（healthy/unhealthy）
- 状态变更时输出告警日志（可扩展为 WebSocket 推送）

---

## 9. awm-service-memory — 记忆管理

```
com.awm.service.memory
├── MemoryService.java    # 记忆 CRUD
└── MemoryManager.java    # 对话上下文构建 + 摘要触发
```

### MemoryManager

| 方法 | 说明 |
|------|------|
| `buildContext(agentId, sessionId)` | 构建对话上下文 |
| `summarizeIfNeeded(agentId)` | 检查是否需要摘要长期记忆 |

**记忆类型**：
- `short_term`：短期记忆，保留最近 N 轮对话（默认 10 轮，可配置 `config.memory.short_term.window_size`）
- `long_term`：长期记忆，当短期记忆超过 `window_size * 2` 时触发摘要

**上下文构建**：最近 N 条短期记忆 + 所有长期记忆摘要

---

## 10. awm-service-knowledge — 知识库 & RAG

```
com.awm.service.knowledge
├── KnowledgeBaseService.java  # 知识库 CRUD + 文档管理
└── RagService.java            # RAG 检索增强生成
```

### 知识库工作流

1. 创建知识库 → 生成 Qdrant collection
2. 上传文档 → MinIO 存储 + 文档分块 + Embedding 向量化 + 写入 Qdrant
3. RAG 查询 → Query Embedding → Qdrant 相似度检索 → 注入 Prompt 上下文

---

## 11. awm-web — 展示层

```
com.awm.web
├── controller/
│   ├── AgentController.java          # /api/agents
│   ├── DepartmentController.java    # /api/departments
│   ├── McpServerController.java     # /api/mcp/servers
│   ├── ChatGroupController.java     # /api/chat/groups
│   ├── TaskController.java          # /api/tasks
│   ├── MemoryController.java        # /api/agents/{id}/memories
│   ├── KnowledgeBaseController.java # /api/knowledge-bases
│   └── DashboardController.java     # /api/dashboard
└── config/
    ├── CorsConfig.java              # CORS 跨域配置
    ├── WebMvcConfig.java            # Web MVC 配置
    └── WebSocketConfig.java         # WebSocket 配置（预留）
```

---

## 12. awm-app — 启动层

```
com.awm.app
└── AwmApplication.java    # @SpringBootApplication + @EnableScheduling
```

包含：
- `application.yml` — 主配置
- `application-dev.yml` — 开发环境配置
- `db/migration/V1__init_schema.sql` — 建表
- `db/migration/V2__init_data.sql` — 初始数据

---

## 桌面应用 (awm-desktop)

```
awm-desktop/src
├── main/                # Electron 主进程
│   └── index.ts         # BrowserWindow 创建
├── preload/             # 预加载脚本
└── renderer/            # Vue 3 渲染进程
    ├── main.ts          # Vue 入口
    ├── App.vue          # 根组件
    ├── router/          # 路由（4 个视图）
    ├── stores/          # Pinia 状态
    │   ├── agent.ts     # Agent 状态
    │   ├── chat.ts      # 聊天状态
    │   ├── dashboard.ts # 仪表盘状态
    │   └── task.ts       # 任务状态
    ├── api/             # HTTP API 封装
    ├── composables/     # 组合式函数
    │   ├── useAgent.ts  # Agent 操作
    │   ├── useSSE.ts    # SSE 连接
    │   └── useWebSocket.ts # WebSocket（预留）
    ├── components/      # 16 个组件
    │   ├── contacts/    # 通讯录（DeptTree, AgentList, AgentDetail, AgentConfigEditor）
    │   ├── chat/        # 聊天（GroupList, ChatArea, MessageBubble, ChatInput, TaskCard）
    │   ├── dashboard/   # 仪表盘（StatsCards, AgentStatusTable, TaskProgress, McpHealthPanel）
    │   ├── common/      # 通用（StatusDot, SkillTag）
    │   └── layout/      # 布局（AppSidebar, AppHeader）
    └── views/           # 4 个页面
        ├── ContactsView.vue   # 通讯录（Agent 管理）
        ├── ChatView.vue       # 群聊协作
        ├── DashboardView.vue  # 仪表盘
        └── SettingsView.vue   # 设置
```
