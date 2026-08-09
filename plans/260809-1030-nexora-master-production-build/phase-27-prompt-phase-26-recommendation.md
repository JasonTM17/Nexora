---
phase: 27
title: "Prompt Phase 26 — Recommendation"
status: pending
priority: P2
effort: "6-9 days"
dependencies: [25, 26]
---

# Prompt Phase 26 — Recommendation

## Outcome

Implement an initial explainable, reproducible recommendation engine using approved event/content signals and safe fallbacks.

## Initial Approach

Prefer a rules/weighted-signal or content-similarity baseline before opaque ML. Every recommendation includes reason codes and source signal version. Advanced ML requires a later evidence-backed decision.

## Requirements

- Eligible-item and permission filtering before scoring.
- Signal definitions, freshness, consent/purpose and deletion behavior.
- Deterministic score/tie-break and bounded candidate set.
- Cold-start/no-signal fallback to curated/popular-with-provenance items.
- Feedback/evaluation avoids self-reinforcing fake success claims.

## Planned Ownership

Platform `recommendation/**`, scoring configuration, API, renderer/UI integration and evaluation fixtures. Signal vocabulary is coordinated with analytics owner.

## Validation

- Known signals produce expected rank/explanation.
- Unauthorized/ineligible items never appear.
- Cold start, stale/missing signals, deleted user/content and provider/DB failure.
- Tenant isolation and consent withdrawal.
- Offline evaluation and qualitative fixture review.

## Commit Plan

- `feat(recommendations): add explainable signal scorer`
- `feat(recommendations): expose authorized recommendation API`
- `test(recommendations): add reproducible ranking fixtures`

## Acceptance

- [ ] Recommendations are derived from captured/fixture-labeled evidence.
- [ ] Explanation and fallback are visible.
- [ ] No cross-tenant/content-permission leakage.
- [ ] Quality limitations and lack of causal proof are explicit.

## Stop Conditions

Fake recommendations, unauthorized candidates scored, opaque unexplained model, consent/deletion ignored, metric presented as causation.
