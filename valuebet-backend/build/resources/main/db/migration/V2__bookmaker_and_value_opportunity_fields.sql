CREATE TABLE bookmaker (
    id BIGSERIAL PRIMARY KEY,
    external_key VARCHAR(100) NOT NULL UNIQUE
);

ALTER TABLE value_opportunity
    ADD COLUMN bookmaker_id BIGINT REFERENCES bookmaker(id),
    ADD COLUMN min_odds_ev_0 NUMERIC(8, 3),
    ADD COLUMN min_odds_ev_2 NUMERIC(8, 3),
    ADD COLUMN kelly_fraction NUMERIC(6, 4);

ALTER TABLE odds_snapshot
    ADD COLUMN closing_line BOOLEAN NOT NULL DEFAULT FALSE;
