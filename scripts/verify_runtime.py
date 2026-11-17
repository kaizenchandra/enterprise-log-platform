"""Run against the local Compose stack with TOKEN and INGESTION_KEY set."""
import datetime
import json
import os
import time
import urllib.request
import uuid

base = 'http://127.0.0.1:8080'


def call(path, body=None, ingest=False):
    headers = {'Content-Type': 'application/json'}
    headers.update({'X-Ingestion-Key': os.environ['INGESTION_KEY']} if ingest else {
        'Authorization': 'Bearer ' + os.environ['TOKEN']})
    req = urllib.request.Request(base + path, data=json.dumps(body).encode() if body else None, headers=headers)
    with urllib.request.urlopen(req, timeout=15) as r: return json.load(r)


now = datetime.datetime.now(datetime.timezone.utc);
event = str(uuid.uuid4())
b = {'sourceId': 'demo', 'logs': [
    {'eventId': event, 'timestamp': now.isoformat(), 'message': 'duplicatecheck ' + event, 'severity': 'ERROR'}]}
for _ in range(2): assert call('/api/v1/logs/ingest', b, True)['accepted'] == 1
q = {'from': (now - datetime.timedelta(minutes=5)).isoformat(), 'to': (now + datetime.timedelta(minutes=1)).isoformat(),
     'text': event}
for _ in range(20):
    result = call('/api/v1/logs/search', q)
    if result['hits']['total']['value']: break
    time.sleep(1)
assert result['hits']['total']['value'] == 1, result
print('Duplicate delivery: one indexed document')
req = urllib.request.Request(base + '/api/v1/logs/stream', headers={'Authorization': 'Bearer ' + os.environ['TOKEN']})
with urllib.request.urlopen(req, timeout=10) as r:
    assert r.readline().startswith(b': heartbeat')
    print('Authenticated SSE: heartbeat received')
