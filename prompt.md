````
# MASTER PROMPT: Design and Develop a Production-Grade Distributed Log Aggregation, Search, and Analytics Platform Using Java 21, Spring Boot, and Next.js

## 1. ROLE AND MISSION

Act as a **Principal Software Engineer, Distinguished Distributed Systems Architect, Enterprise Solution Architect, Java 21 and Spring Boot Expert, Next.js Architect, Observability Platform Engineer, Search Infrastructure Architect, Security Engineer, Cloud Architect, and Site Reliability Engineer** with 15+ years of experience building large-scale enterprise logging and observability systems.

Your task is to **design, develop, test, benchmark, secure, document, and deploy an end-to-end Distributed Log Aggregation Platform** using:

- Java 21
- Spring Boot
- Next.js with TypeScript
- Apache Kafka
- OpenSearch
- PostgreSQL
- Redis where justified
- OpenTelemetry Collector
- Fluent Bit or another supported collection agent
- S3-compatible object storage
- Docker and Kubernetes
- Micrometer and OpenTelemetry
- Prometheus and Grafana where justified

Treat this as a real enterprise engineering project, not merely an interview design exercise or a static dashboard demonstration.

You must deliver:

1. Functional and non-functional requirements
2. High-Level Design (HLD)
3. Low-Level Design (LLD)
4. Distributed log collection
5. HTTP/gRPC/OTLP log ingestion
6. Agent-based log shipping
7. Kafka-based durable buffering
8. Backpressure and overload control
9. Log parsing and normalization
10. Structured and unstructured log support
11. Log enrichment
12. Sensitive-data redaction
13. Log indexing and storage
14. Full-text and structured search
15. Time-range filtering
16. Distributed query execution
17. Live log streaming
18. Log aggregation and analytics
19. Log-based alerting
20. Index lifecycle management
21. Retention and archival
22. Multi-tenancy and RBAC
23. Usage quotas
24. Ingestion pipeline administration
25. Saved queries and dashboards
26. Log-to-trace correlation
27. Self-observability
28. Security and compliance controls
29. Automated tests
30. Performance and capacity benchmarks
31. Docker and Kubernetes deployment
32. Functional Next.js user and admin interfaces
33. Complete source code and documentation

**Do not stop after HLD, LLD, diagrams, pseudocode, configuration samples, or static UI mockups. Deliver a working distributed logging platform with Java 21 backend services, real ingestion/indexing pipelines, functional Next.js dashboards, search capabilities, alerting, automated verification, and deployment infrastructure.**

### Mandatory engineering principles

- Use Java 21 and verified compatible Spring Boot dependencies.
- Use a verified compatible Next.js release with TypeScript and App Router.
- Follow existing repository instructions, conventions, architecture, and dependency versions.
- Apply DDD, SOLID, Clean Architecture, Hexagonal Architecture, and separation of concerns pragmatically.
- Prefer supported collection protocols and mature search engines instead of implementing distributed indexing from scratch.
- Keep backend ingestion, parsing, indexing, and query operations independently scalable where justified.
- Separate the ingestion data plane from management and query-facing capabilities.
- Define log acceptance, durable persistence, indexing, and search visibility as distinct milestones.
- Use bounded queues, concurrency, retries, timeouts, buffers, and connection pools.
- Define at-least-once delivery, duplicate handling, and partial-failure behavior explicitly.
- Do not claim exactly-once end-to-end ingestion without a demonstrated protocol.
- Treat all incoming logs as untrusted data.
- Do not execute instructions, scripts, commands, or code embedded inside log messages.
- Implement sensitive-data filtering and access controls before indexing.
- Avoid unrestricted high-cardinality metrics and unbounded search queries.
- Keep tenant isolation enforceable on the backend, not just in the frontend.
- Never claim lossless ingestion when the system can drop or evict data.
- Distinguish tests actually executed from tests merely proposed.
- Do not fabricate throughput, latency, benchmark results, or production readiness.
- Explain significant architecture choices, trade-offs, and operational risks.

---

# PART A — PRIMARY OBJECTIVE

## 2. BUILD AN ENTERPRISE LOG AGGREGATION PLATFORM

Build a centralized logging platform for distributed Java, Go, Node.js, and containerized applications.

It must support:

- Application logs
- Container logs
- Kubernetes logs
- Infrastructure logs
- System logs
- JSON logs
- Plain-text logs
- Multiline stack traces
- HTTP/gRPC ingestion
- OpenTelemetry logs
- Live log viewing
- Historical search
- Time-range selection
- Severity filtering
- Service filtering
- Full-text queries
- Structured field filtering
- Log analytics
- Alerting
- Export and archival
- Access control
- Tenant-specific retention

### Example end-to-end scenario

An enterprise runs 200 microservices on Kubernetes.

One service experiences intermittent payment failures.

The application produces:

```json
{
  "timestamp": "2026-11-17T10:00:00Z",
  "severity": "ERROR",
  "service": "payment-service",
  "environment": "production",
  "trace_id": "0123456789abcdef0123456789abcdef",
  "span_id": "0123456789abcdef",
  "event": "payment.authorization.timeout",
  "message": "Payment provider exceeded request deadline",
  "duration_ms": 1500
}
```

The platform must:

1. Collect the log through an authorized collector.
2. Add trusted infrastructure metadata.
3. Validate its schema and size.
4. Normalize timestamps and severity.
5. Identify the correct tenant and environment.
6. Remove or mask sensitive fields.
7. Assign a stable log identity.
8. Buffer the record safely.
9. Publish or forward it through the durable ingestion pipeline.
10. Index the record.
11. Make it searchable in the correct tenant scope.
12. Display the record in the Next.js log explorer.
13. Allow filtering by service, severity, and time.
14. Show related logs using trace ID.
15. Link to an external trace viewer where configured.
16. Generate a log-based alert when an approved threshold is exceeded.
17. Enforce retention and archival policies.
18. Recover safely from Kafka, OpenSearch, collector, or worker failures.

### Fundamental correctness invariants

- A tenant can never search or retrieve another tenant's logs without explicit authorized cross-tenant access.
- Log acceptance acknowledgments correspond to a clearly defined durability milestone.
- Duplicate deliveries do not silently inflate exact log-event counts under the documented deduplication model.
- Events retain distinguishable event timestamps and ingestion timestamps.
- Sensitive-data protections are applied before searchable persistence, with clearly documented handling of raw archives and buffers.
- Retention and deletion policies apply to all relevant storage tiers.
- Invalid or malicious log records cannot execute code or compromise the platform.
- Backend search queries enforce authorization independently of frontend filters.

---

# PART B — FUNCTIONAL REQUIREMENTS

## 3. LOG SOURCE MANAGEMENT

Support sources such as:

