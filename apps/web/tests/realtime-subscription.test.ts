import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it, vi } from "vitest";
import {
  nextRealtimeReconnectDelayMs,
  subscribeToRealtimeDescriptor,
  type RealtimeDescriptor,
} from "../app/lib/realtime-subscription";

const app = (path: string) => readFileSync(resolve(import.meta.dirname, "..", path), "utf8");

const descriptor: RealtimeDescriptor = {
  topic: "tenant:10000000-0000-4000-8000-000000000001:publication",
  eventType: "PUBLICATION_INVALIDATED",
  eventVersion: 1,
  authorizationEpoch: 7,
  expiresAt: "2026-08-10T01:00:00Z",
  transportToken: "scoped-realtime-token",
  privateChannel: true,
  delivery: "broadcast",
  reconnectBackoffMs: [1000, 2000, 5000, 10000],
  onEvent: "REFETCH_DURABLE_STATE",
};

describe("Realtime subscription lifecycle", () => {
  it("requests descriptors through same-origin BFF without accepting browser-built topics", () => {
    const route = app("app/api/bff/realtime/descriptors/route.ts");
    const hook = app("app/lib/realtime-subscription.ts");
    const proxy = app("proxy.ts");

    expect(route).toContain("requireSameOrigin(request)");
    expect(route).toContain("X-Nexora-Organization-Id");
    expect(route).toContain("Authorization: `Bearer ${session.accessToken}`");
    expect(route).toContain("eventType: body.eventType");
    expect(route).not.toContain("topic: body.topic");
    expect(route).not.toContain("NEXORA_REALTIME_JWT_SECRET");
    expect(hook).toContain("/api/bff/realtime/descriptors");
    expect(hook).toContain("credentials: \"same-origin\"");
    expect(proxy).toContain("NEXT_PUBLIC_SUPABASE_URL");
    expect(proxy).toContain("websocket.protocol");
  });

  it("joins private Supabase channels with the scoped descriptor token", () => {
    const refetchDurableState = vi.fn();
    const markState = vi.fn();
    const channel = {
      on: vi.fn(() => channel),
      subscribe: vi.fn((callback: (status: string) => void) => {
        callback("SUBSCRIBED");
        return channel;
      }),
      unsubscribe: vi.fn(),
    };
    const client = {
      realtime: { setAuth: vi.fn() },
      channel: vi.fn(() => channel),
      removeChannel: vi.fn(),
    };

    const subscription = subscribeToRealtimeDescriptor(descriptor, { refetchDurableState, markState }, client);
    const onEvent = (channel.on as any).mock.calls[0][2] as () => void;
    onEvent();
    subscription.close();

    expect(client.realtime.setAuth).toHaveBeenCalledWith("scoped-realtime-token");
    expect(client.channel).toHaveBeenCalledWith(descriptor.topic, { config: { private: true } });
    expect(channel.on).toHaveBeenCalledWith("broadcast", { event: "PUBLICATION_INVALIDATED" }, expect.any(Function));
    expect(refetchDurableState).toHaveBeenCalledWith("SUBSCRIBED");
    expect(refetchDurableState).toHaveBeenCalledWith("REFETCH_DURABLE_STATE");
    expect(channel.unsubscribe).toHaveBeenCalled();
    expect(client.removeChannel).toHaveBeenCalledWith(channel);
  });

  it("bounds reconnect attempts and stops after the contract sequence", () => {
    expect(nextRealtimeReconnectDelayMs(0, descriptor)).toBe(1000);
    expect(nextRealtimeReconnectDelayMs(3, descriptor)).toBe(10000);
    expect(nextRealtimeReconnectDelayMs(4, descriptor)).toBeNull();
  });
});
