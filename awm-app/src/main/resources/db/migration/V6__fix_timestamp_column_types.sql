-- ============================================
-- V6: 修复 TIMESTAMPTZ -> TIMESTAMP
-- PG JDBC 42.7.x 无法自动转换 TIMESTAMPTZ 为 LocalDateTime
-- 统一使用 TIMESTAMP (without timezone) 以兼容 java.time.LocalDateTime
-- ============================================

-- agent 表
ALTER TABLE agent ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE agent ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE;

-- department 表
ALTER TABLE department ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE department ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE;

-- mcp_server 表
ALTER TABLE mcp_server ALTER COLUMN last_check_at TYPE TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE mcp_server ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE mcp_server ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE;

-- agent_mcp_binding 表
ALTER TABLE agent_mcp_binding ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;

-- chat_group 表
ALTER TABLE chat_group ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE chat_group ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE;

-- chat_group_member 表
ALTER TABLE chat_group_member ALTER COLUMN joined_at TYPE TIMESTAMP WITHOUT TIME ZONE;

-- message 表
ALTER TABLE message ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;

-- task 表
ALTER TABLE task ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE task ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE;

-- memory 表
ALTER TABLE memory ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;

-- knowledge_base 表
ALTER TABLE knowledge_base ALTER COLUMN last_indexed_at TYPE TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE knowledge_base ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE knowledge_base ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE;

-- agent_knowledge_binding 表
ALTER TABLE agent_knowledge_binding ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;

-- mcp_call_log 表
ALTER TABLE mcp_call_log ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;
