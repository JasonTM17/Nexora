import { NextRequest, NextResponse } from "next/server";
import { authenticatedPlatformSession, problemResponse, requireSameOrigin } from "../../../_shared";

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
    const response = await fetch(`${baseUrl}/api/v1/rag/feedback`, {
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

export async function POST(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  try {
    const body = await request.json() as { organizationId?: string; runId?: string; rating?: string; comment?: string };
    if (!body.organizationId || !body.runId || !body.rating) {
      return NextResponse.json(
        { code: "VALIDATION_FAILED", message: "Run ID, rating and organization are required.", traceId: null },
        { status: 400 },
      );
    }
    const { accessToken, baseUrl, applyCookies } = await authenticatedPlatformSession();
    const response = await fetch(`${baseUrl}/api/v1/rag/feedback`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
        Authorization: accessToken ? `Bearer ${accessToken}` : "",
        "X-Nexora-Organization-Id": body.organizationId,
      },
      body: JSON.stringify({ runId: body.runId, rating: body.rating, comment: body.comment ?? "" }),
    });
    const data = await response.json();
    if (!response.ok) {
      return NextResponse.json(data, { status: response.status });
    }
    return applyCookies(NextResponse.json(data, { status: 201, headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}

export async function DELETE(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  const organizationId = request.headers.get("X-Nexora-Organization-Id") ?? request.nextUrl.searchParams.get("organizationId");
  const feedbackId = request.nextUrl.searchParams.get("feedbackId");
  if (!organizationId || !feedbackId) {
    return NextResponse.json(
      { code: "VALIDATION_FAILED", message: "Organization ID and feedback ID are required.", traceId: null },
      { status: 400 },
    );
  }
  try {
    const { accessToken, baseUrl, applyCookies } = await authenticatedPlatformSession();
    const response = await fetch(`${baseUrl}/api/v1/rag/feedback/${encodeURIComponent(feedbackId)}`, {
      method: "DELETE",
      headers: {
        Accept: "application/json",
        Authorization: accessToken ? `Bearer ${accessToken}` : "",
        "X-Nexora-Organization-Id": organizationId,
      },
    });
    if (!response.ok) {
      const data = await response.json();
      return NextResponse.json(data, { status: response.status });
    }
    return applyCookies(NextResponse.json({ success: true }, { headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}
