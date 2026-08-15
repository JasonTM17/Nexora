import { NextRequest, NextResponse } from "next/server";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../_shared";

export async function GET(request: NextRequest) {
  const organizationId = request.nextUrl.searchParams.get("organizationId");
  if (!organizationId) {
    return NextResponse.json(
      { code: "REQUEST_FAILED", message: "Choose an organization before browsing knowledge.", traceId: null },
      { status: 400 },
    );
  }
  const cursor = request.nextUrl.searchParams.get("cursor") ?? undefined;
  const limit = Number(request.nextUrl.searchParams.get("limit") ?? "25");
  try {
    const session = await authenticatedClient();
    const result = await session.client.listKnowledgeBases(
      { "X-Nexora-Organization-Id": organizationId },
      { cursor, limit: Number.isFinite(limit) && limit > 0 && limit <= 100 ? limit : undefined },
    );
    return session.applyCookies(NextResponse.json(result.data, { headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}

export async function POST(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  try {
    const body = await request.json() as { organizationId?: string; name?: string; description?: string };
    if (!body.organizationId || !body.name?.trim()) {
      return NextResponse.json(
        { code: "VALIDATION_FAILED", message: "A name and organization are required.", traceId: null },
        { status: 400 },
      );
    }
    const session = await authenticatedClient();
    const result = await session.client.createKnowledgeBase(
      { name: body.name.trim(), description: body.description?.trim() || undefined },
      { "X-Nexora-Organization-Id": body.organizationId },
    );
    return session.applyCookies(NextResponse.json(result.data, { status: 201, headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}