- Spring Boot applications
- Go services
- Node.js services
- Docker containers
- Kubernetes pods
- Virtual machines
- Syslog-compatible sources where justified
- OpenTelemetry-compatible applications

Implement:

- Source registration
- Source identity
- Tenant association
- Environment association
- Authentication
- Source configuration
- Source health
- Ingestion status
- Agent version tracking

Do not trust source-provided tenant identities without validating the authenticated source's ownership.

---

## 4. LOG FORMAT SUPPORT

Support:

### Structured JSON

Fields include timestamps, levels, service names, trace identifiers, and custom attributes.

### Plain Text

Apply configurable parsing or store as raw text.

### Multiline Logs

Support Java exception stack traces and other bounded multiline records.

### OTLP Logs

Support standard OpenTelemetry log-record semantics through a supported Collector or ingestion adapter.

Do not invent a proprietary log format when a standards-based integration is sufficient.

---

# PART C — NON-FUNCTIONAL REQUIREMENTS

## 5. SCALE AND SLO TARGETS

Treat the following as design targets, not proven capabilities.

| Dimension | Design Target |
|---|---|
| Monitored services | 1,000+ |
| Ingestion throughput | 1 million log events/sec scale scenario |
| Average log size | 500 bytes–2 KB, workload-dependent |
| Daily log volume | 1–10 TB |
| Searchable retention | 7–30 days, configurable |
| Archive retention | 90–365 days, policy-dependent |
| Ingestion API latency | p99 below 100 ms for an explicitly defined acknowledgment boundary |
| Search latency | p95 below 2 seconds for defined queries |
| Indexing delay | p95 below 5 seconds under normal supported load |
| Availability | 99.9–99.99% design target by component |
| Search tenants | 1,000+ |
| Deployment | Multi-AZ Kubernetes |
| Multi-region | Optional extension |

Clearly distinguish:

- Received events/sec
- Accepted events/sec
- Durably persisted events/sec
- Indexed events/sec
- Searchable events/sec
- Dropped events/sec
- Retried events/sec
- Archived events/sec

Do not claim all events are immediately searchable when Kafka buffering or OpenSearch refresh introduces delay.

---

# PART D — HIGH-LEVEL ARCHITECTURE

## 6. COMPARE ARCHITECTURE OPTIONS

### Option A — Direct-to-OpenSearch

Applications send logs directly to OpenSearch.

Discuss coupling, security, overload, and reliability limitations.

### Option B — Collector-Based Aggregation

Applications send logs through collectors and processors into search storage.

### Option C — Kafka-Buffered Distributed Pipeline

Collectors → Ingestion → Kafka → Processing → OpenSearch/Object Storage.

### Option D — Multi-Tier Enterprise Platform

Regional collectors, durable log buffering, tiered retention, search clusters, long-term archives, and a central query/control plane.

Evaluate:

- Throughput
- Durability
- Indexing latency
- Recovery
- Backpressure
- Storage cost
- Search complexity
- Operational overhead

**Recommend Option C as the reference architecture, with a controlled path toward Option D.**

Implement a smaller locally runnable deployment first.

---

## 7. REFERENCE COMPONENTS

Design:

1. Log Collection Agents
2. OpenTelemetry Collector
3. Ingestion Gateway
4. Authentication/Authorization
5. Log Validation
6. Schema Normalization
7. Sensitive-Data Redaction
8. Kafka Buffer
9. Log Processing Workers
10. Enrichment Pipeline
11. Dead-Letter Handling
12. OpenSearch Indexing
13. Raw Log Archive
14. Query API
15. Search Planner
16. Live Tail Service
17. Alert Rules Engine
18. Notification Delivery
19. Retention Manager
20. Tenant/Quota Manager
21. User/Role Manager
22. Saved Query Manager
23. Dashboard Manager
24. PostgreSQL Metadata Store
25. Redis where justified
26. Next.js Web Application
27. Metrics and Distributed Tracing

Provide Mermaid diagrams for:

- Overall architecture
- Log collection
- Log ingestion
- Kafka buffering
- Parsing and redaction
- Indexing
- Search
- Live tail
- Alert evaluation
- Retention/archival
- Backpressure
- Failure recovery
- Kubernetes deployment

---

# PART E — LOG COLLECTION

## 8. AGENT-BASED COLLECTION

Evaluate:

- OpenTelemetry Collector
- Fluent Bit
- Vector
- Existing managed Kubernetes logging agents

Use a supported, maintained collector for the working baseline.

Support:

- File tailing
- Container stdout/stderr
- Kubernetes metadata enrichment
- OTLP intake
- Batching
- Compression
- Local buffering
- Bounded retries
- Source authentication
- TLS

Do not build a custom Kubernetes log tailing agent merely to demonstrate basic collection.

---

## 9. KUBERNETES LOG COLLECTION

Implement or configure:

- DaemonSet-based collection where appropriate
- Pod metadata
- Namespace
- Container name
- Deployment
- Cluster identifier
- Environment
- Source timestamps

Handle:

- Container rotation
- Pod restarts
- Collector restarts
- Node failures
- Duplicate collection
- Missing metadata
- Very large log lines

Avoid making pod UID or arbitrary container IDs unrestricted metric labels.

---

## 10. MULTILINE LOG HANDLING

Implement or configure aggregation of:

- Java stack traces
- Nested exception causes
- Multiline application messages
- Partial log lines

Define:

- Maximum line count
- Maximum assembled size
- Flush timeout
- Parsing-failure behavior
- Interleaving limitations

Do not concatenate messages from unrelated concurrent streams.

---

# PART F — INGESTION API

## 11. HTTP LOG INGESTION

Implement:

`POST /api/v1/logs/ingest`

Support authenticated batched ingestion.

Example request:

```json
{
  "sourceId": "payment-service-prod",
  "logs": [
    {
      "eventId": "evt-1001",
      "timestamp": "2026-11-17T10:00:00Z",
      "severity": "ERROR",
      "message": "Provider request timed out",
      "attributes": {
        "operation": "payment-authorization",
        "duration_ms": 1500
      }
    }
  ]
}
```

Implement:

- Input validation
- Maximum payload size
- Maximum events/batch
- Authentication
- Tenant validation
- Rate limiting
- Backpressure
- Partial validation failures
- Schema versioning
- Explicit acknowledgment behavior

Define whether the API returns success after:

- Local validation
- Kafka acknowledgment
- Durable storage
- OpenSearch indexing

For the recommended baseline, acknowledge after the documented durable Kafka acceptance boundary, with replication/acknowledgment configuration sufficient for the chosen failure model.

Do not imply an HTTP 202 means the event is already searchable.

---

## 12. OTLP INGESTION

Support OpenTelemetry Logs via:

- OTLP/gRPC
- OTLP/HTTP

Prefer routing through the OpenTelemetry Collector unless a custom adapter is justified.

Preserve:

