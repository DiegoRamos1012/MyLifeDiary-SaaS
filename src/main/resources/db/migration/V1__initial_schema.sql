-- Initial Database Schema for MyLifeDiary
-- Consolidates all tables and indexes in a single migration

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id                 UUID NOT NULL PRIMARY KEY,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    full_name          VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    birth_date         DATE,
    user_status        VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    role               VARCHAR(20) NOT NULL DEFAULT 'USER',
    deletion_requested_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_user_status ON users(user_status);

-- Refresh tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         UUID NOT NULL PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked    BOOLEAN NOT NULL DEFAULT FALSE
);

-- Habits table
CREATE TABLE IF NOT EXISTS habits (
    id                UUID NOT NULL PRIMARY KEY,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title             VARCHAR(80) NOT NULL,
    description       TEXT,
    habit_category    VARCHAR(40) NOT NULL,
    goal_daily        INTEGER,
    start_date        DATE NOT NULL,
    CONSTRAINT chk_habits_goal_daily_positive CHECK (goal_daily IS NULL OR goal_daily > 0)
);
CREATE INDEX IF NOT EXISTS idx_habits_user_id ON habits(user_id);
CREATE INDEX IF NOT EXISTS idx_habits_user_start_date ON habits(user_id, start_date);

-- Habit logs table
CREATE TABLE IF NOT EXISTS habit_logs (
    id                UUID NOT NULL PRIMARY KEY,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    habit_id          UUID NOT NULL REFERENCES habits(id) ON DELETE CASCADE,
    date              DATE NOT NULL,
    completed         BOOLEAN NOT NULL,
    note              VARCHAR(500),
    CONSTRAINT uq_habit_logs_habit_date UNIQUE (habit_id, date)
);
CREATE INDEX IF NOT EXISTS idx_habit_logs_habit_id ON habit_logs(habit_id);
CREATE INDEX IF NOT EXISTS idx_habit_logs_date ON habit_logs(date);

-- Journals table
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

-- Journal entries table
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

-- Addictions table
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

-- Addiction logs table
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

