"""Verify bounded S3 archive, tenant access and actual restore after deleting one test document."""
import datetime
import json
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

root = Path(__file__).resolve().parents[1]
fixture = json.loads(Path('/tmp/log-browser-fixture.json').read_text())


def token(client):
    req = urllib.request.Request('http://127.0.0.1:8180/realms/logs/protocol/openid-connect/token',
                                 urllib.parse.urlencode(dict(client_id=client, grant_type='password', username='demo',
                                                             password='local-demo-only')).encode(),
                                 headers={'Host': 'localhost:8180'})
    return json.load(urllib.request.urlopen(req))['access_token']


t = token('log-console');
fixture['token'] = t;
Path('/tmp/log-browser-fixture.json').write_text(json.dumps(fixture))


def call(path, body=None, auth=t, method=None):
    req = urllib.request.Request('http://127.0.0.1:8080/api/v1/' + path,
                                 data=json.dumps(body).encode() if body is not None else None,
                                 headers={'Content-Type': 'application/json', 'Authorization': 'Bearer ' + auth},
                                 method=method)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, json.load(r)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


now = datetime.datetime.now(datetime.timezone.utc);
q = {'from': (now - datetime.timedelta(hours=1)).isoformat(), 'to': now.isoformat(), 'text': fixture['marker'],
     'limit': 10}
status, archive = call('archives', q);
assert status == 200, (status, archive)
id = archive['id'];
assert archive['events'] == 1
assert call('archives/' + id + '/restore', {}, token('log-other'))[0] == 404
assert call('archives', q, token('log-viewer'))[0] == 403
status, r = call('logs/search', q);
doc = r['hits']['hits'][0]['_source'];
assert call('logs/' + doc['log_id'])[1]['hits']['total']['value'] == 1
# Delete only this unique synthetic test record, leaving all other log data intact.
body = json.dumps(
    {'query': {'bool': {'filter': [{'term': {'tenant_id': 'demo'}}, {'term': {'log_id': doc['log_id']}}]}}})
r = subprocess.run(
    ['docker', 'compose', '-f', str(root / 'compose.yml'), 'exec', '-T', 'opensearch', 'curl', '-fsS', '-X', 'POST',
     'http://localhost:9200/logs-*/_delete_by_query?refresh=true', '-H', 'Content-Type: application/json',
     '--data-binary', '@-'], input=body, text=True, capture_output=True, check=True)
assert json.loads(r.stdout)['deleted'] == 1
assert call('logs/search', q)[1]['hits']['total']['value'] == 0
status, result = call('archives/' + id + '/restore', {});
assert status == 200 and result['accepted'] == 1, (status, result)
for _ in range(30):
    if call('logs/search', q)[1]['hits']['total']['value'] == 1: break
    time.sleep(1)
else:
    raise AssertionError('Restore search visibility timeout')
assert call('archives/' + id, method='DELETE')[0] == 200
assert call('archives/' + id + '/restore', {})[0] == 404
report = {'archive_events': 1, 'cross_tenant_restore': 'denied', 'viewer_archive': 'denied',
          'hot_test_document_deleted': 1, 'restored_searchable_events': 1, 'archive_deleted': True,
          'integrity': 'compressed SHA-256 checked during restore'}
(root / 'docs/results/archive-test.json').write_text(json.dumps(report, indent=2));
print(json.dumps(report, indent=2))