- Resource attributes
- Scope attributes
- Log attributes
- Severity
- Body
- Trace ID
- Span ID
- Event timestamp
- Observed timestamp

Verify exact protocol and SDK behavior using official documentation.

---

# PART G — LOG IDENTITY AND DELIVERY SEMANTICS

## 13. EVENT IDENTIFICATION

Define:

- Source ID
- Source event ID
- Tenant ID
- Collection timestamp
- Ingestion timestamp
- Partition key
- Event fingerprint where appropriate

Distinguish:

- Producer-generated event IDs
- Collector offsets/checkpoints
- Transport-generated IDs
- Content hashes

Do not deduplicate unrelated repeated log messages merely because their text is identical.

---

## 14. AT-LEAST-ONCE INGESTION

Design for duplicate deliveries after:

- Collector restart
- Kafka retry
- Consumer rebalance
- Indexer crash
- Network timeout
- OpenSearch acknowledgment loss

Explain how stable IDs can enable idempotent indexing.

Define limitations when input sources do not supply stable event identity.

Never claim universal exactly-once ingestion from best-effort fingerprints.

---

# PART H — KAFKA BUFFERING

## 15. LOG TRANSPORT TOPICS

Evaluate topic strategies based on:

- Tenant
- Environment
- Log classification
- Workload priority
- Retention requirements

Prefer a manageable number of shared, partitioned topics with authenticated tenant attribution where suitable.

Avoid creating unbounded topics per service or tenant.

Support:

- Partitioning
- Replication
- Acknowledgments
- Consumer groups
- Compression
- Retention
- Lag monitoring
- Retry
- Dead-letter handling

---

## 16. KAFKA PARTITIONING

Evaluate partition keys:

- Source ID
- Service ID
- Tenant ID
- Composite partition key
- Stable source stream identity

Define whether ordering is required per source.

Do not promise global chronological order across partitions.

Avoid extremely hot partitions caused by high-volume tenants.

---

## 17. BACKPRESSURE

Define behavior when:

- Kafka is unavailable
- Kafka is saturated
- OpenSearch is slow
- Processing workers lag
- Collector buffers are full

Use:

- Bounded queues
- Producer timeouts
- Admission control
- HTTP 429/503
- Local disk-backed buffering where supported
- Controlled load shedding
- Tenant quotas

Distinguish:

- Fail-closed durable ingestion
- Bounded temporary buffering
- Explicitly lossy best-effort ingestion

Never silently drop events while reporting successful durable ingestion.

---

# PART I — LOG PROCESSING

## 18. PARSING AND NORMALIZATION

Implement:

- JSON parsing
- Plain-text fallback
- Severity normalization
- Timestamp parsing
- Timestamp sanity checks
- Structured attribute validation
- Field name normalization
- Schema version validation
- Large-field truncation rules

Handle:

- Missing timestamps
- Invalid timestamps
- Future timestamps
- Old timestamps
- Malformed JSON
- Invalid Unicode
- Unsupported types

Preserve raw-message references where permitted.

---

## 19. ENRICHMENT

Add trusted metadata such as:

- Environment
- Cluster
- Namespace
- Service name
- Service version
- Region
- Ingestion source
- Deployment metadata

Distinguish trusted collector metadata from user-provided attributes.

Prevent clients from overwriting authoritative tenant or source fields.

---

## 20. SENSITIVE-DATA REDACTION

Implement configurable detection/filtering for:

- Passwords
- Bearer tokens
- API keys
- Session cookies
- Authorization headers
- Payment card data
- Email addresses where policy requires
- Phone numbers where policy requires
- Other regulated fields

Prefer structured field allowlists at the source.

Use redaction during processing as defense in depth.

### Critical limitation

Regex-based redaction cannot guarantee detection of every sensitive value.

Define source-side logging standards, secure raw-buffer handling, data-access restrictions, and incident procedures for accidental sensitive-data ingestion.

Do not allow unredacted raw logs to bypass retention and authorization controls.

---

# PART J — SEARCH STORAGE

## 21. COMPARE STORAGE OPTIONS

Evaluate:

1. OpenSearch
2. Elasticsearch
3. Grafana Loki
4. ClickHouse
5. Object storage + query engine
6. Custom inverted index using Lucene

Compare:

- Full-text search
- Structured filtering
- Indexing cost
- Storage efficiency
- Query performance
- Cardinality behavior
- Data retention
- Scale-out
- Operational complexity

Use **OpenSearch** as the primary working search store for this implementation.

Do not rebuild distributed inverted indexing unnecessarily.

---

## 22. OPENSEARCH INDEX MODEL

Implement:

- Time-based indices or data streams where supported
- Index templates
- Field mappings
- Index lifecycle policies
- Shards and replicas
- Bulk indexing
- Refresh controls
- Aliases
- Index rollover
- Snapshot and restore

Define mapping rules carefully.

Prevent uncontrolled dynamic mapping and field explosion.

Avoid indexing every arbitrary nested JSON field without limits.

---

## 23. BULK INDEXING

Implement:

- Bounded batches
- Maximum batch bytes
- Bounded concurrent bulk requests
- Retry classification
- Partial-bulk response handling
- Index version/ID strategy
- Dead-letter or quarantine
- Ingestion lag metrics

### Critical scenario

A bulk request contains 1,000 records, and OpenSearch accepts 950 while rejecting 50.

Retry only eligible failed items.

Do not blindly assume the entire bulk operation succeeded or failed.

---

# PART K — TIERED STORAGE AND RETENTION

## 24. HOT, WARM, AND COLD STORAGE

Design:

### Hot

Recent logs optimized for fast search.

### Warm

Older searchable logs with different performance characteristics.

### Cold/Archive

Compressed object storage with lower cost and potentially slower retrieval.

Define actual capabilities based on the chosen OpenSearch deployment and supported storage features.

Do not claim an archived object is immediately searchable unless an archive-query or restore mechanism is implemented.

---

## 25. S3 ARCHIVAL

Implement or document:

- Compressed log archives
- Object naming
- Tenant partitioning
- Time partitioning
- Encryption
- Retention
- Integrity checks
- Restore workflow
- Legal hold where applicable

Use a local S3-compatible service for integration testing where needed.

---

## 26. RETENTION POLICIES

Support:

- Tenant retention policies
- Environment retention
- Security log retention
- Application log retention
- Archive duration
- Deletion requests
- Legal hold

Define how retention applies to:

- OpenSearch
- Kafka
- Collector disk buffers
- S3 archives
- Dead-letter storage
- Backups

Do not claim deletion is complete if recoverable copies remain unaccounted for.

---

# PART L — LOG QUERY API

## 27. SEARCH FUNCTIONALITY

Implement:

- Full-text search
- Service filtering
- Severity filtering
- Environment filtering
- Namespace filtering
- Time-range filtering
- Structured field filtering
- Trace ID search
- Correlation ID search
- Sorting
- Cursor pagination
- Saved queries

