CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE market_type AS ENUM ('ONE_X_TWO', 'OVER_UNDER', 'ASIAN_HANDICAP');
CREATE TYPE outcome AS ENUM ('ONE', 'DRAW', 'TWO');

CREATE TABLE event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id VARCHAR(128),
    name VARCHAR(255) NOT NULL,
    competition VARCHAR(255),
    start_time TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE odds_snapshot (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES event(id),
    market_type market_type NOT NULL,
    outcome outcome,
    line NUMERIC(8, 3),
    bookmaker VARCHAR(100) NOT NULL,
    odds NUMERIC(8, 3) NOT NULL,
    implied_probability NUMERIC(6, 4),
    captured_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE value_opportunity (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES event(id),
    market_type market_type NOT NULL,
    outcome outcome,
    line NUMERIC(8, 3),
    odds NUMERIC(8, 3) NOT NULL,
    true_probability NUMERIC(6, 4) NOT NULL,
    edge NUMERIC(6, 4) NOT NULL,
    source VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_odds_snapshot_event_market_outcome_capture
    ON odds_snapshot (event_id, market_type, outcome, captured_at);

CREATE INDEX idx_value_opportunity_event_market_outcome
    ON value_opportunity (event_id, market_type, outcome);
