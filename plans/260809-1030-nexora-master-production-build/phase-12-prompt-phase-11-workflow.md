---
phase: 12
title: "Prompt Phase 11 — Content Workflow"
status: pending
priority: P1
effort: "4-6 days"
dependencies: [6, 10]
---

# Prompt Phase 11 — Content Workflow

## Outcome

Implement a tenant-safe review state machine for submit, approve, reject and publish authorization with complete actor/reason history.

## State Contract

Proposed states: `DRAFT → IN_REVIEW → APPROVED → PUBLISHED`, with `REJECTED` returning to editable draft through an explicit transition. Exact states are finalized during implementation ADR.

## Requirements

- Transition preconditions and permitted roles are server-owned.
- Review operates on an immutable candidate version or explicit draft revision.
- Every transition records actor, timestamp, reason/comment, tenant and correlation ID using safe metadata.
- Concurrent review/publish attempts have deterministic conflict behavior.
- UI exposes pending work, history, reasons, denied actions and stale-version conflict.

## Planned Ownership

Platform `cms/workflow/**`, related migration/audit event; web `admin/review/**`; shared workflow enum/contract. Page publication state is coordinated with Phase 9 owner.

## Validation

- Creator cannot self-publish unless role policy explicitly permits.
- Reviewer cannot review another tenant or stale candidate.
- Reject requires reason if accepted contract says so.
- Duplicate transition is idempotent or explicit conflict.
- Permission removal during review takes effect safely.

## Commit Plan

- `feat(workflow): add page review state machine`
- `feat(admin): add content review workspace`
- `test(security): verify workflow transition permissions`

## Acceptance

- [ ] End-to-end creator → reviewer → publisher scenario passes.
- [ ] Transition history is immutable and queryable.
- [ ] Publish still executes Phase 9 transactional validation.
- [ ] UI states and accessibility checks pass.

## Stop Conditions

Client-controlled state mutation, transition without fresh permission check, mutable review history, self-approval contrary to accepted policy.
