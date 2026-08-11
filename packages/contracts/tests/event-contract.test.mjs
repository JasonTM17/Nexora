import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import test from "node:test";

const domain = JSON.parse(await readFile(new URL("../domain/v1/event-contract.json", import.meta.url), "utf8"));
const fixture = JSON.parse(await readFile(new URL("../fixtures/v1/event-contract.json", import.meta.url), "utf8"));
const digestPattern = new RegExp(domain.eventEnvelope.digest.pattern);
const routingByType = new Map(domain.eventRouting.matrix.map((entry) => [entry.eventType, entry]));
const catalogByType = new Map(domain.safePayload.safeDisplayCatalog.map((entry) => [entry.eventType, entry]));
const vectorByEventId = new Map(fixture.digestTestVectors.map((entry) => [entry.eventId, entry]));

function canonicalJson(value) {
  if (value === null || typeof value !== "object") return JSON.stringify(value);
  if (Array.isArray(value)) return `[${value.map(canonicalJson).join(",")}]`;
  return `{${Object.keys(value).sort().map((key) => `${JSON.stringify(key)}:${canonicalJson(value[key])}`).join(",")}}`;
}

function sha256Wire(value) {
  return `sha256:${createHash("sha256").update(value, "utf8").digest("hex")}`;
}

function payloadDigest(safePayload) {
  return sha256Wire(`nexora:event-payload:1.1\n${canonicalJson(safePayload)}`);
}

function idempotencyKeyDigest(event, opaqueIdempotencyKey) {
  const routing = routingByType.get(event.eventType);
  return sha256Wire(`nexora:event-idempotency:1.1\n${routing.operation}\n${event.organizationId}\n${event.topic}\n${event.eventType}\n${event.resourceType}\n${event.resourceId}\n${opaqueIdempotencyKey}`);
}

function expectedTopic(event) {
  const routing = routingByType.get(event.eventType);
  const owner = routing.scope === "tenant" ? event.organizationId : event.resourceId;
  return `${routing.scope}:${owner}:${routing.purpose}`;
}

function isExactSafeDisplay(event) {
  const display = event.safePayload.safeDisplay;
  const catalog = catalogByType.get(event.eventType);
  if (!catalog || !display || typeof display !== "object" || Array.isArray(display)) return false;
  if (Object.keys(display).sort().join(",") !== "label,status,variant") return false;
  if (display.label !== catalog.label || !/^[A-Z][A-Z0-9_]{1,63}$/.test(display.status)) return false;
  return catalog.statusVariants.some((tuple) => tuple.status === display.status && tuple.variant === display.variant);
}

test("freezes one versioned, operation-bound route and display catalog for every event type", () => {
  assert.equal(domain.task, "M3-T01");
  assert.equal(domain.status, "frozen-contract-only");
  assert.equal(domain.contractVersion, "1.1.0");
  assert.deepEqual(domain.topicVocabulary.scopes, ["tenant", "resource"]);
  assert.ok(domain.eventEnvelope.requiredFields.includes("schemaVersion"));
  assert.ok(domain.eventEnvelope.requiredFields.includes("idempotencyKeyDigest"));

  assert.equal(routingByType.size, domain.eventTypes.length);
  assert.deepEqual([...routingByType.keys()], domain.eventTypes);
  assert.ok([...routingByType.values()].every((entry) => /^[a-z]+\.[a-z]+$/.test(entry.operation)));
  assert.ok([...routingByType.values()].every((entry) => entry.ownership.includes("current ACTIVE membership")));

  assert.equal(catalogByType.size, domain.eventTypes.length);
  assert.deepEqual([...catalogByType.keys()], domain.eventTypes);
  for (const [eventType, catalog] of catalogByType) {
    assert.equal(catalog.label, eventType);
    assert.ok(catalog.statusVariants.length > 0, eventType);
    assert.equal(new Set(catalog.statusVariants.map((tuple) => `${tuple.status}:${tuple.variant}`)).size, catalog.statusVariants.length, eventType);
    assert.ok(catalog.statusVariants.every((tuple) => /^[A-Z][A-Z0-9_]{1,63}$/.test(tuple.status)), eventType);
  }

  assert.equal(domain.compatibility.minimumAcceptedSchemaVersion, "1.1.0");
  assert.match(domain.compatibility.legacySchemaVersionPolicy, /reject 1\.0\.0 envelopes fail closed/i);
  assert.match(domain.compatibility.eventVersionRule, /does not substitute for schemaVersion/i);
});

