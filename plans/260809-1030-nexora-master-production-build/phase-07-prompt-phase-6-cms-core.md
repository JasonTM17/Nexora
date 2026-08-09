---
phase: 7
title: "Prompt Phase 6 — CMS Core"
status: pending
priority: P1
effort: "5-7 days"
dependencies: [5, 6]
---

# Prompt Phase 6 — CMS Core

## Outcome

Create tenant-safe page aggregates, slugs, metadata, listing, draft creation and basic editing as the durable foundation for schema-driven composition.

## Domain Contract

- Page identity is distinct from immutable page versions.
- Slug uniqueness is tenant/site scoped and enforced by database constraint.
- Metadata has explicit validation and no raw executable content.
- SEO metadata is tenant/site/page scoped and typed: title, description, canonical policy, Open Graph/Twitter fields and allowlisted JSON-LD inputs; sitemap/robots policy is site-owned rather than arbitrary raw text.
- Draft changes use optimistic concurrency/version markers.
- Deletion/archive/retention behavior is explicit.

## Implementation Slices

1. Page/site/draft schema and migrations.
2. Repository/service transaction boundaries.
3. Create/list/get/update/archive APIs with stable pagination/errors.
4. Basic admin list/editor UI and complete states.
5. Tenant, authorization, slug conflict and concurrency tests.
6. Audit events for sensitive page mutations.
7. SEO metadata editor/API fixtures with length, URL, locale, unsafe structured-data and cross-tenant tests.

## Planned Ownership

Platform `cms/page/**`, page migrations, web `admin/pages/**`, page contracts. One migration owner and one page contract owner.

## Validation

- Same slug allowed in different tenants but rejected in same scope.
- Stable cursor/order pagination.
- Cross-tenant IDs and enumeration attempts denied.
- Concurrent update returns explicit conflict instead of lost write.
- List/editor states: loading, empty, error, forbidden, conflict, destructive confirmation.
- SEO preview shows computed canonical/social metadata and validation; it never executes arbitrary JSON-LD or trusts an editor-supplied foreign canonical host without policy.

## Commit Plan

- `feat(cms): add tenant-scoped page aggregate`
- `feat(cms): expose draft page APIs`
- `feat(admin): add page list and basic editor`
- `test(cms): cover slug and concurrency invariants`

## Acceptance

- [ ] Two tenants can independently create and edit pages.
- [ ] Durable page/draft state survives restart.
- [ ] Database constraints match API validation.
- [ ] Browser flow and API integration tests pass.

## Risks

Combining mutable page and immutable version state, global slug uniqueness, weak pagination order, soft-delete leakage.
