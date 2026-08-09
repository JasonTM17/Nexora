# Nexora Decision Log

## Status Vocabulary

- `OPEN`: material user decision not yet made.
- `PROPOSED`: recommended default awaiting approval.
- `ACCEPTED`: binding decision; changes require a new entry.
- `LATER_GOAL`: intentionally unresolved because it is outside the active release Goal.
- `SUPERSEDED`: replaced by a later accepted decision.

## Blocking Decisions

| ID | Decision | Proposed default | Status | Blocks |
|---|---|---|---|---|
| DEC-001 | Goal completion line | One finite v0.1 Goal for M0-M4, then new Goals for M5-M8; master plan retains all phases | ACCEPTED | Formal Goal |
| DEC-002 | Repository boundary | Initialize `D:\Nexora`; exclude local `engineer/` bundle | PROPOSED | Prompt Phase 1 |
| DEC-003 | Git integration topology | Protected `main` plus `integration/<milestone>` and reviewed worker branches | PROPOSED | Remote writes |
| DEC-004 | Merge approval | Project Manager may authorize reviewed worker integration; milestone merge to `main` requires combined PASS plus Advisor/Kongming dual receipt, while first remote push/PR and production release require user approval | PROPOSED | Workflow activation |
| DEC-005 | Deployment target | Production-shaped local + staging first; select paid cloud/Kubernetes target before M7 | LATER_GOAL | Prompt Phases 34-39 |
| DEC-006 | Authentication | Supabase Auth identity; Spring validates tokens and owns domain authorization | PROPOSED | Prompt Phases 4-5 |
| DEC-007 | Browser data access | Browser domain traffic uses the same-origin Next.js BFF/Server Component boundary before Spring; direct Supabase is limited to approved Auth, signed private Storage and authorized private Realtime flows | PROPOSED | Threat model |
| DEC-008 | Tenant model | Shared database/schema with mandatory tenant keys, composite constraints, application checks, RLS defense in depth | PROPOSED | Data model |
| DEC-009 | AI data policy | Provider receives only authorized minimal context; sensitive logging disabled/redacted; deterministic CI provider required | PROPOSED | Prompt Phases 17-21 |
| DEC-010 | AI provider contract | DeepSeek-compatible adapter uses env-only secret; base URL and model ID verified live during provider phase | PROPOSED | Live AI smoke |
| DEC-011 | v0.1 external-call, Stitch and cloud budget | DeepSeek: USD 5 total, max 25 calls, concurrency 1, one bounded retry and kill switch. Stitch comparison: three directions, exactly four shared anchors per direction, one initial generation plus at most two bounded edit passes per anchor, hence at most 36 generation/edit operations total. Remaining selected-direction inventory is hand-designed/hand-built by default; any further Stitch operation requires a separately accepted numeric amendment and receipt. No credit purchase or paid upgrade without separate R3 approval. New managed-cloud spend is USD 0 during M0-M4 unless a later R3 receipt names provider/project/region, hard cap, TTL and teardown owner | OPEN | Formal Goal, Stitch dispatch and any R3 call/provision |
| DEC-012 | Stitch usage | Explore three evidence-backed directions, user approves screenshots/DESIGN.md, then hand-build and verify production Next.js | ACCEPTED | UI implementation |
| DEC-013 | Initial page blocks | Hero, RichText, FeatureGrid, CTA, FAQ | PROPOSED | Prompt Phases 7-9 |
| DEC-014 | Distributed systems trigger | DB jobs/outbox first; Go/NATS/Redis boundaries require phase evidence and benchmarks | PROPOSED | Prompt Phases 13-14, 24, 27 |
| DEC-015 | Public license | Apache-2.0 for the repository and first public release, with third-party notices/provenance handled separately | ACCEPTED | Formal Goal and initial push |
| DEC-016 | Data lifecycle and privacy | Accept per-plane retention, account versus tenant deletion, user export, chat/document purge, analytics anonymization, audit exceptions, provider limitations and purge-on-restore behavior before M4 storage/chat dispatch or production claims | OPEN | M4 lifecycle work and Security/DR |
| DEC-017 | Availability targets | Candidate: 99.9% public/CMS/retrieval and 99.5% generation availability; critical-service RTO <=30m, full data-plane RTO <=60m, Postgres RPO <=15m, object RPO <=60m, JetStream replay/snapshot contract, app rollback <=10m; revise from measured load/cost before M7 | LATER_GOAL | Ops acceptance |
| DEC-018 | Pre-Goal plan pin | After plan approval, perform a minimal public-safe local Git/control-plane commit; pin its SHA plus plan digest before Goal creation | PROPOSED | Formal Goal |
| DEC-019 | v0.1 release classification | `v0.1.0-alpha.1`, a production-shaped developer preview; no full production-ready claim before M6/M7 gates | PROPOSED | Goal wording and release claims |
| DEC-020 | Embedding route | Local version-pinned TEI with `Qwen/Qwen3-Embedding-0.6B` at fixed 1024 dimensions, subject to M0 hardware/corpus benchmark | PROPOSED | Prompt Phase 17 |
| DEC-021 | Public site routing | Path-based v0.1 routing behind a validated site resolver; preserve host/subdomain abstraction for a later approved deployment | PROPOSED | Prompt Phase 9 public render |
| DEC-022 | Documentation and distribution | Real screenshots, requested GIF, architecture diagrams, GitHub About/Releases, GHCR images, SBOM/provenance and live verification are mandatory | ACCEPTED | Master-program release acceptance |
| DEC-023 | Managed web/data split | Target architecture is Vercel for primary Next.js web and managed Supabase for Auth/Postgres/pgvector/Storage/private Realtime, with Spring owning domain access. M0-M4 uses local/CI or separately authorized non-production preview validation; accepting the architecture does not authorize a paid project, production claim or recurring spend | PROPOSED | Hosted validation and M7 |
| DEC-024 | Backend production runtime | Managed Kubernetes aligned with Prompt Phases 34-39, minimum resilient critical-service capacity; provider/region selected under budget before M7 | LATER_GOAL | Prompt Phases 34-39 |
| DEC-025 | UI component boundary | Ant Design 6.x is the primary admin/builder library; evaluate Ant Design X 2.x for RAG chat/sources; Tailwind/custom primitives own branded public pages; do not ship shadcn as a second full design system | ACCEPTED | Frontend foundation and UI tasks |
| DEC-026 | Innovation program boundary | Preserve Option A; allow only small hooks that strengthen existing M2/M4 contracts, keep full adaptive simulator/AI co-editor/impact radar/content credentials/connectors in future Goals until same-revision Advisor/Kongming and user approval | PROPOSED | Innovation dispatch, not current Goal activation |
| DEC-027 | Frontend CSP/cache surfaces | M1 must approve a route-level ADR: dynamic Studio/auth may use strict nonce CSP; public schema pages preserve tested cacheability through external/static CSS, hashes or another documented compatible strategy; no blanket nonce or default `unsafe-inline` | PROPOSED | M1 frontend-foundation dispatch |
| DEC-028 | Supabase platform boundary | Domain data stays behind Spring in application-owned, non-exposed schemas; no implicit Data API grants; only provider-documented policies/triggers touch managed schemas; managed extension versions are observed and compatibility-tested rather than SQL-pinned | PROPOSED | M1 database foundation and M3 Realtime dispatch |