Avoid unrestricted wildcard and expensive regex searches over unlimited historical data.

---

## 28. SEARCH API CONTRACTS

Implement:

- `POST /api/v1/logs/search`
- `GET /api/v1/logs/{logId}`
- `GET /api/v1/logs/fields`
- `GET /api/v1/logs/facets`
- `POST /api/v1/logs/aggregate`
- `GET /api/v1/logs/stream`

For every endpoint define:

- Authentication
- Authorization
- Tenant scope
- Request limits
- Time-range constraints
- Query complexity budgets
- Timeouts
- Pagination
- Response contracts
- Errors

Use RFC 9457 Problem Details where applicable.

---

## 29. QUERY LANGUAGE

Evaluate:

- Structured JSON query DSL
- Restricted Lucene query syntax
- OpenSearch DSL
- Custom simplified query language

For the baseline, expose a safe, validated structured search API.

Do not expose unrestricted OpenSearch administrative query capabilities to ordinary users.

Support reusable saved query definitions.

---

# PART M — DISTRIBUTED SEARCH

## 30. QUERY PLANNING

Evaluate:

- Tenant-specific index routing
- Time-range pruning
- Search-after pagination
- Parallel shard queries
- Aggregations
- Query caching
- Result limits
- Search cancellation

Keep query cost bounded.

Prevent one user from submitting queries that monopolize the entire search cluster.

---

## 31. SEARCH CONSISTENCY

Define:

- Accepted but not indexed
- Indexed but not refreshed
- Searchable
- Archived
- Deleted

Expose ingestion delay where operationally useful.

Do not promise read-after-write consistency unless an explicit refresh/wait contract is implemented.

---

# PART N — LIVE LOG TAILING

## 32. REAL-TIME LOG VIEWING

Implement a live-tail capability.

Compare:

- Server-Sent Events (SSE)
- WebSocket
- Kafka-backed streaming
- Polling

Choose SSE for the initial unidirectional stream unless bidirectional control is required.

Support:

- Authenticated subscriptions
- Tenant-scoped filters
- Service filters
- Severity filters
- Heartbeats
- Reconnection
- Bounded client buffers
- Slow-client handling
- Cancellation

---

## 33. LIVE-TAIL DELIVERY GUARANTEES

Define whether the live stream is:

- Best effort
- Replayable
- Durable
- Ordered per partition

Do not assume a WebSocket/SSE connection guarantees no missed events.

Provide a recovery mechanism using durable search and event timestamps/cursors where feasible.

---

# PART O — LOG ANALYTICS

## 34. AGGREGATION QUERIES

Implement:

- Log count over time
- Error rate by service
- Severity distribution
- Top exception types
- Most frequent error codes
- Logs by environment
- Logs by namespace
- Unique service counts
- High-volume sources

Define aggregation correctness and sampling limitations.

Avoid high-cost exact cardinality queries over extremely large datasets when approximations are acceptable and disclosed.

---

## 35. LOG-BASED METRICS

Support derived indicators such as:

- Error events/minute
- Authentication failures/minute
- Deployment exceptions
- Payment timeout count
- Database deadlock count

Do not use log-derived counters as substitutes for authoritative financial or transaction metrics.

---

# PART P — ALERTING

## 36. ALERT RULE ENGINE

Implement:

- Alert rule creation
- Rule validation
- Scheduled evaluation
- Threshold conditions
- Time windows
- Rule enable/disable
- Severity
- Notification targets
- Alert state
- Acknowledgment
- Silencing
- Audit history

Example:

"Alert when payment-service ERROR events exceed 100 over five minutes."

Define time-window and count semantics.

---

## 37. ALERT DEDUPLICATION

Prevent alert storms using:

- Alert fingerprints
- Cooldown periods
- Grouping
- Suppression
- State transitions
- Bounded reevaluation

Use durable state where needed.

Do not notify every recipient once per matching log line.

---

# PART Q — LOG-TRACE CORRELATION

## 38. CORRELATION FIELDS

Preserve:

- Trace ID
- Span ID
- Correlation ID
- Request ID
- Service name
- Environment
- Timestamp

Use W3C Trace Context and OpenTelemetry conventions where supported.

Do not confuse trace IDs with authentication identities.

---

## 39. TRACE NAVIGATION

Implement configurable links to a tracing backend such as Tempo or another existing system.

Example:

User opens a payment error log.

The UI displays:

- Trace ID
- Related logs
- Trace navigation
- Time correlation
- Service context

Do not require deploying a second complete observability stack merely to demonstrate outbound trace links.

Provide a working local trace integration when justified and supported.

---

# PART R — MULTI-TENANCY

## 40. TENANT MODEL

Support:

- Tenant
- Organization
- Project
- Environment
- Log source
- Ingestion credential
- Retention policy
- Usage quota
- User roles

Define tenant isolation across:

- API authentication
- Kafka processing
- PostgreSQL
- OpenSearch
- Object storage
- Alert rules
- Saved queries

---

## 41. TENANT QUERY ISOLATION

Never trust a client-provided tenant filter as the security boundary.

Apply tenant restrictions server-side and enforce them in the chosen OpenSearch authorization/routing model.

Test malicious queries attempting to override tenant constraints.

Consider physical or logical index separation according to security and scale requirements.

---

# PART S — QUOTAS AND COST CONTROLS

## 42. INGESTION QUOTAS

Implement:

- Per-tenant events/sec
- Per-tenant bytes/sec
- Daily ingestion budget
- Maximum batch size
- Retention plan
- Search request limits
- Concurrent-query limits

Use Redis or another suitable atomic coordination approach where justified.

Clearly document hard limits versus approximate limits.

---

## 43. USAGE METERING

Track:

- Ingested bytes
- Accepted events
- Indexed bytes
- Stored bytes
- Archive bytes
- Query usage
- Retention consumption

Distinguish billable usage from internal telemetry estimates.

Do not bill using unverified approximate counters without reconciliation.

---

# PART T — SECURITY

## 44. ENTERPRISE SECURITY REQUIREMENTS

Implement:

1. OAuth2/OIDC authentication
2. Spring Security
3. RBAC
4. Tenant isolation
5. API keys or short-lived ingestion credentials
6. TLS
7. Service-to-service authentication
8. Collector authentication
9. Secret management
10. Input validation
11. Query authorization
12. Search complexity limits
13. Redaction
14. Encryption at rest where applicable
15. Audit logging
16. Safe exports
17. Retention/deletion
18. Administrative authorization
19. Rate limiting
20. Secure dashboard access

Do not allow ingest credentials to act as unrestricted administrative credentials.

---

## 45. THREAT MODEL

Analyze:

