import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { createElement, type ReactNode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AdminDirectory } from "../app/admin/admin-directory";

vi.mock("next/link", async () => {
  const React = await import("react");
  return { default: ({ children, ...props }: { children: ReactNode }) => React.createElement("a", props, children) };
});

const membership = {
  membershipId: "22222222-2222-2222-2222-222222222222",
  organizationId: "11111111-1111-1111-1111-111111111111",
  subjectId: "33333333-3333-3333-3333-333333333333",
  status: "ACTIVE" as const,
  role: "ADMIN" as const,
  version: 4,
};

beforeEach(() => {
  vi.stubGlobal("fetch", vi.fn(async (path: string) => ({
    ok: true,
    json: async () => path.includes("access-context")
      ? { subjectId: "33333333-3333-3333-3333-333333333333", sessionId: "44444444-4444-4444-4444-444444444444", assuranceLevel: "aal2", memberships: [{ organizationId: membership.organizationId, membershipId: membership.membershipId, membershipVersion: membership.version, role: membership.role }], tenantSelectionRequired: false }
      : [membership],
  })));
});

afterEach(() => { vi.unstubAllGlobals(); });

describe("membership removal confirmation", () => {
  it("focuses, traps keyboard navigation, closes on Escape or Cancel, and restores the trigger", async () => {
    render(createElement(AdminDirectory, { mode: "users" }));
    const remove = await screen.findByRole("button", { name: "Remove" });
    fireEvent.click(remove);

    const cancel = await screen.findByRole("button", { name: "Cancel" });
    const confirm = screen.getByRole("button", { name: "Remove member" });
    await waitFor(() => expect(document.activeElement).toBe(cancel));

    confirm.focus(); fireEvent.keyDown(document, { key: "Tab" });
    expect(document.activeElement).toBe(cancel);
    cancel.focus(); fireEvent.keyDown(document, { key: "Tab", shiftKey: true });
    expect(document.activeElement).toBe(confirm);

    fireEvent.keyDown(document, { key: "Escape" });
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(document.activeElement).toBe(remove);

    fireEvent.click(remove);
    fireEvent.click(await screen.findByRole("button", { name: "Cancel" }));
    await waitFor(() => expect(screen.queryByRole("dialog")).toBeNull());
    expect(document.activeElement).toBe(remove);
  });
});
