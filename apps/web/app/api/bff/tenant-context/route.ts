import { NextResponse, type NextRequest } from "next/server";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../_shared";

export async function POST(request: NextRequest) {
  const csrf = requireSameOrigin(request);
  if (csrf) return csrf;
  try {
    const body: unknown = await request.json();
    if (!body || typeof body !== "object" || typeof (body as { organizationId?: unknown }).organizationId !== "string") {
      return NextResponse.json({ code: "INVALID_REQUEST", message: "Choose an organization.", traceId: null }, { status: 400 });
    }
    const session = await authenticatedClient();
    const response = await session.client.resolveTenantContext({ organizationId: (body as { organizationId: string }).organizationId });
    return session.applyCookies(NextResponse.json(response.data));
  } catch (error) {
    return problemResponse(error);
  }
}
