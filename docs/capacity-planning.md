# Capacity assumptions, not measurements

At 1 million events/s × 1,000 bytes, ingress is 1 GB/s, 8 Gbit/s before overhead and 86.4 TB/day. Thus the requested
1–10 TB/day and sustained million-event/s scenarios are different workloads: 1–10 TB/day at 1 KB implies about
11,574–115,741 events/s.

Kafka 24-hour RF3 storage is approximately 259.2 TB before compression and headroom. Seven-day hot retention at an
unmeasured assumed 1.5× index amplification and one replica is 1.8144 PB. Thirty-day raw retention is 2.592 PB. Never
size from these assumptions without measuring actual mappings/compression/replicas.

Shard count = ceil(retained indexed bytes / measured safe shard size), with daily rollover, bounded active shards and
measured query concurrency. Collector buffer seconds = usable buffer bytes / ingress bytes per second. Cost per GB =
monthly compute + storage + requests + network / reconciled accepted GB. No monetary estimate is provided without
regional prices and workload measurements.

k6 script batches 100 events; RATE=100/1000/5000/10000 offers 10k/100k/500k/1m events/s. These are load-generator
settings, not supported throughput claims. The default tenant quota is 5k events/s, so high-scale tests require a
deliberate policy change, multiple tenants and sufficiently provisioned generators. Record generator dropped_iterations,
202 rate, Kafka lag, search counts after quiescence, CPU/GC, disk, p50/p95/p99. No benchmark has yet been executed.

## Executed workloads

The system suite records a small four-client 2,000-event burst with search reconciliation;
`benchmarks/measured_pipeline.py` adds a rate-paced 30-second 1,000-events/sec scenario with 500-character messages,
concurrent searches and container resource snapshots. Run after `scripts/system_test.py` creates the private fixture:

```bash
python3 benchmarks/measured_pipeline.py
# Optional pacing settings, subject to hard quotas and generator limits:
DURATION=60 RATE=1000 python3 benchmarks/measured_pipeline.py
```

The first paced run accepted/reconciled 30,000 events but recorded acknowledgment p99 135.46 ms, exceeding the 100 ms
design target for that workload. It is retained as results/paced-before-tuning.json. Subsequent code precompiles
redaction expressions and sets Kafka linger=5ms, batch.size=64KiB and LZ4 compression within the existing 32MiB producer
buffer. Repeated measurements are stored separately; ambient load, cold/warm JVM state and larger indexed datasets mean
these runs are not a controlled causal A/B experiment. No loss or throughput claim extends beyond the reconciled
synthetic run.

The batching/compression experiment regressed and was reverted. Final configuration uses original producer settings
(linger 0, 16KiB batch size, no compression) and precompiled bounded redaction. Final measured p99 is 23.30 ms for the
short paced run; this does not establish production capacity or a long-term SLO. See verification.md for all three runs
and timing limitations.
