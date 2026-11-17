CREATE TABLE archive_manifests
(
    id          uuid PRIMARY KEY,
    tenant      text        NOT NULL,
    object_key  text        NOT NULL UNIQUE,
    sha256      text        NOT NULL,
    event_count integer     NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now()
);
