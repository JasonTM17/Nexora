import { NextRequest, NextResponse } from "next/server";
import type { MembershipMutationRequest, MembershipStatus, TenantRole } from "../../../../../../../packages/contracts/src/generated/platform-api";
import { authenticatedClient, problemResponse, requireSameOrigin } from "../../_shared";

function mutationBody(value: unknown): { organizationId: string; mutation: MembershipMutationRequest } | null {
  if (!value || typeof value !== "object" || Array.isArray(value)) return null;
  const body = value as Record<string, unknown>;
  const expectedVersion = body.expectedVersion;
  if (typeof body.organizationId !== "string" || typeof expectedVersion !== "number" || !Number.isSafeInteger(expectedVersion) || expectedVersion < 1) return null;
  if (typeof body.role === "string" && body.status === undefined) {
    return { organizationId: body.organizationId, mutation: { expectedVersion, role: body.role as TenantRole } };
  }
  if (typeof body.status === "string" && body.role === undefined) {
    return { organizationId: body.organizationId, mutation: { expectedVersion, status: body.status as MembershipStatus } };
  }
  return null;
}

export async function PATCH(request: NextRequest, { params }: { params: Promise<{ membershipId: string }> }) {
  const csrf = requireSameOrigin(request);
  if (csrf) return csrf;

  const body = mutationBody(await request.json().catch(() => null));
  if (!body) {
    return NextResponse.json(
      { code: "REQUEST_FAILED", message: "The member change was incomplete.", traceId: null },
      { status: 400 },
    );
  }

  try {
    const session = await authenticatedClient();
    const result = await session.client.updateMembership(await params, body.mutation, { organizationId: body.organizationId });
    return session.applyCookies(NextResponse.json(result.data, { headers: { "Cache-Control": "private, no-store" } }));
  } catch (error) {
    return problemResponse(error);
  }
}
