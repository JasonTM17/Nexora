import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const domain = JSON.parse(await readFile(new URL("../domain/v1/event-contract.json", import.meta.url), "utf8"));
const fixture = JSON.parse(await readFile(new URL("../fixtures/v1/event-contract.json", import.meta.url), "utf8"));

test("freezes versioned event envelope, subject ownership and outbox state", () => {
  assert.equal(domain.task, "M3-T01");
  assert.equal(domain.status, "frozen-contract-only");
  assert.deepEqual(domain.topicVocabulary.scopes, ["tenant", "resource", "job"]);
  assert.deepEqual(domain.outbox.states, ["PENDING", "CLAIMED", "PUBLISHED", "FAILED", "DEAD_LETTER"]);
  assert.match(domain.eventEnvelope.rules[2], /scope:tenant-or-resource-id:purpose/i);
  assert.ok(domain.eventTypes.includes("PUBLICATION_INVALIDATED"));
  assert.ok(domain.eventTypes.includes("OUTBOX_RECORDED"));
  assert.match(domain.limitations[0], /no migration, producer implementation/i);
});

test("keeps safe payloads and idempotency fail closed", () => {
  assert.ok(domain.safePayload.allowedFields.includes("jobState"));
  assert.ok(domain.safePayload.allowedFields.includes("safeDisplay"));
  for (const forbidden of ["body", "prompt", "token", "secret", "providerPayload", "rawHtml", "pii"]) {
    assert.ok(domain.safePayload.forbiddenFields.includes(forbidden), forbidden);
  }
  assert.match(domain.idempotency.sameRequest, /original terminal receipt/i);
  assert.match(domain.idempotency.differentRequest, /IDEMPOTENCY_KEY_REUSED/);
  assert.match(domain.idempotency.retention, /future persistence policy/i);
});

test("canonical fixture covers two tenants, allowed topics and unsafe routing denials", () => {
  assert.equal(fixture.tenants.length, 2);
  assert.notEqual(fixture.tenants[0].organizationId, fixture.tenants[1].organizationId);
  assert.equal(fixture.events.length, 2);
  assert.match(fixture.events[0].topic, /^tenant:/);
  assert.match(fixture.events[1].topic, /^resource:/);
  assert.equal(fixture.events[0].safePayload.safeDisplay.status, "queued");
  assert.equal(fixture.events[1].safePayload.jobState, "RUNNING");
  assert.equal(fixture.idempotency.differentRequest, "IDEMPOTENCY_KEY_REUSED");
  assert.ok(fixture.idempotency.negativeCases.some((caseText) => /unowned subject/i.test(caseText)));
  assert.ok(fixture.idempotency.negativeCases.some((caseText) => /providerPayload/i.test(caseText)));
  assert.ok(fixture.idempotency.negativeCases.some((caseText) => /terminal outbox failure/i.test(caseText)));
});
