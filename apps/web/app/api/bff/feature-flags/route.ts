import { NextRequest, NextResponse } from "next/server";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../_shared";

/** Evaluate a feature flag for the authenticated subject. */
export async function GET(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;

  const flagKey = request.nextUrl.searchParams.get("flagKey");
  const organizationId = request.nextUrl.searchParams.get("organizationId");
  if (!flagKey || !organizationId) {
    return NextResponse.json(
      { code: "VALIDATION_FAILED", message: "flagKey and organizationId are required.", traceId: null },
      { status: 400 },
    );
  }

  try {
    const session = await authenticatedClient();
    const result = await session.client.evaluateFlag(
      { flagKey },
      { "X-Nexora-Organization-Id": organizationId },
    );
    return session.applyCookies(NextResponse.json(result.data, { headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}

/** List all flags for the tenant. */
export async function LIST(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;

  const organizationId = request.nextUrl.searchParams.get("organizationId");
  if (!organizationId) {
    return NextResponse.json(
      { code: "VALIDATION_FAILED", message: "organizationId is required.", traceId: null },
      { status: 400 },
    );
  }

  try {
    const session = await authenticatedClient();
    const result = await session.client.listFlags(
      {},
      { "X-Nexora-Organization-Id": organizationId },
    );
    return session.applyCookies(NextResponse.json(result.data));
  } catch (error) {
    return problemResponse(error);
  }
}
