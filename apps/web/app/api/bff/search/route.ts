import { NextRequest, NextResponse } from "next/server";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../_shared";

/** Search across authorized sources. */
export async function GET(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  const organizationId = request.nextUrl.searchParams.get("organizationId");
  const query = request.nextUrl.searchParams.get("query");
  const limit = request.nextUrl.searchParams.get("limit") ?? undefined;
  const cursor = request.nextUrl.searchParams.get("cursor") ?? undefined;
  if (!organizationId || !query) {
    return NextResponse.json({ code: "VALIDATION_FAILED", message: "organizationId and query are required.", traceId: null }, { status: 400 });
  }
  try {
    const session = await authenticatedClient();
    const result = await session.client.search(
      { query, limit: limit ? parseInt(limit, 10) : undefined, cursor },
      { "X-Nexora-Organization-Id": organizationId },
    );
    return session.applyCookies(NextResponse.json(result.data));
  } catch (error) {
    return problemResponse(error);
  }
}
