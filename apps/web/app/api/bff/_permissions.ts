import { NextRequest } from "next/server";

/**
 * M6-R02: BFF permission enforcement middleware.
 * Checks if the current user has the required permission for the route.
 */

type Role = "OWNER" | "ADMIN" | "EDITOR" | "REVIEWER" | "USER";

const ROLE_PERMISSIONS: Record<Role, string[]> = {
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
 * Extract role from request headers/cookies.
 * In production, this comes from the validated JWT claims.
 */
export function getRoleFromRequest(request: NextRequest): string | null {
  // Role is set by the auth middleware via x-nexora-role header
  return request.headers.get("x-nexora-role");
}

/**
 * Enforce a permission. Returns null if allowed, or a Response if denied.
 */
export function enforcePermission(
  request: NextRequest,
  permission: string,
): { allowed: boolean; role: string | null; response?: Response } {
  const role = getRoleFromRequest(request);
  if (!role) {
    return {
      allowed: false,
      role: null,
      response: Response.json(
        { code: "UNAUTHORIZED", message: "Authentication required.", traceId: null },
        { status: 401 },
      ),
    };
  }
  if (!hasPermission(role, permission)) {
    return {
      allowed: false,
      role,
      response: Response.json(
        { code: "PERMISSION_DENIED", message: `Requires permission: ${permission}`, traceId: null },
        { status: 403 },
      ),
    };
  }
  return { allowed: true, role };
}

/** Route → required permission mapping */
export const ROUTE_PERMISSIONS: Record<string, string> = {
  "/api/bff/knowledge": "knowledge.read",
  "/api/bff/rag": "rag.query",
  "/api/bff/feature-flags": "flags.read",
  "/api/bff/analytics": "analytics.read",
  "/api/bff/notifications": "notifications.read",
  "/api/bff/experiments": "experiments.read",
  "/api/bff/memberships": "members.read",
  "/api/bff/profile": "organization.read",
};
