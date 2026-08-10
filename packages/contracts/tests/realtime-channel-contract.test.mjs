import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const eventContract = JSON.parse(await readFile(new URL("../domain/v1/event-contract.json", import.meta.url), "utf8"));
const channelContract = JSON.parse(await readFile(new URL("../realtime/v1/channel-contract.json", import.meta.url), "utf8"));
const fixture = JSON.parse(await readFile(new URL("../realtime/fixtures/v1/channel-contract.json", import.meta.url), "utf8"));

const eventRoutes = new Map(eventContract.eventRouting.matrix.map((route) => [route.eventType, route]));

test("freezes private Realtime channels to the integrated event-routing matrix", () => {
  assert.equal(channelContract.task, "M3-T01");
  assert.equal(channelContract.privateChannel.required, true);
  assert.equal(channelContract.privateChannel.clientConfig.private, true);
  assert.equal(channelContract.privateChannel.publicChannelAllowed, false);
  assert.deepEqual(channelContract.wireTopics.activeScopes, ["tenant", "resource"]);
  assert.match(channelContract.authority.wireTopicDecision, /tenant\/resource/i);

  for (const channel of channelContract.channels) {
    const route = eventRoutes.get(channel.eventType);
    assert.ok(route, channel.eventType);
    assert.match(channel.topicTemplate, new RegExp(`^${route.scope}:\\{`));
    assert.match(channel.topicTemplate, new RegExp(`:${route.purpose}$`));
    assert.match(channel.owner, /current ACTIVE membership/i);
  }

  assert.ok(channelContract.excludedEventTypes.some((entry) => entry.eventType === "NOTIFICATION_ENQUEUED"));
  assert.ok(channelContract.excludedEventTypes.some((entry) => entry.eventType === "OUTBOX_RECORDED"));
  assert.ok(channelContract.wireTopics.logicalReferenceExamples.every((topic) => topic.startsWith("org:")));
  assert.ok(channelContract.wireTopics.rules.some((rule) => /not Realtime topics/i.test(rule)));
});

test("bounds reconnects and preserves durable truth across authorization changes", () => {
  assert.deepEqual(channelContract.lifecycle.reconnectBackoffMs, [1000, 2000, 5000, 10000]);
  assert.equal(channelContract.lifecycle.maximumAutomaticReconnectAttempts, 4);
  assert.match(channelContract.lifecycle.onSubscribe, /refetch/i);
  assert.match(channelContract.lifecycle.onDuplicateOrOutOfOrder, /refetch/i);
  assert.match(channelContract.lifecycle.onTokenRefresh, /close the channel/i);
  assert.match(channelContract.lifecycle.onMembershipRemovalOrExpiry, /discard the descriptor/i);
  assert.ok(channelContract.safety.prohibitions.includes("No Realtime-only business state."));
  assert.ok(channelContract.safety.prohibitions.includes("No public channel."));
});

test("covers descriptor and presence denial cases without fabricating live authorization", () => {
  assert.equal(fixture.subjects.filter((subject) => subject.membershipStatus === "ACTIVE").length, 2);
  assert.equal(fixture.descriptors.length, 2);
  assert.ok(fixture.descriptors.every((descriptor) => descriptor.private && descriptor.result === "ISSUED"));
  assert.ok(fixture.negativeCases.some((entry) => entry.name === "guessedCrossTenantTopic" && entry.result === "REALTIME_DESCRIPTOR_DENIED"));
  assert.ok(fixture.negativeCases.some((entry) => entry.name === "removedMembershipDescriptor" && entry.result === "REALTIME_STALE_DESCRIPTOR"));
  assert.ok(fixture.negativeCases.some((entry) => entry.name === "publicChannelAttempt" && entry.private === false));
  assert.ok(fixture.negativeCases.some((entry) => entry.name === "legacyLogicalTopicAttempt"));
  assert.ok(fixture.negativeCases.some((entry) => entry.name === "unsafePresencePayload" && entry.result === "PAYLOAD_REJECTED"));
  assert.deepEqual(fixture.negativeCases.find((entry) => entry.name === "reconnectExhausted").attempts, [1000, 2000, 5000, 10000]);
  assert.ok(fixture.negativeCases.some((entry) => entry.name === "tokenRefreshFailure" && entry.result === "REALTIME_AUTH_REFRESH_REQUIRED"));
});
