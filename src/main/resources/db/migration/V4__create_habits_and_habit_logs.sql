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
