ALTER TABLE event
    ADD COLUMN home_team VARCHAR(255),
    ADD COLUMN away_team VARCHAR(255);

CREATE TYPE bet_result AS ENUM ('PENDING', 'WON', 'LOST', 'VOID');

CREATE TABLE bet (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL REFERENCES event(id),
    market_type market_type NOT NULL,
    outcome outcome NOT NULL,
    stake NUMERIC(12, 2) NOT NULL,
    odds_taken NUMERIC(8, 3) NOT NULL,
    bookmaker_id BIGINT REFERENCES bookmaker(id),
    result bet_result NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bet_event_market_outcome ON bet (event_id, market_type, outcome);
