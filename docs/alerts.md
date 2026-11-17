# Alerts and notification outbox

Enabled rules query indexed ERROR events over a rolling five-minute event-time window every 30 seconds. PostgreSQL locks
rule rows with SKIP LOCKED to prevent concurrent evaluators from duplicating a state transition. FIRING/RESOLVED history
and a notification outbox record commit in the same transaction. Partial/timed-out search results do not change rule
state.

A dispatcher claims up to ten due outbox rows using row locks and sends to a **server-configured trusted webhook**,
never a caller-supplied URL. HTTP calls have a two-second timeout. A server-configured key authenticates requests;
Idempotency-Key is the stable notification UUID. Successful 2xx marks delivery; failures retry after 30 seconds, up to
eight attempts, then remain visible as dead. A crash after remote success but before commit can redeliver: receivers
must deduplicate the UUID. The local payment demo has a bounded synthetic receiver for testing; no external recipients
are contacted.

`GET /api/v1/alert-notifications` is tenant-scoped and admin-only. Missing webhook URL/key leaves the outbox undelivered
for later configuration. Configure HTTPS and managed secrets outside local development. Acknowledgment, silencing,
grouped cooldown and dead-notification replay controls remain unfinished. Late indexing can miss event-time windows, and
log counts are not authoritative financial counters.
