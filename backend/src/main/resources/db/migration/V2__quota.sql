CREATE TABLE quota_windows
(
    tenant text        NOT NULL,
    bucket timestamptz NOT NULL,
    events bigint      NOT NULL,
    bytes  bigint      NOT NULL,
    PRIMARY KEY (tenant, bucket)
);