test("binds fixture payload and idempotency digests to canonical preimages", () => {
  assert.equal(fixture.events.length, domain.eventTypes.length);
  assert.equal(fixture.digestTestVectors.length, fixture.events.length);

  for (const event of fixture.events) {
    const vector = vectorByEventId.get(event.eventId);
    assert.ok(vector, event.eventId);
    assert.equal(event.schemaVersion, domain.eventEnvelope.version);
    assert.match(event.idempotencyKeyDigest, digestPattern);
    assert.match(event.payloadDigest, digestPattern);
    assert.equal(event.payloadDigest, payloadDigest(event.safePayload), `${event.eventType} payload digest`);
    assert.equal(event.idempotencyKeyDigest, idempotencyKeyDigest(event, vector.opaqueIdempotencyKey), `${event.eventType} idempotency digest`);

    const mutatedPayload = structuredClone(event.safePayload);
    mutatedPayload.safeDisplay.status = "TAMPERED";
    assert.notEqual(event.payloadDigest, payloadDigest(mutatedPayload), `${event.eventType} rejects a format-valid digest with another payload preimage`);
    assert.notEqual(event.idempotencyKeyDigest, idempotencyKeyDigest(event, `${vector.opaqueIdempotencyKey}-other`), `${event.eventType} binds the opaque key preimage`);
  }
});

test("canonical fixture enforces exact routing, ownership, display tuples and fail-closed negatives", () => {
  assert.equal(fixture.tenants.length, 2);
  assert.equal(fixture.subjects.length, 2);
  assert.equal(fixture.ownershipMatrix.length, 3);
  assert.ok(fixture.ownershipMatrix.some((row) => row.canPublish && row.canConsume));
  assert.ok(fixture.ownershipMatrix.some((row) => row.organizationId === fixture.tenants[1].organizationId && !row.canPublish && !row.canConsume));

  for (const event of fixture.events) {
    const subject = fixture.subjects.find((candidate) => candidate.subjectId === event.subjectId);
    assert.ok(subject, event.eventType);
    assert.equal(subject.organizationId, event.organizationId, `${event.eventType} subject ownership`);
    assert.equal(event.topic, expectedTopic(event), `${event.eventType} exact topic routing`);
    assert.ok(isExactSafeDisplay(event), `${event.eventType} exact safe display tuple`);
  }

  const publication = fixture.events.find((event) => event.eventType === "PUBLICATION_INVALIDATED");
  const unsafeLabel = structuredClone(publication);
  unsafeLabel.safePayload.safeDisplay.label = "alice@example.test";
  assert.equal(isExactSafeDisplay(unsafeLabel), false, "free-form identifier is not a catalog key");

  const unsafeTuple = structuredClone(publication);
  unsafeTuple.safePayload.safeDisplay.variant = "success";
  assert.equal(isExactSafeDisplay(unsafeTuple), false, "status and variant must be an exact tuple");

  const wrongRoute = structuredClone(publication);
  wrongRoute.topic = `tenant:${publication.organizationId}:outbox`;
  assert.notEqual(wrongRoute.topic, expectedTopic(wrongRoute), "event type may not select another route purpose");

  const formatValidWrongDigest = `sha256:${"a".repeat(64)}`;
  assert.match(formatValidWrongDigest, digestPattern);
  assert.notEqual(formatValidWrongDigest, payloadDigest(publication.safePayload), "wire syntax is insufficient without preimage verification");

  const legacy = structuredClone(publication);
  legacy.schemaVersion = "1.0.0";
  assert.notEqual(legacy.schemaVersion, domain.compatibility.minimumAcceptedSchemaVersion, "legacy free-form display schema is fail-closed");

  for (const name of ["unsafeSafeDisplayRejected", "freeFormSafeDisplayRejected", "nonSha256DigestRejected", "formatValidWrongPayloadDigestRejected", "wrongRouteForEventTypeRejected", "wrongSafeDisplayVariantRejected", "legacySchemaRejected", "crossTenantGuessDenied", "unownedSubjectDenied", "reusedKeyDifferentFingerprintDenied", "terminalOutboxFailureVisible"]) {
    assert.ok(fixture.idempotency.negativeCases.some((entry) => entry.name === name), name);
  }
  assert.equal(fixture.idempotency.differentRequest, "IDEMPOTENCY_KEY_REUSED");
});
