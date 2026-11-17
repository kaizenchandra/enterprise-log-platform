# Log Atlas — distributed log platform baseline

Java 21 / Spring Boot 3.5.16, Next.js 16.4.6 / React 19.3.1, Kafka, PostgreSQL, OpenSearch and OpenTelemetry Collector.
This is an incremental implementation, **not completion of the enterprise acceptance criteria**.

Implemented: authenticated HTTP batches and Collector OTLP/JSON bridge, authoritative source ownership, validation and
pre-Kafka redaction, bounded publication, PostgreSQL coordinated per-second quotas, Kafka-acknowledged 202, stable-ID
indexing, scoped search and time/severity/service/trace filters, search-after, analytics, saved queries, polling SSE,
alert transitions, hot retention jobs, source registration, Next.js console, local infrastructure, Java demo generator,
Kubernetes application baseline and k6 workload.

## Run

Requires Docker Compose, approximately 6 GB free Docker memory and free ports 3100, 8080, 8180, 4317, 4318.

```bash
cd enterprise-log-platform
docker compose up -d --build
./scripts/bootstrap.sh
# Put returned source key into .env as INGESTION_KEY=...
docker compose up -d --force-recreate collector
# Start/recreate demo producers after source registration:
docker compose up -d order-service payment-service inventory-service
# Open http://127.0.0.1:3100 and establish a session with the printed OIDC token.
export INGESTION_KEY='registered-source-key'
export TOKEN='OIDC-access-token'
./scripts/smoke.sh
```

Bootstrap once per fresh database; source registration conflicts on repeated IDs. Obtain a new development token from
Keycloak when its short lifetime expires. All demo producers currently share the registered payment source, so
authoritative service_name is payment-service; demo.service retains producer identity. Register independent
sources/collector instances for separate authoritative service identities.

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml verify
npm --prefix frontend ci
npm --prefix frontend run typecheck
npm --prefix frontend run build
INGESTION_KEY=... RATE=100 k6 run benchmarks/ingest.js
docker compose down # retains volumes
```

## Guarantees and limits

202 means every reported accepted event received a Kafka producer acknowledgment; it does not mean indexed or
searchable. With timeout, some events may already be durable: retry immutable IDs/timestamps. Partial validation reports
rejected positions. OTLP reports partialSuccess for invalid records. Replication factor is **one locally**, so disk/node
loss is outside this durability boundary. Production requires replicated brokers and ISR enforcement.

Index creation uses stable tenant/source/event IDs. Duplicate conflicts retain first content in the same daily index.
Changing timestamp across dates defeats deduplication. Events without producer event.id on the OTLP bridge get generated
IDs and can duplicate. Indexing is asynchronous, Kafka offsets commit after index acknowledgment/conflict, and
OpenSearch refresh adds visibility delay. Live tail is bounded best effort over searchable records.

See [architecture](docs/architecture.md), [security](docs/security.md), [capacity](docs/capacity-planning.md), [runbooks](docs/runbooks.md),
and [verification](docs/verification.md).

## Incomplete acceptance criteria

- Full OTLP resource/scope/observed-timestamp/severity-number/body-type preservation, protobuf HTTP adapter, source
  health and file/container collection configurations.
- Quarantine replay controls, PIT pagination, arbitrary structured-field filtering and safe export. Batch indexing,
  selective retry, durable quarantine, facets and tenant-scoped get-by-ID are implemented.
- Automatic archive-before-expiry, archive retention scheduling, all-tier erasure, backup inventory, production legal
  hold and index rollover/ISM. Bounded S3 archive/restore/delete is implemented.
- Organization/project/user management, ingestion key rotation, tenant-specific quota plans/daily budgets, audience
  validation, production OIDC browser login, TLS/mTLS and complete audit trails.
- Alert acknowledgment/silencing, notification replay controls, complete operational dashboards and trace-viewer
  navigation. A trusted-webhook outbox, timeline meters and saved-filter loading are implemented.
- Prometheus scraping/Grafana dashboards, custom pipeline loss/lag metrics, Collector DaemonSet, production
  Helm/stateful topology/network policies.
- Testcontainers, sustained concurrency/accessibility suites, all 45 scenario analyses and all 50 architecture-review
  responses. Real-container security/recovery tests and Playwright browser tests are supplied; see evidence for
  execution status.
- Production-scale performance, multi-node durability and complete retention/archive race evidence. Small local
  measurements and selected crash recovery checks are recorded.

Measured local workloads are recorded in docs/results. No production-scale throughput, SLO achievement, regulatory
compliance or production readiness is claimed.

Additional workflows: [testing](docs/testing.md), [archives](docs/archives.md), [alerts](docs/alerts.md).

The console now listens at **http://127.0.0.1:3100**; PUBLIC_ORIGIN must exactly match the browser origin. The backend
checks JWT audience `log-platform`. Notification tests use only the local payment demo receiver. See
docs/results/system-test.json, archive-test.json, policy-test.json, notification-failure.json, query-benchmark.json and
browser.json for executed evidence.
