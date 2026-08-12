CREATE TABLE charging_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR NOT NULL,
    charger_id VARCHAR NOT NULL,
    status VARCHAR NOT NULL,
    battery_level INTEGER,
    charged_kwh NUMERIC(12,3),
    last_sequence BIGINT NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_charging_session_session_id UNIQUE (session_id)
);

CREATE TABLE charging_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR NOT NULL,
    session_id VARCHAR NOT NULL,
    charger_id VARCHAR NOT NULL,
    event_type VARCHAR NOT NULL,
    sequence BIGINT NOT NULL,
    battery_level INTEGER,
    charged_kwh NUMERIC(12,3),
    occurred_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_charging_event_event_id UNIQUE (event_id)
);

CREATE INDEX idx_charging_event_session_sequence
    ON charging_event (session_id, sequence);