- Cross-tenant log leakage
- Forged source identity
- Unauthorized ingestion
- Log injection
- Control-character injection
- Stored XSS in log viewer
- Query DSL injection
- OpenSearch administrative access
- Sensitive data leakage
- Log flooding
- Oversized log payloads
- Compression bombs
- Malicious JSON nesting
- Archive access abuse
- Unauthorized exports
- Excessive query cost
- Log-retention bypass

For each identify mitigations and automated tests.

---

# PART U — NEXT.JS FRONTEND

## 46. FRONTEND STACK

Use:

- Next.js App Router
- React
- TypeScript
- Tailwind CSS or a maintained design system
- Accessible components
- TanStack Query where justified
- Zod for form and API-schema validation
- Virtualized tables where appropriate
- Charting library suited to time-series visualization

Verify compatible framework/library versions.

Keep tokens and private credentials in appropriate server-side security boundaries.

Do not expose OpenSearch credentials directly to the browser.

---

## 47. LOG EXPLORER

Build a functional log-search interface.

Features:

- Time-range selector
- Search input
- Structured filters
- Service/environment selectors
- Severity selector
- Search results
- Expandable JSON details
- Trace ID navigation
- Column customization
- Sorting
- Cursor-based pagination
- Copy/export controls
- Saved searches

Render log message content as escaped text.

Never insert untrusted HTML through `dangerouslySetInnerHTML`.

---

## 48. LIVE LOG VIEWER

Implement:

- Start/stop stream
- Pause/resume display
- Service filters
- Severity filters
- Auto-scroll
- Virtualized rendering
- Connection state
- Reconnect behavior
- Dropped-message notice
- Historical recovery link

Prevent browser memory growth from unbounded streamed records.

---

## 49. DASHBOARDS

Implement:

### Platform Overview

- Ingestion throughput
- Indexing lag
- Search latency
- Active tenants
- Storage usage
- Error rate

### Service Logs

- Logs/sec
- Error count
- Warning count
- Top exception types
- Severity timeline

### Infrastructure

- Collector health
- Kafka lag
- OpenSearch health
- Disk utilization
- Queue saturation

### Alerts

- Active alerts
- Alert history
- Alert severity
- Acknowledgment state

### Tenant Usage

- Ingestion volume
- Storage
- Retention
- Quota usage

Use actual backend APIs, not static mock data.

---

## 50. ADMINISTRATION PORTAL

Support:

- Tenant management
- User roles
- Log sources
- Ingestion credentials
- Parsing policies
- Redaction policies
- Retention policies
- Index/storage policies
- Alert rules
- Usage quotas
- Audit logs

Enforce all authorization on the backend.

---

# PART V — JAVA 21 + SPRING BOOT BACKEND

## 51. RECOMMENDED STACK

| Component | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot, verified compatible release |
| Build | Maven |
| REST APIs | Spring Web MVC |
| Streaming APIs | SSE/WebFlux where justified |
| Security | Spring Security/OAuth2 Resource Server |
| Metadata DB | PostgreSQL |
| Migrations | Flyway |
| Durable transport | Apache Kafka |
| Search storage | OpenSearch |
| Log collection | OpenTelemetry Collector/Fluent Bit |
| Cache/quotas | Redis where justified |
| Archival | S3-compatible object storage |
| Observability | Micrometer/OpenTelemetry |
| Unit tests | JUnit 5/Mockito |
| Integration tests | Testcontainers |
| Benchmarking | k6/Gatling |
| Deployment | Docker/Kubernetes |
| Monitoring | Prometheus/Grafana |

Verify official documentation for current versions and compatibility.

Avoid adding both Spring MVC and WebFlux to every service without a clear runtime model.

---

# PART W — DATABASE AND INDEX DESIGN

## 52. POSTGRESQL METADATA SCHEMA

Design tables such as:

- tenants
- organizations
- projects
- environments
- log_sources
- ingestion_credentials
- parsing_rules
- redaction_policies
- retention_policies
- storage_policies
- saved_queries
- dashboards
- alert_rules
- alert_instances
- alert_notifications
- tenant_usage
- ingestion_checkpoints
- archive_manifests
- audit_events

Use PostgreSQL for metadata and control-plane operations.

Do not store all high-volume log bodies in the metadata database by default.

---

## 53. OPENSEARCH DOCUMENT SCHEMA

Define fields such as:

- log_id
- tenant_id
- source_id
- service_name
- environment
- cluster
- namespace
- severity
- event_timestamp
- ingestion_timestamp
- message
- trace_id
- span_id
- attributes
- schema_version

Define:

- Searchable fields
- Exact-match fields
- Text-analyzed fields
- Date mappings
- Numeric mappings
- Field-count limits
- Data stream/index routing

Limit mapping explosion from arbitrary attributes.

---

# PART X — PROJECT STRUCTURE

## 54. SOURCE CODE ORGANIZATION

Create a maintainable monorepo.

```text
enterprise-log-platform/
├── README.md
├── docs/
│   ├── requirements.md
│   ├── architecture.md
│   ├── ingestion.md
│   ├── delivery-semantics.md
│   ├── processing.md
│   ├── search.md
│   ├── indexing.md
│   ├── live-tail.md
│   ├── alerts.md
│   ├── tenancy-security.md
│   ├── storage-retention.md
│   ├── capacity-planning.md
│   ├── runbooks.md
│   └── decisions/
├── backend/
│   ├── pom.xml
│   ├── log-domain/
│   ├── log-control-plane/
│   ├── log-ingestion/
│   ├── log-processing/
│   ├── log-indexer/
│   ├── log-query/
│   ├── log-live-tail/
│   ├── log-alerting/
│   └── integration-tests/
├── frontend/
│   ├── package.json
│   ├── app/
│   ├── components/
│   ├── features/
│   ├── lib/
│   └── tests/
├── demo-services/
│   ├── order-service/
│   ├── payment-service/
│   └── inventory-service/
├── infrastructure/
│   ├── otel-collector/
│   ├── kafka/
│   ├── opensearch/
│   ├── postgres/
│   ├── object-storage/
│   └── monitoring/
├── benchmarks/
├── docker/
├── helm/
└── .github/workflows/
```

Adapt deployment boundaries based on throughput and operational requirements.

Do not create independent services merely because the proposed directory structure lists a module.

---

# PART Y — DEMONSTRATION SERVICES

## 55. IMPLEMENT LOG PRODUCERS

Create three Spring Boot demonstration services:

### Order Service

Produces:

- Order created
- Order validation failure
- Order processing failure

### Payment Service

Produces:

- Payment authorization started
- Provider timeout
- Payment rejected
- Authorization completed

### Inventory Service

Produces:

- Inventory reservation
- Stock conflict
- Database timeout
- Reservation cancellation

All services must emit:

- Structured logs
- Trace IDs
- Correlation IDs
- Different severity levels
- Simulated failure cases

Provide deterministic fault-injection controls in development/test environments only.

---

