---
phase: 15
title: "Prompt Phase 14 — Transactional Outbox"
status: pending
priority: P1
effort: "4-7 days"
dependencies: [3, 10]
---

# Prompt Phase 14 — Transactional Outbox

## Outcome

Guarantee that committed domain changes produce durable publishable events without dual-write loss, while accepting at-least-once delivery and handling duplicates.

## Architecture

- Business row and outbox row commit in the same database transaction.
- Publisher claims rows with concurrency-safe locking/lease semantics.
- Attempts are bounded with backoff, terminal failure/dead-letter state and operator visibility.
- Consumers remain idempotent; outbox does not claim exactly-once delivery.
- Task-level M3 execution establishes this durable contract before dispatching the optional distributed Go/NATS boundary. Numeric Prompt Phase order does not override dual-write safety.

## Implementation Slices

1. Versioned outbox schema, indexes, retention and safe payload contract.
2. Domain transaction integration for selected events.
3. Claim/publish/ack/failure state machine.
4. Metrics, tracing, health/readiness impact and operator query.
5. Duplicate, crash, concurrent publisher and poison-event tests.

## Planned Ownership

M3-DB01 exclusively owns the outbox/event-ledger migration. The outbox backend worker owns platform `events/outbox/**` and its NATS adapter. M3-T01 owns the shared event envelope. No one task writes more than one of these boundaries.

## Validation

- Crash after business commit before publish.
- Crash after publish before marking sent.
- Two publishers cannot process the same lease unsafely.
- NATS outage recovers without blocking business transaction indefinitely.
- Duplicate downstream event produces one logical effect.

## Commit Plan

- `feat(events): add transactional outbox schema`
- `feat(events): publish outbox events with bounded retry`
- `test(events): verify crash and duplicate recovery`

## Acceptance

- [ ] No committed selected event can be silently lost.
- [ ] Poison event reaches visible terminal state.
- [ ] Metrics expose backlog, age, failures and attempts.
- [ ] Retention/deletion and sensitive payload rules are documented.

## Stop Conditions

Business commit waits on remote NATS for correctness, infinite retry, unsafe payloads, untested duplicate delivery, ambiguous locking.
