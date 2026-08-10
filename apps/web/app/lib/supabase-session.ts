import { createServerClient, type CookieOptions } from "@supabase/ssr";
import { cookies } from "next/headers";
import { NextResponse } from "next/server";

type PendingCookie = { name: string; value: string; options: CookieOptions };

export class SessionUnavailableError extends Error {
  constructor() {
    super("No verified server session is available.");
    this.name = "SessionUnavailableError";
  }
}

function credentials() {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY;
  if (!url || !key) throw new SessionUnavailableError();
  return { url, key };
}

function cookieOptions(options: CookieOptions): CookieOptions {
  return { ...options, httpOnly: true, path: "/", sameSite: "lax", secure: process.env.NODE_ENV === "production" };
}

export async function serverSession() {
  const cookieStore = await cookies();
  const pending: PendingCookie[] = [];
  const { url, key } = credentials();
  const supabase = createServerClient(url, key, {
    auth: { autoRefreshToken: false, persistSession: false },
    cookies: {
      getAll: () => cookieStore.getAll(),
      setAll: (items) => items.forEach(({ name, value, options }) => pending.push({ name, value, options: cookieOptions(options) })),
    },
    cookieOptions: cookieOptions({}),
  });
  const { data: claims, error: claimsError } = await supabase.auth.getClaims();
  const { data: { session }, error: sessionError } = await supabase.auth.getSession();
  if (claimsError || !claims?.claims || sessionError || !session?.access_token) throw new SessionUnavailableError();
  return {
    accessToken: session.access_token,
    applyCookies(response: NextResponse) {
      pending.forEach(({ name, value, options }) => response.cookies.set(name, value, options));
      response.headers.set("Cache-Control", "private, no-store");
      return response;
    },
  };
}

export function callbackSession(response: NextResponse, requestCookies: { getAll(): { name: string; value: string }[] }) {
  const { url, key } = credentials();
  return createServerClient(url, key, {
    auth: { autoRefreshToken: false, persistSession: true },
    cookies: {
      getAll: () => requestCookies.getAll(),
      setAll: (items) => items.forEach(({ name, value, options }) => response.cookies.set(name, value, cookieOptions(options))),
    },
    cookieOptions: cookieOptions({}),
  });
}