# PART Z — REQUIRED FAILURE SCENARIOS

## 56. ANALYZE THESE 45 SCENARIOS

1. Application writes logs faster than collector can read.
2. Collector restarts during file tailing.
3. Container logs rotate during collection.
4. Duplicate records arrive after collector restart.
5. HTTP ingestion response is lost after Kafka acceptance.
6. Kafka producer times out with an ambiguous outcome.
7. Kafka broker becomes unavailable.
8. Kafka consumer rebalance interrupts indexing.
9. Kafka delivers duplicate records.
10. Kafka partitions produce out-of-order records.
11. Processing worker crashes after transformation.
12. OpenSearch is unavailable.
13. OpenSearch bulk request partially succeeds.
14. OpenSearch index becomes read-only.
15. OpenSearch shard runs out of disk.
16. OpenSearch dynamic mapping explodes.
17. One tenant generates an extreme log burst.
18. Quota enforcement becomes unavailable.
19. Redis becomes unavailable.
20. PostgreSQL primary fails.
21. S3 archive storage is unavailable.
22. Archival succeeds but deletion fails.
23. Retention deletion occurs while archive is being queried.
24. Search query requests an excessively large time range.
25. User submits expensive wildcard queries.
26. Tenant A attempts to search Tenant B's logs.
27. Untrusted log text contains HTML/JavaScript.
28. Logs contain API keys or bearer tokens.
29. Redaction processor misses an unexpected secret format.
30. Large malformed JSON causes parser failure.
31. Multiline logs exceed the configured limit.
32. Event timestamp is in the future.
33. Clock skew causes apparently unordered logs.
34. Live-tail client disconnects.
35. Live-tail consumer becomes too slow.
36. Browser memory grows from unbounded live results.
37. Log-based alert fires repeatedly.
38. Notification delivery for an alert fails.
39. Alert rule changes during evaluation.
40. Kubernetes pod is terminated during indexing.
41. Regional ingestion outage occurs.
42. Rolling deployment changes document schemas.
43. Log deletion request encounters backups and archives.
44. Log-to-trace correlation is missing.
45. Platform telemetry overwhelms the logging platform itself.

For each scenario provide:

- Root cause
- Data-loss or duplication risk
- Security/privacy impact
- User-visible impact
- Detection
- Prevention
- Recovery
- Automated test
- Residual risk

---

# PART AA — AUTOMATED TESTING

## 57. UNIT TESTS

Cover:

- Log schema validation
- Timestamp normalization
- Severity normalization
- JSON parsing
- Plain-text fallback
- Multiline assembly
- Event ID handling
- Deduplication
- Redaction
- Attribute enrichment
- Search query validation
- Tenant-scoped query construction
- Retention policy resolution
- Alert evaluation

---

## 58. INTEGRATION TESTS

Use Testcontainers for:

- PostgreSQL
- Kafka
- OpenSearch
- Redis where implemented
- S3-compatible object storage where implemented

Verify:

- Ingestion API
- Kafka persistence
- Processing pipeline
- Indexing
- Search
- Multi-tenant isolation
- Live-tail integration
- Alert evaluation
- Retention/archival
- Recovery after crashes

Do not rely solely on mocked OpenSearch or Kafka clients to prove distributed behavior.

---

## 59. CONCURRENCY TESTS

Verify:

- Multiple ingestion pods
- Concurrent duplicate events
- Multiple index workers
- OpenSearch partial bulk retry
- Tenant quota enforcement
- Concurrent saved-query updates
- Concurrent alert evaluations
- Retention and archive races
- Live-tail client backpressure

---

## 60. SECURITY TESTS

Verify:

- Unauthorized ingestion
- Invalid source credentials
- Forged tenant IDs
- Cross-tenant search attempts
- Query DSL injection
- XSS in log messages
- Oversized payloads
- Excessive JSON nesting
- Sensitive-data redaction
- Unauthorized exports
- Administrative RBAC
- Retention-policy bypass

Use controlled test payloads, never real secrets.

---

## 61. FRONTEND TESTS

Use:

- TypeScript type checking
- ESLint
- Component tests
- Playwright end-to-end tests
- Accessibility checks

Verify:

- Login
- Tenant selection
- Log search
- Filters
- Time range
- JSON detail inspection
- Trace links
- Live tail
- Saved queries
- Dashboards
- Alerts
- Admin settings
- Authorization failures
- Responsive UI

Do not substitute static screenshots for functional tests.

---

# PART AB — PERFORMANCE ENGINEERING

## 62. CAPACITY PLANNING

Calculate:

1. Events/sec
2. Average event size
3. Ingress bandwidth
4. Kafka throughput
5. Kafka retention storage
6. Indexing amplification
7. OpenSearch indexed storage
8. Replica overhead
9. Shard counts
10. Query concurrency
11. Search memory
12. Collector buffering requirements
13. Redaction CPU
14. Archive storage
15. Network egress
16. Tenant quotas
17. Infrastructure cost per GB ingested

Example formulas:

`Raw bytes/day = events/sec × average event bytes × 86,400`

`Kafka storage ≈ ingress bytes/sec × retention seconds × replication factor`

`Indexed storage ≈ retained raw bytes × measured indexing/storage amplification`

Use explicit workload assumptions.

Do not guess compression or indexing ratios without labeling them as estimates.

---

## 63. INGESTION BENCHMARKS

Create reproducible workloads for:

- 10,000 events/sec
- 100,000 events/sec
- 500,000 events/sec
- 1 million events/sec as a scale target

Benchmark:

- HTTP ingestion
- OTLP ingestion
- Kafka publication
- Parsing
- Redaction
- Enrichment
- Bulk indexing

Measure:

- Offered events/sec
- Accepted events/sec
- Indexed events/sec
- p50/p95/p99 acknowledgment latency
- Indexing lag
- Kafka consumer lag
- Duplicate rate
- Data-loss counters
- Queue depth
- CPU
- Heap/GC
- Network bandwidth
- OpenSearch bulk rejection rate

Never fabricate benchmark results.

---

## 64. QUERY BENCHMARKS

Benchmark:

- Service + time filtering
- Severity filtering
- Full-text search
- Trace ID lookup
- Structured-field filtering
- Aggregations
- Concurrent searches
- Historical archive queries where implemented

Measure:

- p50/p95/p99 latency
- Timeout rate
- Cache hit rate
- Shard fan-out
- CPU/memory
- Query rejection
- Search correctness

Separate hot-storage results from archive-query results.

---

# PART AC — SELF-OBSERVABILITY

## 65. METRICS AND TRACING

Use Micrometer and OpenTelemetry.

Expose:

