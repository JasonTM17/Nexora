import { NextResponse, type NextRequest } from "next/server";
import { NexoraApiError, PlatformApiClient } from "../../../../../packages/contracts/src/generated/platform-api";
import { SessionUnavailableError, serverSession } from "../../lib/supabase-session";

const SAFE_MESSAGES: Readonly<Record<string, string>> = {
  AUTHENTICATION_REQUIRED: "Your session has expired. Sign in again to continue.",
  MEMBERSHIP_REQUIRED: "An active organization membership is required.",
  PERMISSION_DENIED: "That organization is not available to your current account.",
  TENANT_SELECTION_REQUIRED: "Choose one active organization to continue.",
  VERSION_CONFLICT: "This profile changed elsewhere. Reload it before trying again.",
};

function apiClient(token: string | undefined) {
  return new PlatformApiClient({
    baseUrl: process.env.NEXORA_PLATFORM_API_URL ?? "http://127.0.0.1:8080",
    accessToken: token,
  });
}

export async function authenticatedClient() {
  const session = await serverSession();
  return { client: apiClient(session.accessToken), applyCookies: session.applyCookies };
}

export function problemResponse(error: unknown) {
  if (error instanceof SessionUnavailableError) {
    return NextResponse.json(
      { code: "AUTHENTICATION_REQUIRED", message: SAFE_MESSAGES.AUTHENTICATION_REQUIRED, traceId: null },
      { status: 401, headers: { "Cache-Control": "private, no-store" } },
    );
  }
  if (error instanceof NexoraApiError) {
    const requestedCode = error.problem?.code ?? (error.status === 401 ? "AUTHENTICATION_REQUIRED" : "REQUEST_FAILED");
    const code = SAFE_MESSAGES[requestedCode] ? requestedCode : "REQUEST_FAILED";
    const message = SAFE_MESSAGES[code] ?? "We could not complete that request.";
    return NextResponse.json({ code, message, traceId: error.traceId }, { status: error.status });
  }
  return NextResponse.json(
    { code: "REQUEST_FAILED", message: "We could not reach Nexora. Try again.", traceId: null },
    { status: 502 },
  );
}

export function requireSameOrigin(request: NextRequest) {
  const origin = request.headers.get("origin");
  const fetchSite = request.headers.get("sec-fetch-site");
  if ((origin && origin !== request.nextUrl.origin) || (!origin && fetchSite !== "same-origin")) {
    return NextResponse.json(
      { code: "CSRF_ORIGIN_REQUIRED", message: "This request must come from Nexora.", traceId: null },
      { status: 403 },
    );
  }
  return null;
}
