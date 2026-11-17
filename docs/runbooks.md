# Operations

Kafka publish timeout: 503 means uncertain acceptance. Retry unchanged IDs/timestamps; inspect broker ISR, disks and
producer errors. Never generate fresh IDs on retry.

Indexer backlog: compare consumer-group offsets and broker end offsets. Check OpenSearch disk watermarks, rejected bulk
items, template and JVM pressure. Restore capacity before retention evicts Kafka backlog. Permanent errors currently
block partition consumption; repair schema/data with a reviewed procedure. Never skip offsets without accounting for
loss.

Search failure: verify tenant token, range and storage health. Results can be partial on shard failures/timeouts; do not
treat partial counts as complete evidence. Alert evaluator suppresses transitions on partial queries.

Sensitive-data incident: revoke affected credentials, isolate access, stop source, fix source logging/redaction and
identify collector buffers, Kafka replicas, indices and backups. Hot deletion alone does not establish erasure. No
automated all-tier incident cleanup exists.

Retention: configure each tenant policy explicitly. Audit records only hot deletion passes. Verify delete failures and
backups separately. Do not promise legal hold or archival.

Collector saturation: monitor Collector internal queue/drop metrics, filesystem space and retry expiry. Queues are
bounded; exhausted retries can lose logs. Demo producers do not buffer/retry. Platform self-telemetry should use a
separate sink to avoid recursion.

Notification failures: inspect admin `/api/v1/alert-notifications`. Delivered=false with attempts growing means the
trusted receiver is failing or unreachable; repair its network/authentication/configuration. Retry uses the same
Idempotency-Key, so remote receivers must deduplicate. At eight failed attempts the row is retained as dead; automatic
replay is not implemented. Never edit outbox IDs to force repeated notifications.

Archive failures: hot records are never deleted by archive creation. If S3 succeeds and the manifest commit fails,
inventory reconciliation is needed for orphan cleanup. Restore/delete lock the manifest row and verify tenant ownership.
SHA-256 failure blocks restore. Do not disable verification to restore a corrupted object. The archive tier has no
automatic expiry yet; inventory policies and backups need explicit review before enterprise adoption.
