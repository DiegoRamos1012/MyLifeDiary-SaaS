CREATE TABLE IF NOT EXISTS addictions (
    id                  UUID NOT NULL PRIMARY KEY,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title               VARCHAR(80) NOT NULL,
    description         TEXT,
    addiction_category  VARCHAR(40) NOT NULL,
    start_date          DATE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_addictions_user_id ON addictions(user_id);

CREATE TABLE IF NOT EXISTS addiction_logs (
    id                  UUID NOT NULL PRIMARY KEY,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    addiction_id        UUID NOT NULL REFERENCES addictions(id) ON DELETE CASCADE,
    date                DATE NOT NULL,
    relapsed            BOOLEAN NOT NULL,
    note                VARCHAR(500),
    CONSTRAINT uq_addiction_logs_addiction_date UNIQUE (addiction_id, date)
);

CREATE INDEX IF NOT EXISTS idx_addiction_logs_addiction_id ON addiction_logs(addiction_id);
CREATE INDEX IF NOT EXISTS idx_addiction_logs_date ON addiction_logs(date);

