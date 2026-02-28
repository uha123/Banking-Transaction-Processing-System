-- Transaction Events table (Event Sourcing)
CREATE TABLE IF NOT EXISTS transaction_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID NOT NULL REFERENCES transactions(id),
    event_type      VARCHAR(50) NOT NULL,
    payload         TEXT NOT NULL,
    version         INTEGER NOT NULL DEFAULT 1,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);
