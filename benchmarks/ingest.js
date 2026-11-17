import http from 'k6/http';
import {check} from 'k6';

export const options = {
    scenarios: {
        ingest: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE || 100),
            timeUnit: '1s',
            duration: __ENV.DURATION || '30s',
            preAllocatedVUs: 20,
            maxVUs: 200
        }
    }
};
export default function () {
    const logs = Array.from({length: 100}, (_, i) => ({
        eventId: `${__VU}-${__ITER}-${i}-${Date.now()}`,
        severity: 'INFO',
        message: 'benchmark '.padEnd(500, 'x')
    }));
    const r = http.post(`${__ENV.URL || 'http://localhost:8080'}/api/v1/logs/ingest`, JSON.stringify({
        sourceId: __ENV.SOURCE || 'demo',
        logs
    }), {headers: {'Content-Type': 'application/json', 'X-Ingestion-Key': __ENV.INGESTION_KEY}});
    check(r, {'Kafka acknowledged': r => r.status === 202});
}