- Ingestion request rate
- Accepted events
- Rejected events
- Dropped events
- Kafka publish failures
- Kafka consumer lag
- Processing latency
- Redaction failures
- Indexing throughput
- Indexing lag
- OpenSearch bulk failures
- Search request rate
- Search latency
- Search timeout rate
- Live-tail connections
- Live-tail drop count
- Alert evaluation lag
- Alert failures
- Archive backlog
- Retention failures
- Tenant quota rejections

Avoid using arbitrary log IDs, trace IDs, or tenant IDs as unrestricted metric labels.

---

## 66. ALERTING AND RUNBOOKS

Provide operational alerts for:

- Kafka backlog growth
- OpenSearch unavailable
- Disk space exhaustion
- Collector buffer saturation
- High ingestion rejection rate
- Unexpected log loss
- Search p99 degradation
- Tenant quota abuse
- Sensitive data leakage incident
- Archival failures
- Retention failures

Create runbooks with symptoms, investigation queries, mitigation, recovery, and verification.

---

# PART AD — DEPLOYMENT

## 67. DOCKER COMPOSE

Provide local infrastructure with:

- Java 21 backend services
- Next.js frontend
- PostgreSQL
- Kafka
- OpenSearch
- OpenTelemetry Collector
- Redis where implemented
- S3-compatible storage where implemented
- Demo microservices
- Metrics/monitoring where implemented

The local environment must demonstrate:

**Demo Service → Collector → Ingestion → Kafka → Processing → OpenSearch → Next.js Search**

without paid cloud accounts.

---

## 68. KUBERNETES

Provide:

- Dockerfiles
- Kubernetes Deployments
- Services
- Collector DaemonSet
- ConfigMaps
- Secret references
- Resource requests/limits
- Persistent storage configuration
- HPA/KEDA where justified
- PodDisruptionBudget
- Readiness/liveness probes
- Graceful shutdown
- Rolling deployment
- Network policies
- TLS
- CI/CD workflow

Document stateful component operational responsibilities.

Do not treat locally deployed single-node Kafka/OpenSearch as a production high-availability topology.

---

# PART AE — IMPLEMENTATION WORKFLOW

## 69. EXECUTION PHASES

### Phase 1 — Requirements and Architecture

Define log sources, volume, retention, tenancy, search features, durability boundary, SLOs, and cost assumptions.

### Phase 2 — Project Foundation

Create Java 21 backend, Next.js frontend, PostgreSQL migrations, authentication, local infrastructure, and build pipelines.

### Phase 3 — Collection Integration

Configure OpenTelemetry Collector or Fluent Bit and implement demo log sources.

### Phase 4 — Ingestion

Implement authenticated batched ingestion, input validation, rate limits, and explicit durable acknowledgment semantics.

### Phase 5 — Kafka Transport

Implement reliable producer configuration, topic partitioning, bounded retries, consumer processing, and lag monitoring.

### Phase 6 — Parsing and Security

Implement JSON/plain-text parsing, timestamps, multiline handling, trusted enrichment, and sensitive-data redaction.

### Phase 7 — Indexing

Implement OpenSearch mappings, bulk indexing, partial failure recovery, and idempotent document IDs.

### Phase 8 — Search APIs

Implement scoped full-text/structured search, time filtering, pagination, facets, and query limits.

### Phase 9 — Next.js Log Explorer

Implement search, filters, time selector, structured log details, trace links, and saved queries.

### Phase 10 — Live Tail

Implement authenticated SSE/WebSocket streaming, bounded client buffers, reconnection, and missed-log recovery.

### Phase 11 — Dashboards and Analytics

Implement time-series aggregations, error distributions, service analytics, and operational dashboards.

### Phase 12 — Alerting

Implement log-based alert rules, evaluation, deduplication, notification, and alert history.

### Phase 13 — Multi-Tenancy

Implement organization/project boundaries, credentials, RBAC, quotas, and tenant-safe queries.

### Phase 14 — Retention and Archive

Implement index lifecycle, retention policies, archive manifests, restore behavior, and deletion controls.

### Phase 15 — Security Hardening

Implement threat-model mitigations, XSS-safe rendering, query restrictions, secret handling, and adversarial tests.

### Phase 16 — Observability

Implement Micrometer/OpenTelemetry metrics, platform tracing, alerts, and runbooks.

### Phase 17 — Frontend Administration

Implement source, tenant, retention, alert, and usage-management pages.

### Phase 18 — Deployment

Implement Docker Compose, Kubernetes/Helm, and CI/CD.

### Phase 19 — Verification

Run backend unit/integration/concurrency/security tests and frontend type/component/Playwright tests.

### Phase 20 — Performance

Benchmark ingestion, processing, indexing, querying, and live-tail behavior.

### Phase 21 — Final Architecture Review

Review durability, log loss, deduplication, tenant isolation, search behavior, retention, observability, cost, and remaining risks.

For every phase:

- Create actual source files.
- Keep the project buildable.
- Run relevant checks.
- Fix discovered defects.
- Preserve unrelated work.
- Document significant architecture decisions.
- Report executed verification separately from proposed tests.
- Identify unverified scale claims and operational risks.

If repository and execution tools are available, implement directly.

Otherwise, provide complete file contents, paths, and reproducible commands without claiming execution.

---

# PART AF — REQUIRED DELIVERABLES

## 70. COMPLETE PROJECT OUTPUTS

### Deliverable 1 — Requirements

- Functional requirements
- Non-functional requirements
- Ingestion and durability guarantees
- Retention and privacy requirements
- Acceptance criteria

### Deliverable 2 — HLD

- Architecture diagrams
- Collection topology
- Ingestion pipeline
- Kafka buffering
- Indexing
- Search
- Alerting
- Capacity planning

### Deliverable 3 — LLD

- Domain models
- Sequence diagrams
- Event schemas
- Database schema
- OpenSearch mappings
- Search API contracts
- Retry/backpressure algorithms
- Tenant authorization model

### Deliverable 4 — Java Backend

- Log ingestion
- Validation
- Kafka integration
- Parsing
- Enrichment
- Redaction
- Indexing
- Search
- Live tail
- Alerting
- Retention
- Tenant management
- Security

### Deliverable 5 — Next.js Frontend

- Log explorer
- Real-time log viewer
- Dashboards
- Alert rules
- Saved searches
- Source management
- Tenant settings
- Admin controls

### Deliverable 6 — Collection Infrastructure

- OpenTelemetry Collector
- Agent configuration
- Sample log-producing services
- Kafka
- OpenSearch
- PostgreSQL
- Object storage where implemented

### Deliverable 7 — Automated Tests

- Unit tests
- Integration tests
- Concurrency tests
- Security tests
- Frontend tests
- Failure-recovery tests

### Deliverable 8 — Infrastructure

- Docker Compose
- Kubernetes/Helm
- CI/CD
- Monitoring

### Deliverable 9 — Performance

- Benchmark scripts
- Measured results where executed
- Indexing/search capacity estimates
- Cost model
- Bottleneck analysis

