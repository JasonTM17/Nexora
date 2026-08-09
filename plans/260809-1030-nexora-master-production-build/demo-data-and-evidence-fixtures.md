# Nexora Demo Data and Evidence Fixture Contract

## Outcome

Create one deterministic, tenant-safe, development/test-only fixture pack that supports integration tests, RAG evaluation, screenshots, GIFs and documented demo journeys without fabricated production data or public passwords.

## Canonical Pack

| Class | Required fixture |
|---|---|
| Organization | `Nexora University` plus a separate hostile/control tenant for isolation tests |
| Pages | `Home`, `Programs`, `Events`, `Knowledge Center`, `About` |
| Documents | `Student Handbook 2026`, `Academic Regulations`, `AI Lab Guide`, `Cloud Computing Handbook` |
| Personas | `admin`, `editor`, `reviewer`, `student` role identities plus removed/anonymous/other-tenant fixtures |
| Builder | All five block types, visible/hidden states, draft/published/rolled-back versions and one deliberate conflict |
| Knowledge/RAG | Versioned synthetic document bodies with page anchors, expected chunks, citations, allow/deny queries and injection fixtures |
| Failure states | queued/failed/retry job, Realtime disconnected, provider degraded, no-answer, denied and empty states |

The names are stable public demo labels; UUIDs, timestamps and content checksums are deterministic per seed schema version. Document content is original synthetic Nexora fixture text, not copied manuals or private data.

## Credential Safety

- No password, access token, API key, service-role key or reusable credential is committed.
- Seed accounts are documented by role/identifier and variable name. Local/CI credentials are generated or injected ephemerally through untracked environment/secret authorities.
- Seed tooling refuses a production project/hostname/profile and requires an explicit local/CI/approved-preview environment marker.
- Screenshots/GIFs never expose passwords, reset links, tokens, cookies, project refs or secret-bearing browser panels.

## Reproducibility and Reset

1. Seed manifest pins schema version, migration head, seed-tool version, ordered fixture IDs, source file hashes and expected row/object/chunk counts.
2. Apply is idempotent: a repeated run yields the same logical fixture state or a precise version mismatch.
3. Reset is allowlisted to fixture tenant IDs and blocked outside disposable environments; it never truncates a broad database or Storage bucket.
4. Storage objects and extracted/chunked/vector expectations have independent hashes and a reconciliation watermark.
5. Seed validation runs two-tenant allow/deny checks, page render/schema checks, publish/rollback, upload/process, expected retrieval/citation and deletion propagation.

## Task and Path Ownership

- One seed-contract owner freezes `test-fixtures/demo/**`, logical IDs, source texts and manifest schema.
- M2 seed writer owns the Nexora University users/pages/theme/workflow data after the accepted M2 contracts; it cannot edit migrations/domain code.
- M4 seed writer sequentially extends the same accepted pack with documents/chunks/RAG expectations after M4 schema/contracts; it cannot overwrite M2 identities or publish claims.
- Each extension uses a dedicated `test/...` branch/worktree and is mechanically integrated before the milestone E2E gate.
- Product media writers consume an accepted seed manifest; they cannot silently edit fixtures to make screenshots appear successful.

## Evidence and Media Use

Every capture records seed manifest digest, product SHA, migration/event/page-schema/model revision, route/state/viewport and whether generation was deterministic or a bounded live provider call. Visible demo dashboards are labeled `Demo data` or equivalent. The GIF walkthrough uses the same accepted pack and transcript; no fixture metric is described as live customer or production telemetry.

## Acceptance

- Fresh local/CI setup can apply and validate the pack from documented commands without a committed secret.
- Both tenant-positive and cross-tenant-negative scenarios pass.
- Required pages, documents and personas match the source prompt names and checksums.
- Reset cannot target staging/production or unrecognized tenant/object IDs.
- Builder, publish, RAG and failure-state screenshots are reproducible from the exact accepted manifest.
- Seed content/license/provenance scan passes; product evidence clearly distinguishes fixtures from live service proof.

## Stop Conditions

Known public password, real personal/private data, production-capable reset, non-idempotent seed, fixture edits by a media writer, invented live metrics, copied document content without provenance, or media captured from a different seed/product identity than its manifest.
