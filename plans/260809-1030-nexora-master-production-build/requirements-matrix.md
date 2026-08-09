# Nexora Prompt-to-Plan Requirements Matrix

## Purpose

This matrix maps the 44 execution phases. Bullet-level omission prevention is governed by the source-hashed [Master Requirement Catalog](./master-requirements-catalog.md): its 141 parent IDs—one preamble plus sections `0..139`—and pre-Goal expanded child IDs map every normative source statement and user-chat overlay to decision, task and evidence. `Execution phase` is the AgentKit file number; `Prompt phase` preserves the source numbering 0-43. Detailed implementation contracts live in the linked phase files.

## Goal Disposition — DEC-001

| Execution files | Prompt phases | Milestones | First Goal disposition |
|---:|---:|---|---|
| 01-22 | 0-21 | M0-M4 | `INCLUDED_V0.1` |
| 23-44 | 22-43 | M5-M8 | `FUTURE_GOAL` |

`FUTURE_GOAL` does not mean removed, completed or silently deferred. These requirements remain mandatory for the master program but are not counted against the finite v0.1 Goal completion line.

## Global Gates Applied to Every Phase

| Gate | Requirement |
|---|---|
| G-Baseline | Inspect current source/config/manifests/infra/migrations/tests/docs; label planned versus verified |
| G-Feature | Domain logic, API, validation, auth, tenant isolation, constraints/indexes, failure behavior, telemetry, tests, UI states, docs |
| G-Database | Key, tenant key, FK, constraints, indexes, timestamps, deletion/retention, audit, RLS review |
| G-API | Authentication, authorization, tenant safety, validation, rate/abuse limits, pagination, idempotency, stable error, trace ID, tests |
| G-Frontend | Loading/error/empty/denied/conflict/reconnect, mobile, keyboard, reduced motion, accessibility, destructive confirmation |
| G-RAG | Quality, pre-context permission filtering, citations, injection resistance, latency/token/privacy/evaluation |
| G-Service | Health/readiness, metrics/logs/traces, container, shutdown, resources, secrets, CI, rollback |
| G-Docs/Release | Exact-SHA claims, real media provenance, architecture render, changelog, immutable artifacts, SBOM/attestation and live GitHub verification when applicable |
| G-Commit | Narrow diff, secret scan, tests, formatter, lint/typecheck, Conventional Commit, usable mainline |
| G-Receipt | Exact base/head, owned paths, commands/results, evidence, review, merge, limitations, next milestone |
| G-Dual | Every material C3/R3 candidate has same-identity Advisor and Kongming receipts plus Controller disposition |
| G-Innovation | INN-* is research-only until user/dual approval; no silent Goal/signal/provider/cost expansion |
| G-Supabase | Application-owned non-exposed schemas, explicit Data API grants, documented managed-schema policy DDL only, observed extension compatibility and separate DB/Storage recovery evidence |

## Phase Matrix

