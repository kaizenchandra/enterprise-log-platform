# Bounded archive and restore

Admin `POST /api/v1/archives` accepts the bounded search contract, writes up to 500 redacted indexed events as gzip
NDJSON to the configured S3-compatible service, then commits a PostgreSQL manifest with tenant, object key, SHA-256 and
record count. The response includes the last search cursor for explicitly paginating further pages. Only two
archive/restore operations per JVM run concurrently. Archive pages are not a consistent snapshot without PIT; concurrent
indexing/deletion can affect page coverage.

Objects use tenant/date/UUID.ndjson.gz keys. Browsers receive manifest IDs, never S3 credentials or arbitrary object-key
access. `GET /api/v1/archives` lists current-tenant manifests. Admin `POST /api/v1/archives/{id}/restore` takes a row
lock, verifies tenant ownership and compressed SHA-256, bounds compressed size to 8 MiB, expanded size to 32 MiB and
records to 500, validates every tenant before publication and acknowledges only after Kafka publication. Restored
IDs/timestamps are unchanged. Restore is permitted only within current hot-retention days (default 30); older archives
require a separately designed isolated restore/search tier. It does not bypass hot expiry.

Admin `DELETE /api/v1/archives/{id}` locks the same manifest row, deletes the object and removes its manifest. Restore
and delete serialize across backend replicas via PostgreSQL. Object write succeeding before manifest commit can leave an
orphan if PostgreSQL fails; an inventory reconciler is still required. S3 versioning and backup erasure are not
implemented. Archive pages are an explicit operation, not automatic lossless archive-before-hot-expiry. There is no
automatic archive-retention schedule yet.

Local S3 uses authenticated SeaweedFS with persistent storage and no published S3 port. Pinned MinIO images were
unavailable at both public registries during execution. SeaweedFS configuration follows
its [S3 server configuration](https://github.com/seaweedfs/seaweedfs/blob/master/weed/command/s3.go). The application
uses
the [AWS SDK synchronous S3 client](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3.html).
Production must use managed credentials, encrypted buckets, TLS, least-privilege bucket policies, supported software
lifecycle and backup inventory; local development storage does not establish those controls.
