import { NextResponse, type NextRequest } from "next/server";
import { callbackSession, SessionUnavailableError } from "../../lib/supabase-session";

export async function GET(request: NextRequest) {
  const destination = new URL("/account", request.url);
  const code = request.nextUrl.searchParams.get("code");
  if (!code) {
    destination.searchParams.set("auth", "required");
    return NextResponse.redirect(destination);
  }
  const response = NextResponse.redirect(destination);
  try {
    const { error } = await callbackSession(response, request.cookies).auth.exchangeCodeForSession(code);
    if (!error) return response;
  } catch (error) {
    if (!(error instanceof SessionUnavailableError)) throw error;
  }
  destination.searchParams.set("auth", "failed");
  return NextResponse.redirect(destination);
}
