---
phase: 18
title: "Prompt Phase 17 — PGVector"
status: pending
priority: P1
effort: "4-7 days"
dependencies: [17]
---

# Prompt Phase 17 — PGVector

## Outcome

Generate and persist embeddings with explicit model/dimension provenance, tenant/permission metadata, indexes and safe similarity queries.

## Requirements

- Embedding provider abstraction with timeout, concurrency, token and cost limits.
- Deterministic fake/local provider for CI; live provider smoke separated.
- Vector rows include tenant, document/chunk identity, permission scope, model, dimension, checksum and lifecycle state.
- Query predicates enforce tenant/permission constraints before candidate output.
- Dimension/model changes use separate storage or complete versioned reindex.
- Managed Supabase extension setup uses no explicit SQL version clause; record the actually installed `vector` version and block when it falls outside the application-tested compatibility range.

## Planned Ownership

Vector migration/index, platform `rag/embedding/**` and `rag/vector/**`, provider configuration by variable name, fixtures. Vector migration has one owner.

## Validation

- Known-vector similarity and index plan tests.
- Cross-tenant/unauthorized chunks absent from candidate set.
- Provider timeout/rate/error and bounded retry.
- Reindex idempotency and dimension mismatch rejection.
- Source deletion/tombstone removes future eligibility.
- Local and authorized managed-target receipts distinguish pinned container/CLI versions from the observed Supabase PostgreSQL/`vector` versions and preserve representative query plans.

## Commit Plan

- `feat(rag): add embedding provider contract`
- `feat(rag): persist tenant-scoped vectors`
- `test(rag): verify similarity and permission predicates`

## Acceptance

- [ ] CI passes without paid provider.
- [ ] Live provider evidence, if run, names model/dimension/date separately.
- [ ] Query plan/index behavior is documented for representative corpus.
- [ ] No provider secret or raw document body enters logs/traces.

## Stop Conditions

Provider output dimension untracked, authorization applied after candidates, unbounded reindex/cost, mock presented as live result, or an explicit extension version request presented as a verified managed Supabase pin.