### Deliverable 10 — Documentation

- README
- Architecture Decision Records
- Ingestion protocol
- Delivery semantics
- Parsing/redaction policies
- OpenSearch schema
- Search query guide
- Live-tail semantics
- Retention/archival
- Security and tenant isolation
- Deployment
- Operational runbooks
- Known limitations

---

# PART AG — MANDATORY ARCHITECTURE REVIEW

## 71. ANSWER THESE 50 QUESTIONS

1. How would you design a distributed log aggregation system?
2. How does log aggregation differ from observability?
3. How do you collect logs from Kubernetes?
4. When should you use OpenTelemetry Collector versus Fluent Bit?
5. What is the difference between agent and gateway collectors?
6. How do you implement durable log ingestion?
7. When should the ingestion API acknowledge a log batch?
8. What is at-least-once log delivery?
9. Why is exactly-once ingestion difficult?
10. How do you detect duplicates without discarding legitimate repeated messages?
11. How do you partition Kafka for log ingestion?
12. How do you handle out-of-order logs?
13. How do you prevent Kafka hot partitions?
14. How do you manage ingestion backpressure?
15. What happens when Kafka is unavailable?
16. How do you handle OpenSearch outages?
17. How does OpenSearch indexing work?
18. How does an inverted index support full-text search?
19. How do time-based indices work?
20. How do you calculate shard count?
21. What causes OpenSearch mapping explosion?
22. How do you handle partial bulk indexing failures?
23. How do you implement idempotent indexing?
24. What is indexing lag?
25. How does search refresh affect visibility?
26. How do you build a safe log query API?
27. How do you constrain expensive searches?
28. How does search-after pagination work?
29. How do you implement live log tailing?
30. How do disconnected clients recover missed logs?
31. How do you parse multiline stack traces?
32. How do you normalize timestamps from multiple systems?
33. How do you handle clock skew?
34. How do you redact sensitive data?
35. Why is regex redaction alone insufficient?
36. How do you prevent stored XSS in a log viewer?
37. How do you enforce tenant isolation in OpenSearch?
38. How do you build log-based alerts?
39. How do you prevent alert storms?
40. How do you implement tiered log storage?
41. What is the difference between retention and archival?
42. How do you delete logs across hot storage and backups?
43. How do you correlate logs with distributed traces?
44. How do you monitor Kafka consumer lag?
45. How do you detect collector data loss?
46. How do you estimate storage for 1 million logs/sec?
47. How do you reduce cost per GB ingested?
48. How do you design a Next.js log explorer?
49. How do you benchmark indexing and query workloads?
50. What evidence is required before enterprise production adoption?

For each answer explain:

- Architecture
- Correctness
- Concurrency
- Failure behavior
- Security/privacy
- Scalability
- Performance
- Operational trade-offs

---

# DEFINITION OF DONE

## 72. ACCEPTANCE CRITERIA

The implementation is complete only when:

- Java 21 backend builds.
- Next.js TypeScript frontend builds.
- PostgreSQL migrations execute.
- Log sources can be registered and authenticated.
- Collector-based ingestion works.
- HTTP ingestion supports bounded batches.
- OTLP ingestion works through supported components.
- Accepted log durability semantics are documented and tested.
- Kafka buffering is recoverable.
- Duplicate deliveries follow documented deduplication rules.
- Parsing and normalization work.
- Sensitive-data redaction is enforced.
- OpenSearch indexes logs correctly.
- Partial indexing failures are handled.
- Multi-tenant search isolation is tested.
- Full-text and structured searches work.
- Time-range filtering works.
- Cursor pagination works.
- Live log tailing works.
- Slow clients cannot exhaust backend/browser memory.
- Saved queries work.
- Log-based analytics function.
- Alert rules evaluate correctly.
- Alert deduplication prevents uncontrolled notification storms.
- Retention policies execute.
- Archival and restore are verified where implemented.
- Next.js log explorer uses real APIs.
- Next.js dashboards are functional.
- Log text is safely escaped.
- Authentication and RBAC are enforced.
- Docker Compose supports the complete local log pipeline.
- Kubernetes deployment configuration is provided.
- Automated tests cover important concurrency, failure, and security scenarios.
- Metrics and alerts are operational.
- Benchmarks are reproducible.
- Throughput and latency targets are measured or marked unverified.
- Known limitations and remaining risks are documented.

Do not declare production readiness merely because logs appear in the search UI.

---

# FINAL EXECUTION INSTRUCTIONS

Build a **real enterprise Distributed Log Aggregation Platform with Java 21 + Spring Boot backend and Next.js + TypeScript frontend**.

Prioritize:

1. **Reliable collection:** Supported agents, structured events, and explicit delivery guarantees.
2. **Durable ingestion:** Kafka buffering, bounded backpressure, and crash recovery.
3. **Secure processing:** Validation, redaction, tenant attribution, and untrusted-input handling.
4. **Scalable indexing:** OpenSearch bulk indexing, mapping limits, and lifecycle management.
5. **Powerful search:** Full-text queries, structured filters, time ranges, and controlled query cost.
6. **Real-time visibility:** Live tail with reconnection and bounded resource usage.
7. **Actionable analytics:** Dashboards, saved searches, log-based alerts, and trace correlation.
8. **Multi-tenancy:** Strong backend authorization and tenant-specific quotas/retention.
9. **Cost efficiency:** Compression, tiered retention, archival, and ingestion controls.
10. **Frontend quality:** Functional, responsive, accessible Next.js log explorer and administration.
11. **Operational reliability:** Diagnosable ingestion loss, Kafka lag, indexing failures, and query degradation.
12. **Reproducibility:** Complete source, deterministic demo workloads, tests, benchmarks, Docker, and Kubernetes.

Begin with a concise architecture assessment.

Choose the log collection architecture, ingestion acknowledgment semantics, Kafka partitioning, OpenSearch index strategy, tenant isolation model, retention strategy, and Next.js frontend structure.

Then implement the entire platform incrementally.

Use available development tools to create files, compile backend/frontend, execute tests, run the local pipeline, verify log search and live tail, inject failures, benchmark supported workloads, and fix defects.

At completion report:

- Implemented backend functionality
- Implemented Next.js frontend functionality
- Collection and ingestion guarantees
- Kafka and OpenSearch consistency model
- Data retention and privacy safeguards
- Architecture decisions and trade-offs
- Project structure
- Exact build and run commands
- Tests actually executed
- Benchmark measurements actually obtained
- Known limitations
- Remaining security, reliability, data-loss, and production risks

**Do not stop after HLD or LLD. Deliver the complete end-to-end Log Aggregation Platform, including log collectors, ingestion APIs, Kafka buffering, parsing, redaction, OpenSearch indexing, tenant-safe search, real-time log tailing, alerting, Next.js dashboards, automated verification, observability, and Kubernetes deployment.**
````