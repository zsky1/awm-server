-- V8: Update existing agents' runtime_status from 'offline' to 'idle'
-- New agents default to 'idle' (ready for work), not 'offline'
-- Existing agents that were 'offline' should be 'idle' if they are active

UPDATE agent
SET runtime_status = 'idle'
WHERE runtime_status = 'offline' AND lifecycle_status = 'active';
