import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const domain = JSON.parse(await readFile(new URL("../domain/v1/cms-publishing.json", import.meta.url), "utf8"));
const fixture = JSON.parse(await readFile(new URL("../fixtures/v1/cms-publishing.json", import.meta.url), "utf8"));

test("freezes immutable publish, rollback and idempotency semantics", () => {
  assert.equal(domain.task, "M2-C02");
  assert.equal(domain.status, "frozen-contract-only");
  assert.match(domain.versioning.immutability, /never mutate/i);
  assert.match(domain.versioning.rollback, /distinct new/i);
  assert.match(domain.publishing.idempotency.sameRequest, /without creating another version/i);
  assert.match(domain.publishing.idempotency.differentRequest, /IDEMPOTENCY_KEY_REUSED/);
  assert.ok(domain.publishing.receipt.includes("idempotencyKeyDigest"));
});

test("keeps CMS tenant, concurrency and workflow boundaries fail closed", () => {
  assert.deepEqual(domain.pageAggregate.slug.scope, ["organizationId", "siteId"]);
  assert.match(domain.pageAggregate.draftConcurrency, /VERSION_CONFLICT/);
  assert.deepEqual(domain.workflow.transitions.DRAFT, ["IN_REVIEW"]);
  assert.ok(domain.workflow.rules.some((rule) => /self-approve/i.test(rule)));
  assert.match(domain.authorization.tenantBoundary, /never authorization proof/i);
  assert.ok(domain.errors.includes("PERMISSION_DENIED"));
  assert.ok(domain.errors.includes("WORKFLOW_TRANSITION_DENIED"));
});

test("types SEO and theme safety instead of accepting executable editor input", () => {
  assert.match(domain.seo.fields.canonical, /foreign host/i);
  assert.match(domain.seo.fields.jsonLd, /arbitrary JSON, script, HTML/i);
  assert.deepEqual(domain.theme.forbidden, ["arbitrary CSS", "stylesheet text", "script", "unsafe URL", "unbounded font or asset URL"]);
  assert.match(domain.seo.crawlerRule, /exact published tenant\/site version/i);
  assert.ok(domain.safeAuditVocabulary.forbiddenFields.includes("draftContent"));
});

test("canonical fixture covers scoped slugs and idempotent publication", () => {
  assert.equal(fixture.pages[0].slug, fixture.pages[1].slug);
  assert.notEqual(fixture.pages[0].organizationId, fixture.pages[1].organizationId);
  assert.equal(fixture.publication.retryResult, "same-receipt-no-new-version");
  assert.equal(fixture.publication.differentFingerprintResult, "IDEMPOTENCY_KEY_REUSED");
  assert.ok(fixture.negativeCases.some((caseText) => /stale draftVersion/i.test(caseText)));
  assert.ok(fixture.negativeCases.some((caseText) => /foreign canonical host/i.test(caseText)));
});
