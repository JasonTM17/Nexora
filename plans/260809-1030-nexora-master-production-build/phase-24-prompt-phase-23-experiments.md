---
phase: 24
title: "Prompt Phase 23 — Experiments"
status: pending
priority: P2
effort: "6-9 days"
dependencies: [23]
---

# Prompt Phase 23 — Experiments

## Outcome

Create experiments, variants and stable assignment with correct exposure/conversion semantics; defer final metrics claims until the real Phase 24 analytics pipeline is integrated.

## Requirements

- Explicit hypothesis, primary metric, guardrails, audience, start/end and status.
- Variant weights validated to accepted total and assignment tied to experiment version.
- Exposure emitted only when treatment is actually rendered/used.
- Conversion attribution window and deduplication are documented.
- Stop/pause does not rewrite prior assignments/evidence.
- Dashboard separates fixture, collecting, insufficient-data and analyzed states.

## Planned Ownership

Experiment migration/domain/API, admin UI, shared exposure/conversion event contract. Assignment and event semantics are serialized with flags/analytics owners.

## Validation

- Stable assignment and weight distribution fixtures.
- Tenant/audience/permission isolation.
- No exposure before actual treatment.
- Duplicate exposure/conversion handling.
- Paused/ended/version-changed behavior.
- Honest insufficient-sample display; no automatic winner without accepted statistical rule.

## Commit Plan

- `feat(experiments): add experiment and variant domain`
- `feat(experiments): persist stable assignments`
- `feat(admin): add experiment lifecycle UI`
- `test(experiments): verify exposure semantics`

## Acceptance

- [ ] Domain and assignment work independently of analytics dashboard.
- [ ] Phase 24 receipt completes real event/metric integration.
- [ ] Audit and consent/privacy rules are applied.
- [ ] Dashboard makes data provenance and uncertainty visible.

## Stop Conditions

Winner label without rule/sample, exposure emitted on assignment alone, mutable historical variant meaning, cross-tenant experiment access.
