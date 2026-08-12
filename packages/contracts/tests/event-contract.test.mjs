import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import test from "node:test";

const domain = JSON.parse(await readFile(new URL("../domain/v1/event-contract.json", import.meta.url), "utf8"));
const fixture = JSON.parse(await readFile(new URL("../fixtures/v1/event-contract.json", import.meta.url), "utf8"));
const digestPattern = new RegExp(domain.eventEnvelope.digest.pattern);
const uuidPattern = new RegExp(domain.fieldRules.uuid.pattern);
const traceIdPattern = new RegExp(domain.fieldRules.traceId.pattern);
const topicPattern = new RegExp(domain.fieldRules.topic.pattern);
const maxEventVersion = domain.fieldRules.eventVersion.maximum;
const jobStates = new Set(domain.safePayload.valueRules.jobState.enum);
const routingByType = new Map(domain.eventRouting.matrix.map((entry) => [entry.eventType, entry]));
const catalogByType = new Map(domain.safePayload.safeDisplayCatalog.map((entry) => [entry.eventType, entry]));
const vectorByEventId = new Map(fixture.digestTestVectors.map((entry) => [entry.eventId, entry]));

function canonicalJson(value) {
  if (typeof value === "string") {
    assert.equal(hasLoneSurrogate(value), false, "RFC 8785 input must be an I-JSON string");
    return JSON.stringify(value);
  }
  if (typeof value === "number") {
    assert.equal(Number.isFinite(value), true, "RFC 8785 input must contain finite JSON numbers");
    return JSON.stringify(value);
  }
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
  const input = {
    operation: routing.operation,
    organizationId: event.organizationId,
    topic: event.topic,
    eventType: event.eventType,
    resourceType: event.resourceType,
    resourceId: event.resourceId,
    opaqueIdempotencyKey,
  };
  return sha256Wire(`nexora:event-idempotency:1.1\n${canonicalJson(input)}`);
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

function hasLoneSurrogate(value) {
  for (let index = 0; index < value.length; index += 1) {
    const codePoint = value.charCodeAt(index);
    if (codePoint >= 0xd800 && codePoint <= 0xdbff) {
      if (index + 1 >= value.length || value.charCodeAt(index + 1) < 0xdc00 || value.charCodeAt(index + 1) > 0xdfff) return true;
      index += 1;
    } else if (codePoint >= 0xdc00 && codePoint <= 0xdfff) {
      return true;
    }
  }
  return false;
}

function isSafePayload(event) {
  const payload = event.safePayload;
  const routing = routingByType.get(event.eventType);
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) return false;
  if (!domain.safePayload.requiredFields.every((field) => field in payload)) return false;
  if (Object.keys(payload).some((field) => !domain.safePayload.allowedFields.includes(field))) return false;
  if (domain.safePayload.valueRules.uuidFields.some((field) => field in payload && (typeof payload[field] !== "string" || !uuidPattern.test(payload[field])))) return false;
  if (!routing || !routing.resourceTypes.includes(payload.resourceType)) return false;
  if (typeof payload.traceId !== "string" || !traceIdPattern.test(payload.traceId)) return false;
  if ("correlationId" in payload && (typeof payload.correlationId !== "string" || !traceIdPattern.test(payload.correlationId))) return false;
  if (!Number.isInteger(payload.eventVersion) || payload.eventVersion < 1 || payload.eventVersion > maxEventVersion) return false;
  if (event.eventType === "JOB_PROGRESS_CHANGED") {
    if (!jobStates.has(payload.jobState) || !Number.isInteger(payload.progress) || payload.progress < 0 || payload.progress > 100) return false;
  } else if ("jobState" in payload || "progress" in payload) {
    return false;
  }
  if (payload.schemaVersion !== domain.eventEnvelope.version) return false;
  if (payload.resourceId !== event.resourceId || payload.resourceType !== event.resourceType
      || payload.organizationId !== event.organizationId || payload.subjectId !== event.subjectId
      || payload.actorId !== event.actorId || payload.eventVersion !== event.eventVersion
      || payload.traceId !== event.traceId) return false;
  return isExactSafeDisplay(event);
}

function isUtcInstant(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?Z$/.exec(value);
  if (!match) return false;
  const instant = new Date(value);
  return !Number.isNaN(instant.valueOf())
    && instant.getUTCFullYear() === Number(match[1])
    && instant.getUTCMonth() + 1 === Number(match[2])
    && instant.getUTCDate() === Number(match[3])
    && instant.getUTCHours() === Number(match[4])
    && instant.getUTCMinutes() === Number(match[5])
    && instant.getUTCSeconds() === Number(match[6]);
}

