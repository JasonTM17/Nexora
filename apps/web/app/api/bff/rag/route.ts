import { NextRequest, NextResponse } from "next/server";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../_shared";

export async function POST(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  try {
    const body = await request.json() as { organizationId?: string; query?: string };
    if (!body.organizationId || !body.query?.trim()) {
      return NextResponse.json(
        { code: "VALIDATION_FAILED", message: "A query and organization are required.", traceId: null },
        { status: 400 },
      );
    }
    const session = await authenticatedClient();
    const result = await session.client.ask(
      { query: body.query.trim() },
      { "X-Nexora-Organization-Id": body.organizationId },
    );
    return session.applyCookies(NextResponse.json(result.data, { headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}
