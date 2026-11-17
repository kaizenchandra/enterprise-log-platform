# Executed verification

The results below were executed locally on 2026-11-17 against the real Compose stack. They do not establish production
readiness.

| Verification                           | Result                                                                                                                                      |
|----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Final Java 21 Maven verify             | Passed: 9 tests, zero failures/errors                                                                                                       |
| Next.js TypeScript check               | Passed                                                                                                                                      |
| Final Docker Next.js production build  | Passed                                                                                                                                      |
| PostgreSQL migrations V1–V5            | Executed on PostgreSQL 17                                                                                                                   |
| Real-container security/recovery suite | 18 checks passed; see system-test.json                                                                                                      |
| HTTP → Kafka → OpenSearch              | Acknowledged, searchable, redacted                                                                                                          |
| Duplicate delivery                     | One indexed document for immutable identity/timestamp                                                                                       |
| Broker recreation                      | Previously unindexed accepted records survived on persistent volume                                                                         |
| OpenSearch downtime + worker SIGKILL   | Previously accepted records became searchable after restoration                                                                             |
| Kafka downtime                         | Returned 503; unchanged-ID retry accepted after readiness                                                                                   |
| Tenant/RBAC/input security             | Unauthorized, forged-token, source-forgery, viewer-admin, cross-tenant, raw DSL, oversized-body and time-budget checks passed               |
| S3 archive/restore                     | SHA-256 verified; synthetic hot document deleted, restored through Kafka, searchable again; archive deletion verified                       |
| Archive isolation                      | Other tenant restore denied; viewer archive denied                                                                                          |
| Hot retention and hold                 | Expired synthetic event preserved under hot hold, deleted after hold release; 30-day policy restored                                        |
| Alert/outbox                           | One FIRING transition across two evaluations; one durable notification delivered to local synthetic receiver                                |
| Playwright                             | 2 real-browser tests passed: explorer/session/XSS/details/saved queries/analytics/sources/live-tail and unauthorized/foreign-origin denials |

## Measured small local workloads

- Ingestion: 2,000 events, 20 batches × 100 events, four closed-loop clients. All accepted and reconciled as searchable.
  Kafka-acknowledged rate 7,699.02 events/sec across 0.260 seconds; p50 46.97 ms, p95 65.45 ms, maximum 73.67 ms per
  batch. All searchable after 1.333 seconds from workload start.
- Search: 30 serial tenant/time/full-text marker queries against a warm local index, 2,000 matching events with a
  100-row response limit. p50 12.27 ms, p95 21.80 ms, maximum 40.19 ms.

These short experiments are not a sustained throughput benchmark, p99 evidence, CPU/GC study, production-scale capacity
claim, or SLO validation. Messages are short synthetic records, not the requested 500B–2KB full workload distribution.
See raw JSON under results.

## Defects found and corrected

- Kafka volume ownership prevented broker initialization: one-shot ownership initializer added; unindexed-record
  recreation test then passed.
- Spring ERROR dispatch changed explicit ingestion 403/413 into 401: error dispatch permitted while ordinary resources
  remain protected.
- Fixed five-second broker restart assumption failed: test now retries within a bounded period using unchanged IDs.
- Frontend loopback port was unreachable locally: moved to 3100 and verified reachability.
- BFF origin validation used the internal container origin: now compares against configured PUBLIC_ORIGIN; HTTPS cookie
  behavior follows that origin. UI no longer reports a rejected session request as success.
- Existing Chromium cache was incomplete: installed the matching Playwright browser in /tmp and reran successfully.
- Public MinIO image registries rejected the pinned image: replaced local S3 fixture with authenticated SeaweedFS.
- Retention now treats version conflicts as incomplete, rather than auditing deletion as successful.

## Remaining evidence gaps

No Testcontainers suite, multi-node/AZ durability, sustained overload/slow-client memory test, region failure,
archive-corruption injection, exhaustive archive/deletion race test, all-tier legal hold/erasure, full OTLP semantic
conformance, accessibility audit or 1m-event/sec test has been executed. CI workflow is provided but has not run on
GitHub in this session. Kubernetes manifests are not deployed or production validated.

Notification receiver failure injection also passed: one outbox row remained undelivered while the local receiver was
stopped, then delivered after restart on attempt two, preserving its notification ID. Evidence:
results/notification-failure.json. The final-revision system suite was subsequently rerun successfully, including a real
OTLP Collector-to-search trace-correlation check and the measurements above.

A paced workload was added after the small burst: 30,000 offered events at 1,000/sec for 30 seconds, 500-character
messages, 50 events/batch, with concurrent queries. The first run accepted all 600 batches and reconciled all events,
but acknowledgment p99 was 135.46 ms and exceeded the 100 ms target. See results/paced-before-tuning.json. The repeat
follows precompiled redaction patterns and explicit bounded Kafka batching/compression; results/paced-benchmark.json
includes container CPU/memory snapshots. This adds one short sustained sample, not production capacity or an
availability/SLO guarantee.

## Final paced result and tuning regression

The same 30,000-event, 1,000/sec offered, 500-character-message workload was repeated. All 600 requests returned 202,
all 30,000 events reconciled in search, acknowledgment p50/p95/p99/max were 9.22/15.42/23.30/33.53 ms, and all 30
concurrent queries completed with p95 54.80 ms. Six coarse resource snapshots included backend peak CPU 74.19% (100%
represents one core) and a final backend memory sample of 454.9 MiB. These are snapshots, not a CPU/GC profile.

An intermediate precompiled-pattern + Kafka-batching/compression experiment regressed to p99 455.00 ms and backend peak
CPU 291.62%; it is preserved as results/paced-batching-regression.json. Kafka tuning was reverted. Inspection found the
email expression's excessive scanning on long ordinary text; it now has bounded local/domain matching and skips email
matching when no @ exists. A 16,000-character adversarial input regression is among the nine passing backend tests. The
before/after runs differ in ambient load, index size and JVM state, so do not interpret them as a controlled causal
performance study.

The final paced run's 36.91-second observation window includes trailing probe/resource-sampling time. It did not capture
a separate acknowledgment-only elapsed interval; the script has been corrected for future runs. Report acknowledgment
percentiles and reconciled counts directly, not the observation-window rate as sustainable capacity. The 1m/sec scale
scenario and long-term SLOs remain unverified.
