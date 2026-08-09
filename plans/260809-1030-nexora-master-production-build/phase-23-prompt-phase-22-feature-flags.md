---
phase: 23
title: "Prompt Phase 22 — Feature Flags"
status: pending
priority: P2
effort: "5-8 days"
dependencies: [5, 6, 15]
---

# Prompt Phase 22 — Feature Flags

## Outcome

Implement tenant-scoped flags, targeting, stable percentage rollout, kill switch and audit with deterministic server/client-consistent assignment.

## Domain Contract

- Flag key/version/environment/tenant scope and safe default.
- Targeting rule schema is allowlisted and bounded.
- Percentage assignment uses a documented stable hash over approved subject key and salt/version.
- Kill switch overrides targeting immediately according to cache policy.
- Evaluation result includes reason metadata for debugging, not sensitive attributes.

## Implementation Slices

1. Flag/rule migration and audit interface.
2. Pure deterministic evaluator and assignment fixtures.
3. CRUD/evaluation APIs with RBAC.
4. Admin UI and change history.
5. Cache/invalidation/failure/default behavior.
6. Distribution, stability, cross-tenant and kill-switch tests.

## Planned Ownership

Platform `flags/**`, flag migration, web `admin/flags/**`, shared evaluation contract. Hash/rule vocabulary has one owner.

## Commit Plan

- `feat(flags): add tenant flag domain`
- `feat(flags): implement deterministic rollout evaluator`
- `feat(admin): add audited flag management`
- `test(flags): verify stable assignment and kill switch`

## Acceptance

- [ ] Assignment is stable for same inputs and isolated across tenants/environments.
- [ ] Kill switch behavior and cache propagation are tested.
- [ ] Unauthorized mutation/evaluation details are denied.
- [ ] Audit record exists for every sensitive change.

## Stop Conditions

Random per-request assignment, browser-only authority, targeting arbitrary code, unsafe default enabling critical feature, unaudited kill-switch mutation.
