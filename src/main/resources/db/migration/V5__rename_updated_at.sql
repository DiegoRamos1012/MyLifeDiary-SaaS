-- Migration to rename legacy timestamp columns to updated_at across tables.

-- Rename column in users table
ALTER TABLE users RENAME COLUMN last_time_changed TO updated_at;

-- Rename column in habits table
ALTER TABLE habits RENAME COLUMN last_time_changed TO updated_at;

-- Rename column in habit_logs table
ALTER TABLE habit_logs RENAME COLUMN last_time_changed TO updated_at;


