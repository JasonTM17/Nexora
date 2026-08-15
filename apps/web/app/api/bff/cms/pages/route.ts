import { NextRequest, NextResponse } from "next/server";
import { authenticatedClient, problemResponse, requirePermission, requireSameOrigin } from "../../_shared";

/** List all CMS pages for the tenant. */
export async function GET(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  const organizationId = request.nextUrl.searchParams.get("organizationId");
  if (!organizationId) {
    return NextResponse.json(
      { code: "VALIDATION_FAILED", message: "organizationId is required.", traceId: null },
      { status: 400 },
    );
  }
  const permCheck = await requirePermission(request, "page.read", organizationId);
  if (permCheck) return permCheck;
  try {
    const session = await authenticatedClient();
    const cursor = request.nextUrl.searchParams.get("cursor") ?? undefined;
    const limit = request.nextUrl.searchParams.get("limit") ?? "25";
    const result = await session.client.listCmsPages(
      { organizationId },
      { cursor, limit: parseInt(limit, 10) || 25 },
    );
    return session.applyCookies(NextResponse.json(result.data, { headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}

/** Create a new CMS page. */
export async function POST(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  try {
    const body = await request.json() as Record<string, unknown>;
    if (!body.organizationId || !body.title) {
      return NextResponse.json(
        { code: "VALIDATION_FAILED", message: "organizationId and title are required.", traceId: null },
        { status: 400 },
      );
    }
    const permCheck = await requirePermission(request, "page.create", String(body.organizationId));
    if (permCheck) return permCheck;
    const session = await authenticatedClient();
    const result = await session.client.createCmsPage(
      body as Parameters<typeof session.client.createCmsPage>[0],
      { organizationId: String(body.organizationId) },
    );
    return session.applyCookies(NextResponse.json(result.data, { status: 201, headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}
