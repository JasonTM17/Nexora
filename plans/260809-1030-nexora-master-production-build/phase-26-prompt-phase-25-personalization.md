---
phase: 26
title: "Prompt Phase 25 — Personalization"
status: pending
priority: P2
effort: "7-10 days"
dependencies: [8, 23, 25]
---

# Prompt Phase 25 — Personalization

## Outcome

Add bounded explainable rules-based personalization to the schema renderer with consent, safe defaults and deterministic fallback.

## Requirements

- Allowlisted condition vocabulary and versioned audience/rule schema.
- Inputs have defined source, freshness, privacy purpose and missing-value behavior.
- Rules select only approved component variants/content, never executable code.
- Decision returns reason and rule/version for audit/debug.
- Anonymous/no-consent/no-match/error paths use safe default content.
- Authenticated users can bookmark authorized published content and follow/unfollow tenant-approved topics; bookmark/topic-follow state has explicit privacy purpose, tenant scope and deletion/export behavior.
- `/bookmarks` exposes stable pagination, removed/unpublished/unavailable states and reauthorizes every target; a bookmark never preserves access to content the user can no longer view.

## Planned Ownership

Platform `personalization/**`, bookmark/topic-follow migrations and API, `/bookmarks`/topic-preference UI, rule migration/API, renderer decision hook, admin rule UI and evaluation fixtures. Renderer contract changes require schema owner review; migration and UI/API writers remain separate.

## Validation

- Deterministic decision for same context.
- Cross-tenant rule/data isolation.
- Missing/stale/conflicting attributes and rule priority.
- Consent withdrawal and deletion propagation.
- Bookmark/follow create/delete/idempotency, stable pagination, removed-content reauthorization, cross-tenant denial and profile export/account-delete propagation.
- Flag/experiment interaction order is explicit and tested.
- Public cache key cannot leak personalized output.

## Commit Plan

- `feat(personalization): add bounded audience rules`
- `feat(preferences): add tenant-safe bookmarks and topic follows`
- `feat(renderer): apply explainable personalization decisions`
- `test(security): isolate personalized content by tenant and consent`

## Acceptance

- [ ] Default page remains correct when personalization fails/disabled.
- [ ] Decision is explainable without exposing sensitive attributes.
- [ ] Cache/SSR strategy prevents cross-user leakage.
- [ ] Admin/public UI states and audit evidence pass.
- [ ] `/bookmarks` and topic controls are permission-safe, responsive and accessible; unavailable targets do not leak title/content and derived personalization works without those signals.

## Stop Conditions

Arbitrary expression execution, personalized shared cache leakage, hidden profiling without consent, missing safe default.