## Already Accepted Constraints

| ID | Constraint | Evidence |
|---|---|---|
| DEC-A01 | Discuss and approve plan before formal Goal | User request |
| DEC-A02 | Use AK workflow, Advisor and Kongming supervision | User request |
| DEC-A03 | Small logical commits and reviewed push | User request |
| DEC-A04 | No branch prefix `codex/` | Repository instruction |
| DEC-A05 | Secrets stay local and untracked | User request plus security baseline |
| DEC-A06 | Repository docs include images, GIF and architecture diagrams | User request on 2026-08-09 |
| DEC-A07 | GitHub About, Releases, Packages/GHCR and Docker distribution are required | User request on 2026-08-09 |
| DEC-A08 | Vercel, Supabase and backend continuity receive Advisor/Kongming supervision | User request on 2026-08-09 |
| DEC-A09 | Stitch plus Ant Design must produce a distinctive, professional Nexora UI informed by leading products | User request on 2026-08-09 |
| DEC-A10 | Every material matter requires independent Advisor and Kongming supervision before advancement | User request on 2026-08-09 |

## Accepted Decision Record — DEC-001

- Accepted on: 2026-08-09.
- User answer: Option A is accepted.
- Binding interpretation: the first formal Goal includes M0-M4 / Prompt Phases 0-21 only; M5-M8 remain mandatory in the master plan and are executed through later finite Goals.
- Consequence: M5-M8 work is `FUTURE_GOAL`, not missing v0.1 work. v0.1 completion criteria may not require Kubernetes, GitOps or disaster-recovery evidence from later milestones.
- Change control: expanding or shrinking this line requires a new accepted decision and a re-pinned Goal/plan revision.

## Accepted Decision Record — DEC-015

- Accepted on: 2026-08-09.
- User answer: “Oke luôn” to the proposed Apache-2.0 license.
- Binding interpretation: the repository, source distribution and first public release use Apache License 2.0; dependency, model, font, design-reference and media licenses remain independently inventoried and do not inherit the repository license.
- Bootstrap consequence: M1-T01 creates the canonical `LICENSE` from the official Apache-2.0 text plus a third-party notice/provenance workflow before the first public push. No copyright owner/year is invented where the canonical license does not require it.
- Change control: a different repository license requires a new accepted decision, provenance review and same-revision Advisor/Kongming receipts before public redistribution.

## v0.1 Activation Decisions

The finite M0-M4 Goal requires accepted values for DEC-002 through DEC-004, DEC-006 through DEC-016, DEC-018 through DEC-021 and DEC-025 through DEC-028. DEC-012, DEC-015, DEC-022 and DEC-025 are already accepted.

- Pre-Goal deterministic fixtures and local validation may proceed only inside the approved control-plane bootstrap. The formal Goal does not activate until DEC-011 is accepted because its completion requires bounded live DeepSeek/embedding evidence and its design wave consumes Stitch quota. DEC-011 acceptance authorizes only the stated call/operation ceilings; it does not authorize a paid upgrade or managed-cloud provision.
- DEC-015 is resolved as Apache-2.0. The public-push gate now checks the canonical license text plus independent third-party dependency/model/font/media provenance rather than asking for another license choice.
- DEC-005, DEC-017 and DEC-024 are later-Goal decisions and do not block local/integration v0.1 activation. A hosted Vercel/Supabase v0.1 claim requires DEC-023 plus budget/authority.

## Decision Procedure

Advisor asks one material question at a time. After an answer:

1. Record exact answer and rationale.
2. Mark decision accepted.
3. Propagate to all affected plan/phase files.
4. Run whole-plan consistency sweep.
5. Ask the next highest-impact open decision.
