#!/usr/bin/env bash
set -euo pipefail
: "${INGESTION_KEY:?Set the registered demo source credential}"
: "${TOKEN:?Set an OIDC access token}"
EVENT=$(python3 -c 'import uuid;print(uuid.uuid4())')
curl -4 -fsS http://localhost:8080/api/v1/logs/ingest -H "X-Ingestion-Key: $INGESTION_KEY" -H 'Content-Type: application/json' -d "{\"sourceId\":\"demo\",\"logs\":[{\"eventId\":\"$EVENT\",\"severity\":\"ERROR\",\"message\":\"payment timeout password=fake-secret\"}]}"
sleep 3
curl -4 -fsS http://localhost:8080/api/v1/logs/search -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d "$(python3 -c 'import datetime,json;now=datetime.datetime.now(datetime.timezone.utc);print(json.dumps({"from":(now-datetime.timedelta(hours=1)).isoformat(),"to":now.isoformat(),"text":"payment timeout"}))')" | python3 -c 'import json,sys;r=json.load(sys.stdin);assert r["hits"]["total"]["value"]>0;assert "fake-secret" not in json.dumps(r);print("Search and redaction verified")'
