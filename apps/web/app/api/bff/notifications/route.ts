import { NextRequest, NextResponse } from "next/server";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../_shared";

/** List notifications for the authenticated user. */
export async function GET(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  const organizationId = request.nextUrl.searchParams.get("organizationId");
  const limit = request.nextUrl.searchParams.get("limit") ?? undefined;
  const cursor = request.nextUrl.searchParams.get("cursor") ?? undefined;
  if (!organizationId) {
    return NextResponse.json({ code: "VALIDATION_FAILED", message: "organizationId is required.", traceId: null }, { status: 400 });
  }
  try {
    const session = await authenticatedClient();
    const result = await session.client.listNotifications(
      { organizationId },
      { limit: limit ? parseInt(limit, 10) : undefined, cursor },
    );
    return session.applyCookies(NextResponse.json(result.data));
  } catch (error) {
    return problemResponse(error);
  }
}

/** Get unread count. */
export async function COUNT(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  const organizationId = request.nextUrl.searchParams.get("organizationId");
  if (!organizationId) {
    return NextResponse.json({ code: "VALIDATION_FAILED", message: "organizationId is required.", traceId: null }, { status: 400 });
  }
  try {
    const session = await authenticatedClient();
    const result = await session.client.unreadNotificationCount({ organizationId });
    return session.applyCookies(NextResponse.json(result.data));
  } catch (error) {
    return problemResponse(error);
  }
}

/** Mark a notification as read. */
export async function PATCH(request: NextRequest) {
  const originCheck = requireSameOrigin(request);
  if (originCheck) return originCheck;
  try {
    const body = await request.json() as { organizationId?: string; notificationId?: string };
    if (!body.organizationId || !body.notificationId) {
      return NextResponse.json({ code: "VALIDATION_FAILED", message: "organizationId and notificationId are required.", traceId: null }, { status: 400 });
    }
    const session = await authenticatedClient();
    await session.client.markNotificationRead({ notificationId: body.notificationId }, { organizationId: body.organizationId });
    return session.applyCookies(NextResponse.json({ updated: true }, { status: 204 }));
  } catch (error) {
    return problemResponse(error);
  }
}
