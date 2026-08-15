import { NextRequest, NextResponse } from "next/server";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../_shared";

export async function POST(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  try {
    const body = await request.json() as { organizationId?: string; title?: string };
    if (!body.organizationId || !body.title?.trim()) {
      return NextResponse.json(
        { code: "VALIDATION_FAILED", message: "A title and organization are required.", traceId: null },
        { status: 400 },
      );
    }
    const session = await authenticatedClient();
    const result = await session.client.create(
      { title: body.title.trim() },
      { "X-Nexora-Organization-Id": body.organizationId },
    );
    return session.applyCookies(NextResponse.json(result.data, { status: 201, headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}
