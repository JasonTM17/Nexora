---
title: "Nexora Master Production Build"
description: "Files-first production program for the complete Nexora prompt, with 44 traceable phases, supervised multi-agent delivery, and evidence-gated integration."
status: in-progress
priority: P1
effort: "Multi-release program; estimate after Prompt Phase 0"
branch: "pending-repository-bootstrap"
tags: [feature, frontend, backend, database, auth, ai, infra, critical]
blockedBy: []
blocks: []
created: 2026-08-09
---

# Nexora Master Production Build

## Status

`USER-APPROVED — C0 CONTROL-PLANE IN PROGRESS`. This plan permits only the approved local C0-01 through C0-07 control-plane work. It is not permission to implement product code, push, spend money, provision infrastructure, release, or deploy; the formal Goal remains blocked until C0-06/07 complete.

## Outcome

Build the complete Nexora AI-native adaptive digital experience platform described by the supplied master prompt. Preserve Prompt Phases 0-43. Deliver them through integration milestones so each accepted increment is runnable, secure, observable, reviewable, and recoverable.

## Canonical Authority

1. Current explicit user instructions, the user-approved [Outcome Contract](./outcome-contract.md), and the activated Goal contract when one exists.
2. `ACCEPTED` entries in the [Decision Log](./decision-log.md), each backed by its user/dual-review receipt where required.
3. The pinned master-source identity plus the source-hashed [parent requirements catalog](./master-requirements-catalog.md) and its expanded child catalog once C0-01R is accepted.
4. This approved plan, the [v0.1 Release Contract](./release-v0.1-contract.md), and [Requirements Matrix](./requirements-matrix.md).
5. Individual `phase-*.md` implementation contracts.
6. [Workflow Configuration](./workflow-configuration.md) and [Acceptance and Evidence Contract](./acceptance-and-evidence.md).
7. Runtime task views, chat messages, dashboards, and GitHub issues are projections only.

A lower authority cannot weaken, omit or silently reinterpret a higher authority. A conflict blocks dispatch until the Controller records the exact source/decision identities, obtains the required user and dual-review disposition, updates affected child requirements and re-pins the semantic candidate.

## Review Documents

| Document | Purpose |
|---|---|
| [Outcome Contract](./outcome-contract.md) | Product boundary, invariants, completion semantics |
| [v0.1 Release Contract](./release-v0.1-contract.md) | Accepted finite M0-M4 Goal boundary and evidence line |
| [Technology Decisions](./technology-decisions.md) | Chosen stack, alternatives, current source evidence and change triggers |
| [UI/UX, Stitch and Ant Design Strategy](./ui-ux-stitch-ant-design-strategy.md) | Reference research, three visual directions, AntD boundaries, design threads and UI gates |
| [Innovation and Differentiation Backlog](./innovation-and-differentiation-backlog.md) | Missing-capability analysis, breakthrough candidates, experiments, kill criteria and no-scope-creep placement |
| [Requirements Matrix](./requirements-matrix.md) | Prompt Phase 0-43 traceability and exact phase links |
| [Master Requirement Catalog](./master-requirements-catalog.md) | Source hash, all master sections, user-chat overlays and pre-Goal child-requirement expansion |
| [Delivery Roadmap](./delivery-roadmap.md) | Milestones, dependencies, parallelism, release gates |
| [Workflow Configuration](./workflow-configuration.md) | Roles, models, task states, worktrees, review, merge, keepalive |
| [Team Operating Model](./team-operating-model.md) | Thread/subagent roles, model routing, branch/worktree lifecycle and merge receipts |
| [Thread, Branch and Worktree Runbook](./thread-branch-worktree-runbook.md) | Exact dispatch packet, writer lease, worktree, review, integration, timeout and manager-dashboard procedure |
| [M0-M4 Execution Ledger](./m0-m4-execution-ledger.md) | Proposed task waves and exclusive ownership for the first Goal |
| [Documentation, Media and GitHub Distribution](./documentation-media-and-github-release.md) | Real screenshots/GIF/diagrams, About, Releases, GHCR, SBOM and provenance |
| [Production Continuity and Hosting](./production-continuity-and-hosting.md) | Vercel/Supabase/backend topology, SLOs, rollback, monitoring and recovery |
| [Supabase Platform Boundary](./supabase-platform-boundary.md) | Managed-schema ownership, Data API grants, Realtime RLS, extension drift and restore evidence |
| [Data Lifecycle and Privacy](./data-lifecycle-and-privacy-contract.md) | Account/tenant deletion, export, chat/document purge, anonymization, retention and backup reconciliation |
| [Demo Data and Evidence Fixtures](./demo-data-and-evidence-fixtures.md) | Deterministic Nexora University seed, credential safety, reset boundaries and media provenance |
| [Tóm tắt duyệt bằng tiếng Việt](./approval-summary-vi.md) | Technology, team, branch flow, releases and production direction in one review view |
| [Acceptance and Evidence](./acceptance-and-evidence.md) | PASS/HOLD/STOP/DEFER and proof requirements |
| [Risk Register](./risk-register.md) | Product, security, AI, delivery, cost, and operations risks |
| [Decision Log](./decision-log.md) | User decisions required before Goal activation |
| [Gói quyết định chốt trước Goal](./activation-decision-pack-vi.md) | Các mặc định còn lại, tác động, rào chắn và cách người dùng chấp thuận/sửa đổi |
| [Goal Contract Template](./goal-contract-template.md) | Exact binding between the future Goal and this plan |
| [Semantic Digest Contract](./semantic-digest-contract.md) | Reproducible same-candidate file set, canonicalization, framing, manifest and review identity |
| [Validation Log](./validation-log.md) | Structural validation, red-team disposition, consistency sweep |

