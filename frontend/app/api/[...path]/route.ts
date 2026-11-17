import {NextRequest} from 'next/server';

async function proxy(r: NextRequest, {params}: { params: Promise<{ path: string[] }> }) {
    const {path} = await params;
    const token = r.cookies.get('log_session')?.value;
    if (!token) return Response.json({error: 'Sign in required'}, {status: 401});
    if (r.method === 'POST' && r.headers.get('origin') !== (process.env.PUBLIC_ORIGIN || r.nextUrl.origin)) return new Response(null, {status: 403});
    const upstream = await fetch(`${process.env.BACKEND_URL || 'http://localhost:8080'}/api/v1/${path.map(encodeURIComponent).join('/')}?${r.nextUrl.searchParams}`, {
        method: r.method,
        headers: {Authorization: `Bearer ${token}`, 'Content-Type': 'application/json'},
        body: r.method === 'POST' ? await r.text() : undefined,
        cache: 'no-store'
    });
    return new Response(upstream.body, {
        status: upstream.status,
        headers: {'Content-Type': upstream.headers.get('Content-Type') || 'application/json'}
    })
}

export const GET = proxy;
export const POST = proxy;
