# Nexora

Nexora is a tenant-aware CMS and knowledge workspace delivered as a polyglot
monorepo. The active line integrates milestones M0-M3: repository foundation,
Java/Spring platform API, Next.js web surface, tenant CMS core, durable event
outbox, NATS JetStream, a bounded Go event-ingestion edge and a private
Realtime descriptor path. Milestone M4 (knowledge and secure RAG) remains
planned, not implemented.

## Implemented now

- Apache-2.0 repository license and third-party provenance boundary.
- Monorepo skeleton under `apps/`, `services/`, `packages/`, `database/`,
  `infrastructure/`, `observability/`, and `docs/`.
- Reproducible toolchain declarations for Node, pnpm, Java, and Go.
- Spring Boot platform API under `apps/platform-api`: identity/tenancy, RBAC,
  CMS core with immutable publishing, transactional outbox, event admission
  and idempotent persistence consumer, Realtime descriptors, health/readiness
  and metrics.
- Next.js web shell under `apps/web` with branded tokens, strict TypeScript
  and private Realtime subscription handling.
- Go event-ingestion service under `services/event-ingestion`: bounded HTTP
  admission, JetStream ack-only publish, per-principal rate limiting and an
  aggregate concurrency cap. See the
  [Go/NATS ADR](./docs/adr/adr-m3-go-nats-event-ingestion.md) for its boundary.
- Flyway migrations under `database/migrations` (V001-V021) with RLS-forced
  application schemas, outbox and event-ledger functions.
- Loopback-only local PostgreSQL and NATS Compose with health checks and named
  volumes, plus CI gates for foundation, Go and Java unit checks.
- `.env.local`, `engineer/`, `.worktrees/`, and AgentKit runtime state remain
  ignored.

## Planned, not implemented

Knowledge management, document ingestion, pgvector retrieval and secure RAG
(M4), analytics/personalization/notifications (M5-M8), production deployment,
provider configuration, hosted Supabase/Vercel provisioning and release remain
later owned tasks. The empty directories are layout markers, not working
application features.

## Local commands

Run `make help` (or inspect `Makefile` on systems without Make) to see the
canonical commands. `pwsh ./tools/validate-repo.ps1` performs deterministic
repository checks, `make go-check` vets and tests the Go service, and
`docker compose config --quiet` validates the local dependency definition
without starting services. `make compose-health` starts the loopback-only
dependencies and waits for their health checks.

The first public push, remote configuration changes, provider calls, release,
provisioning, and deployment are intentionally outside this branch.