## Integration Milestones

| Milestone | Prompt phases | Integrated outcome | Goal disposition |
|---|---:|---|---|
| M0 | 0 | Truthful baseline and approved execution contract | v0.1 Goal |
| M1 | 1-3 | Buildable monorepo, Spring platform, Next.js shell | v0.1 Goal |
| M2 | 4-11 | Secure tenant CMS and adaptive publishing | v0.1 Goal |
| M3 | 12-14 | Realtime and durable event reliability | v0.1 Goal |
| M4 | 15-21 | Permission-aware knowledge and secure RAG | v0.1 Goal |
| M5 | 22-29 | Flags, experiments, analytics, adaptive intelligence | FUTURE_GOAL |
| M6 | 30-32 | Security, observability, measured performance | FUTURE_GOAL |
| M7 | 33-39 | Supply chain, deployment, GitOps, tested recovery | FUTURE_GOAL |
| M8 | 40-43 | Product polish, evidence-backed documentation, final review | FUTURE_GOAL |

## First Goal Success Criteria

The future first formal Goal's completion boundary is accepted under `DEC-001`: it will end after M4 / Prompt Phase 21. Its complete measurable contract is [v0.1 Release Contract](./release-v0.1-contract.md). M5-M8 are not counted as incomplete v0.1 work, and no Goal is active yet.

## Master Program Success Criteria

- [ ] Every Prompt Phase 0-43 has a completed phase receipt or an explicit user-approved `DEFER` decision.
- [ ] Every merged scope records exact base/head, owned paths, checks, reviewer verdict, merge SHA, and limitations.
- [ ] Every material C3/R3 decision or candidate has independent same-identity Advisor and Kongming receipts plus a recorded Controller disposition.
- [ ] Cross-tenant tests show zero unauthorized success; unauthorized chunks never reach LLM context.
- [ ] Critical UI works at 375px and desktop, by keyboard, with complete loading/error/empty/denied states.
- [ ] Deployment, restore, security, AI-quality, scale, and performance claims link to contemporaneous evidence.
- [ ] README, real screenshots, requested GIF and rendered architecture diagrams reconcile to the exact release SHA through a media manifest.
- [ ] GitHub About, Releases and GHCR packages are live-observed; release/image/deployment digests, SBOM and provenance reconcile.
- [ ] Production continuity is measured against accepted SLO/RPO/RTO with rollback and isolated restore drills; no ping hack or unsupported zero-downtime claim.
- [ ] `main` and public remote contain the same reviewed release head, with no secret-shaped tracked content.
- [ ] Whole-plan consistency sweep reports zero unresolved contradictions.

## Activation Boundary

The future v0.1 Goal may activate only after the M0-M4 activation decisions identified in [Decision Log](./decision-log.md) are accepted, the user approves this plan/workflow, a minimal control-plane Git commit provides a real plan SHA/digest, and whole-plan validation passes. Later-Goal decisions do not block v0.1. Implementation command is intentionally omitted until then.

<!-- slug: nexora-master-production-build -->
