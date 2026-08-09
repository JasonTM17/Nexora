# Nexora Outcome Contract

## Status

- State: `USER-APPROVED — C0 CONTROL-PLANE IN PROGRESS; FORMAL GOAL NOT YET ACTIVE`
- Owner: User
- Maintainer: Controller/Delivery Manager
- Change authority: User approval required for product boundary, release target, cost, hosting, security posture, or completion semantics

## Product Thesis

Nexora is a multi-tenant adaptive digital experience platform combining schema-driven content composition, immutable publishing, secure organizational knowledge, permission-aware RAG, experimentation, analytics, personalization, and production operations.

The system must demonstrate depth rather than simulated breadth. A capability counts only when its domain logic, UI states, tenant isolation, tests, observability, failure handling, documentation, and evidence all exist.

## Primary Users

| Persona | Core jobs |
|---|---|
| Organization owner | Configure tenant, members, roles, policies, themes, providers, budgets |
| Content creator | Compose pages from allowlisted blocks, preview, autosave, submit |
| Reviewer/publisher | Review, approve/reject, publish, rollback, inspect audit trail |
| Knowledge manager | Upload sources, observe jobs, control access, reindex/delete safely |
| End visitor/member | View adaptive published pages and search or chat within permissions |
| Platform operator | Observe health, traces, cost, backups, deployments, and incidents |

## Complete Scope

The master program preserves Prompt Phases 0-43:

- Repository, Spring, Next.js, identity, tenancy, RBAC.
- CMS, schema renderer, builder, versions, publishing, themes, workflow, realtime.
- Go ingestion, NATS, transactional outbox.
- Knowledge management, ingestion, pgvector, hybrid/secure RAG, reranking, evaluation.
- Feature flags, experiments, analytics, personalization, recommendation, notifications, audit, search.
- Threat modeling, telemetry, performance, containers, Kubernetes, Helm, Terraform, Argo CD, CI/CD, disaster recovery.
- Final product polish, documentation, attacker review, and Staff-level engineering review.

Scope is preserved in the master plan. `DEC-001` is accepted: the first formal Goal is the finite [v0.1 release](./release-v0.1-contract.md) covering M0-M4 / Prompt Phases 0-21. M5-M8 are `FUTURE_GOAL` and remain mandatory for complete master-program delivery.

## First Release Boundary

The first Goal delivers a real integrated tenant CMS, publishing, knowledge and secure-RAG slice. It is classified as `v0.1.0-alpha.1`, a production-shaped developer preview. It must not be called fully production-ready because whole-product hardening, hosted deployment, disaster recovery and final review remain in M6-M8.

If the user requires serving real production users immediately after M4, the Goal must be amended before activation to pull in minimum C3 security, SBOM/scans, staging deployment, rollback and restore-smoke gates.

## Product Invariants

1. **Tenant authority:** tenant context derives from authenticated membership, never a browser-controlled organization ID alone.
2. **Defense in depth:** application authorization and database/storage policies both enforce isolation.
3. **Safe page model:** only versioned allowlisted component schemas render. No arbitrary JavaScript, React, CSS, or unsanitized HTML.
4. **Immutable publication:** published versions are immutable; rollback creates a new version.
5. **Durable truth:** PostgreSQL stores business truth. Realtime, NATS, Redis, caches, and vectors cannot become the sole authoritative state.
6. **Permission before generation:** authorization filters execute before LLM context construction; hidden output is not a substitute for filtered input.
7. **Real citations:** every citation resolves to an authorized source and exact chunk.
8. **Bounded automation:** retries, concurrency, tokens, file sizes, job attempts, and cost have explicit ceilings.
9. **Truthful claims:** mocks/unit tests do not prove live providers, databases, queues, deployment, scale, or security.
10. **Recoverability:** destructive state changes and releases require rollback or restore evidence.
11. **Evidence-backed presentation:** screenshots, GIFs, diagrams, badges, Releases and packages identify the exact reviewed source/artifact and never invent live behavior.
12. **Measured continuity:** availability is an accepted SLO backed by health, redundancy, monitoring, rollback and restore drills—not a ping loop or zero-downtime promise.
13. **Dual supervision of material work:** every C3/R3 or materially consequential candidate has independent Advisor and Kongming receipts for the same identity plus a Controller disposition before advancement.

## Reference Architecture

```text
Browser: Node.js 24 LTS, Next.js 16 App Router, React, strict TypeScript,
         Tailwind 4 for branded public surfaces, Ant Design 6 for Studio,
         and evaluated Ant Design X adapters for secure RAG interactions
Domain/API: Java 25 LTS, Spring Boot 4.1 line, Maven, modular monolith first
Data/Auth/Storage: Supabase Auth + private Storage + PostgreSQL, forced RLS,
                   Flyway single migration authority and pgvector
Async: durable PostgreSQL jobs and transactional outbox first; private Realtime;
       Go 1.26/NATS JetStream only at the accepted M3 boundary
AI: DeepSeek V4 Flash generation; separately versioned embedding/rerank contracts;
    deterministic CI and live-provider evidence kept separate
Observability: OpenTelemetry, structured logs and Prometheus-compatible metrics
Delivery: Docker/Compose for v0.1; Vercel web and paid Supabase data plane;
          managed Kubernetes/Helm/Terraform/Argo proposed for backend in M7;
          GitHub Releases and attested GHCR images reconcile to exact SHA/digest
```

Detailed choices, alternatives, evidence links and change triggers are in [Technology Decisions](./technology-decisions.md). Exact patches remain unpinned until Prompt Phase 0 verifies compatibility from official sources.

The mandatory repository/media/release evidence is defined in [Documentation, Media and GitHub Distribution](./documentation-media-and-github-release.md). Runtime availability and agent keepalive are separated in [Production Continuity and Hosting](./production-continuity-and-hosting.md).

## Initial UX Standard

- Brand direction: distinctive “Nexora Signal Atelier” baseline, finalized only after three Stitch directions and explicit user selection.
- Public surfaces: expressive, editorial, fast, content-led.
- Admin/builder: branded Ant Design Studio wrappers, dense but calm, direct manipulation with keyboard-equivalent operations; no default template appearance.
- Knowledge workspace: restrained, source-centric, citation-forward, with Ant Design X used only behind owned secure-RAG adapters when validated.
- All critical flows: 375px and desktop, keyboard complete, visible focus, reduced motion, no horizontal overflow, honest states and data.

## Completion Semantics

A capability is complete only when:

```text
implemented on owned branch
→ targeted checks pass
→ exact-head independent test/review passes
→ manager accepts
→ material candidate receives same-identity Advisor FIT and Kongming PASS
→ integrated sequentially
→ combined main checks pass
→ evidence receipt recorded
→ remote synchronization verified when release scope requires it
```

Worker completion, a local commit, a screenshot, or a deterministic test alone is insufficient.

## Forbidden Shortcuts

- Fake analytics, AI answers, benchmarks, screenshots, tests, integrations, or deployment claims.
- Post-retrieval tenant filtering when retrieval-time filtering is possible.
- Stitch export pasted into production as finished React.
- Implementer self-review/self-merge.
- Direct worker pushes to `main`.
- Unbounded Goal retries or silent scope reduction.
- Credential values in prompts, plans, logs, reports, commits, screenshots, or memory.

## Outcome Change Protocol

Any requested change to scope, stack, tenant boundary, provider/data policy, hosting, budget, release target, or completion line creates a `DEC-*` entry. Dependent work remains `NEEDS_USER` until accepted and propagated through all affected phase files.
