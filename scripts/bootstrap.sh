#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
docker compose exec -T opensearch curl -4 -fsS -X PUT http://localhost:9200/_index_template/logs -H 'Content-Type: application/json' --data-binary @- < infrastructure/opensearch/template.json
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic logs-v1 --partitions 12 --replication-factor 1 --config retention.ms=86400000
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --alter --topic logs-v1 --partitions 12 2>/dev/null || docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --describe --topic logs-v1
docker compose exec -T kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic logs-quarantine-v1 --partitions 12 --replication-factor 1 --config retention.ms=86400000
TOKEN=$(curl -4 -fsS http://localhost:8180/realms/logs/protocol/openid-connect/token -d grant_type=password -d client_id=log-console -d username=demo -d password=local-demo-only | python3 -c 'import sys,json; print(json.load(sys.stdin)["access_token"])')
printf 'Paste the OIDC token into the console session form:\n%s\n' "$TOKEN"
curl -4 -fsS http://localhost:8080/api/v1/sources -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"id":"demo","service":"payment-service","environment":"development"}'
printf '\nSet the returned key as INGESTION_KEY in .env, then recreate collector and demo services.\n'
