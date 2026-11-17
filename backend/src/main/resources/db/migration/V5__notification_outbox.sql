CREATE TABLE alert_notifications
(
    id           uuid PRIMARY KEY,
    tenant       text        NOT NULL,
    rule_id      uuid        NOT NULL REFERENCES alert_rules (id),
    payload      jsonb       NOT NULL,
    attempts     integer     NOT NULL DEFAULT 0,
    next_attempt timestamptz NOT NULL DEFAULT now(),
    delivered    boolean     NOT NULL DEFAULT false,
    dead         boolean     NOT NULL DEFAULT false
);
CREATE INDEX notification_due ON alert_notifications (next_attempt) WHERE delivered=false AND dead=false;
