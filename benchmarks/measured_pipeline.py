"""Rate-paced local workload. Requires private fixture produced by scripts/system_test.py."""
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

root = Path(__file__).resolve().parents[1];
fixture = json.loads(Path('/tmp/log-browser-fixture.json').read_text())
data = urllib.parse.urlencode(
    dict(client_id='log-console', grant_type='password', username='demo', password='local-demo-only')).encode()
req = urllib.request.Request('http://127.0.0.1:8180/realms/logs/protocol/openid-connect/token', data,
                             headers={'Host': 'localhost:8180'})
token = json.load(urllib.request.urlopen(req))['access_token'];
marker = 'paced' + uuid.uuid4().hex
seconds = int(os.environ.get('DURATION', '30'));
rate = int(os.environ.get('RATE', '1000'));
batchsize = 50;
requests = int(seconds * rate / batchsize)
started = time.monotonic();
wall = datetime.datetime.now(datetime.timezone.utc);
q = {'from': (wall - datetime.timedelta(minutes=1)).isoformat(),
     'to': (wall + datetime.timedelta(seconds=seconds + 120)).isoformat(), 'text': marker, 'limit': 100}


def call(path, body, ingest=False):
    headers = {'Content-Type': 'application/json'};
    headers.update({'X-Ingestion-Key': fixture['key']} if ingest else {'Authorization': 'Bearer ' + token})
    req = urllib.request.Request('http://127.0.0.1:8080/api/v1/' + path, json.dumps(body).encode(), headers)
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, json.load(r), (time.perf_counter() - start) * 1000
    except urllib.error.HTTPError as e:
        return e.code, {}, (time.perf_counter() - start) * 1000


def send(n):
    stamp = datetime.datetime.now(datetime.timezone.utc).isoformat()
    body = {'sourceId': fixture['source'], 'logs': [
        {'eventId': f'{marker}-{n}-{i}', 'timestamp': stamp, 'severity': 'INFO',
         'message': (marker + ' synthetic log ').ljust(500, 'x')} for i in range(batchsize)]}
    return call('logs/ingest', body, True)


query_samples = []


def probe():
    for _ in range(seconds):
        status, result, ms = call('logs/search', q)
        query_samples.append({'status': status, 'ms': ms,
                              'partial': bool(result.get('timed_out', False)) or result.get('_shards', {}).get('failed',
                                                                                                               0) > 0});
        time.sleep(1)


resource_samples = []


def resources():
    ids = subprocess.check_output(['docker', 'compose', '-f', str(root / 'compose.yml'), 'ps', '-q'], text=True).split()
    for _ in range(max(1, seconds // 5)):
        result = subprocess.run(['docker', 'stats', '--no-stream', '--format', '{{json .}}', *ids], capture_output=True,
                                text=True)
        resource_samples.append({'elapsed_seconds': time.monotonic() - started,
                                 'containers': [json.loads(line) for line in result.stdout.splitlines() if
                                                line.startswith('{')]})
        time.sleep(4)


with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
    probe_future = pool.submit(probe);
    resource_future = pool.submit(resources);
    futures = []
    for n in range(requests):
        delay = started + n * batchsize / rate - time.monotonic()
        if delay > 0: time.sleep(delay)
        futures.append(pool.submit(send, n))
    results = [future.result() for future in futures];
    ack_elapsed = time.monotonic() - started;
    probe_future.result();
    resource_future.result()
elapsed = time.monotonic() - started;
accepted = sum(r.get('accepted', 0) for status, r, _ in results if status == 202);
latencies = sorted(ms for status, _, ms in results if status == 202);
status_counts = {str(status): sum(1 for code, _, _ in results if code == status) for status, _, _ in results}
indexed = 0
for _ in range(120):
    status, r, _ = call('logs/search', q)
    if status == 200: indexed = r['hits']['total']['value']
    if indexed == accepted: break
    time.sleep(1)
else:
    raise AssertionError(f'Count mismatch: accepted={accepted}, indexed={indexed}')


def percentile(values, p): return values[min(len(values) - 1, int((len(values) - 1) * p))] if values else None


query_ms = sorted(s['ms'] for s in query_samples if s['status'] == 200 and not s['partial'])
report = {
    'workload': 'rate-paced local Docker; 500-character messages plus envelope; one tenant; 50 events/batch; <=8 HTTP workers including query probe',
    'offered_events': requests * batchsize, 'target_events_per_second': rate, 'duration_target_seconds': seconds,
    'elapsed_seconds': elapsed, 'ack_workload_seconds': ack_elapsed,
    'observation_window_note': 'elapsed includes concurrent probe and resource-sampler completion; ack_workload_seconds excludes that trailing wait',
    'accepted_events': accepted, 'searchable_events_after_drain': indexed,
    'accepted_events_per_second': accepted / ack_elapsed, 'all_searchable_after_seconds': time.monotonic() - started,
    'http_statuses': status_counts,
    'ack_ms': {'p50': percentile(latencies, .5), 'p95': percentile(latencies, .95), 'p99': percentile(latencies, .99),
               'max': max(latencies) if latencies else None},
    'concurrent_search': {'requests': len(query_samples), 'successful_complete': len(query_ms),
                          'p50_ms': percentile(query_ms, .5), 'p95_ms': percentile(query_ms, .95),
                          'max_ms': max(query_ms) if query_ms else None}, 'resource_samples': resource_samples,
    'limitations': 'One short local workload, no maximum-throughput claim. Search reconciliation is after drain, not per-event p95 visibility lag. No replication/AZ/GC/storage amplification evidence. Coarse CPU/memory snapshots are not profiles.'}
(root / 'docs/results/paced-benchmark.json').write_text(json.dumps(report, indent=2));
print(json.dumps(report, indent=2))
