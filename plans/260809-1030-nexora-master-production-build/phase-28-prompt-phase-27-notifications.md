---
phase: 28
title: "Prompt Phase 27 — Notifications"
status: pending
priority: P2
effort: "6-9 days"
dependencies: [13, 14, 15]
---

# Prompt Phase 27 — Notifications

## Outcome

Persist authorized notifications before delivery, distribute them through a bounded Go worker and Realtime enhancement, and respect user preferences.

## Domain Contract

- Notification record is durable and tenant/user scoped.
- Event-to-notification mapping is versioned, idempotent and permission-aware.
- Delivery attempts/states are bounded; poison messages terminate visibly.
- Realtime is best effort; inbox refetch is authoritative.
- Preferences, quiet hours and mandatory security notification exceptions are explicit.

## Planned Ownership

Notification migration/domain/API, `services/notification-worker/**`, NATS subject/event mapping, web notification center/preferences. Delivery state and event vocabulary are serialized.

## Validation

- Duplicate event yields one logical notification.
- Worker crash/restart, NATS/Realtime outage and poison message.
- Membership/permission change before delivery/read.
- Cross-tenant/user inbox denial.
- Preference/quiet-hour and unread-count consistency.

## Commit Plan

- `feat(notifications): add durable notification records`
- `feat(notifications): add bounded Go delivery worker`
- `feat(web): add accessible notification center and preferences`
- `test(notifications): verify duplicate and failure recovery`

## Acceptance

- [ ] Notification exists durably before realtime delivery.
- [ ] Inbox recovers missed events by refetch.
- [ ] Retry/DLQ/backlog metrics and runbook exist.
- [ ] Sensitive content is minimized in payloads/logs.

## Stop Conditions

Realtime-only notification truth, unbounded retry, cross-user delivery, preferences ignored, raw sensitive event payload copied blindly.
