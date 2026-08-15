"use client";

import { useCallback, useEffect, useState } from "react";
import type { Permission } from "../lib/permissions";

interface AccessContextResponse {
  memberships: ReadonlyArray<{
    organizationId: string;
    role: string;
  }>;
  permissions?: string[];
  tenantSelectionRequired?: boolean;
}

/**
 * Hook to check if the current user has a permission.
 * Reads from the access-context BFF route.
 */
export function usePermission(permission: Permission, organizationId?: string): boolean {
  const [hasPermission, setHasPermission] = useState(false);

  const check = useCallback(async () => {
    try {
      const params = organizationId ? `?organizationId=${encodeURIComponent(organizationId)}` : "";
      const res = await fetch(`/api/bff/access-context${params}`, { credentials: "same-origin" });
      if (!res.ok) { setHasPermission(false); return; }
      const data: AccessContextResponse = await res.json();
      // Check explicit permissions first, then fall back to role-based
      if (data.permissions && data.permissions.length > 0) {
        setHasPermission(data.permissions.includes(permission));
      } else if (data.memberships && data.memberships.length > 0) {
        const role = data.memberships[0]?.role as import("../lib/permissions").Role;
        const { roleHasPermission } = await import("../lib/permissions");
        setHasPermission(roleHasPermission(role, permission));
      } else {
        setHasPermission(false);
      }
    } catch {
      setHasPermission(false);
    }
  }, [permission, organizationId]);

  useEffect(() => { void check(); }, [check]);

  return hasPermission;
}

/**
 * Component that conditionally renders children based on permission.
 */
export function PermissionGate({
  permission,
  organizationId,
  children,
  fallback = null,
}: {
  permission: Permission;
  organizationId?: string;
  children: React.ReactNode;
  fallback?: React.ReactNode;
}) {
  const hasPermission = usePermission(permission, organizationId);
  return <>{hasPermission ? children : fallback}</>;
}
