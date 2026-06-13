-- ============================================
-- AWM Add model_config_id to agent table
-- ============================================

ALTER TABLE agent ADD COLUMN model_config_id VARCHAR(36);
