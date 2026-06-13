-- ============================================
-- AWM Initial Data
-- ============================================

-- Insert initial departments
INSERT INTO department (id, name, parent_id, sort_order) VALUES
    ('dept-tech', '技术部', NULL, 1),
    ('dept-ops', '运维部', NULL, 2),
    ('dept-market', '市场部', NULL, 3),
    ('dept-legal', '法务部', NULL, 4);
