CREATE TABLE IF NOT EXISTS journals (
    id            UUID NOT NULL PRIMARY KEY,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title         VARCHAR(80) NOT NULL,
    is_locked     BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_journals_user_id ON journals(user_id);

CREATE TABLE IF NOT EXISTS journal_entrys (
    id          UUID NOT NULL PRIMARY KEY,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    journal_id  UUID NOT NULL REFERENCES journals(id) ON DELETE CASCADE,
    content     TEXT NOT NULL,
    date        DATE NOT NULL,
    mood        VARCHAR(30) NOT NULL,
    CONSTRAINT uq_journal_entrys_journal_date UNIQUE (journal_id, date)
);

CREATE INDEX IF NOT EXISTS idx_journal_entrys_journal_id ON journal_entrys(journal_id);
CREATE INDEX IF NOT EXISTS idx_journal_entrys_date ON journal_entrys(date);

