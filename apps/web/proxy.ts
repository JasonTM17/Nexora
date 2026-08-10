import { NextResponse, type NextRequest } from "next/server";

function supabaseConnectSources() {
  const configured = process.env.NEXT_PUBLIC_SUPABASE_URL;
  if (!configured) return [];
  const url = new URL(configured);
  const websocket = new URL(configured);
  websocket.protocol = url.protocol === "http:" ? "ws:" : "wss:";
  return [url.origin, websocket.origin];
}

function privateCsp(nonce: string) {
  return [
    "default-src 'self'",
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'`,
    "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data:",
    "font-src 'self'",
    ["connect-src 'self'", ...supabaseConnectSources()].join(" "),
    "object-src 'none'",
    "frame-ancestors 'none'",
    "base-uri 'self'",
    "form-action 'self'",
  ].join("; ");
}

/**
 * Sensitive product surfaces are dynamic and no-store, so each request can
 * receive a nonce that Next automatically applies to its hydration scripts.
 */
export function proxy(request: NextRequest) {
  const nonce = btoa(crypto.randomUUID());
  const contentSecurityPolicy = privateCsp(nonce);
  const requestHeaders = new Headers(request.headers);

  requestHeaders.set("x-nonce", nonce);
  requestHeaders.set("Content-Security-Policy", contentSecurityPolicy);

  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set("Content-Security-Policy", contentSecurityPolicy);
  response.headers.set("Cache-Control", "private, no-store, max-age=0");
  return response;
}

export const config = {
  matcher: ["/studio/:path*", "/ai/:path*", "/builder/:path*", "/account/:path*", "/auth/callback", "/api/bff/:path*"],
};
