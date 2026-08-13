# ADR — M3 Go/NATS Event Ingestion Boundary

> Status: `PROPOSED` — exact-head review required.
>
> Review identity: this ADR covers the M3-T04 Go event-ingestion worker
> (`services/event-ingestion/**`), the M3-T05 Spring idempotent persistence
> consumer (`apps/platform-api/.../events/consumer/**`) and their joint evidence
> on `test/m3-go-spring-joint`. Per the M0-M4 execution ledger, dispatch of
> M3-T04 requires same-revision Advisor `FIT` and Kongming `PASS` plus Controller
> disposition.

## Context

Nexora's event spine is PostgreSQL-first: M3-T01 froze the versioned event
envelope, M3-DB01 owns the outbox/event-ledger schema, and M3-T02 publishes
committed domain events through NATS JetStream with bounded retry. Two
capabilities remain to close M3: an untrusted-HTTP event admission edge and a
durable, idempotent consumer that persists accepted events.

The accepted direction (DEC-014, TD-018, TD-019) places a small Go service at
the admission edge, but the ledger makes that boundary conditional: it needs a
justified ADR, a real idempotent consumer, and a reproducible comparison
against the simpler Spring-only endpoint before it is retained.

## Decision

**Adopt the bounded Go event-ingestion edge, but demote it to a thin forwarder
whose only authority is Spring's admission decision.**

The Go process owns nothing authoritative:

- It validates the canonical `1.1.0` envelope shape (single document, unknown
  fields rejected, bounded body/header/bearer sizes) and refuses to publish
  anything that fails validation.
- It never parses the caller's bearer or a client-supplied organization
  identifier as authority. It forwards the exact bearer to the Spring-owned
  `/api/v1/internal/event-admission` boundary, which resolves fresh
  RLS-backed membership, requires `page.publish`, and returns a short-lived
  admission decision.
- It exact-compares the returned decision (organization, subject, actor,
  resource, event type, version, schema, topic) against the envelope, applies a
  per-principal fixed-window rate limit and an aggregate in-flight concurrency
  cap, then publishes only after a JetStream acknowledgement with the event ID
  as `Nats-Msg-Id` for deduplication.
- The route is registered only when both `NEXORA_EVENT_INGESTION_ADMISSION_URL`
  and `NEXORA_EVENT_INGESTION_NATS_URL` are configured; otherwise the process
  serves health/metrics only. Failure to publish returns a bounded `503` — the
  client is responsible for retry, and nothing is acknowledged that was not
  persisted in JetStream.

The **Spring platform remains the single authority** for identity, membership,
tenant resolution, event vocabulary and ledger persistence. The Spring
idempotent consumer (`EventLedgerConsumer` + `EventLedgerRepository` →
`nexora.record_event_ledger_entry`) persists each accepted envelope before
acknowledging it, converges duplicates/replays to exactly one ledger row, and
terminally rejects malformed envelopes. M3-R01 owns root Compose/CI/Make wiring
and wires a single ingestion replica, matching the in-memory rate limiter's
single-instance assumption.

### Retention criterion

The Go boundary is **conditionally retained**. Joint M3-T04/M3-T05 evidence
must show, with recorded raw results (commit SHA, environment, dataset,
concurrency, warmup, samples), that the Go forwarder plus Spring admission
path is at least operationally comparable to the direct Spring admission
endpoint. The recorded probe (`CmsPageIntegrationTests` →
`writeJointBenchmark`) deliberately makes **no** throughput or production
claim; if a later bounded load profile cannot justify the extra process, the
boundary collapses to the Spring-only endpoint and the Go service is removed.

## Alternatives considered

| Alternative | Disposition | Reason |
|---|---|---|
| **Spring-only admission endpoint** | Rejected for v0.1, retained as fallback | Correct and simpler; but untrusted HTTP admission (body/timeout/rate/backpressure limits) and publish backpressure would share the modular monolith's JVM/GC/thread pool. The Go process isolates those limits and can be operated and killed independently. Re-entry condition: the joint benchmark showing no operational benefit. |
| **Go as event authority** (Go parses JWT, resolves tenant, writes DB) | Rejected | Violates DEC-014/TD-018: Spring owns domain authorization; a second credential-parse authority would double the tenant-trust boundary with no benefit. |
| **Kafka or a heavier broker** | Deferred (existing TD-018) | NATS JetStream satisfies the accepted at-least-once/idempotent-consumer contract with a smaller operational footprint. |
| **Fire-and-forget publish** | Rejected | A silent dual-write loss path; publish must be acked or the request fails. |
| **Go service with multiple replicas now** | Rejected | The in-memory rate limiter is single-instance; multi-replica wiring needs a shared limiter plus its own review. |

## Consequences

- **Positive:** tenant authority stays single-sourced through Spring; untrusted
  admission resource limits are process-isolated; publish failures are explicit
  and bounded; the consumer provides real idempotent persistence with replay
  convergence, discharging the ledger's "real idempotent consumer" criterion.
- **Cost:** one more process, container, health surface and operational
  boundary; a duplicated (Go/Java/SQL) projection of the frozen envelope that
  is bounded by the pinned contract fixture/digest rather than a compile-time
  shared source.
- **Known limits:** `readyz` reflects local serve state, not NATS connectivity;
  the rate limiter is in-memory and single-instance; trace correlation is a
  validated `Nexora-Trace-Id` carried on the message, not full OTel context
  propagation; the benchmark is a local diagnostic without a retention
  decision. These remain documented, honest boundaries until M3-R01 and later
  observability work.

## Rollback

The Go service is an additive edge. Removing it restores the Spring-only
admission path: M3-R01 stops the Go service definition and the Spring admission
endpoint remains the direct entry point. No migration, contract or domain
change is required; only root runtime wiring changes.
