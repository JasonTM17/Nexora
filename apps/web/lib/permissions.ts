/**
 * M6-R02: Comprehensive permission system.
 *
 * Permission keys follow the pattern `resource.action`.
 * Roles are assigned permissions. The permission matrix is resolved
 * server-side and exposed via /api/bff/access-context.
 */

export type Permission =
  | "organization.read"
  | "organization.manage"
  | "members.read"
  | "members.manage"
  | "pages.read"
  | "pages.create"
  | "pages.publish"
  | "knowledge.read"
  | "knowledge.manage"
  | "rag.query"
  | "analytics.read"
  | "flags.read"
  | "flags.manage"
  | "experiments.read"
  | "experiments.manage"
  | "notifications.read";

export type Role = "OWNER" | "ADMIN" | "EDITOR" | "REVIEWER" | "USER";

/** Default role → permission mapping. Server is authority; this is for UI hints. */
export const ROLE_PERMISSIONS: Record<Role, Permission[]> = {
  OWNER: [
    "organization.read", "organization.manage",
    "members.read", "members.manage",
    "pages.read", "pages.create", "pages.publish",
    "knowledge.read", "knowledge.manage",
    "rag.query",
    "analytics.read",
    "flags.read", "flags.manage",
    "experiments.read", "experiments.manage",
    "notifications.read",
  ],
  ADMIN: [
    "organization.read", "organization.manage",
    "members.read", "members.manage",
    "pages.read", "pages.create", "pages.publish",
    "knowledge.read", "knowledge.manage",
    "rag.query",
    "analytics.read",
    "flags.read", "flags.manage",
    "experiments.read", "experiments.manage",
    "notifications.read",
  ],
  EDITOR: [
    "organization.read",
    "members.read",
    "pages.read", "pages.create",
    "knowledge.read", "knowledge.manage",
    "rag.query",
    "notifications.read",
  ],
  REVIEWER: [
    "organization.read",
    "members.read",
    "pages.read",
    "knowledge.read",
    "rag.query",
    "notifications.read",
  ],
  USER: [
    "organization.read",
    "members.read",
    "pages.read",
    "knowledge.read",
    "rag.query",
    "notifications.read",
  ],
};

/** Human-readable permission labels (English). Use i18n in UI. */
export const PERMISSION_LABELS: Record<Permission, string> = {
  "organization.read": "View organization",
  "organization.manage": "Manage organization",
  "members.read": "View members",
  "members.manage": "Manage members",
  "pages.read": "View pages",
  "pages.create": "Create pages",
  "pages.publish": "Publish pages",
  "knowledge.read": "View knowledge",
  "knowledge.manage": "Manage knowledge",
  "rag.query": "Ask AI questions",
  "analytics.read": "View analytics",
  "flags.read": "View feature flags",
  "flags.manage": "Manage feature flags",
  "experiments.read": "View experiments",
  "experiments.manage": "Manage experiments",
  "notifications.read": "View notifications",
};

/** Check if a role has a permission. */
export function roleHasPermission(role: Role, permission: Permission): boolean {
  return ROLE_PERMISSIONS[role]?.includes(permission) ?? false;
}

/** Check if any of the roles has the permission. */
export function rolesHavePermission(roles: Role[], permission: Permission): boolean {
  return roles.some((role) => roleHasPermission(role, permission));
}