function isCanonicalEnvelope(event, opaqueIdempotencyKey) {
  if (!domain.eventEnvelope.requiredFields.every((field) => field in event)) return false;
  if (Object.keys(event).some((field) => !domain.eventEnvelope.allowedFields.includes(field))) return false;
  if (!uuidPattern.test(event.eventId) || !uuidPattern.test(event.organizationId) || !uuidPattern.test(event.subjectId) || !uuidPattern.test(event.actorId) || !uuidPattern.test(event.resourceId)) return false;
  if (!routingByType.has(event.eventType) || !routingByType.get(event.eventType).resourceTypes.includes(event.resourceType) || !traceIdPattern.test(event.traceId) || !topicPattern.test(event.topic)) return false;
  if (!Number.isInteger(event.eventVersion) || event.eventVersion < 1 || event.eventVersion > maxEventVersion || event.schemaVersion !== domain.eventEnvelope.version) return false;
  if (!digestPattern.test(event.idempotencyKeyDigest) || !digestPattern.test(event.payloadDigest) || !isUtcInstant(event.occurredAt)) return false;
  if (event.topic !== expectedTopic(event)) return false;
  if (!isSafePayload(event) || event.payloadDigest !== payloadDigest(event.safePayload)) return false;
  return opaqueIdempotencyKey === undefined || event.idempotencyKeyDigest === idempotencyKeyDigest(event, opaqueIdempotencyKey);
}

