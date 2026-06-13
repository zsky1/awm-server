-- ============================================
-- AWM Add Model Config Table
-- ============================================

-- Model Config table
CREATE TABLE model_config (
    id              VARCHAR(36)     PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    provider        VARCHAR(50)     NOT NULL,
    model           VARCHAR(100)    NOT NULL,
    api_key         VARCHAR(500),
    base_url        VARCHAR(500),
    temperature     DOUBLE PRECISION DEFAULT 0.7,
    max_tokens      INTEGER         DEFAULT 4096,
    is_default      BOOLEAN         NOT NULL DEFAULT FALSE,
    description     TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_model_config_provider ON model_config(provider);
CREATE INDEX idx_model_config_is_default ON model_config(is_default);
