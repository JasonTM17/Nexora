import { NextRequest } from "next/server";

/**
 * M6-R02: BFF permission enforcement.
 * Role-permission matrix for the 5 Nexora roles.
 */

export type Role = "OWNER" | "ADMIN" | "EDITOR" | "REVIEWER" | "USER";

export const ROLE_PERMISSIONS: Record<Role, string[]> = {
  OWNER: ["organization.read", "organization.manage", "members.read", "members.manage", "pages.read", "pages.create", "pages.publish", "knowledge.read", "knowledge.manage", "rag.query", "analytics.read", "flags.read", "flags.manage", "experiments.read", "experiments.manage", "notifications.read"],
  ADMIN: ["organization.read", "organization.manage", "members.read", "members.manage", "pages.read", "pages.create", "pages.publish", "knowledge.read", "knowledge.manage", "rag.query", "analytics.read", "flags.read", "flags.manage", "experiments.read", "experiments.manage", "notifications.read"],
  EDITOR: ["organization.read", "members.read", "pages.read", "pages.create", "knowledge.read", "knowledge.manage", "rag.query", "notifications.read"],
  REVIEWER: ["organization.read", "members.read", "pages.read", "knowledge.read", "rag.query", "notifications.read"],
  USER: ["organization.read", "members.read", "pages.read", "knowledge.read", "rag.query", "notifications.read"],
};

export function hasPermission(role: string, permission: string): boolean {
  return ROLE_PERMISSIONS[role as Role]?.includes(permission) ?? false;
}

/**
 * Get role for an organization by calling the access-context.
 * Returns null if no membership found.
 */
export async function getRoleForOrganization(
  request: NextRequest,
  organizationId: string,
): Promise<string | null> {
  try {
    const res = await fetch(
      `${request.nextUrl.origin}/api/bff/access-context?organizationId=${encodeURIComponent(organizationId)}`,
      { headers: { cookie: request.headers.get("cookie") ?? "" } },
    );
    if (!res.ok) return null;
    const data = await res.json() as {
      memberships?: Array<{ organizationId: string; role: string }>;
    };
    const membership = data.memberships?.find((m) => m.organizationId === organizationId)
      ?? data.memberships?.[0];
    return membership?.role ?? null;
  } catch {
    return null;
  }
}
