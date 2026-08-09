---
phase: 10
title: "Prompt Phase 9 — Versioning and Publishing"
status: pending
priority: P1
effort: "7-10 days"
dependencies: [7, 8, 9]
---

# Prompt Phase 9 — Versioning and Publishing

## Outcome

Make preview, immutable publication and rollback transactionally correct and idempotent; public pages update without frontend redeployment.

## Domain Invariants

- Published version rows/documents never mutate.
- Publish validates schema, permissions, workflow and theme references inside a defined transaction boundary.
- Idempotency key prevents duplicate versions from retried requests.
- Rollback creates a new version pointing to copied validated content.
- Realtime/cache invalidation is downstream enhancement; durable state is correct without it.
- Each immutable publication freezes the validated SEO snapshot used for HTML title/description/canonical, Open Graph/Twitter and allowlisted JSON-LD; sitemap/robots resolve only published tenant/site state.

## Implementation Slices

1. Version/publication schema and state transitions.
2. Authorized preview token/session contract.
3. Publish transaction, idempotency and audit record.
4. Public resolution/query/cache key including tenant/site identity.
5. Rollback transaction and history UI.
6. Invalidation interface; concrete Realtime in Prompt Phase 12 and outbox in Phase 14.
7. Tenant/site SEO render, canonical-host policy, sitemap/robots generation and cache invalidation.

## Validation

- Concurrent publish and duplicate request cases.
- Mutation attempt on published version fails.
- Unauthorized preview/publish/rollback denied.
- Realtime/cache outage still serves correct durable version after recovery/refetch.
- Rollback history remains immutable and traceable.
- Public E2E shows update without web build/redeploy.
- Browser/crawler fixtures verify title/description/canonical/OG/Twitter/JSON-LD plus sitemap/robots across publish, rollback, locale and tenant host/path rules.

## Commit Plan

- `feat(publishing): add immutable page versions`
- `feat(publishing): implement idempotent publish transaction`
- `feat(publishing): add rollback and public resolution`
- `test(publishing): cover concurrency and failure recovery`

## Acceptance

- [ ] Publish receipt links actor, source draft, version, schema and audit event.
- [ ] Public render is tenant-safe and cache key cannot cross tenants.
- [ ] Rollback creates a distinct latest version.
- [ ] Integration and browser evidence pass at exact head.
- [ ] SEO output derives from the exact published version, contains no draft/private content and invalidates with publication/rollback without cross-tenant cache leakage.

## Stop Conditions

Published rows mutate, cache/realtime becomes sole truth, ambiguous transaction boundary, unverified tenant cache key, arbitrary executable structured data, foreign canonical injection or draft content in crawler output.
