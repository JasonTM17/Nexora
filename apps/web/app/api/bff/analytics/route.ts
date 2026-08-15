import { NextRequest, NextResponse } from "next/server";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../_shared";

/** Record an analytics event. */
export async function POST(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  try {
    const body = await request.json() as {
      organizationId?: string;
      eventType?: string;
      resourceType?: string;
      resourceId?: string;
      properties?: Record<string, unknown>;
      clientContext?: Record<string, unknown>;
      idempotencyKey?: string;
    };
    if (!body.organizationId || !body.eventType) {
      return NextResponse.json(
        { code: "VALIDATION_FAILED", message: "organizationId and eventType are required.", traceId: null },
        { status: 400 },
      );
    }
    const session = await authenticatedClient();
    await session.client.recordAnalyticsEvent(
      {
        eventType: body.eventType,
        resourceType: body.resourceType,
        resourceId: body.resourceId,
        properties: body.properties,
        clientContext: body.clientContext,
        idempotencyKey: body.idempotencyKey,
      },
      { "X-Nexora-Organization-Id": body.organizationId },
    );
    return session.applyCookies(NextResponse.json(null, { status: 202 }));
  } catch (error) {
    return problemResponse(error);
  }
}

/** Get analytics aggregation. */
export async function GET(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  const organizationId = request.nextUrl.searchParams.get("organizationId");
  const since = request.nextUrl.searchParams.get("since");
  if (!organizationId) {
    return NextResponse.json(
      { code: "VALIDATION_FAILED", message: "organizationId is required.", traceId: null },
      { status: 400 },
    );
  }
  try {
    const session = await authenticatedClient();
    const result = await session.client.aggregateAnalytics(
      { since: since ?? new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString() },
      { "X-Nexora-Organization-Id": organizationId },
    );
    return session.applyCookies(NextResponse.json(result.data));
  } catch (error) {
    return problemResponse(error);
  }
}
