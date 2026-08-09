---
phase: 37
title: "Prompt Phase 36 — Terraform"
status: pending
priority: P2
effort: "8-14 days"
dependencies: [35, 36]
---

# Prompt Phase 36 — Terraform

## Outcome

Automate only the accepted infrastructure target with reviewed state, security, cost, recovery and managed-service boundaries.

## Preconditions

`DEC-005`, budget, region/data policy, environment model, credentials mechanism and ownership are accepted. Terraform is not written generically before a real target exists.

## Requirements

- Provider/version lock, remote encrypted state/locking and bootstrap procedure.
- Least-privilege identities, network boundaries, encryption and secret references.
- Modules only where reuse is real; explicit inputs/outputs and no sensitive output.
- Cost estimate/ceiling, tags/ownership and drift procedure.
- Managed versus self-managed boundaries documented.
- Destruction/import/migration/backup considerations.

## Planned Ownership

`infrastructure/terraform/**`, policy/validation CI and architecture decision. Provider/backend/state/outputs are controlled by one infrastructure owner.

## Validation

- Format, validate, lint/security/policy checks.
- Plan for each approved environment; review destructive changes.
- Apply only after R3 approval in isolated target.
- Idempotent second plan, drift/import and recovery tests where safe.
- No secrets or state committed/logged.

## Commit Plan

- `feat(terraform): define approved platform infrastructure`
- `chore(terraform): add policy and plan validation`
- `docs(operations): document state and recovery boundaries`

## Acceptance

- [ ] Reviewed plan matches approved architecture/budget.
- [ ] State is protected and recoverable.
- [ ] Apply evidence and resource inventory exist when deployment is in scope.
- [ ] Managed-service assumptions and limitations are explicit.

## Stop Conditions

No target/budget, local public state, secret output, unreviewed destroy/replace, provider credentials in files/logs.
