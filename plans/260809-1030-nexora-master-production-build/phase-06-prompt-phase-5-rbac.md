---
phase: 6
title: "Prompt Phase 5 — RBAC"
status: pending
priority: P1
effort: "4-6 days"
dependencies: [5]
---

# Prompt Phase 5 — RBAC

## Outcome

Provide tenant-scoped roles, permissions, assignments, evaluator and accessible administration with deny-by-default enforcement.

## Requirements

- Versioned permission vocabulary owned by the domain contract.
- Built-in roles plus controlled custom assignments if accepted.
- Server-side evaluator and method/endpoint guards; UI checks are convenience only.
- Tenant-safe role/permission CRUD with last-owner/invariant protection.
- Audit actor, target, permission change and safe metadata.

## Implementation Steps

1. Freeze permission taxonomy for current milestones.
2. Add roles, permissions and assignment migrations/constraints/indexes.
3. Implement pure evaluator with deny-by-default and caching semantics.
4. Apply guards to organization/member administration.
5. Build role matrix UI with complete permission/error states.
6. Run exhaustive actor/action/tenant matrix.

## Planned Paths

Platform `authorization/**`, RBAC migrations, web `roles/**` and shared permission types. The permission vocabulary has one contract owner.

## Validation

- Owner/admin/editor/viewer/anonymous and custom assignment cases.
- Cross-tenant role and assignment access denial.
- Privilege escalation, removed permission, last-owner, stale cache and concurrent update cases.
- Accessibility of permission matrix and destructive confirmation.

## Commit Plan

- `feat(authz): add organization permission model`
- `feat(authz): enforce permission evaluator`
- `feat(admin): add accessible role management`
- `test(security): prevent role escalation`

## Acceptance

- [ ] Every protected endpoint names required permission.
- [ ] UI cannot grant a permission the actor lacks authority to assign.
- [ ] Security matrix passes at exact head.
- [ ] Audit records are safe and immutable enough for current phase.

## Stop Conditions

UI-only enforcement, global role assignment across tenants, allow-by-default fallthrough, unbounded stale permission cache.
