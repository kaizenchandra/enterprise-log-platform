import {NextRequest, NextResponse} from 'next/server';

export async function POST(r: NextRequest) {
    if (r.headers.get('origin') !== (process.env.PUBLIC_ORIGIN || r.nextUrl.origin)) return new Response(null, {status: 403});
    const {token} = await r.json();
    if (typeof token !== 'string' || token.length > 16000) return new Response(null, {status: 400});
    const res = NextResponse.json({ok: true});
    res.cookies.set('log_session', token, {
        httpOnly: true,
        secure: (process.env.PUBLIC_ORIGIN || r.nextUrl.origin).startsWith('https:'),
        sameSite: 'strict',
        path: '/',
        maxAge: 900
    });
    return res;
}

export async function DELETE(r: NextRequest) {
    if (r.headers.get('origin') !== (process.env.PUBLIC_ORIGIN || r.nextUrl.origin)) return new Response(null, {status: 403});
    const response = NextResponse.json({ok: true});
    response.cookies.delete('log_session');
    return response;
}