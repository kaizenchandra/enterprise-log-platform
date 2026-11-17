"""Real-stack regression, fault injection and benchmark. Requires local Compose stack."""
import concurrent.futures
import datetime
import json
import os
import statistics
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1];
BASE = 'http://127.0.0.1:8080';
report = {'checks': []}


def docker(*args): return subprocess.run(['docker', 'compose', '-f', str(ROOT / 'compose.yml'), *args], check=True,
                                         capture_output=True, text=True).stdout


def request(path, body=None, token=None, key=None, base=BASE):
    h = {'Content-Type': 'application/json'}
    if token: h['Authorization'] = 'Bearer ' + token
    if key: h['X-Ingestion-Key'] = key
    req = urllib.request.Request(base + path, data=json.dumps(body).encode() if body is not None else None, headers=h)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, json.load(r)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def login(client):
    data = urllib.parse.urlencode(
        dict(grant_type='password', client_id=client, username='demo', password='local-demo-only')).encode()
    with urllib.request.urlopen(
        urllib.request.Request('http://127.0.0.1:8180/realms/logs/protocol/openid-connect/token', data,
                               headers={'Host': 'localhost:8180'}), timeout=10) as r: return json.load(r)[
        'access_token']


def check(name, condition):
    assert condition, name
    report['checks'].append(name);
    print('PASS', name, flush=True)


def query(text):
    now = datetime.datetime.now(datetime.timezone.utc)
    return {'from': (now - datetime.timedelta(hours=1)).isoformat(),
            'to': (now + datetime.timedelta(minutes=1)).isoformat(), 'text': text, 'limit': 100}


def wait_count(marker, count, timeout=90):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        try:
            status, r = request('/api/v1/logs/search', query(marker), token)
            if status == 200 and r['hits']['total']['value'] == count: return r
        except Exception:
            pass
        time.sleep(1)
    raise AssertionError('visibility timeout: ' + marker)


def health():
    for _ in range(60):
        try:
            if request('/actuator/health')[0] == 200: return
        except Exception:
            pass
        time.sleep(1)
    raise AssertionError('API unavailable')


# Provision storage explicitly so a fresh CI stack uses safe mappings, not dynamic text fields.
for _ in range(60):
    init = subprocess.run(
        ['docker', 'compose', '-f', str(ROOT / 'compose.yml'), 'exec', '-T', 'opensearch', 'curl', '-fsS', '-X', 'PUT',
         'http://localhost:9200/_index_template/logs', '-H', 'Content-Type: application/json', '--data-binary', '@-'],
        input=(ROOT / 'infrastructure/opensearch/template.json').read_text(), capture_output=True, text=True)
    if init.returncode == 0: break
    time.sleep(1)
else:
    raise RuntimeError('Search template provisioning failed')
health()
for _ in range(60):
    try:
        token = login('log-console');other = login('log-other');viewer = login('log-viewer');break
    except Exception:
        time.sleep(1)
else:
    raise RuntimeError('OIDC startup failed')
source = 'test-' + uuid.uuid4().hex[:12]
status, r = request('/api/v1/sources', {'id': source, 'service': 'test-service', 'environment': 'test'}, token)
assert status == 200, (status, r)
key = r['key'];
os.environ['INGESTION_KEY'] = key
docker('up', '-d', '--force-recreate', 'collector')
collector_marker = 'collector' + uuid.uuid4().hex
payload = {'resourceLogs': [{'scopeLogs': [{'logRecords': [
    {'severityText': 'INFO', 'body': {'stringValue': collector_marker}, 'traceId': '0123456789abcdef0123456789abcdef',
     'spanId': '0123456789abcdef',
     'attributes': [{'key': 'event.id', 'value': {'stringValue': collector_marker}}]}]}]}]}
for _ in range(20):
    try:
        if request('/v1/logs', payload, base='http://127.0.0.1:4318')[0] == 200: break
    except Exception:
        pass
    time.sleep(1)
else:
    raise AssertionError('Collector unavailable')
collected = wait_count(collector_marker, 1)
check('OTLP Collector to searchable trace-correlated log',
      collected['hits']['hits'][0]['_source']['trace_id'] == '0123456789abcdef0123456789abcdef')
stamp = datetime.datetime.now(datetime.timezone.utc).isoformat()


def batch(marker, n=1): return {'sourceId': source, 'logs': [
    {'eventId': marker + '-' + str(i), 'timestamp': stamp, 'severity': 'ERROR',
     'message': marker + ' <script>window.pwned=1</script> password=fake-secret'} for i in range(n)]}


