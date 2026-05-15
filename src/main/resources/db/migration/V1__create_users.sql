CREATE TABLE IF NOT EXISTS users (
    id                 UUID NOT NULL PRIMARY KEY,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    last_time_changed  TIMESTAMP WITH TIME ZONE NOT NULL,
    full_name          VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    birth_date         DATE,
    user_status        VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    role               VARCHAR(20) NOT NULL DEFAULT 'USER',
    deletion_requested_at TIMESTAMP WITH TIME ZONE
);

