---
phase: 14
title: "Prompt Phase 13 — Go Event Ingestion"
status: pending
priority: P2
effort: "5-8 days"
dependencies: [2, 5]
dispatch_after: [15]
---

# Prompt Phase 13 — Go Event Ingestion

## Outcome

Create a justified Go edge collector that validates, rate-limits and publishes canonical events to NATS with real metrics, tracing and load evidence.

## Boundary Justification

Before implementation, record why this workload benefits from an independent Go service rather than the Spring modular monolith. Acceptance requires measurable or operational rationale, not technology checklist value.

The M3 execution ledger dispatches this service only after the event vocabulary and outbox/idempotency contract are frozen. It must have a real idempotent persistence consumer and a benchmark against the simpler Spring endpoint. A skeleton, health check, or future M5 analytics promise is insufficient.

## Requirements

- Versioned event envelope, schema validation and event ID.
- Tenant/user authority derived from trusted authentication context.
- Payload size/type allowlist; no arbitrary PII.
- Rate/abuse limits, request/body timeouts and graceful shutdown.
- NATS subject vocabulary, publish acknowledgment/failure semantics.
- Metrics/traces/log correlation and health/readiness.

## Planned Ownership

The Go worker owns only `services/event-ingestion/**` and its service-local tests/container. The event-contract owner owns assigned contract source under `packages/contracts/**` but excludes every Node dependency manifest; the milestone dependency-window owner alone may change those manifests or `pnpm-lock.yaml`. The migration train owns DB changes; M3-R01 owns root Compose/CI/Make wiring. The Go worker stops rather than taking any shared boundary.

## Validation

- Table-driven validation/auth/rate tests.
- NATS unavailable/slow, client disconnect, duplicate event ID and oversized body cases.
- Container integration and trace correlation.
- Reproducible load test records environment, dataset, concurrency, duration, SHA and raw results.

## Commit Plan

- `chore(events): initialize Go ingestion service`
- `feat(events): validate and publish canonical events`
- `test(perf): add reproducible ingestion load profile`

## Acceptance

- [ ] Architecture decision justifies Java/Go boundary.
- [ ] A real idempotent consumer persists accepted events without claiming M5 analytics.
- [ ] A reproducible comparison shows why the Go boundary is retained over a Spring-only endpoint.
- [ ] Invalid/unauthorized events never publish.
- [ ] Backpressure/failure behavior is bounded and observable.
- [ ] Performance claims cite raw contemporary evidence.

The Go branch first freezes its exact event interface/fixture head in `IMPLEMENTED/VERIFYING`; that freeze may dispatch the separate M3-T05 persistence-consumer branch but is not acceptance. The real-consumer criterion above is discharged only by joint M3-T04/M3-T05 evidence and the M3 integration gate. Any change to the frozen Go head invalidates the downstream pin and repeats the paired review.

## Stop Conditions

No boundary justification, unauthenticated tenant IDs trusted, fire-and-forget data loss hidden, fake throughput target, unbounded memory/body/retry.