check('unauthorized search', request('/api/v1/logs/search', query('x'))[0] == 401)
check('invalid ingestion credential', request('/api/v1/logs/ingest', batch('x'), key='invalid')[0] == 401)
check('source identity forgery', request('/api/v1/logs/ingest', {'sourceId': 'forged', 'logs': []}, key=key)[0] == 403)
check('viewer cannot register source', request('/api/v1/sources', {'id': 'forged'}, viewer)[0] == 403)
check('forged JWT rejected', request('/api/v1/logs/search', query('x'), token=token[:-8] + 'invalid')[0] == 401)
check('range budget', request('/api/v1/logs/search', {'from': '2020-01-01T00:00:00Z', 'to': stamp}, token)[0] == 400)
q = query('x');
q['query'] = {'match_all': {}};
check('raw DSL rejected', request('/api/v1/logs/search', q, token)[0] == 400)
b = batch('oversize');
b['logs'][0]['message'] = 'x' * 1048577;
check('oversize payload', request('/api/v1/logs/ingest', b, key=key)[0] == 413)
marker = 'security' + uuid.uuid4().hex
b = batch(marker);
check('durable ingest boundary', request('/api/v1/logs/ingest', b, key=key)[0] == 202)
request('/api/v1/logs/ingest', b, key=key);
r = wait_count(marker, 1)
check('duplicate and redaction', 'fake-secret' not in json.dumps(r))
check('cross tenant search', request('/api/v1/logs/search', query(marker), other)[1]['hits']['total']['value'] == 0)
# Regression: acknowledged data must survive broker and worker container recreation.
buffered = 'buffered' + uuid.uuid4().hex
try:
    docker('stop', 'opensearch')
    check('buffer before broker recreation', request('/api/v1/logs/ingest', batch(buffered, 3), key=key)[0] == 202)
    docker('kill', '-s', 'SIGKILL', 'api')
    docker('up', '-d', '--force-recreate', 'kafka')
finally:
    docker('up', '-d', 'opensearch', 'api')
health()
check('broker recreation retained unindexed Kafka records', wait_count(buffered, 3) is not None)
# Search outage: accept to Kafka while OpenSearch is stopped, then recover without new delivery.
recovery = 'recovery' + uuid.uuid4().hex
try:
    docker('stop', 'opensearch');
    check('accept during search outage', request('/api/v1/logs/ingest', batch(recovery, 5), key=key)[0] == 202)
    docker('kill', '-s', 'SIGKILL', 'api')
finally:
    docker('up', '-d', 'opensearch', 'api')
health();
check('worker crash and storage outage recovery', wait_count(recovery, 5) is not None)
# Kafka outage must not return successful acceptance; preserve original IDs on retry.
ambiguous = 'outage' + uuid.uuid4().hex
try:
    docker('stop', 'kafka');
    status, _ = request('/api/v1/logs/ingest', batch(ambiguous), key=key);
    check('broker outage fails closed', status == 503)
finally:
    docker('up', '-d', 'kafka')
for _ in range(12):
    time.sleep(2)
    if request('/api/v1/logs/ingest', batch(ambiguous), key=key)[0] == 202: break
else:
    raise AssertionError('broker restoration timed out')
check('broker restored accepts retry', True);
wait_count(ambiguous, 1)
# Small measured closed-loop workload; no extrapolation to production-scale capacity.
bench = 'bench' + uuid.uuid4().hex;
latencies = [];
accepted = 0;
started = time.monotonic()


def produce(i):
    t = time.perf_counter();
    status, r = request('/api/v1/logs/ingest', batch(bench + '-' + str(i), 100), key=key);
    return status, r, time.perf_counter() - t


with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    for status, r, elapsed in pool.map(produce, range(20)):
        latencies.append(elapsed * 1000)
        if status == 202: accepted += r['accepted']
ack_seconds = time.monotonic() - started;
wait_count(bench, accepted);
visible_seconds = time.monotonic() - started
latencies.sort()
report['benchmark'] = {'offered_events': 2000, 'accepted_events': accepted, 'searchable_events': accepted, 'clients': 4,
                       'batch_size': 100, 'ack_seconds': ack_seconds, 'until_searchable_seconds': visible_seconds,
                       'accepted_events_per_second': accepted / ack_seconds, 'ack_ms_p50': statistics.median(latencies),
                       'ack_ms_p95': latencies[int(len(latencies) * .95) - 1], 'ack_ms_max': max(latencies),
                       'workload': 'closed loop, synthetic short messages, one tenant, local Docker; not a capacity limit or SLO proof'}
query_ms = []
for _ in range(30):
    start = time.perf_counter();
    status, result = request('/api/v1/logs/search', query(bench), token);
    assert status == 200;
    query_ms.append((time.perf_counter() - start) * 1000)
query_ms.sort();
report['query_benchmark'] = {'requests': 30, 'concurrency': 1,
                             'query': 'full-text marker + tenant + time range, 100 results',
                             'p50_ms': statistics.median(query_ms), 'p95_ms': query_ms[28], 'max_ms': max(query_ms)}
(ROOT / 'docs/results/system-test.json').write_text(json.dumps(report, indent=2))
# Private short-lived browser test fixture; do not commit credentials.
fixture = {'token': login('log-console'), 'marker': marker, 'key': key, 'source': source}
path = Path('/tmp/log-browser-fixture.json');
path.write_text(json.dumps(fixture));
path.chmod(0o600)
print(json.dumps(report['benchmark'], indent=2))
