import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const domain = JSON.parse(await readFile(new URL("../domain/v1/event-contract.json", import.meta.url), "utf8"));
const fixture = JSON.parse(await readFile(new URL("../fixtures/v1/event-contract.json", import.meta.url), "utf8"));

test("freezes versioned event envelope, subject ownership and outbox state", () => {
  assert.equal(domain.task, "M3-T01");
  assert.equal(domain.status, "frozen-contract-only");
  assert.deepEqual(domain.topicVocabulary.scopes, ["tenant", "resource", "job"]);
  assert.deepEqual(domain.eventRouting.matrix.map((entry) => entry.eventType), [
    "PUBLICATION_INVALIDATED",
    "WORKFLOW_TRANSITIONED",
    "JOB_PROGRESS_CHANGED",
    "NOTIFICATION_ENQUEUED",
    "PRESENCE_CHANGED",
    "OUTBOX_RECORDED",
  ]);
  assert.ok(domain.eventRouting.matrix.every((entry) => entry.ownership.includes("current ACTIVE membership")));
  assert.deepEqual(domain.outbox.states, ["PENDING", "CLAIMED", "PUBLISHED", "FAILED", "DEAD_LETTER"]);
  assert.deepEqual(domain.outbox.transitions.CLAIMED, ["PUBLISHED", "FAILED"]);
  assert.equal(domain.outbox.attempts.maxAttempts, 5);
  assert.equal(domain.outbox.terminalSemantics.DEAD_LETTER, "terminal failure with operator-visible record");
  assert.match(domain.eventEnvelope.rules[2], /scope:tenant-or-resource-id:purpose/i);
  assert.ok(domain.eventTypes.includes("PUBLICATION_INVALIDATED"));
  assert.ok(domain.eventTypes.includes("OUTBOX_RECORDED"));
  assert.match(domain.limitations[0], /no migration, producer implementation/i);
});

test("keeps safe payloads and idempotency fail closed", () => {
  assert.ok(domain.safePayload.allowedFields.includes("jobState"));
  assert.ok(domain.safePayload.allowedFields.includes("safeDisplay"));
  assert.deepEqual(domain.safePayload.nestedShapes.safeDisplay.requiredFields, ["label", "status"]);
  assert.ok(domain.safePayload.nestedShapes.safeDisplay.allowedFields.includes("progressText"));
  assert.equal(domain.safePayload.nestedShapes.safeDisplay.valueRules.label.maximumLength, 80);
  assert.equal(domain.safePayload.nestedShapes.safeDisplay.valueRules.variant.enum.length, 5);
  assert.ok(domain.safePayload.nestedShapes.safeDisplay.rules.some((rule) => /No nested objects/i.test(rule)));
  for (const forbidden of ["body", "prompt", "token", "secret", "providerPayload", "rawHtml", "pii"]) {
    assert.ok(domain.safePayload.forbiddenFields.includes(forbidden), forbidden);
  }
  assert.match(domain.idempotency.requestKey, /carries one opaque idempotency key digest/i);
  assert.match(domain.idempotency.sameRequest, /original terminal receipt/i);
  assert.match(domain.idempotency.differentRequest, /IDEMPOTENCY_KEY_REUSED/);
  assert.match(domain.idempotency.retention, /future persistence policy/i);
});

test("canonical fixture covers two tenants, ownership routing and unsafe denials", () => {
  assert.equal(fixture.tenants.length, 2);
  assert.equal(fixture.subjects.length, 2);
  assert.notEqual(fixture.tenants[0].organizationId, fixture.tenants[1].organizationId);
  assert.equal(fixture.ownershipMatrix.length, 3);
  assert.equal(fixture.events.length, 2);
  assert.match(fixture.events[0].topic, /^tenant:/);
  assert.match(fixture.events[1].topic, /^resource:/);
  assert.deepEqual(Object.keys(fixture.events[0].safePayload.safeDisplay).sort(), ["hint", "label", "status", "variant"]);
  assert.deepEqual(Object.keys(fixture.events[1].safePayload.safeDisplay).sort(), ["label", "progressText", "state", "status"]);
  assert.equal(fixture.events[0].safePayload.safeDisplay.variant, "warning");
  assert.equal(fixture.events[1].safePayload.safeDisplay.progressText, "45%");
  assert.equal(fixture.routingMatrix.length, 6);
  assert.ok(fixture.routingMatrix.some((entry) => entry.eventType === "OUTBOX_RECORDED" && entry.purpose === "outbox"));
  assert.ok(fixture.ownershipMatrix.some((row) => row.canPublish === true && row.canConsume === true));
  assert.ok(fixture.ownershipMatrix.some((row) => row.organizationId === fixture.tenants[1].organizationId && row.canPublish === false && row.canConsume === false));
  assert.ok(fixture.idempotency.negativeCases.some((entry) => entry.name === "unsafeSafeDisplayRejected" && entry.result === "REJECTED"));
  assert.ok(fixture.idempotency.negativeCases.some((entry) => entry.name === "crossTenantGuessDenied" && entry.result === "DENIED"));
  assert.equal(fixture.idempotency.differentRequest, "IDEMPOTENCY_KEY_REUSED");
  assert.ok(fixture.idempotency.negativeCases.some((entry) => entry.name === "terminalOutboxFailureVisible" && entry.result === "VISIBLE"));
});
