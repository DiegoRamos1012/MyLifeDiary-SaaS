-- Migration to rename last_time_changed to updated_at across all tables
-- This provides a more consistent naming convention with DTOs

-- Rename column in users table
ALTER TABLE users RENAME COLUMN last_time_changed TO updated_at;

-- Rename column in habits table
ALTER TABLE habits RENAME COLUMN last_time_changed TO updated_at;

-- Rename column in habit_logs table
ALTER TABLE habit_logs RENAME COLUMN last_time_changed TO updated_at;

