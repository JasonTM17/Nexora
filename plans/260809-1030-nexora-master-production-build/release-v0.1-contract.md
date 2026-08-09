# Nexora v0.1 Release Contract

## Decision State

- Release strategy: `ACCEPTED` through `DEC-001` on 2026-08-09.
- Candidate Goal scope: milestones M0-M4, Prompt Phases 0-21, execution files 01-22.
- Master-program scope retained: M0-M8, Prompt Phases 0-43.
- Current activation state: `PENDING USER REVIEW`; this contract is not an active Goal.

## Three Authority Layers

| Layer | Scope | Completion meaning |
|---|---|---|
| Master program | M0-M8 / Prompt Phases 0-43 | The complete Nexora production program is delivered and finally reviewed |
| Release v0.1 | M0-M4 / Prompt Phases 0-21 | A finite integrated tenant CMS, publishing, knowledge, and secure-RAG release is accepted |
| Runtime task | One bounded requirement slice | The reviewed exact HEAD is merged and its combined integration gate passes |

M5-M8 are `FUTURE_GOAL`, not missing v0.1 work and not silently deferred. They stay mandatory for the complete master program.

## v0.1 Product Outcome

Deliver a production-oriented functional `v0.1.0-alpha.1` release in which an organization can:

```text
create or join an organization
-> receive tenant-scoped permissions
-> compose a page from five safe schema-driven blocks
-> review, publish and rollback an immutable version
-> see the public page update without a frontend rebuild
-> upload an authorized document
-> run bounded durable ingestion
-> retrieve with lexical plus vector search
-> ask DeepSeek-backed chat using authorized context only
-> persist, reload, cancel, regenerate and delete tenant-scoped conversation history
-> inspect real, resolvable citations
```

An unauthorized actor must never place another tenant's row, object, chunk, vector, citation, event, chat session/message or prompt context into an accepted result.

## Included Milestones

| Milestone | Prompt phases | Required integrated receipt |
|---|---:|---|
| M0 | 0 | Approved baseline, architecture, threat model, runtime inventory, decisions, Goal warmup |
| M1 | 1-3 | Fresh-clone reproducible repository, Spring platform, Next.js product shell, shared contracts |
| M2 | 4-11 | Two-tenant identity/RBAC, page builder, review, immutable publish, public render and rollback |
| M3 | 12-14 | Private Realtime, bounded Go event ingress, NATS boundary and transactional outbox with durable fallback |
| M4 | 15-21 | Secure upload, durable ingestion, pgvector, hybrid retrieval, permission-before-context RAG, persistent tenant chat/history, citations and evaluation |

Prompt Phase 13 remains included because the accepted v0.1 line is M0-M4. The Go/NATS boundary must still earn an ADR and measurable acceptance; it may not invent M5 analytics capability.

## Explicit v0.1 Non-Goals

These remain planned and mandatory in later Goals, but do not block v0.1 completion:

- M5 flags, experiments, product analytics, personalization, recommendation, notification completion, full audit UI and global search.
- M6 complete platform security hardening, full observability stack and whole-product performance program.
- M7 production cloud target, Kubernetes, Helm, Terraform, Argo CD, hardened deployment pipeline and disaster-recovery drill.
- M8 final product-wide polish, final evidence publication, final attacker review and Staff-level program review.
- Full Innovation Backlog capabilities such as the adaptive scenario simulator, page-wide Impact Radar, AI co-editor, Content Credentials and connector/action fabric. Only a separately accepted refinement that strengthens an existing M2/M4 contract without changing the v0.1 outcome may be folded into this Goal.

v0.1 must not be described as fully production-certified. It is a real production-oriented integrated release; final production eligibility belongs to the master-program M6-M8 gates and the accepted production-continuity contract.

## Completion Contract

The v0.1 Goal completes only when all of the following are true:

- [ ] Every included Prompt Phase 0-21 has an accepted phase receipt.
- [ ] M0 baseline documents are accepted on `main`; M1-M4 integration branches are merged sequentially into the approved `main` release head.
- [ ] The complete two-tenant CMS/publishing/SEO and secure-RAG persistent-chat journeys pass on that exact main HEAD using the accepted deterministic seed manifest.
- [ ] Cross-tenant, stale-membership, unsafe-page, unsafe-upload, prompt-injection and unauthorized-context STOP tests pass.
- [ ] Critical UI states pass keyboard, 375px, desktop, reduced-motion and accessibility checks.
- [ ] Deterministic CI evidence and bounded live Supabase/DeepSeek/NATS/embedding evidence are labeled separately; deterministic evidence alone cannot complete the DeepSeek-backed release.
- [ ] Exact base/head, changed paths, commands, exit codes, review verdicts, merge SHA and limitations are recorded.
- [ ] Tracked and staged secret/provenance scans pass; no credential value is present in history or artifacts.
- [ ] `main` and the approved public remote release head match; publication remains an explicit R3 action, and withholding that approval leaves the Goal `NEEDS_USER` rather than complete.
- [ ] No unresolved blocker exists inside M0-M4.
- [ ] Its required public prerelease README, M0-M4 architecture visuals, real captures/GIF, notes and media manifest identify the exact release head and limitations; absent R3 authority leaves the Goal `NEEDS_USER`.

Open M5-M8 decisions, including the final Kubernetes/cloud target and final SLO/RPO/RTO, do not block this release.

GitHub Packages/GHCR, hardened deployment and DR remain M7 obligations unless the user explicitly expands this Goal. Their absence must be stated in the alpha prerelease and may not be hidden by a production-ready badge.

