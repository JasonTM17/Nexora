#!/usr/bin/env node
// M4-T08 reproducible retrieval evaluation report.
// The corpus is the accepted M4-S01 demo seed; expected outcomes are pinned
// in the manifest. This script only reads the fixture files and computes the
// deterministic allow/deny expectations; it never calls a provider.
import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../../../", import.meta.url));
const manifest = JSON.parse(await readFile(`${root}test-fixtures/demo/m4-seed-manifest.json`, "utf8"));

let allowQueries = 0;
let denyQueries = 0;
const rows = [];

for (const expectation of manifest.retrievalExpectations) {
  const expectedOutcome = expectation.expectedOutcome ?? (expectation.expectedChunkSource ? "ANSWERED" : "NO_MATCH");
  if (expectedOutcome === "ANSWERED") allowQueries += 1;
  else denyQueries += 1;
  rows.push({
    query: expectation.query,
    expectedOutcome,
    crossTenantDenied: expectation.crossTenantDenied ?? false,
  });
}

const report = {
  corpus: manifest.seedSchemaVersion,
  evaluatedQueries: rows.length,
  allowQueries,
  denyQueries,
  expectations: rows,
  generatedAt: new Date().toISOString(),
  honesty: "Deterministic fixture expectations only; no live model quality claim.",
};

process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
