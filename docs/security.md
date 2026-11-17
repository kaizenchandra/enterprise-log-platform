# Security and privacy

Source credentials are random opaque values stored as SHA-256 hashes; ingestion cannot invoke user/admin endpoints. The
source database establishes tenant, service and environment. JWT tenant claims establish query scope. Admin requires an
admin claim. Keycloak local realm and password grant are development conveniences; replace with authorization-code +
PKCE, audience validation, trusted issuer configuration and managed users before deployment. The decoder verifies
signature, expiration and issuer; audience enforcement remains required.

Redaction occurs before Kafka, so searchable storage and transport get masked records. Regex covers common
token/password/email patterns and secret-named attributes, not all secrets. Payment card/phone detection and policy
management are missing. Collector buffers may contain raw data before gateway redaction: restrict disk permissions and
encrypt disks. Never ship actual sensitive data during testing.

React renders log text with escaped text nodes; no HTML insertion. BFF stores tokens in HttpOnly SameSite cookies and
checks POST origin. Browser token bootstrap is a local-development flow, not full OIDC login. No arbitrary OpenSearch
DSL is exposed. Attributes, event sizes and batch sizes are bounded. Compression is refused at gateway to avoid
decompression bombs. Deep nesting limits use Jackson defaults; tighter limits and adversarial testing remain required.

Compose has plaintext internal traffic and disabled OpenSearch security, with only loopback application/collector ports
exposed. It is not a secure production topology. Kubernetes manifest is an application starting point, not a turnkey
deployment: configure mTLS, restricted network policies, ingress TLS, audience checks, external secrets, backups and
stateful HA first.

## Subsequent hardening

The backend now validates the `log-platform` token audience as well as signature, issuer and expiry. Local clients
log-console (demo admin), log-viewer (demo viewer), and log-other (other-tenant admin) enable real authorization tests.
JSON request unknown fields are rejected, including attempts to supply unrestricted search DSL. Error dispatcher
requests are permitted so intentionally returned 403/413 statuses remain correct; this does not permit direct access to
application resources.
