-- Add role column to users, defaulting to USER for backward compatibility
ALTER TABLE IF EXISTS users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'USER';


