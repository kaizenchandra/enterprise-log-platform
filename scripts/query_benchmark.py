import datetime
import json
import statistics
import time
import urllib.request
from pathlib import Path

fixture = json.loads(Path('/tmp/log-browser-fixture.json').read_text());
now = datetime.datetime.now(datetime.timezone.utc)
q = {'from': (now - datetime.timedelta(hours=1)).isoformat(), 'to': now.isoformat(), 'text': fixture['marker'],
     'limit': 100};
samples = []
for _ in range(30):
    start = time.perf_counter()
    req = urllib.request.Request('http://127.0.0.1:8080/api/v1/logs/search', json.dumps(q).encode(),
                                 headers={'Content-Type': 'application/json',
                                          'Authorization': 'Bearer ' + fixture['token']})
    with urllib.request.urlopen(req, timeout=10) as response:
        result = json.load(response);
        assert not result['timed_out'] and result['_shards']['failed'] == 0
    samples.append((time.perf_counter() - start) * 1000)
samples.sort();
report = {'requests': 30, 'concurrency': 1,
          'query': 'tenant + one-hour range + full-text unique marker; warm local index',
          'matching_events': result['hits']['total']['value'], 'p50_ms': statistics.median(samples),
          'p95_ms': samples[28], 'max_ms': max(samples),
          'claim': 'small serial sample, not a loaded-query SLO or cluster capacity benchmark'}
(Path(__file__).resolve().parents[1] / 'docs/results/query-benchmark.json').write_text(json.dumps(report, indent=2));
print(json.dumps(report, indent=2))
