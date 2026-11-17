"use client";
import {useEffect, useRef, useState} from 'react';

type Saved = { id: string; name: string; definition: { text?: string; service?: string; severity?: string } };
type Aggregate = { aggregations?: { timeline?: { buckets: { key_as_string: string; doc_count: number }[] } } };
type Hit = { _id: string; _source: Record<string, unknown>; sort: unknown[] };
export default function Page() {
    const [tab, T] = useState('Explorer'), [text, X] = useState(''), [service, S] = useState(''), [severity, V] = useState(''), [hours, H] = useState(1), [hits, L] = useState<Hit[]>([]), [cursor, C] = useState<unknown[] | null>(null), [status, M] = useState('Ready'), [data, D] = useState<unknown>(null), [token, K] = useState('');
    const stream = useRef<EventSource | null>(null);
    useEffect(() => () => stream.current?.close(), []);

    function tail() {
        if (stream.current) {
            stream.current.close();
            stream.current = null;
            M('Live tail stopped');
            return;
        }
        const es = new EventSource('/api/logs/stream?' + new URLSearchParams({service, severity}));
        stream.current = es;
        es.onmessage = e => {
            const event = JSON.parse(e.data);
            L(old => [{_id: event.log_id, _source: event, sort: []}, ...old].slice(0, 1000));
        };
        es.addEventListener('gap', () => M('Live window overflow: recover with historical search'));
        es.onerror = () => M('Reconnecting; search to recover gaps');
        es.onopen = () => M('Live tail connected · best effort · latest 1,000 events');
    }

    const query = () => ({
        from: new Date(Date.now() - hours * 3600000).toISOString(),
        to: new Date().toISOString(),
        text,
        service,
        severity,
        limit: 100
    });

    async function api(path: string, body?: unknown) {
        const r = await fetch('/api/' + path, {
            method: body ? 'POST' : 'GET',
            headers: {'Content-Type': 'application/json'},
            body: body ? JSON.stringify(body) : undefined
        });
        if (!r.ok) throw Error(`${r.status}: request failed`);
        return r.json()
    }

    async function search(next = false) {
        try {
            M('Searching…');
            const r = await api('logs/search', {...query(), cursor: next ? cursor : null});
            L(r.hits.hits);
            C(r.hits.hits.at(-1)?.sort || null);
            M(`${r.hits.total.value} matching events${r.timed_out ? ' · partial timeout' : ''}`)
        } catch (e) {
            M(String(e))
        }
    }

    async function load(path: string, body?: unknown) {
        try {
            D(await api(path, body))
        } catch (e) {
            M(String(e))
        }
    }

    return <main>
        <aside><h1>◈ Log Atlas</h1><small>Distributed log intelligence</small>
            <nav>{['Explorer', 'Analytics', 'Saved searches', 'Alerts', 'Sources', 'Archives', 'Admin'].map(t => <button
                key={t} aria-current={tab === t ? 'page' : undefined} onClick={() => {
                T(t);
                D(null);
                if (t === 'Sources') load('sources');
                if (t === 'Archives') load('archives');
                if (t === 'Alerts') load('alerts');
                if (t === 'Saved searches') load('saved-queries')
            }}>{t}</button>)}</nav>
            <p>Kafka buffered · tenant scoped</p></aside>
        <section>
            <header>
                <div><small>WORKSPACE / OBSERVABILITY</small><h2>{tab}</h2></div>
                <button onClick={() => fetch('/api/session', {method: 'DELETE'})}>Sign out</button>
            </header>
            <details>
                <summary>Authenticate with an OIDC access token</summary>
                <form onSubmit={async e => {
                    e.preventDefault();
                    const response = await fetch('/api/session', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({token})
                    });
                    if (!response.ok) {
                        M('Session failed: ' + response.status);
                        return;
                    }
                    K('');
                    M('Session established')
                }}><input type="password" aria-label="OIDC access token" value={token}
                          onChange={e => K(e.target.value)}/>
                    <button>Establish session</button>
                </form>
            </details>
            <div role="status">{status}</div>
            {(tab === 'Explorer' || tab === 'Analytics') && <>
                <div className="filters"><input aria-label="Search message" placeholder="Search log messages…"
                                                value={text} onChange={e => X(e.target.value)}/><input
                    aria-label="Service" placeholder="All services" value={service}
                    onChange={e => S(e.target.value)}/><select aria-label="Severity" value={severity}
                                                               onChange={e => V(e.target.value)}>{['', 'TRACE', 'DEBUG', 'INFO', 'WARNING', 'ERROR', 'FATAL'].map(v =>
                    <option key={v}>{v}</option>)}</select><select aria-label="Time range" value={hours}
                                                                   onChange={e => H(Number(e.target.value))}>{[1, 6, 24, 168].map(v =>
                    <option key={v} value={v}>Last {v} hours</option>)}</select>
                    <button onClick={() => tab === 'Explorer' ? search() : load('logs/aggregate', query())}>Run query
                    </button>
                </div>
                {tab === 'Explorer' && <>
                    <button onClick={tail}>Start / stop live tail</button>
                    <button onClick={() => load('saved-queries', {name: text || 'Saved search', query: query()})}>Save
                        search
                    </button>
                    <div className="results">{hits.map(h => <article key={h._id}>
                        <div className="row">
                            <time>{String(h._source.event_timestamp)}</time>
                            <strong
                                className={String(h._source.severity)}>{String(h._source.severity)}</strong><b>{String(h._source.service_name)}</b>
                        </div>
                        <pre>{String(h._source.message)}</pre>
                        <details>
                            <summary>Event details and trace context</summary>
                            <pre>{JSON.stringify(h._source, null, 2)}</pre>
                        </details>
                    </article>)}</div>
                    <button disabled={!cursor} onClick={() => search(true)}>Next page</button>
                </>}</>}
            {tab === 'Archives' && <div>
                <button onClick={() => load('archives', query())}>Archive current search page</button>
                <p>Archives contain redacted records. Restore is limited to the tenant's current hot-retention
                    window.</p>{Array.isArray(data) && data.map((archive: { id: string; event_count: number }) => <p
                key={archive.id}>{archive.id} · {archive.event_count} events <button
                onClick={() => load('archives/' + archive.id + '/restore', {})}>Restore</button></p>)}</div>}
            {tab === 'Admin' && <form onSubmit={e => {
                e.preventDefault();
                const f = new FormData(e.currentTarget);
                load('retention', {days: f.get('days')})
            }}><h3>Hot retention</h3><label>Days<input name="days" type="number" min="1" max="30" required/></label>
                <button>Update retention</button>
            </form>}
            {tab === 'Admin' && <form onSubmit={e => {
                e.preventDefault();
                const f = new FormData(e.currentTarget);
                load('sources', {id: f.get('id'), service: f.get('service'), environment: f.get('environment')})
            }}><h3>Register source</h3>{['id', 'service', 'environment'].map(k => <label key={k}>{k}<input name={k}
                                                                                                           required
                                                                                                           maxLength={64}/></label>)}
                <button>Create ingestion credential</button>
                <p>Credential is displayed once. Store it in a secret manager.</p></form>}
            {tab === 'Alerts' && <form onSubmit={e => {
                e.preventDefault();
                const f = new FormData(e.currentTarget);
                load('alert-rules', {service: f.get('service'), threshold: f.get('threshold')})
            }}><h3>Five minute ERROR threshold</h3><label>Service<input name="service" required/></label><label>Event
                threshold<input name="threshold" type="number" min="1" required/></label>
                <button>Create rule</button>
            </form>}
            {tab === 'Analytics' && data !== null && <div aria-label="Log count timeline"
                                                          className="timeline">{((data as Aggregate).aggregations?.timeline?.buckets || []).map(bucket =>
                <div key={bucket.key_as_string}>
                    <time>{bucket.key_as_string}</time>
                    <meter min={0}
                           max={Math.max(1, ...((data as Aggregate).aggregations?.timeline?.buckets || []).map(b => b.doc_count))}
                           value={bucket.doc_count}/>
                    <span>{bucket.doc_count}</span></div>)}</div>}
            {tab === 'Saved searches' && Array.isArray(data) && data.map((saved: Saved) => <button key={saved.id}
                                                                                                   onClick={() => {
                                                                                                       X(saved.definition.text || '');
                                                                                                       S(saved.definition.service || '');
                                                                                                       V(saved.definition.severity || '');
                                                                                                       T('Explorer');
                                                                                                       M('Saved filters loaded; select time range and run query')
                                                                                                   }}>Load {saved.name}</button>)}
            {data !== null && <pre className="panel">{JSON.stringify(data, null, 2)}</pre>}
            <footer>Accepted events become searchable after indexing and refresh. Counts reflect indexed events.
            </footer>
        </section>
    </main>
}
