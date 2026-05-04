CREATE SEQUENCE IF NOT EXISTS dashboard.streaming_train_sequence AS BIGINT START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS dashboard.streaming_event_sequence AS BIGINT START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS dashboard.streaming_outbox (
    id UUID PRIMARY KEY,
    topic TEXT NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);

CREATE INDEX IF NOT EXISTS idx_streaming_outbox_pending
    ON dashboard.streaming_outbox (next_attempt_at, created_at)
    WHERE published_at IS NULL;
