import { NextRequest, NextResponse } from "next/server";
import {
  authenticatedPlatformSession,
  problemResponse,
  requireSameOrigin,
  safeProblemResponse,
} from "../../_shared";

const EVENTS = new Set(["PUBLICATION_INVALIDATED", "WORKFLOW_TRANSITIONED", "JOB_PROGRESS_CHANGED", "PRESENCE_CHANGED"]);

function descriptorBody(value: unknown) {
  if (!value || typeof value !== "object" || Array.isArray(value)) return null;
  const body = value as Record<string, unknown>;
  if (typeof body.organizationId !== "string" || typeof body.eventType !== "string" || !EVENTS.has(body.eventType)) {
    return null;
  }
  if (body.resourceId !== undefined && typeof body.resourceId !== "string") return null;
  return {
    organizationId: body.organizationId,
    request: {
      eventType: body.eventType,
      ...(body.resourceId ? { resourceId: body.resourceId } : {}),
    },
  };
}

export async function POST(request: NextRequest) {
  const csrf = requireSameOrigin(request);
  if (csrf) return csrf;

  const body = descriptorBody(await request.json().catch(() => null));
  if (!body) {
    return NextResponse.json(
      { code: "REQUEST_FAILED", message: "The Realtime channel request was incomplete.", traceId: null },
      { status: 400, headers: { "Cache-Control": "private, no-store" } },
    );
  }

  try {
    const session = await authenticatedPlatformSession();
    const upstream = await fetch(`${session.baseUrl}/api/v1/realtime/descriptors`, {
      method: "POST",
      headers: {
        Accept: "application/json",
        Authorization: `Bearer ${session.accessToken}`,
        "Content-Type": "application/json",
        "X-Nexora-Organization-Id": body.organizationId,
      },
      body: JSON.stringify(body.request),
    });
    const payload = await upstream.json().catch(() => null);
    if (!upstream.ok) {
      return session.applyCookies(safeProblemResponse(upstream.status, payload));
    }
    return session.applyCookies(
      NextResponse.json(payload, { headers: { "Cache-Control": "private, no-store" } }),
    );
  } catch (error) {
    return problemResponse(error);
  }
}
