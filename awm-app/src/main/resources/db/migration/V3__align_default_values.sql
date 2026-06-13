-- ============================================
-- Align default values with code conventions (lowercase)
-- ============================================

-- Agent: lifecycle_status default 'DRAFT' -> 'draft'
ALTER TABLE agent ALTER COLUMN lifecycle_status SET DEFAULT 'draft';
ALTER TABLE agent ALTER COLUMN runtime_status SET DEFAULT 'offline';

-- MCP Server: health_status default 'UNKNOWN' -> 'unknown'
ALTER TABLE mcp_server ALTER COLUMN health_status SET DEFAULT 'unknown';

-- Chat Group Member: role default 'MEMBER' -> 'member'
ALTER TABLE chat_group_member ALTER COLUMN role SET DEFAULT 'member';

-- Message: message_type default 'text' is already lowercase, no change needed

-- Task: status default 'PENDING' -> 'pending'
ALTER TABLE task ALTER COLUMN status SET DEFAULT 'pending';

-- Update any existing rows with uppercase values to lowercase
UPDATE agent SET lifecycle_status = LOWER(lifecycle_status) WHERE lifecycle_status != LOWER(lifecycle_status);
UPDATE agent SET runtime_status = LOWER(runtime_status) WHERE runtime_status != LOWER(runtime_status);
UPDATE mcp_server SET health_status = LOWER(health_status) WHERE health_status != LOWER(health_status);
UPDATE chat_group_member SET role = LOWER(role) WHERE role != LOWER(role);
UPDATE task SET status = LOWER(status) WHERE status != LOWER(status);
