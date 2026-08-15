import { NextResponse, type NextRequest } from "next/server";
import { NexoraApiError, PlatformApiClient } from "../../../../../packages/contracts/src/generated/platform-api";
import { SessionUnavailableError, serverSession } from "../../lib/supabase-session";
import { getRoleForOrganization, hasPermission } from "./_permissions";

const SAFE_MESSAGES: Readonly<Record<string, string>> = {
  AUTHENTICATION_REQUIRED: "Your session has expired. Sign in again to continue.",
  MEMBERSHIP_REQUIRED: "An active organization membership is required.",
  PERMISSION_DENIED: "That organization is not available to your current account.",
  REALTIME_AUTH_REFRESH_REQUIRED: "Realtime authorization needs a fresh sign-in.",
  REALTIME_DEGRADED_REFETCH_REQUIRED: "Realtime updates are degraded. Refresh durable data before continuing.",
  REALTIME_DESCRIPTOR_DENIED: "Realtime updates are not available for that channel.",
  REALTIME_STALE_DESCRIPTOR: "Realtime authorization changed. Refresh the page before reconnecting.",
  TENANT_SELECTION_REQUIRED: "Choose one active organization to continue.",
  VERSION_CONFLICT: "This profile changed elsewhere. Reload it before trying again.",
};

export function platformApiBaseUrl() {
  return process.env.NEXORA_PLATFORM_API_URL ?? "http://127.0.0.1:8080";
}

function apiClient(token: string | undefined) {
  return new PlatformApiClient({
    baseUrl: platformApiBaseUrl(),
    accessToken: token,
  });
}

export async function authenticatedClient() {
  const session = await serverSession();
  return { client: apiClient(session.accessToken), applyCookies: session.applyCookies };
}

export async function authenticatedPlatformSession() {
  const session = await serverSession();
  return { accessToken: session.accessToken, baseUrl: platformApiBaseUrl(), applyCookies: session.applyCookies };
}

export function safeProblemResponse(status: number, problem: unknown) {
  const body = problem && typeof problem === "object" && !Array.isArray(problem) ? problem as Record<string, unknown> : {};
  const requestedCode = typeof body.code === "string" ? body.code : "REQUEST_FAILED";
  const code = SAFE_MESSAGES[requestedCode] ? requestedCode : "REQUEST_FAILED";
  const message = SAFE_MESSAGES[code] ?? "We could not complete that request.";
  const traceId = typeof body.traceId === "string" ? body.traceId : null;
  return NextResponse.json({ code, message, traceId }, { status, headers: { "Cache-Control": "private, no-store" } });
}

export function problemResponse(error: unknown) {
  if (error instanceof SessionUnavailableError) {
    return NextResponse.json(
      { code: "AUTHENTICATION_REQUIRED", message: SAFE_MESSAGES.AUTHENTICATION_REQUIRED, traceId: null },
      { status: 401, headers: { "Cache-Control": "private, no-store" } },
    );
  }
  if (error instanceof NexoraApiError) {
    return safeProblemResponse(error.status, error.problem ?? {
      code: error.status === 401 ? "AUTHENTICATION_REQUIRED" : "REQUEST_FAILED",
      traceId: error.traceId,
    });
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

/**
 * Enforce a permission for the current request.
 * Returns null if allowed, or a Response if denied.
 * Fetches the role from access-context for the given organization.
 */
export async function requirePermission(
  request: NextRequest,
  permission: string,
  organizationId: string,
): Promise<Response | null> {
  const role = await getRoleForOrganization(request, organizationId);
  if (!role) {
    return NextResponse.json(
      { code: "UNAUTHORIZED", message: "Authentication required.", traceId: null },
      { status: 401, headers: { "Cache-Control": "private, no-store" } },
    );
  }
  if (!hasPermission(role, permission)) {
    return NextResponse.json(
      { code: "PERMISSION_DENIED", message: `Requires permission: ${permission}`, traceId: null },
      { status: 403, headers: { "Cache-Control": "private, no-store" } },
    );
  }
  return null;
}
