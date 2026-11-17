CREATE TABLE sources
(
    id              text PRIMARY KEY,
    tenant          text        NOT NULL,
    service         text        NOT NULL,
    environment     text        NOT NULL,
    credential_hash text UNIQUE NOT NULL
);
CREATE TABLE saved_queries
(
    id         uuid PRIMARY KEY,
    tenant     text  NOT NULL,
    name       text  NOT NULL,
    definition jsonb NOT NULL
);
CREATE TABLE alert_rules
(
    id             uuid PRIMARY KEY,
    tenant         text    NOT NULL,
    service        text    NOT NULL,
    threshold      bigint  NOT NULL CHECK (threshold > 0),
    enabled        boolean NOT NULL DEFAULT true,
    firing         boolean NOT NULL DEFAULT false,
    last_evaluated timestamptz
);
CREATE TABLE alert_history
(
    id          bigserial PRIMARY KEY,
    rule_id     uuid REFERENCES alert_rules (id),
    tenant      text        NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    state       text        NOT NULL,
    event_count bigint      NOT NULL
);
CREATE TABLE audit_events
(
    id          bigserial PRIMARY KEY,
    tenant      text        NOT NULL,
    action      text        NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now()
);
