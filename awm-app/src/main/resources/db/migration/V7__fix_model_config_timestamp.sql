-- ============================================
-- V7: 补充修复 model_config 表的 TIMESTAMPTZ 列
-- V6 遗漏了 V4 新增的 model_config 表
-- PG JDBC 42.7.x 无法自动转换 TIMESTAMPTZ 为 LocalDateTime
-- ============================================

-- model_config 表 (V4 新增)
ALTER TABLE model_config ALTER COLUMN created_at TYPE TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE model_config ALTER COLUMN updated_at TYPE TIMESTAMP WITHOUT TIME ZONE;
