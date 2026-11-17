import datetime
import json
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

root = Path(__file__).resolve().parents[1]
data = urllib.parse.urlencode(
    dict(client_id='log-console', grant_type='password', username='demo', password='local-demo-only')).encode()
req = urllib.request.Request('http://127.0.0.1:8180/realms/logs/protocol/openid-connect/token', data,
                             headers={'Host': 'localhost:8180'})
token = json.load(urllib.request.urlopen(req))['access_token']


def call(path, body=None, key=None):
    headers = {'Content-Type': 'application/json'};
    headers.update({'X-Ingestion-Key': key} if key else {'Authorization': 'Bearer ' + token})
    req = urllib.request.Request('http://127.0.0.1:8080/api/v1/' + path,
                                 json.dumps(body).encode() if body is not None else None, headers)
    with urllib.request.urlopen(req, timeout=20) as r: return json.load(r)


source = 'policy-' + uuid.uuid4().hex[:12];
key = call('sources', {'id': source, 'service': source, 'environment': 'test'})['key']
now = datetime.datetime.now(datetime.timezone.utc);
old = now - datetime.timedelta(days=2);
marker = uuid.uuid4().hex
call('logs/ingest', {'sourceId': source, 'logs': [
    {'eventId': marker, 'timestamp': old.isoformat(), 'severity': 'INFO', 'message': marker}]}, key)
q = {'from': (now - datetime.timedelta(days=3)).isoformat(), 'to': now.isoformat(), 'text': marker}
for _ in range(30):
    if call('logs/search', q)['hits']['total']['value'] == 1: break
    time.sleep(1)
else:
    raise AssertionError('Policy test log not visible')
# This test changes only the demo tenant hot-retention policy; restore prior default in finally.
try:
    call('retention', {'days': '1', 'legalHold': 'true'});
    assert call('retention/run', {})['deleted'] == 0
    assert call('logs/search', q)['hits']['total']['value'] == 1
    call('retention', {'days': '1', 'legalHold': 'false'});
    assert call('retention/run', {})['deleted'] >= 1
    assert call('logs/search', q)['hits']['total']['value'] == 0
finally:
    call('retention', {'days': '30', 'legalHold': 'false'})
# A unique service isolates rule counts from other test sources.
rule = call('alert-rules', {'service': source, 'threshold': '1'})['id']
call('logs/ingest',
     {'sourceId': source, 'logs': [{'eventId': marker + '-alert', 'severity': 'ERROR', 'message': 'policy alert'}]},
     key)
for _ in range(45):
    rows = [r for r in call('alerts') if r['rule_id'] == rule and r['state'] == 'FIRING']
    if rows: break
    time.sleep(1)
else:
    raise AssertionError('Alert did not fire')
time.sleep(32)
assert len([r for r in call('alerts') if r['rule_id'] == rule and r['state'] == 'FIRING']) == 1
for _ in range(20):
    notifications = [r for r in call('alert-notifications') if r['rule_id'] == rule]
    if len(notifications) == 1 and notifications[0]['delivered']: break
    time.sleep(1)
else:
    raise AssertionError('Notification outbox delivery failed')
result = {'notification': 'one durable outbox record delivered to local synthetic receiver',
          'hot_legal_hold': 'preserved synthetic old record', 'hot_retention': 'deleted expired record',
          'retention_default': 'restored to 30 days', 'alert': 'fired once',
          'deduplication': 'no repeated FIRING transition on next evaluation'}
(root / 'docs/results/policy-test.json').write_text(json.dumps(result, indent=2));
print(json.dumps(result, indent=2))
