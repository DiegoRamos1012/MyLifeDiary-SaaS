-- Adds password protection support to journals and date granularity for daily notes.

ALTER TABLE IF EXISTS journals
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

ALTER TABLE IF EXISTS journal_entrys
    ADD COLUMN IF NOT EXISTS date DATE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_journal_entrys_journal_date
    ON journal_entrys (journal_id, date);

