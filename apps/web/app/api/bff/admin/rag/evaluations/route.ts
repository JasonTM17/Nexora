import { NextRequest, NextResponse } from "next/server";
import { authenticatedPlatformSession, problemResponse } from "../../../_shared";

export async function GET(request: NextRequest) {
  const organizationId = request.headers.get("X-Nexora-Organization-Id") ?? request.nextUrl.searchParams.get("organizationId");
  if (!organizationId) {
    return NextResponse.json(
      { code: "TENANT_SELECTION_REQUIRED", message: "An active organization is required.", traceId: null },
      { status: 400 },
    );
  }
  try {
    const { accessToken, baseUrl, applyCookies } = await authenticatedPlatformSession();
    const response = await fetch(`${baseUrl}/api/v1/rag/evaluation`, {
      headers: {
        Accept: "application/json",
        Authorization: accessToken ? `Bearer ${accessToken}` : "",
        "X-Nexora-Organization-Id": organizationId,
      },
    });
    const data = await response.json();
    if (!response.ok) {
      return NextResponse.json(data, { status: response.status });
    }
    return applyCookies(NextResponse.json(data, { headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}
