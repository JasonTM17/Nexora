import { cookies } from "next/headers";
import { NextResponse, type NextRequest } from "next/server";
import { NexoraApiError, PlatformApiClient } from "../../../../../packages/contracts/src/generated/platform-api";

const ACCESS_COOKIE = "nexora_access_token";

function apiClient(token: string | undefined) {
  return new PlatformApiClient({
    baseUrl: process.env.NEXORA_PLATFORM_API_URL ?? "http://127.0.0.1:8080",
    accessToken: token,
  });
}

export async function authenticatedClient() {
  const token = (await cookies()).get(ACCESS_COOKIE)?.value;
  return apiClient(token);
}

export function problemResponse(error: unknown) {
  if (error instanceof NexoraApiError) {
    const code = error.problem?.code ?? (error.status === 401 ? "AUTHENTICATION_REQUIRED" : "REQUEST_FAILED");
    const message = code === "AUTHENTICATION_REQUIRED"
      ? "Your session has expired. Sign in again to continue."
      : error.problem?.message ?? "We could not complete that request.";
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