| Exec | Prompt | Milestone | Contract | Required outcome | Principal dependencies | Acceptance focus |
|---:|---:|---|---|---|---|---|
| 01 | 0 | M0 | [Deep Scout](./phase-01-start.md) | Assessment, system overview, implementation plan; current state, gaps, risks, dependency strategy, sequence | User-provided prompt and actual workspace | Source-backed baseline; no implementation inference |
| 02 | 1 | M1 | [Repository Foundation](./phase-02-prompt-phase-1-repository-foundation.md) | Monorepo, standards, formatter/lint/editor, Makefile, env template, Compose, base CI | Phase 0 and repository decision | Fresh setup and baseline checks; clean public-safe initial commits |
| 03 | 2 | M1 | [Java Platform](./phase-03-prompt-phase-2-java-platform-foundation.md) | Spring profiles, logs, exceptions, OpenAPI, DB/migrations, health, telemetry baseline | Phase 1 | Starts locally; migrations/OpenAPI/health/log/trace verified |
| 04 | 3 | M1 | [Frontend Foundation](./phase-04-prompt-phase-3-frontend-foundation.md) | Next.js strict TS, Tailwind/custom public UI, Ant Design Studio, optional Ant Design X adapters, app/BFF/auth shell and error boundaries | Phase 1; selected Stitch direction; contracts with Phase 2 | SSR/lint/type/build/bundle/a11y and complete branded shell states |
| 05 | 4 | M2 | [Identity and Tenancy](./phase-05-prompt-phase-4-identity-and-tenancy.md) | Organizations, members, auth, tenant context, authorization foundation | Phases 2-3 | Membership-derived context; two-tenant allow/deny tests |
| 06 | 5 | M2 | [RBAC](./phase-06-prompt-phase-5-rbac.md) | Roles, permissions, assignments, evaluator, admin UI | Phase 4 | Role matrix and security denial tests; tenant-safe admin UI |
| 07 | 6 | M2 | [CMS Core](./phase-07-prompt-phase-6-cms-core.md) | Pages, slug, metadata, listing, draft creation/editing | Phases 4-5 | Tenant-safe CRUD, uniqueness, stable errors, UI states |
| 08 | 7 | M2 | [Schema-Driven UI](./phase-08-prompt-phase-7-schema-driven-ui.md) | Registry, validation, dynamic renderer, versioned schemas; five initial blocks | Phases 3 and 6 | Persisted validated schemas render; unsafe code impossible |
| 09 | 8 | M2 | [Page Builder](./phase-09-prompt-phase-8-page-builder.md) | Library panel, canvas, DnD, properties, duplicate/delete, preview, autosave | Phases 6-7 | Compose/recover/preview; keyboard alternative and complete states |
| 10 | 9 | M2/M3 | [Versioning and Publishing](./phase-10-prompt-phase-9-versioning-and-publishing.md) | Immutable versions, draft, preview, publish, rollback, invalidation | Phases 6-8; Realtime 12; Outbox 14 | Idempotent durable publish; rollback new version; visitor updates without redeploy |
| 11 | 10 | M2 | [Theme Engine](./phase-11-prompt-phase-10-theme-engine.md) | Tokens, editor, preview, publication, dynamic CSS variables | Phases 3, 6, 9 | Constrained safe tokens and versioned theme publication |
| 12 | 11 | M2 | [Workflow](./phase-12-prompt-phase-11-workflow.md) | Review, approve, reject, publish permission | Phases 4-5 and 9 | State-transition and actor/reason/audit tests |
| 13 | 12 | M3 | [Supabase Realtime](./phase-13-prompt-phase-12-supabase-realtime.md) | Private channels, publish broadcast, jobs, notifications, presence | Phases 4-5; consumers | Channel authorization tests; durable refetch fallback |
| 14 | 13 | M3 | [Go Event Ingestion](./phase-14-prompt-phase-13-go-event-ingestion.md) | Validated ingestion API, NATS, metrics, tracing, load tests | Phase 1/event/auth contracts; runtime `dispatch_after` execution Phase 15 outbox | Rate-limited ingestion, real consumer, trace/metrics and reproducible comparison evidence |
| 15 | 14 | M3 | [Transactional Outbox](./phase-15-prompt-phase-14-transactional-outbox.md) | Outbox table, publisher, retry, locking, metrics | Spring/data/publishing foundations; event vocabulary | Atomic write/outbox before Go dispatch; bounded retry, locking and duplicate evidence |
| 16 | 15 | M4 | [Knowledge Management](./phase-16-prompt-phase-15-knowledge-management.md) | Knowledge bases, document records, storage/upload, jobs, progress UI | Phases 4-5, 2-3; 12 optional | Tenant-safe upload and durable restart-safe job/status |
| 17 | 16 | M4 | [Document Ingestion](./phase-17-prompt-phase-16-document-ingestion.md) | Extraction, normalization, chunking, metadata, indexing state | Phase 15 | Hostile input limits, retry/resume and deterministic chunk tests |
| 18 | 17 | M4 | [PGVector](./phase-18-prompt-phase-17-pgvector.md) | Embeddings, vector storage/query/indexes | Phase 16 and provider decision | Model/dimension provenance; tenant-safe retrieval/index plan |
| 19 | 18 | M4 | [Hybrid RAG](./phase-19-prompt-phase-18-hybrid-rag.md) | Lexical/vector retrieval, fusion, configurable top-K | Phase 17 | Deterministic hybrid retrieval and evaluation metrics |
| 20 | 19 | M4 | [Secure RAG](./phase-20-prompt-phase-19-secure-rag.md) | Permission filter, tenant-scoped persistent chat/history, context builder, provider abstraction, stream, citations | Phases 4-5 and 18 | Zero unauthorized context/history; idempotent message lifecycle; grounded resolvable citations; no-answer/delete path |
| 21 | 20 | M4 | [Reranking](./phase-21-prompt-phase-20-reranking.md) | Reranker abstraction and optional provider | Phases 18-19 | Baseline versus rerank benchmark and safe fallback |
| 22 | 21 | M4 | [RAG Observability](./phase-22-prompt-phase-21-rag-observability.md) | Retrieval trace, scores, feedback, dashboard, evaluation scripts | Phases 19-20 | Redacted trace inspection and versioned quality report |
| 23 | 22 | M5 | [Feature Flags](./phase-23-prompt-phase-22-feature-flags.md) | Flags, targeting, percentage rollout, stable assignment, kill switch, audit | Phases 4-5; audit contract | Deterministic assignment/kill switch and auditable changes |
| 24 | 23 | M5 | [Experiments](./phase-24-prompt-phase-23-experiments.md) | Experiments, variants, exposure, conversion, metrics/dashboard | Phase 22; metrics completed by 24 | Stable assignment and honest exposure/conversion semantics |
| 25 | 24 | M5 | [Analytics](./phase-25-prompt-phase-24-analytics.md) | Frontend SDK, Go ingestion, NATS, persistence, dashboard queries | Phases 13-14 and 3 | End-to-end event provenance, idempotency and truthful dashboards |
| 26 | 25 | M5 | [Personalization](./phase-26-prompt-phase-25-personalization.md) | Rules-based personalization integrated with renderer | Phases 7, 22, 24 | Deterministic rules, consent/safe default and fallback |
| 27 | 26 | M5 | [Recommendation](./phase-27-prompt-phase-26-recommendation.md) | Explainable initial engine using event signals | Phases 24-25 | Reproducible scoring, explanation and no fake recommendation |
| 28 | 27 | M5 | [Notifications](./phase-28-prompt-phase-27-notifications.md) | Durable persistence, realtime delivery, Go worker, preferences | Phases 12-14, auth | Persist-before-deliver, bounded retry/DLQ/idempotency/refetch |
| 29 | 28 | M5 | [Audit](./phase-29-prompt-phase-28-audit.md) | Immutable audit events and admin UI | Foundations and sensitive producers | Safe metadata only, tenant-safe immutable query and coverage |
| 30 | 29 | M5 | [Search](./phase-30-prompt-phase-29-search.md) | Hybrid global search, filters, authorization | CMS/RAG/auth | Stable filtered pagination and zero cross-tenant leakage |
| 31 | 30 | M6 | [Security Hardening](./phase-31-prompt-phase-30-security-hardening.md) | Threat model and attack/fix auth, tenancy, RAG, upload, SSRF, XSS, rate, secrets, export/deletion/anonymization | All exposed features | Finding ledger, reproductions, privacy-plane tests, fixes or accepted residual risk without false compliance claims |
| 32 | 31 | M6 | [Observability](./phase-32-prompt-phase-31-observability.md) | Complete metrics, traces, logs, Grafana, alerts | All services | End-to-end correlation, redaction, actionable alerts/runbooks |
| 33 | 32 | M6 | [Performance](./phase-33-prompt-phase-32-performance.md) | Load tests, bottlenecks, evidence-based optimization, before/after docs | Phase 31 and stable paths | Reproducible raw evidence; no unsupported targets |
| 34 | 33 | M7 | [Containerization](./phase-34-prompt-phase-33-containerization.md) | Production images, GHCR distribution, security scan, SBOM/provenance | Stable service boundaries | Reproducible minimal non-root images tied to scans/SBOM/attestation/digest |
| 35 | 34 | M7 | [Kubernetes](./phase-35-prompt-phase-34-kubernetes.md) | Probes, resources, PDB, NetworkPolicy, justified HPA | Phase 33 | Render/apply evidence, termination and policy verification |
| 36 | 35 | M7 | [Helm](./phase-36-prompt-phase-35-helm.md) | Reusable charts and environment values | Phase 34 | Lint/template/schema validation for approved environments |
| 37 | 36 | M7 | [Terraform](./phase-37-prompt-phase-36-terraform.md) | Realistic infrastructure automation and managed boundaries | Hosting decision, phases 34-35 | fmt/validate/plan; state, cost, security and destroy boundaries |
| 38 | 37 | M7 | [GitOps](./phase-38-prompt-phase-37-gitops.md) | Argo CD and deployment workflow | Phases 35-36 | Reconciliation, promotion and rollback evidence |
| 39 | 38 | M7 | [CI/CD Hardening](./phase-39-prompt-phase-38-ci-cd-hardening.md) | Tests, scans, Vercel previews, GHCR images, release, deployment and rollback | Phases 30-37 | Required gates and provenance from SHA to release/package/deployed digest |
| 40 | 39 | M7 | [Disaster Recovery](./phase-40-prompt-phase-39-disaster-recovery.md) | Supabase/database/object backup, Vercel/app rollback and service recovery tests | Actual deployment/storage target | Stated RPO/RTO and isolated application-level restore drill |
| 41 | 40 | M8 | [Final Product Polish](./phase-41-prompt-phase-40-final-product-polish.md) | Responsive, states, accessibility, consistency, copy; no lorem | All user-facing phases | Route/state QA, truthful copy/data, zero serious/critical a11y |
| 42 | 41 | M8 | [Final Documentation](./phase-42-prompt-phase-41-final-documentation.md) | Architecture, screenshots/GIF, diagrams, API, setup, deploy, security, GitHub distribution and benchmarks | All evidence | New engineer reproduces setup/deploy/recovery; docs/media/release claims trace to evidence |
| 43 | 42 | M8 | [Final Red Team](./phase-43-prompt-phase-42-final-red-team.md) | Attack all listed surfaces; document and fix | Phases 30-41 | Independent finding ledger, regressions and residual-risk approval |
| 44 | 43 | M8 | [Final Engineering Review](./phase-44-prompt-phase-43-final-engineering-review.md) | Staff-level architecture/technology/failure/security/ops/reproducibility review | Phases 0-42 | All review weaknesses fixed or explicitly accepted |

## Source Tensions Made Explicit

- Prompt Phase 9 references Realtime and reliability delivered later; split durable acceptance from later enhancement receipt.
- Prompt Phase 22 needs audit before dedicated Phase 28; create a minimal audit contract early and complete it later.
- Prompt Phase 23 needs analytics from Phase 24; domain acceptance precedes metrics acceptance.
- Prompt Phase 15 progress must survive without Realtime.
- Prompt Phase 31 completes—not begins—telemetry.
- Prompt Phases 33-39 require real targets; checked-in configuration without runtime proof is HOLD.

## Traceability Rule

A requirement may be checked only when its linked phase receipt exists. Requirements marked `FUTURE_GOAL` by accepted `DEC-001` remain unchecked master-program work and do not block v0.1. Any scope-specific exclusion inside M0-M4 requires an explicit accepted `DEFER` decision; never leave included work ambiguously incomplete while claiming the Goal complete.
