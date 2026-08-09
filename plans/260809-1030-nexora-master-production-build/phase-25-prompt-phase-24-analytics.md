---
phase: 25
title: "Prompt Phase 24 — Analytics"
status: pending
priority: P2
effort: "8-12 days"
dependencies: [14, 15, 24]
---

# Prompt Phase 24 — Analytics

## Outcome

Deliver a privacy-aware browser SDK, validated Go/NATS ingestion, idempotent persistence and tenant-safe query/dashboard path with real provenance.

## Event Contract

Versioned event name, event ID, occurred/received timestamps, tenant/site, trusted subject/session pseudonym, experiment context, approved properties, consent/purpose and schema version. Reject arbitrary high-cardinality/sensitive payloads.

## Implementation Slices

1. Browser SDK batching, consent, retry/backoff, sendBeacon/fallback and limits.
2. Extend Phase 13 collector for analytics envelope.
3. NATS consumer/persistence with deduplication and poison handling.
4. Partition/index/retention strategy and aggregate queries.
5. Tenant-safe APIs and dashboard states.
6. End-to-end trace, failure and volume tests.

## Planned Ownership

Web analytics SDK, event contract, Go collector extension, analytics consumer/storage migration, dashboard queries/UI. Event vocabulary/migration are serialized.

## Validation

- Consent off/withdrawn, offline, duplicate, out-of-order and delayed events.
- Forged tenant/user attributes replaced/denied.
- Consumer restart and duplicate delivery produce one logical record/effect.
- Query pagination/time range/timezone/tenant filters.
- Dashboard matches known seeded event set and labels fixtures/live correctly.

## Commit Plan

- `feat(analytics): add consent-aware browser SDK`
- `feat(events): ingest canonical analytics events`
- `feat(analytics): persist idempotent event stream`
- `feat(admin): add truthful analytics queries and dashboard`

## Acceptance

- [ ] Browser → collector → NATS → consumer → database → dashboard scenario passes.
- [ ] Retention/deletion and privacy purpose are documented/tested.
- [ ] Metrics reconcile with deterministic fixture and real captured run.
- [ ] Backpressure/retry/cost limits are observable.

## Stop Conditions

Trusting browser tenant IDs, collecting without consent/purpose, fake dashboard data, unbounded properties/cardinality/retention, non-idempotent consumer.
