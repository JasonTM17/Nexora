# Nexora Architecture Baseline

## Status and reading rule

**Status: planned v0.1 architecture; no product runtime is implemented or
provisioned by this document.** This baseline translates the accepted M0--M4
control-plane candidate into bounded system, data and trust views for later
exclusive implementation packets. It is not deployment evidence, a provider
configuration receipt, or permission to call a provider.

The authoritative scope and task sequence remain the pinned control-plane
documents under `plans/260809-1030-nexora-master-production-build/`, especially
the technology decisions, Supabase platform boundary, data-lifecycle contract,
execution ledger and requirements catalog. Where a later implementation differs
materially, the implementer must open an ADR and obtain the required same-head
Advisor FIT, Kongming PASS and Controller disposition before proceeding.

## Views

| Document | Purpose | Primary implementation owners |
|---|---|---|
| [System overview](./system-overview.md) | Phase-0 current-state, planned topology, risk and dependency summary | M0-T04 architect; later owners validate each boundary |
| [System and module view](./system-and-modules.md) | Planned services, interfaces, ownership and alternatives | M1 foundation, M2 domain/API, M3 events, M4 RAG workers |
| [Data and trust view](./data-and-trust.md) | Data classification, request authorization and permitted egress | Migration train, platform API, web and security owners |
| [Failure semantics](./failure-semantics.md) | Planned degraded behavior, retry boundaries and STOP conditions | Each domain owner; later operational acceptance validates it |

## Baseline invariants

1. PostgreSQL is the durable domain truth. Realtime, cache, search indexes,
   embeddings and event delivery are derived or advisory until reconciled with
   that truth.
2. Spring is the domain authorization and write boundary. Browsers do not call
   domain PostgREST or Spring directly in the v0.1 default path.
3. Tenant identity comes from validated identity plus fresh server-side
   membership, and is enforced in both service authorization and non-owner RLS.
4. No secret is placed in browser code, source documentation, screenshots,
   receipts or this baseline. `NEXT_PUBLIC_*` values are public by definition.
5. Provider integrations are bounded adapters. They can be unavailable,
   rejected by policy or require explicit external authority; their absence must
   produce a safe, observable product state rather than a fabricated success.
6. The planned release topology and continuity controls are future work. Local
   or CI evidence cannot be represented as hosted or production evidence.

## Decision boundaries retained for later packets

- The version lines in the plan are candidates, not lockfile, image or managed
  service pins. Each responsible M1+ packet must record exact compatible pins.
- A surface-specific CSP/cache ADR is mandatory before frontend foundation work:
  Studio/auth and cacheable public pages are distinct security/performance
  surfaces.
- Direct browser database access, arbitrary public Realtime channels, public
  source buckets, browser-held service roles, generic action connectors and
  live external-provider use are outside this baseline unless a later accepted
  decision explicitly changes the boundary.
- A paid project, production deployment, release, first push or provider call
  requires separate user authority; none is implied by these documents.

## Traceability

| Architecture concern | Planned authority |
|---|---|
| Repository, runtimes, service shape and API | `technology-decisions.md` TD-001 through TD-032 |
| Managed PostgreSQL/Auth/Storage/Realtime limits | `supabase-platform-boundary.md` and DEC-028 |
| Retention, deletion, export and AI data handling | `data-lifecycle-and-privacy-contract.md` |
| Writer boundaries and review gates | `m0-m4-execution-ledger.md`, `thread-branch-worktree-runbook.md` |
| Requirement-level provenance | `master-requirements-catalog-expanded.md` |

## Evidence boundary

The Mermaid diagrams here are diagram-as-code planning artifacts. A later
release-documentation owner must render, checksum and retain them with the
specific reviewed architecture head. A rendered diagram alone does not prove a
running build, health, SLO, backup, rollback or restore capability.
