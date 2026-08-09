---
phase: 19
title: "Prompt Phase 18 — Hybrid RAG"
status: pending
priority: P1
effort: "4-7 days"
dependencies: [18]
---

# Prompt Phase 18 — Hybrid RAG

## Outcome

Combine authorized lexical and vector retrieval through a deterministic fusion contract with configurable bounded top-K and measurable quality.

## Architecture

- Lexical and vector adapters emit normalized result objects with document/chunk/provenance and scores.
- Both branches apply equivalent tenant/permission predicates.
- Fusion algorithm and parameters are versioned and deterministic.
- Candidate and context limits prevent token/cost explosion.

## Implementation Slices

1. Lexical index/query with language/tokenization decision.
2. Vector adapter over Phase 17.
3. Reciprocal-rank or accepted fusion strategy.
4. Query configuration and safe defaults.
5. Evaluation fixtures including exact, semantic, rare-term and no-match queries.
6. Query plans/latency traces and failure fallback.

## Planned Ownership

Platform `rag/retrieval/**`, lexical indexes/migration, evaluation fixtures. Retrieval result contract and fusion logic have one owner.

## Validation

- Deterministic ranking and duplicate fusion.
- Permission predicate parity across lexical/vector paths.
- One branch unavailable or empty.
- Top-K/max-candidate bounds.
- Predeclared Recall@K/MRR or equivalent on versioned corpus.

## Commit Plan

- `feat(rag): add authorized lexical retrieval`
- `feat(rag): implement hybrid result fusion`
- `test(rag): add retrieval evaluation corpus`

## Acceptance

- [ ] Hybrid retrieval improves or honestly matches documented baseline.
- [ ] No unauthorized candidate appears.
- [ ] Configuration and corpus provenance are recorded.
- [ ] Latency and query-plan limitations are explicit.

## Stop Conditions

Different auth predicates by retrieval path, rankings not reproducible, unbounded candidates, evaluation corpus/private leakage.
