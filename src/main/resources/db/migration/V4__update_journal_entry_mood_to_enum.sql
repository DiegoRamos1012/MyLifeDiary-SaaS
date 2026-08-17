DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'mood_type') THEN
        CREATE TYPE mood_type AS ENUM (
            'TERRIBLE',
            'SAD',
            'NEUTRAL',
            'HAPPY',
            'AMAZING',
            'BEST_DAY_OF_MY_LIFE'
        );
    END IF;
END $$;

ALTER TABLE journal_entrys
    ALTER COLUMN mood TYPE mood_type
    USING mood::mood_type;
