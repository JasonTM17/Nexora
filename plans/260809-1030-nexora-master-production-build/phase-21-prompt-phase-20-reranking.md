---
phase: 21
title: "Prompt Phase 20 — Reranking"
status: pending
priority: P2
effort: "3-5 days"
dependencies: [19, 20]
---

# Prompt Phase 20 — Reranking

## Outcome

Add an optional bounded reranker behind an abstraction only when reproducible evaluation demonstrates useful quality/cost trade-off.

## Requirements

- Reranker consumes only already-authorized candidates.
- Provider/local implementation, model/version, input limits, timeout and cost are explicit.
- Stable fallback returns original hybrid order without weakening authorization/citations.
- Evaluation compares fixed corpus and thresholds before/after.

## Planned Ownership

Platform `rag/rerank/**`, provider adapter/config, evaluation reports. Retrieval result contract changes require Phase 18 owner review.

## Validation

- Deterministic stub/local behavior.
- Provider timeout/rate/error fallback.
- Candidate limit and long-text truncation.
- Baseline versus rerank Recall/MRR/citation/latency/cost report.
- Permission and citation identity preserved.

## Commit Plan

- `feat(rag): add optional reranker abstraction`
- `test(rag): benchmark reranking quality and cost`

## Acceptance

- [ ] Measurable benefit or explicit decision to disable by default.
- [ ] Failure preserves safe original ranking.
- [ ] No extra unauthorized content/provider egress.
- [ ] Claims cite corpus, model, parameters, date and raw result.

## Stop Conditions

Reranking before authorization, silent fallback policy change, quality claim without fixed corpus, cost/latency unbounded.