test("freezes one versioned, operation-bound route and display catalog for every event type", () => {
  assert.equal(domain.task, "M3-T01");
  assert.equal(domain.status, "frozen-contract-only");
  assert.equal(domain.contractVersion, "1.1.0");
  assert.deepEqual(domain.topicVocabulary.scopes, ["tenant", "resource"]);
  assert.equal(domain.eventEnvelope.digest.idempotencyKeyDigest.canonicalBytes.includes("JCS canonical JSON"), true);
  assert.equal(domain.fieldRules.schemaVersion.enum.length, 1);
  assert.equal(domain.fieldRules.schemaVersion.enum[0], "1.1.0");
  assert.equal(maxEventVersion, 9007199254740991);
  assert.ok(domain.eventEnvelope.requiredFields.includes("schemaVersion"));
  assert.ok(domain.eventEnvelope.requiredFields.includes("idempotencyKeyDigest"));

  assert.equal(routingByType.size, domain.eventTypes.length);
  assert.deepEqual([...routingByType.keys()], domain.eventTypes);
  assert.ok([...routingByType.values()].every((entry) => /^[a-z]+\.[a-z]+$/.test(entry.operation)));
  assert.ok([...routingByType.values()].every((entry) => entry.ownership.includes("current ACTIVE membership")));
  assert.ok([...routingByType.values()].every((entry) => entry.resourceTypes.every((resourceType) => domain.fieldRules.resourceType.enum.includes(resourceType))));

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
    assert.ok(isCanonicalEnvelope(event, vector.opaqueIdempotencyKey), `${event.eventType} canonical scalar fields`);

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

  const unsafeCorrelation = structuredClone(publication);
  unsafeCorrelation.safePayload.correlationId = "alice@example.test";
  assert.equal(isSafePayload(unsafeCorrelation), false, "PII-shaped text cannot hide in an allowed metadata field");

  const unsafeResourceType = structuredClone(publication);
  unsafeResourceType.safePayload.resourceType = "alice_smith";
  unsafeResourceType.resourceType = "alice_smith";
  assert.equal(isCanonicalEnvelope(unsafeResourceType, vectorByEventId.get(publication.eventId).opaqueIdempotencyKey), false, "resource type must be a route-controlled vocabulary token");

  const unsafeJobState = structuredClone(fixture.events.find((event) => event.eventType === "JOB_PROGRESS_CHANGED"));
  unsafeJobState.safePayload.jobState = "ALICE_SMITH";
  assert.equal(isSafePayload(unsafeJobState), false, "job state must be a finite server-controlled vocabulary token");

  const invalidUnicode = structuredClone(publication);
  invalidUnicode.safePayload.correlationId = "\ud800";
  assert.equal(isSafePayload(invalidUnicode), false, "lone surrogate is not I-JSON");
  assert.throws(() => payloadDigest(invalidUnicode.safePayload), /I-JSON string/);

  const unsafeTuple = structuredClone(publication);
  unsafeTuple.safePayload.safeDisplay.variant = "success";
  assert.equal(isExactSafeDisplay(unsafeTuple), false, "status and variant must be an exact tuple");

  const wrongRoute = structuredClone(publication);
  wrongRoute.topic = `tenant:${publication.organizationId}:outbox`;
  assert.notEqual(wrongRoute.topic, expectedTopic(wrongRoute), "event type may not select another route purpose");

  const unexpectedEnvelopeField = structuredClone(publication);
  unexpectedEnvelopeField.correlationId = "alice@example.test";
  assert.equal(isCanonicalEnvelope(unexpectedEnvelopeField, vectorByEventId.get(publication.eventId).opaqueIdempotencyKey), false, "envelope rejects unallowlisted top-level metadata");

  const malformedDigest = structuredClone(publication);
  malformedDigest.payloadDigest = "sha256:not-a-valid-digest";
  assert.equal(isCanonicalEnvelope(malformedDigest, vectorByEventId.get(publication.eventId).opaqueIdempotencyKey), false, "envelope rejects malformed digest fields");

  const unsafeEventVersion = structuredClone(publication);
  unsafeEventVersion.eventVersion = maxEventVersion + 1;
  unsafeEventVersion.safePayload.eventVersion = maxEventVersion + 1;
  assert.equal(isCanonicalEnvelope(unsafeEventVersion, vectorByEventId.get(publication.eventId).opaqueIdempotencyKey), false, "event version must remain JCS-safe across all producers and consumers");

  const stalePayloadDigest = structuredClone(publication);
  stalePayloadDigest.safePayload.safeDisplay.status = "INVALIDATED";
  stalePayloadDigest.safePayload.safeDisplay.variant = "danger";
  assert.equal(isCanonicalEnvelope(stalePayloadDigest, vectorByEventId.get(publication.eventId).opaqueIdempotencyKey), false, "payload digest is recomputed before accepting a catalog-valid mutation");

  const staleIdempotencyDigest = structuredClone(publication);
  staleIdempotencyDigest.resourceId = "30000000-0000-4000-8000-000000000009";
  staleIdempotencyDigest.safePayload.resourceId = staleIdempotencyDigest.resourceId;
  assert.equal(isCanonicalEnvelope(staleIdempotencyDigest, vectorByEventId.get(publication.eventId).opaqueIdempotencyKey), false, "idempotency digest is recomputed against resource scope in known-answer verification");

  const impossibleDate = structuredClone(publication);
  impossibleDate.occurredAt = "2026-02-29T00:00:00Z";
  assert.equal(isCanonicalEnvelope(impossibleDate, vectorByEventId.get(publication.eventId).opaqueIdempotencyKey), false, "UTC calendar instant must exist");

  const formatValidWrongDigest = `sha256:${"a".repeat(64)}`;
  assert.match(formatValidWrongDigest, digestPattern);
  assert.notEqual(formatValidWrongDigest, payloadDigest(publication.safePayload), "wire syntax is insufficient without preimage verification");

  const resourceA = structuredClone(publication);
  resourceA.resourceType = "page\nchild";
  resourceA.resourceId = "record";
  const resourceB = structuredClone(publication);
  resourceB.resourceType = "page";
  resourceB.resourceId = "child\nrecord";
  const vector = vectorByEventId.get(publication.eventId);
  assert.equal(isCanonicalEnvelope(resourceA), false, "newline resource type is outside the grammar");
  assert.equal(isCanonicalEnvelope(resourceB), false, "newline resource id is outside the grammar");
  assert.notEqual(idempotencyKeyDigest(resourceA, vector.opaqueIdempotencyKey), idempotencyKeyDigest(resourceB, vector.opaqueIdempotencyKey), "JCS object framing cannot collide through field delimiters");

  const legacy = structuredClone(publication);
  legacy.schemaVersion = "1.0.0";
  const acceptedSchemaVersions = new Set(domain.fieldRules.schemaVersion.enum);
  assert.equal(acceptedSchemaVersions.has(legacy.schemaVersion), false, "legacy free-form display schema is fail-closed");
  assert.equal(acceptedSchemaVersions.has("1.1.1"), false, "unknown schema versions are fail-closed");

  for (const name of ["unsafeSafeDisplayRejected", "freeFormSafeDisplayRejected", "nonSha256DigestRejected", "formatValidWrongPayloadDigestRejected", "wrongRouteForEventTypeRejected", "wrongSafeDisplayVariantRejected", "legacySchemaRejected", "crossTenantGuessDenied", "unownedSubjectDenied", "reusedKeyDifferentFingerprintDenied", "terminalOutboxFailureVisible"]) {
    assert.ok(fixture.idempotency.negativeCases.some((entry) => entry.name === name), name);
  }
  assert.equal(fixture.idempotency.differentRequest, "IDEMPOTENCY_KEY_REUSED");
});
