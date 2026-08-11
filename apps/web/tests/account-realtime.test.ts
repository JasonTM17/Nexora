import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { createElement } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const realtime = vi.hoisted(() => ({
  request: vi.fn(),
  subscribe: vi.fn(),
  renewDelay: vi.fn(() => 60_000),
  retryDelay: vi.fn(),
}));

vi.mock("../app/lib/realtime-subscription", () => ({
  requestRealtimeDescriptor: realtime.request,
  subscribeToRealtimeDescriptor: realtime.subscribe,
  nextRealtimeDescriptorRenewalDelayMs: realtime.renewDelay,
  nextRealtimeReconnectDelayMs: realtime.retryDelay,
}));

import { AccountAccess } from "../app/account/account-access";

const context = {
  subjectId: "33333333-3333-3333-3333-333333333333",
  sessionId: "44444444-4444-4444-4444-444444444444",
  assuranceLevel: "aal2",
  memberships: [{
    organizationId: "11111111-1111-1111-1111-111111111111",
    membershipId: "22222222-2222-2222-2222-222222222222",
    membershipVersion: 4,
    role: "OWNER",
  }],
  tenantSelectionRequired: false,
};

const descriptor = {
  topic: "tenant:11111111-1111-1111-1111-111111111111:publication",
  eventType: "PUBLICATION_INVALIDATED",
  eventVersion: 1,
  authorizationEpoch: 7,
  expiresAt: "2099-01-01T00:00:00.000Z",
  transportToken: "scoped-realtime-token",
  privateChannel: true,
  delivery: "broadcast" as const,
  reconnectBackoffMs: [1000, 2000, 5000, 10000],
  onEvent: "REFETCH_DURABLE_STATE" as const,
};

let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  realtime.request.mockResolvedValue(descriptor);
  realtime.subscribe.mockImplementation((_descriptor, handlers) => {
    handlers.markState("SUBSCRIBED");
    handlers.refetchDurableState("SUBSCRIBED");
    return { close: vi.fn() };
  });
  realtime.renewDelay.mockReturnValue(60_000);
  fetchMock = vi.fn(async (path: string) => ({
    ok: true,
    json: async () => path.includes("access-context")
      ? { ...context, memberships: [...context.memberships] }
      : { displayName: "Nexora User", locale: "en", reducedMotion: false, highContrast: false, version: 1 },
  }));
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.clearAllMocks();
});

describe("account realtime lifecycle", () => {
  it("refetches durable state after subscription without replacing the active channel", async () => {
    const view = render(createElement(AccountAccess));

    await waitFor(() => expect(realtime.subscribe).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(fetchMock.mock.calls
      .filter(([path]) => path === "/api/bff/access-context")).toHaveLength(2));
    expect(realtime.subscribe).toHaveBeenCalledTimes(1);

    view.unmount();
  });

  it("reissues a server descriptor after a bounded degraded-channel retry", async () => {
    realtime.retryDelay.mockReturnValueOnce(0);
    realtime.subscribe
      .mockImplementationOnce((_descriptor, handlers) => {
        handlers.markState("DEGRADED_REFETCH_REQUIRED");
        return { close: vi.fn() };
      })
      .mockImplementationOnce((_descriptor, handlers) => {
        handlers.markState("SUBSCRIBED");
        return { close: vi.fn() };
      });
    const view = render(createElement(AccountAccess));

    await waitFor(() => expect(realtime.subscribe).toHaveBeenCalledTimes(2));
    expect(realtime.retryDelay).toHaveBeenCalledWith(0, descriptor);
    expect(realtime.request).toHaveBeenCalledTimes(2);

    view.unmount();
  });

  it("stops reconnecting after the descriptor retry sequence is exhausted", async () => {
    realtime.retryDelay.mockImplementation((attempt: number) => attempt < 4 ? 0 : null);
    realtime.subscribe.mockImplementation((_descriptor, handlers) => {
      handlers.markState("DEGRADED_REFETCH_REQUIRED");
      return { close: vi.fn() };
    });
    const view = render(createElement(AccountAccess));

    await waitFor(() => expect(realtime.subscribe).toHaveBeenCalledTimes(5));
    await new Promise(resolve => setTimeout(resolve, 20));
    expect(realtime.subscribe).toHaveBeenCalledTimes(5);
    expect(realtime.retryDelay).toHaveBeenLastCalledWith(4, descriptor);

    view.unmount();
  });

  it("keeps the selected organization subscribed after a multi-membership refetch", async () => {
    const secondMembership = {
      ...context.memberships[0],
      organizationId: "55555555-5555-5555-5555-555555555555",
      membershipId: "66666666-6666-6666-6666-666666666666",
      role: "EDITOR",
    };
    fetchMock.mockImplementation(async (path: string) => ({
      ok: true,
      json: async () => path.includes("access-context")
        ? { ...context, memberships: [...context.memberships, secondMembership], tenantSelectionRequired: true }
        : { displayName: "Nexora User", locale: "en", reducedMotion: false, highContrast: false, version: 1 },
    }));
    const view = render(createElement(AccountAccess));

    const secondOption = await screen.findByRole("radio", { name: /Organization 55555555/i });
    fireEvent.click(secondOption);
    fireEvent.click(screen.getByRole("button", { name: "Continue" }));
    await waitFor(() => expect(realtime.subscribe).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(fetchMock.mock.calls
      .filter(([path]) => path === "/api/bff/access-context")).toHaveLength(2));
    expect(screen.queryByRole("heading", { name: "Choose your organization" })).toBeNull();
    expect(realtime.request).toHaveBeenCalledWith({
      organizationId: secondMembership.organizationId,
      eventType: "PUBLICATION_INVALIDATED",
    });
    expect(realtime.subscribe).toHaveBeenCalledTimes(1);

    view.unmount();
  });
});
