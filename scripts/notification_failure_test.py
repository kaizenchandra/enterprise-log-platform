import json
import subprocess
import time
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

root = Path(__file__).resolve().parents[1]
req = urllib.request.Request('http://127.0.0.1:8180/realms/logs/protocol/openid-connect/token', urllib.parse.urlencode(
    dict(client_id='log-console', grant_type='password', username='demo', password='local-demo-only')).encode(),
                             headers={'Host': 'localhost:8180'})
token = json.load(urllib.request.urlopen(req))['access_token']


def call(path, body=None, key=None):
    headers = {'Content-Type': 'application/json'};
    headers.update({'X-Ingestion-Key': key} if key else {'Authorization': 'Bearer ' + token})
    req = urllib.request.Request('http://127.0.0.1:8080/api/v1/' + path,
                                 json.dumps(body).encode() if body is not None else None, headers)
    with urllib.request.urlopen(req, timeout=20) as response: return json.load(response)


def docker(*args): subprocess.run(['docker', 'compose', '-f', str(root / 'compose.yml'), *args], check=True,
                                  capture_output=True)


source = 'notify-' + uuid.uuid4().hex[:12];
key = call('sources', {'id': source, 'service': source, 'environment': 'test'})['key'];
rule = call('alert-rules', {'service': source, 'threshold': '1'})['id']
try:
    docker('stop', 'payment-service')
    call('logs/ingest', {'sourceId': source, 'logs': [
        {'eventId': str(uuid.uuid4()), 'severity': 'ERROR', 'message': 'synthetic notification outage'}]}, key)
    for _ in range(60):
        rows = [r for r in call('alert-notifications') if r['rule_id'] == rule]
        if rows and rows[0]['attempts'] >= 1:
            assert not rows[0]['delivered'];
            notification = rows[0]['id'];
            break
        time.sleep(1)
    else:
        raise AssertionError('Failed notification attempt not observed')
finally:
    docker('up', '-d', 'payment-service')
for _ in range(60):
    rows = [r for r in call('alert-notifications') if r['rule_id'] == rule]
    if rows and rows[0]['delivered']:
        assert len(rows) == 1 and rows[0]['id'] == notification and rows[0]['attempts'] >= 2;
        break
    time.sleep(1)
else:
    raise AssertionError('Notification recovery failed')
result = {'receiver_outage': 'observed failed attempt', 'durable_retry': 'delivered after receiver restart',
          'same_notification_id': True, 'outbox_rows': len(rows), 'attempts': rows[0]['attempts']}
(root / 'docs/results/notification-failure.json').write_text(json.dumps(result, indent=2));
print(json.dumps(result, indent=2))
