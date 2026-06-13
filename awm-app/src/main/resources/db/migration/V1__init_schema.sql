-- ============================================
-- AWM Schema Init
-- ============================================

-- Agent table
CREATE TABLE agent (
    id              VARCHAR(36)     PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    avatar          VARCHAR(500),
    position        VARCHAR(100),
    department_id    VARCHAR(36),
    supervisor_id    VARCHAR(36),
    persona_prompt   TEXT,
    lifecycle_status VARCHAR(20)     NOT NULL DEFAULT 'DRAFT',
    runtime_status   VARCHAR(20)     NOT NULL DEFAULT 'OFFLINE',
    config          JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Department table
CREATE TABLE department (
    id              VARCHAR(36)     PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    parent_id       VARCHAR(36),
    sort_order      INTEGER         DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- MCP Server table
CREATE TABLE mcp_server (
    id              VARCHAR(36)     PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    endpoint        VARCHAR(500)    NOT NULL,
    description     TEXT,
    tools           JSONB,
    health_status   VARCHAR(20)     NOT NULL DEFAULT 'UNKNOWN',
    last_check_at   TIMESTAMPTZ,
    config          JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Agent-MCP Binding table
CREATE TABLE agent_mcp_binding (
    id              VARCHAR(36)     PRIMARY KEY,
    agent_id        VARCHAR(36)     NOT NULL,
    mcp_server_id   VARCHAR(36)     NOT NULL,
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    config          JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Chat Group table
CREATE TABLE chat_group (
    id              VARCHAR(36)     PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    manager_id      VARCHAR(36)     NOT NULL,
    created_by      VARCHAR(36),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Chat Group Member table
CREATE TABLE chat_group_member (
    id              VARCHAR(36)     PRIMARY KEY,
    group_id        VARCHAR(36)     NOT NULL,
    agent_id        VARCHAR(36)     NOT NULL,
    role            VARCHAR(20)     NOT NULL DEFAULT 'MEMBER',
    joined_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Message table
CREATE TABLE message (
    id              VARCHAR(36)     PRIMARY KEY,
    group_id        VARCHAR(36)     NOT NULL,
    sender_type     VARCHAR(20)     NOT NULL,
    sender_id       VARCHAR(36)     NOT NULL,
    sender_name     VARCHAR(100),
    content         TEXT            NOT NULL,
    message_type    VARCHAR(20)     NOT NULL DEFAULT 'text',
    metadata        JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Task table
CREATE TABLE task (
    id                  VARCHAR(36)     PRIMARY KEY,
    group_id            VARCHAR(36),
    title               VARCHAR(200)    NOT NULL,
    description         TEXT,
    assigned_agent_id   VARCHAR(36),
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    progress            INTEGER         DEFAULT 0,
    parent_task_id      VARCHAR(36),
    priority            INTEGER         DEFAULT 5,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Memory table
CREATE TABLE memory (
    id              VARCHAR(36)     PRIMARY KEY,
    agent_id        VARCHAR(36)     NOT NULL,
    type            VARCHAR(20)     NOT NULL,
    content         TEXT            NOT NULL,
    summary         TEXT,
    token_count     INTEGER,
    session_id      VARCHAR(36),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Knowledge Base table
CREATE TABLE knowledge_base (
    id                  VARCHAR(36)     PRIMARY KEY,
    name                VARCHAR(100)    NOT NULL,
    description         TEXT,
    vector_collection   VARCHAR(100),
    index_status        JSONB,
    last_indexed_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Agent-Knowledge Binding table
CREATE TABLE agent_knowledge_binding (
    id              VARCHAR(36)     PRIMARY KEY,
    agent_id        VARCHAR(36)     NOT NULL,
    kb_id           VARCHAR(36)     NOT NULL,
    alias           VARCHAR(100),
    sync_interval   INTEGER,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- MCP Call Log table
CREATE TABLE mcp_call_log (
    id              VARCHAR(36)     PRIMARY KEY,
    agent_id        VARCHAR(36)     NOT NULL,
    mcp_server_id   VARCHAR(36)     NOT NULL,
    tool_name       VARCHAR(100)    NOT NULL,
    request_params  JSONB,
    response_data   JSONB,
    status          VARCHAR(20)     NOT NULL,
    duration_ms     BIGINT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ============================================
-- Indexes
-- ============================================

-- Agent indexes
CREATE INDEX idx_agent_department ON agent(department_id);
CREATE INDEX idx_agent_supervisor ON agent(supervisor_id);
CREATE INDEX idx_agent_lifecycle_status ON agent(lifecycle_status);
CREATE INDEX idx_agent_runtime_status ON agent(runtime_status);

-- Agent-MCP Binding indexes
CREATE INDEX idx_agent_mcp_binding_agent ON agent_mcp_binding(agent_id);
CREATE INDEX idx_agent_mcp_binding_server ON agent_mcp_binding(mcp_server_id);

-- Chat Group Member indexes
CREATE INDEX idx_chat_group_member_group ON chat_group_member(group_id);
CREATE INDEX idx_chat_group_member_agent ON chat_group_member(agent_id);

-- Message indexes
CREATE INDEX idx_message_group ON message(group_id);
CREATE INDEX idx_message_created_at ON message(created_at);
CREATE INDEX idx_message_sender ON message(sender_id);

-- Task indexes
CREATE INDEX idx_task_group ON task(group_id);
CREATE INDEX idx_task_agent ON task(assigned_agent_id);
CREATE INDEX idx_task_status ON task(status);
CREATE INDEX idx_task_parent ON task(parent_task_id);

-- Memory indexes
CREATE INDEX idx_memory_agent ON memory(agent_id);
CREATE INDEX idx_memory_type ON memory(type);
CREATE INDEX idx_memory_session ON memory(session_id);

-- Agent-Knowledge Binding indexes
CREATE INDEX idx_agent_knowledge_binding_agent ON agent_knowledge_binding(agent_id);
CREATE INDEX idx_agent_knowledge_binding_kb ON agent_knowledge_binding(kb_id);

-- MCP Call Log indexes
CREATE INDEX idx_mcp_call_log_agent ON mcp_call_log(agent_id);
CREATE INDEX idx_mcp_call_log_server ON mcp_call_log(mcp_server_id);
CREATE INDEX idx_mcp_call_log_created_at ON mcp_call_log(created_at);

-- Department indexes
CREATE INDEX idx_department_parent ON department(parent_id);
