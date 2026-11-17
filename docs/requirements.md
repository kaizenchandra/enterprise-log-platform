# Requirements and acceptance status

The requested enterprise specification remains the target. Design SLOs (1m events/s, p99 ingestion <100ms, p95 search <
2s, 99.9–99.99% availability) are **unverified**. No production-scale acceptance is established.

| Capability                          | Current evidence                                                                                                             |
|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| Java 21 backend                     | Built, nine tests passing                                                                                                    |
| Next.js frontend                    | Typecheck and production build passing                                                                                       |
| Metadata migrations                 | Three Flyway migrations executed on PostgreSQL 17                                                                            |
| HTTP → Kafka → OpenSearch → query   | Executed with synthetic secret; search and masking verified                                                                  |
| Collector → gateway → Kafka → index | Demo generator document observed in real index                                                                               |
| Stable IDs                          | Unit tests and duplicate HTTP delivery yielding one document                                                                 |
| Tenant isolation                    | Scoped query construction unit test; cross-tenant real-container search check passed; exhaustive penetration testing missing |
| Live tail                           | Real browser start/stop and SSE heartbeat verified; sustained slow-client test missing                                       |
| Alert/retention                     | Source implemented; hot retention/hold, alert deduplication and local outbox delivery runtime checks passed                  |
| Saved queries/admin                 | API and UI source implemented; real Playwright interaction tests passed                                                      |
| Archive/restore                     | Bounded S3 archive/integrity/restore/delete tested                                                                           |
| HA and benchmark SLOs               | Not established; small local measurements recorded                                                                           |

## Contract bounds

HTTP source API key in X-Ingestion-Key; 1 MiB body; 1–500 events; messages ≤16,384 characters; ≤32 scalar attributes;
≤1,024 characters per attribute value; timestamp ≤5 minutes in future and ≤30 days old. Producer eventId required.
Payload schema version is currently fixed at 1.

Fixed per-tenant quotas: 5,000 accepted candidate events and 5 MiB normalized bytes per wall-clock second, atomically
coordinated in PostgreSQL. Quota reservations precede Kafka and are not refunded on failure, so usage accounting is
conservative rather than billable. Database failure fails ingestion closed. No daily budget or configurable tenant plans
yet.

User endpoints require signed, time-valid JWT and tenant_id. Source/admin creation requires roles containing admin.
Search accepts only typed JSON: from/to, text, service, severity, traceId, cursor, limit. Seven-day max range, up to 500
rows, search timeout two seconds, transport timeout five seconds. Aggregates expose severity/services/minute timeline.
No raw query DSL, regex or administrative storage operations are accepted.

Search response is the OpenSearch search result including hits, total, timeout and shard status; frontend must treat
timeout/shard failures as partial. Cursor is last hit's sort array. API does not promise consistent snapshots across
pages. SSE polls search and may miss events. Searchable persistence is separate from Kafka acknowledgment.
