---
phase: 5
title: "Prompt Phase 4 — Identity and Tenancy"
status: pending
priority: P1
effort: "6-9 days"
dependencies: [3, 4]
---

# Prompt Phase 4 — Identity and Tenancy

## Outcome

Establish authenticated identity, organization membership and tenant context that cannot be selected or forged by request payload alone.

## Architecture

- Supabase Auth is proposed identity issuer; Spring validates current tokens and owns domain authorization.
- Organization context derives from authenticated membership and explicit selection among allowed memberships.
- Tenant-owned tables carry tenant keys, composite uniqueness/indexes and policy review.
- Browser domain traffic uses the same-origin Next.js BFF/Server Component boundary before Spring; approved direct Supabase use is restricted to Auth, signed private Storage and authorized private Realtime contracts.

## Implementation Slices

1. Organization/member schema, migrations and repository boundary.
2. Token validation and safe authentication principal.
3. Membership-derived tenant resolution and request context.
4. Organization/member APIs and UI flows.
5. `/profile` API/UI with allowlisted display/locale/accessibility preferences, optimistic concurrency, validation and lifecycle/export hooks; no authorization data is user-editable.
6. RLS/storage policy foundations and two-tenant fixtures.
7. Audit-safe identity/tenant/profile events.

## Security Test Matrix

Anonymous, valid member, removed member, wrong organization, forged org header/body, expired/invalid token, service role, cross-tenant identifiers, concurrent membership changes.

## Planned Ownership

`apps/platform-api/**/identity/**`, `**/tenant/**`, `**/profile/**`, related migrations/policies, `apps/web/**/auth/**`, `**/organizations/**`, `**/profile/**`, shared auth contracts. Tenant middleware and migrations are serialized; the profile worker cannot edit membership/RBAC rules.

## Commit Plan

- `feat(auth): validate external identity tokens`
- `feat(tenant): derive organization context from membership`
- `feat(profile): add validated user profile management`
- `test(security): verify tenant isolation matrix`

## Acceptance

- [ ] Two organizations and multiple roles are represented in deterministic fixtures.
- [ ] Cross-tenant page/member/object requests are denied at API and policy layers.
- [ ] Service-role credentials never reach browser bundles.
- [ ] Membership removal takes effect according to documented cache/session semantics.
- [ ] Identity logs contain safe identifiers only.
- [ ] Authenticated users can view/update only their allowlisted profile fields; stale writes conflict explicitly, account lifecycle/export hooks are present and `/profile` passes 375px/keyboard/denied/error tests.

## Stop Conditions

Tenant derives from untrusted request alone; RLS bypass behavior unknown; shared singleton holds request tenant state; any cross-tenant success.
