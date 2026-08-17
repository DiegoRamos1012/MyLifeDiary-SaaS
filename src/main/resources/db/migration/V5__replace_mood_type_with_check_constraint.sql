ALTER TABLE journal_entrys
    ALTER COLUMN mood TYPE VARCHAR(30)
    USING mood::text;

ALTER TABLE journal_entrys
    DROP CONSTRAINT IF EXISTS chk_journal_entrys_mood_valid;

ALTER TABLE journal_entrys
    ADD CONSTRAINT chk_journal_entrys_mood_valid
    CHECK (
        mood IN (
            'TERRIBLE',
            'SAD',
            'NEUTRAL',
            'HAPPY',
            'AMAZING',
            'BEST_DAY_OF_MY_LIFE'
        )
    );

DROP TYPE IF EXISTS mood_type;
