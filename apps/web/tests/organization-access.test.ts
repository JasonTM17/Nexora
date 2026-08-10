import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const app = (path: string) => readFileSync(resolve(import.meta.dirname, "..", path), "utf8");

describe("organization access UI guardrails", () => {
  it("keeps tokens server-side and requires same-origin CSRF for mutations", () => {
    const bff = app("app/api/bff/_shared.ts");
    const session = app("app/lib/supabase-session.ts");
    const callback = app("app/auth/callback/route.ts");
    const profileRoute = app("app/api/bff/profile/route.ts");
    const tenantRoute = app("app/api/bff/tenant-context/route.ts");
    expect(bff).toContain("authenticatedClient");
    expect(bff).toContain("requireSameOrigin");
    expect(bff).toContain("PlatformApiClient");
    expect(bff).not.toContain("nexora_access_token");
    expect(session).toContain("createServerClient");
    expect(session).toContain("supabase.auth.getClaims()");
    expect(session).toContain("httpOnly: true");
    expect(session).toContain('sameSite: "lax"');
    expect(session).toContain('secure: process.env.NODE_ENV === "production"');
    expect(callback).toContain("exchangeCodeForSession(code)");
    expect(profileRoute).toContain("requireSameOrigin(request)");
    expect(tenantRoute).toContain("requireSameOrigin(request)");
    expect(bff).not.toContain("localStorage");
  });

  it("renders complete access and stale-profile recovery states", () => {
    const account = app("app/account/account-access.tsx");
    for (const state of ["loading", "empty", "selection", "denied", "session-expired", "error", "profile-conflict"]) {
      expect(account).toContain(`"${state}"`);
    }
    expect(account).toContain('code === "VERSION_CONFLICT"');
    expect(account).toContain("expectedVersion: profile.version");
    expect(account).toContain('href="/auth/callback"');
    expect(account).toContain("server validates this choice");
  });

  it("keeps organization selection keyboard-operable and compact at 375px", () => {
    const account = app("app/account/account-access.tsx");
    const css = app("app/globals.css");
    expect(account).toContain('type="radio"');
    expect(account).toContain('role="status"');
    expect(account).toContain('aria-live="assertive"');
    expect(account).toContain('tabIndex={-1}');
    expect(css).toContain(".nx-org-option");
    expect(css).toContain("min-height: 44px");
    expect(css).toContain(".nx-hero, .nx-card-grid, .nx-account-grid");
  });
});
