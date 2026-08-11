import { createClient } from "@supabase/supabase-js";

export type RealtimeDescriptor = Readonly<{
  topic: string;
  eventType: string;
  eventVersion: number;
  authorizationEpoch: number;
  expiresAt: string;
  transportToken: string;
  privateChannel: true;
  delivery: "broadcast" | "presence";
  reconnectBackoffMs: ReadonlyArray<number>;
  onEvent: "REFETCH_DURABLE_STATE";
}>;

export type RealtimeLifecycleState =
  | "SUBSCRIBED"
  | "REFETCH_DURABLE_STATE"
  | "AUTH_REFRESH_REQUIRED"
  | "DEGRADED_REFETCH_REQUIRED";

type ChannelLike = {
  on(type: string, filter: Record<string, string>, callback: () => void): ChannelLike;
  subscribe(callback: (status: string) => void): ChannelLike;
  unsubscribe(): unknown;
};

type RealtimeClientLike = {
  realtime: { setAuth(token: string): void };
  channel(topic: string, options: { config: { private: true } }): ChannelLike;
  removeChannel?(channel: ChannelLike): unknown;
};

export function nextRealtimeReconnectDelayMs(attempt: number, descriptor: RealtimeDescriptor) {
  if (attempt < 0 || attempt >= descriptor.reconnectBackoffMs.length) return null;
  return descriptor.reconnectBackoffMs[attempt] ?? null;
}

export function nextRealtimeDescriptorRenewalDelayMs(descriptor: RealtimeDescriptor, now = Date.now()) {
  const expiresAt = Date.parse(descriptor.expiresAt);
  if (!Number.isFinite(expiresAt)) return null;
  // Discard stale/near-expiry credentials instead of briefly joining with them.
  const renewalDelay = expiresAt - now - 10_000;
  return renewalDelay >= 1000 ? renewalDelay : null;
}

export async function requestRealtimeDescriptor(input: {
  organizationId: string;
  eventType: RealtimeDescriptor["eventType"];
  resourceId?: string;
}) {
  const response = await fetch("/api/bff/realtime/descriptors", {
    method: "POST",
    credentials: "same-origin",
    headers: { Accept: "application/json", "Content-Type": "application/json" },
    body: JSON.stringify(input),
  });
  const body = await response.json();
  if (!response.ok) throw body;
  return body as RealtimeDescriptor;
}

export function createRealtimeClient(): RealtimeClientLike {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY;
  if (!url || !key) throw new Error("Supabase Realtime client configuration is unavailable.");
  return createClient(url, key);
}

export function subscribeToRealtimeDescriptor(
  descriptor: RealtimeDescriptor,
  handlers: {
    refetchDurableState(reason: RealtimeLifecycleState): void;
    markState(state: RealtimeLifecycleState): void;
  },
  client: RealtimeClientLike = createRealtimeClient(),
) {
  if (!descriptor.privateChannel) {
    throw new Error("Realtime descriptors must use private channels.");
  }
  client.realtime.setAuth(descriptor.transportToken);
  const channel = client.channel(descriptor.topic, { config: { private: true } });
  const eventKind = descriptor.delivery === "presence" ? "presence" : "broadcast";
  const filter = descriptor.delivery === "presence" ? { event: "sync" } : { event: descriptor.eventType };

  channel.on(eventKind, filter, () => {
    handlers.markState("REFETCH_DURABLE_STATE");
    handlers.refetchDurableState("REFETCH_DURABLE_STATE");
  });
  channel.subscribe(status => {
    if (status === "SUBSCRIBED") {
      handlers.markState("SUBSCRIBED");
      handlers.refetchDurableState("SUBSCRIBED");
    }
    if (status === "CHANNEL_ERROR" || status === "TIMED_OUT" || status === "CLOSED") {
      handlers.markState("DEGRADED_REFETCH_REQUIRED");
    }
  });

  return {
    refresh(next: RealtimeDescriptor) {
      client.realtime.setAuth(next.transportToken);
      handlers.markState("AUTH_REFRESH_REQUIRED");
    },
    close() {
      channel.unsubscribe();
      client.removeChannel?.(channel);
    },
  };
}
