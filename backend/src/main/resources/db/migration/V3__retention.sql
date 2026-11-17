CREATE TABLE retention_policies
(
    tenant     text PRIMARY KEY,
    days       integer NOT NULL CHECK (days BETWEEN 1 AND 30),
    legal_hold boolean NOT NULL DEFAULT false
);
