import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const app = (path: string) => readFileSync(resolve(import.meta.dirname, "..", path), "utf8");

describe("RBAC administration UI guardrails", () => {
  it("uses generated directory and mutation contracts through the server BFF", () => {
    const directory = app("app/admin/admin-directory.tsx");
    const list = app("app/api/bff/memberships/route.ts");
    const mutation = app("app/api/bff/memberships/[membershipId]/route.ts");
    expect(directory).toContain("MembershipDirectoryEntry");
    expect(directory).toContain("MembershipMutationResponse");
    expect(directory).toContain("/api/bff/memberships?organizationId=");
    expect(list).toContain("session.client.listMemberships({ organizationId })");
    expect(mutation).toContain("session.client.updateMembership");
    expect(mutation).toContain("requireSameOrigin(request)");
    expect(mutation).toContain("MembershipMutationRequest");
    expect(directory).not.toContain("localStorage");
    expect(mutation).not.toContain("nexora_access_token");
  });

  it("contains loading, denied, error, conflict, and destructive confirmation recovery", () => {
    const directory = app("app/admin/admin-directory.tsx");
    for (const state of ["loading", "empty", "denied", "error", "version-conflict"]) expect(directory).toContain(`"${state}"`);
    expect(directory).toContain('next.code === "VERSION_CONFLICT"');
    expect(directory).toContain("expectedVersion: member.version");
    expect(directory).toContain('role="dialog"');
    expect(directory).toContain("Remove this member?");
    expect(directory).toContain("The server confirms both the current tenant and your permission");
  });

  it("keeps the directory accessible by keyboard and usable at 375px", () => {
    const directory = app("app/admin/admin-directory.tsx");
    const css = app("app/globals.css");
    expect(directory).toContain('aria-label={`Role for ${member.subjectId}`}');
    expect(directory).toContain('scope="col"');
    expect(directory).toContain('aria-live="assertive"');
    expect(directory).toContain("tabIndex={-1}");
    expect(css).toContain(".nx-table-scroll");
    expect(css).toContain(".nx-admin-directory select");
    expect(css).toContain("min-height: 44px");
    expect(css).toContain("@media (max-width: 767px)");
  });
});
