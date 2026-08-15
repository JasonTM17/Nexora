# ADR — M1 Platform Architecture and Technology Choices

> Status: `ACCEPTED` — verified against implemented codebase.

## Context

Nexora needed a polyglot architecture for a tenant-aware CMS and knowledge workspace.
The core requirements: strict tenant isolation, durable event-driven workflows,
secure RAG with permission-before-context, and a type-safe contract between
frontend and backend.

## Decision

### Stack

| Layer | Choice | Rationale |
|---|---|---|
| Web | Next.js 16 / React 19 | RSC, same-origin BFF, strict TypeScript |
| API | Spring Boot 4.1 | Mature ecosystem, strong security, modular monolith |
| Edge | Go 1.26 | Bounded HTTP admission, low memory, fast compile |
| Database | PostgreSQL 17 + pgvector | Durable truth, RLS, vector search in one store |
| Events | NATS JetStream | Durable streaming, bounded retention, ack-only publish |
| Contract | OpenAPI 3.1 + generated client | Zero drift between FE and BE |

### Tenant authority model

**Server-derived tenant authority**: a client-supplied tenant ID, object path,
channel name, document ID, or role never establishes tenant authority. The
server resolves a current active membership from the JWT subject + organization
header, then sets `nexora.tenant_id` for RLS.

### Persistence pattern

JdbcTemplate (not JPA) with plain carrier objects. SQL functions own the write
boundary and set the tenant context for RLS. Reads filter by tenant inline.
This keeps the persistence layer explicit and auditable.

## Consequences

**Positive**:
- Tenant isolation enforced at DB (RLS), service (tenant context), and API (JWT + header) layers
- Generated client eliminates FE↔BE contract drift
- Go edge isolates untrusted HTTP admission from the monolith

**Negative**:
- JdbcTemplate requires more boilerplate than JPA
- polyglot means multiple toolchains to maintain

**Neutral**:
- Modular monolith (not microservices) for v0.1 — services split later if scale demands

## Alternatives Considered

| Alternative | Why rejected |
|---|---|
| Next.js API routes as backend | Violates separation of concerns; blast radius too large |
| JPA/Hibernate | Hides SQL; harder to audit RLS tenant context |
| Microservices from day 1 | Operational overhead unjustified at v0.1 scale |
| REST without OpenAPI contract | Drift between FE and BE; generated client is zero-cost |

## References

- Implementation: `apps/web`, `apps/platform-api`, `services/event-ingestion`
- Migrations: `database/migrations/V001`–`V028`
- Threat model: `docs/security/threat-model.md`
