import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const domain = JSON.parse(await readFile(new URL("../domain/v1/knowledge-rag.json", import.meta.url), "utf8"));
const fixture = JSON.parse(await readFile(new URL("../fixtures/v1/knowledge-rag.json", import.meta.url), "utf8"));

test("knowledge-rag contract is frozen with authority rules", () => {
  assert.equal(domain.contractVersion, "1.0.0");
  assert.equal(domain.task, "M4-C01");
  assert.equal(domain.status, "frozen-contract-only");
  assert.equal(domain.authority.tenantAuthority.includes("never authority"), true);
  assert.equal(domain.authority.retrievalAuthority.includes("STOP"), true);
  assert.equal(domain.authority.chatAuthority.includes("reauthorizes"), true);
});

test("document lifecycle is bounded and honest", () => {
  const states = domain.document.states;
  assert.equal(states.includes("UPLOADED"), true);
  assert.equal(states.includes("READY"), true);
  assert.deepEqual(domain.document.retrievalEligibleStates, ["READY"]);
  assert.equal(domain.document.formats.urlIngestion, "disabled-until-independent-ssrf-gate");
  assert.equal(domain.document.limits.maxBytesPerDocument, 52428800);
  assert.equal(domain.document.limits.maxChunksPerDocument, 5000);
  assert.equal(
    domain.document.rules.some((rule) => rule.includes("MIME is sniffed")),
    true,
  );
});

test("job states are durable and bounded", () => {
  assert.equal(domain.documentJob.maxAttempts, 5);
  assert.deepEqual(domain.documentJob.terminalStates, ["SUCCEEDED", "FAILED", "CANCELLED"]);
  assert.equal(
    domain.documentJob.rules.some((rule) => rule.includes("same database transaction")),
    true,
  );
});

test("chunk and vector contracts pin model and strategy versions", () => {
  assert.equal(domain.chunk.chunking.strategyVersion, "nexora-chunk-v1");
  assert.equal(domain.chunk.chunking.maxTokens, 1500);
  assert.equal(domain.chunk.chunking.overlapTokens, 100);
  assert.equal(domain.vector.model.v01Accepted.modelId, "qwen3-embedding-0.6b");
  assert.equal(domain.vector.model.v01Accepted.dimensions, 1024);
  assert.equal(
    domain.vector.model.rules.some((rule) => rule.includes("dimension mismatch")),
    true,
  );
});

test("retrieval fusion is deterministic and bounded", () => {
  assert.equal(domain.retrieval.fusion.algorithm, "reciprocal-rank-fusion");
  assert.equal(domain.retrieval.fusion.version, "nexora-rrf-v1");
  assert.equal(domain.retrieval.fusion.parameters.finalTopK, 10);
  assert.equal(domain.retrieval.fusion.parameters.maxCandidatesForContext, 8);
  assert.equal(
    domain.retrieval.fusion.rules.some((rule) => rule.includes("equivalent tenant/permission predicates") || rule.includes("degrades")),
    true,
  );
});

test("chat messages never mislabel drafts and regenerate preserves lineage", () => {
  assert.equal(domain.chat.message.roles.includes("assistant"), true);
  assert.equal(domain.chat.message.states.includes("STREAMING"), true);
  assert.equal(domain.chat.message.fields.includes("clientMessageId"), true);
  assert.equal(domain.chat.message.fields.includes("clientMessageIdDigest"), true);
  assert.equal(
    domain.chat.message.rules.some((rule) => rule.includes("never labeled COMPLETED")),
    true,
  );
  assert.equal(
    domain.chat.message.rules.some((rule) => rule.includes("parentMessageId")),
    true,
  );
});

test("citation rules require resolvable authorized sources", () => {
  assert.equal(
    domain.chat.citations.rules.some((rule) => rule.includes("resolves to an authorized chunk")),
    true,
  );
  assert.equal(
    domain.chat.citations.rules.some((rule) => rule.includes("unavailable-source state")),
    true,
  );
});

test("deletion order makes sources retrieval-ineligible first", () => {
  assert.equal(domain.lifecycle.deletionOrder[0], "mark document DELETED (retrieval-ineligible)");
  assert.equal(domain.lifecycle.deletionOrder[domain.lifecycle.deletionOrder.length - 1], "record durable receipt");
  assert.equal(
    domain.lifecycle.deletionOrder.some((step) => step.includes("cancel or terminalize")),
    true,
  );
  assert.equal(
    domain.lifecycle.deletionOrder.some((step) => step.includes("index entries")),
    true,
  );
  assert.equal(domain.lifecycle.chatDeletionOrder.length >= 4, true);
  assert.equal(
    domain.lifecycle.rules.some((rule) => rule.includes("never remain retrievable")),
    true,
  );
});

test("security invariants cover the stop conditions", () => {
  const invariants = domain.securityInvariants;
  assert.equal(invariants.some((rule) => rule.includes("rechecked before context assembly")), true);
  assert.equal(invariants.some((rule) => rule.includes("untrusted data")), true);
  assert.equal(invariants.some((rule) => rule.includes("tenant plus subject")), true);
});

test("fixture keeps the two tenants disjoint with a positive control", () => {
  const a = fixture.organizationA;
  const b = fixture.organizationB;
  assert.notEqual(a.organizationId, b.organizationId);
  assert.notEqual(a.knowledgeBaseId, b.knowledgeBaseId);
  assert.deepEqual(fixture.expectedRetrieval.crossTenantProbeForOrgA, []);
  assert.deepEqual(fixture.expectedRetrieval.crossTenantProbeForOrgB, ["41000000-0000-4000-8000-000000000001"]);
  assert.equal(
    fixture.chunks.orgBSecretChunk.documentId,
    fixture.documents.orgBSecret.documentId,
  );
  assert.equal(
    fixture.documents.orgAText.state,
    "READY",
  );
  assert.equal(fixture.chat.orgBMemberProbeSession.subjectId, b.memberSubjectId);
  assert.equal(fixture.chat.orgAMember.subjectId, a.memberSubjectId);
});

test("every fixture chunk references a declared document", () => {
  const documentIds = new Set(Object.values(fixture.documents).map((document) => document.documentId));
  for (const chunk of Object.values(fixture.chunks)) {
    assert.equal(documentIds.has(chunk.documentId), true, `chunk ${chunk.chunkId} references unknown document`);
  }
});
