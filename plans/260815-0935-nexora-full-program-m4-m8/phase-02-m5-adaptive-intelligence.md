---
phase: 2
title: "M5 Adaptive Intelligence"
status: pending
priority: P1
effort: "10-14 days"
dependencies: [1]
---

# Phase 2: M5 Adaptive Intelligence

## Overview
M5 covers product analytics, experimentation/feature flags, and notification completion. Builds on M3 event backbone (outbox + NATS) and M4 knowledge.

## Requirements
- [ ] Feature flags system (tenant-scoped, server-derived)
- [ ] Experimentation framework (A/B with consistent hashing)
- [ ] Product analytics pipeline (event collection → aggregation)
- [ ] Notification system (multi-channel: in-app, email-ready)
- [ ] Global search over pages + knowledge (authorized, hybrid)
- [ ] Admin analytics dashboard UI

## Implementation Steps

1. **Feature flags**
   - Schema: `feature_flags` table (tenant_id, key, enabled, rules)
   - Service: `FeatureFlagService` with tenant-scoped evaluation
   - Cache: short-TTL in-memory with tenant isolation
   - API: `GET /api/v1/feature-flags/:key` → {enabled, variant}

2. **Experimentation**
   - Consistent hashing on subject_id → stable variant assignment
   - Schema: `experiments` + `experiment_variants`
   - Track exposure events to analytics

3. **Product analytics**
   - Event collection via existing outbox/NATS
   - Schema: `analytics_events` (tenant_id, type, subject, properties JSONB)
   - Aggregation queries for dashboard
   - Privacy: respect tenant data boundaries

4. **Notifications**
   - Schema: `notifications` table (tenant_id, user_id, type, payload, read_at)
   - Delivery: in-app (Realtime descriptor) + email-ready adapter
   - Preference: per-user notification preferences

5. **Global search**
   - Authorized hybrid search over pages + knowledge
   - Snippet sanitization (no XSS, no unauthorized text leak)
   - Cursor pagination, stable under concurrent updates

6. **Admin UI**
   - Analytics dashboard (web app)
   - Feature flag management
   - Experiment results view

## Todo
- [ ] Feature flags backend + API
- [ ] Experimentation framework
- [ ] Analytics pipeline + schema
- [ ] Notification system
- [ ] Global search service
- [ ] Admin dashboard UI

## Success Criteria
- Feature flags evaluate correctly per tenant
- Experiments assign stable variants
- Analytics events collected and queryable
- Notifications delivered via Realtime
- Search returns authorized results only

## Commit Plan
```
feat(flags): add tenant-scoped feature flag service
feat(experiment): add A/B experimentation framework
feat(analytics): add product analytics pipeline
feat(notification): add notification system
feat(search): add authorized global search
feat(web): add admin analytics dashboard
```