## Evidence Classes

| Class | Proves | Does not prove |
|---|---|---|
| E0 static | Source/config/contract is present | It runs |
| E1 deterministic | Unit, schema, contract and fixture behavior | Live provider or distributed integration |
| E2 local integration | Real local Postgres/Supabase/NATS/service behavior | Hosted staging/production behavior |
| E3 browser | User-visible workflow and accessibility at exact HEAD | Cloud resilience or scale |
| E4 live provider | Credentialed provider request, model ID, date and bounded result | General availability or production capacity |
| E5 release | Reviewed main SHA, artifact identity and remote equality | Deployment or recovery unless separately observed |

## Estimate Envelope

After adding bullet-level requirement expansion, Supabase compatibility, profile/SEO/hide-show, deterministic seed and persistent chat/lifecycle work, the planning baseline is roughly 115-180 person-days for M0-M4 before integration overhead. Reserve 25-35% for independent review, security rework, integration and evidence, giving an initial envelope of approximately 144-243 person-days. This is capacity planning, not a delivery promise; M0 replaces it with task estimates based on the real repository, hardware, provider limits and accepted child catalog.

Parallel agents can reduce elapsed time only where paths and contracts are genuinely independent. They do not reduce the security, test, integration or evidence scope.

## Pre-Goal Control-Plane Bootstrap

The future Goal must pin a Git SHA, but the current workspace is not yet a Git repository. Resolve that in this exact order:

1. User approves this parent plan, recommended decisions and workflow; that answer is approval evidence, not remote-write/provision authority.
2. Resolve the repository boundary and exact approval point for remote write; run a public-safe secret/provenance check without printing values. Accepted Apache-2.0 plus third-party provenance remains mandatory before publication.
3. Initialize exact `D:\Nexora` with `main`, configure the supplied `origin` without pushing, reserve ignored `engineer/`, `.worktrees/` and local env/runtime state, then create one root seed commit containing only the user-approved parent plan/governance/ignore/env-template allowlist.
4. Treat step 3 as the sole pre-ledger exception: one Git Manager, one direct `main` commit, no concurrent writer, no product code and no remote mutation. Pin its staged paths, public-safe scan and user-approved candidate digest.
5. Resolve the absolute Git common directory and create the versioned shared control ledger/genesis record bound to the seed SHA/parent digest/user-decision receipt with child catalog explicitly `PENDING_C0_01R`. Before either semantic writer dispatches, run the complete C0-05 gate: prove two temporary worktrees under `D:\Nexora\.worktrees\` contend against the same database with exactly one same-boundary lease winner, then dry-run the dependency graph to prove `M3-T04` stays non-`READY` until `M3-T02` is `INTEGRATED` and the Go/NATS ADR is dual-reviewed; M3-T05 consumes only a pinned M3-T04 `frozen_interfaces` head while M3-T04 remains non-`MERGE_READY`, head movement blocks both, and joint evidence makes both `MERGE_READY` without a cycle.
6. Through that ledger, dispatch `docs/c0-decision-ratification` in its own worktree/lease to apply only the exact user-approved decisions. Obtain exact-head independent and dual receipts; Git Manager mechanically integrates its `MERGE_READY` head and records the resulting clean main SHA before any catalog writer starts.
7. From that exact integrated decision head, dispatch `docs/c0-requirements-catalog` in its own worktree/lease. Expand all 141 source-hashed parent spans over lines `1..5169` plus every `UREQ-*` into stable bullet-level IDs with zero unclassified normative lines; obtain same-catalog Advisor/Kongming receipts, then Git Manager mechanically integrates its exact `MERGE_READY` head and records the second clean main SHA.
8. Run combined decision/catalog/public-safety checks on final control-plane `main`. Reproduce `NEXORA-SEMANTIC-DIGEST-1` with two implementations and generate a sanitized manifest containing logical/relative paths only; record final semantic/file-list/manifest/source/parent/child-catalog digests and final main SHA in a hash-chained ledger binding event.
9. Verify the unchanged C0-05 contention/graph receipt identities, then run same-candidate Advisor/Kongming activation review and Goal warmup; create the finite M0-M4 Goal only after verdict `READY`, with innovation hooks defaulting to an empty list.
10. Treat the first remote push as a separate approved R3 action under `DEC-004`; it is still required before the requested v0.1 release can complete.

No product implementation begins during this bootstrap.

## Activation Blockers

Before the v0.1 Goal is created, the Controller must have accepted values for:

- Repository boundary, accepted Apache-2.0 artifact and third-party provenance receipt.
- Supabase platform boundary: accepted DEC-028, no implicit domain Data API exposure, supported managed-schema policy DDL only and observed extension compatibility.
- Branch/integration topology and merge/push authority.
- v0.1 release identity and public site routing.
- AI chat, embedding and data-egress contracts, including fixed vector dimension.
- Accepted DEC-011 ceilings: DeepSeek USD/calls/retry/kill switch; Stitch direction/screen/edit-operation quota with no paid upgrade; default USD 0 new managed-cloud spend in M0-M4 unless a separate R3 receipt names provider/project/region, hard cap, TTL and teardown owner.
- Initial document limits plus accepted DEC-016 export/account-versus-tenant deletion/chat-document purge/anonymization/retention contract.
- Semantic/file-list/manifest/source/catalog digests, baseline Git SHA and clean public-safe repository evidence.

The Goal requires live provider evidence, so “deterministic-only” may describe preflight/progress but is not a release-completion alternative unless the user explicitly amends the product outcome and release identity.
