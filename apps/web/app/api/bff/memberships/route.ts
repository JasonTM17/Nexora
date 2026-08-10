import { NextRequest, NextResponse } from "next/server";
import { authenticatedClient, problemResponse } from "../_shared";

export async function GET(request: NextRequest) {
  const organizationId = request.nextUrl.searchParams.get("organizationId");
  if (!organizationId) {
    return NextResponse.json(
      { code: "REQUEST_FAILED", message: "Choose an organization before managing members.", traceId: null },
      { status: 400 },
    );
  }

  try {
    const session = await authenticatedClient();
    const result = await session.client.listMemberships({ organizationId });
    return session.applyCookies(NextResponse.json(result.data, { headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}
