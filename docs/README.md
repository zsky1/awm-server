# AWM - Agent Workforce Manager

> 像管理人类员工一样管理 AI Agent 的智能工作力管理平台

## 项目简介

AWM（Agent Workforce Manager）是一个 AI Agent 编排与管理平台，核心思想是将 AI Agent 视为"数字员工"，为每个 Agent 配置独立的身份、技能、工具、规则、记忆和知识库，并支持多 Agent 通过群聊协作完成复杂任务。

### 核心价值

- **Agent 即员工**：每个 Agent 拥有独立的 persona、技能标签、MCP 工具集和知识库
- **总管调度**：群聊内指定"总管"Agent，自动拆解任务并分配给最合适的成员
- **工具即能力**：通过 MCP（Model Context Protocol）协议动态注册和调用外部工具
- **记忆即经验**：短期/长期记忆机制，让 Agent 拥有上下文和经验积累
- **知识即专业**：RAG 向量检索，让 Agent 基于专属知识库回答问题

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 3.2.5 |
| JDK | OpenJDK | 17 |
| ORM | MyBatis-Plus | 3.5.6 |
| 数据库 | PostgreSQL | 16 |
| 缓存 | Redis | 7 |
| 向量数据库 | Qdrant | latest |
| 对象存储 | MinIO | latest |
| 数据库迁移 | Flyway | 10.10.0 |
| HTTP 客户端 | OkHttp | 4.12.0 |
| JSON | Jackson | (Spring Boot 管理) |
| 工具库 | Hutool | 5.8.25 |
| 映射 | MapStruct | 1.5.5 |
| 桌面应用 | Electron 28 + Vue 3.4 + Naive UI | - |

## 快速开始

### 1. 启动基础设施

```bash
cd docker
docker compose up -d
```

启动后服务清单：
- PostgreSQL: `localhost:5432` (awm/awm123)
- Redis: `localhost:6379`
- Qdrant: `localhost:6333`
- MinIO: `localhost:9000` (API) / `localhost:9001` (控制台, awm/awm123456)

### 2. 启动后端

在 IDEA 中运行 `AwmApplication.main()`，或命令行：

```bash
mvn spring-boot:run -pl awm-app
```

Flyway 会自动执行数据库迁移脚本（`db/migration/V1__init_schema.sql`、`V2__init_data.sql`）。

### 3. 启动桌面应用

```bash
cd awm-desktop
npm install
npm run dev
```

## 项目结构

```
awm-server/                     # 后端 Maven 多模块项目
├── pom.xml                     # 父 POM（依赖管理）
├── awm-app/                    # 启动模块（Spring Boot 入口）
├── awm-common/                 # 通用工具（Result、异常、常量）
├── awm-model/                  # 数据模型（Entity、DTO、VO）
├── awm-dal/                    # 数据访问层（Mapper 接口）
├── awm-ai-engine/              # AI 引擎（LLM/MCP/Embedding 客户端）
├── awm-service/                # 业务服务层（6 个子模块）
│   ├── awm-service-agent/       # Agent & 部门管理
│   ├── awm-service-chat/       # 群聊 & 消息 & SSE
│   ├── awm-service-task/       # 任务 & 调度
│   ├── awm-service-mcp/        # MCP 工具管理 & 健康检查
│   ├── awm-service-memory/     # 记忆管理
│   └── awm-service-knowledge/  # 知识库 & RAG
├── awm-web/                    # Web 层（Controller & 配置）
├── docker/                     # Docker Compose 基础设施
└── docs/                       # 项目文档

awm-desktop/                    # Electron 桌面应用
├── src/main/                   # Electron 主进程
├── src/preload/                # 预加载脚本
└── src/renderer/              # Vue 3 渲染进程
    ├── api/                    # API 调用封装
    ├── components/             # UI 组件
    ├── composables/            # 组合式函数
    ├── stores/                 # Pinia 状态管理
    ├── views/                  # 页面视图
    └── router/                 # 路由配置
```

## 文档索引

| 文档 | 说明 |
|------|------|
| [架构设计文档](./Architecture-Design.md) | 系统架构、模块设计、数据库设计、API 设计 |
| [模块说明文档](./Module-Guide.md) | 各模块职责、核心类、调用关系详解 |
| [API 接口文档](./API-Reference.md) | 全部 REST API 接口说明 |

## 配置说明

核心配置在 `awm-app/src/main/resources/application.yml`：

```yaml
# 数据库
spring.datasource.url: jdbc:postgresql://localhost:5432/awm
spring.datasource.username: awm
spring.datasource.password: awm123

# AI LLM（默认 DeepSeek）
ai.llm.base-url: https://api.deepseek.com/v1
ai.llm.api-key: ${AI_API_KEY:sk-placeholder}
ai.llm.model: deepseek-chat

# 向量数据库
qdrant.host: localhost
qdrant.port: 6333

# 对象存储
minio.endpoint: http://localhost:9000
minio.access-key: awm
minio.secret-key: awm123456
```
