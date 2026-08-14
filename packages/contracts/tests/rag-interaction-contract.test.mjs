import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import test from "node:test";

const domain = JSON.parse(await readFile(new URL("../domain/v1/rag-interaction.json", import.meta.url), "utf8"));

test("stream contract never labels drafts completed early", () => {
  assert.equal(domain.stream.protocol, "same-origin-sse");
  const events = domain.stream.events.map((entry) => entry.event);
  assert.equal(events.includes("stream.start"), true);
  assert.equal(events.includes("stream.delta"), true);
  assert.equal(events.includes("stream.citations"), true);
  assert.equal(events.includes("stream.complete"), true);
  assert.equal(events.includes("stream.error"), true);
  assert.equal(
    domain.stream.rules.some((rule) => rule.includes("never labeled COMPLETED")),
    true,
  );
});

test("no-answer triggers are enumerated and honest", () => {
  assert.equal(domain.noAnswer.triggers.includes("emptyCandidateSet"), true);
  assert.equal(domain.noAnswer.triggers.includes("bestFusedScoreBelowThreshold"), true);
  assert.equal(domain.noAnswer.rules.some((rule) => rule.includes("honest no-answer")), true);
});

test("provider boundary separates deterministic CI from live", () => {
  assert.equal(domain.provider.deterministicCiProvider, "recorded-chat-provider");
  assert.equal(
    domain.provider.rules.some((rule) => rule.includes("untrusted data")),
    true,
  );
  assert.equal(
    domain.provider.rules.some((rule) => rule.includes("Raw prompts and source text are denied")),
    true,
  );
});

test("trace outcomes match the retrieval run vocabulary", () => {
  assert.deepEqual(domain.trace.outcomes, ["ANSWERED", "NO_ANSWER", "LOW_CONFIDENCE", "CANCELLED", "FAILED"]);
  assert.equal(domain.trace.rules.some((rule) => rule.includes("disabled from default traces")), true);
});

test("conversation idempotency and lineage are frozen", () => {
  assert.equal(domain.conversation.messageFields.includes("clientMessageIdDigest"), true);
  assert.equal(domain.conversation.messageFields.includes("parentMessageId"), true);
  assert.equal(
    domain.conversation.rules.some((rule) => rule.includes("idempotent via clientMessageId")),
    true,
  );
  assert.equal(
    domain.conversation.rules.some((rule) => rule.includes("Regenerate creates a new revision")),
    true,
  );
});

test("security invariants cover the stop conditions", () => {
  const invariants = domain.securityInvariants;
  assert.equal(invariants.some((rule) => rule.includes("rechecked before context assembly")), true);
  assert.equal(invariants.some((rule) => rule.includes("STOP")), true);
  assert.equal(invariants.some((rule) => rule.includes("tenant plus subject")), true);
  assert.equal(invariants.some((rule) => rule.includes("never labeled completed")), true);
});
