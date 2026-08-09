---
phase: 1
title: "Prompt Phase 0 — Deep Scout"
status: pending
priority: P1
effort: "2-4 days"
dependencies: []
---

# Prompt Phase 0 — Deep Scout

## Context

- Milestone: M0 — Truthful Baseline.
- Source: master prompt Prompt Phase 0.
- Current fact: workspace has no application code or Git repository; all architecture paths below remain planned until verified.

## Outcome

After C0-07 activates the pinned Goal, revalidate its exact control-plane/source/catalog/decision identities against observed repository/runtime truth, then produce the evidence-backed M0 architecture/problem baseline, dependency graph, risk register, implementation sequence and a `READY/HOLD` handoff for Prompt Phase 1. Do not implement product features or recreate/expand the Goal.

## Requirements

- Inspect repository boundary, hidden files, Git/remote state, instructions, manifests, source, tests, migrations, infrastructure, docs, assets and local tooling.
- Verify installed toolchain and current stable framework compatibility from primary sources.
- Separate product source from local `engineer/` AgentKit bundle.
- Revalidate—without weakening or silently changing—the pinned product personas, v0.1 completion line, full-program scope, non-goals, deployment intent, data classification and budget accepted through C0.
- Produce system, trust-boundary, tenant, publishing, ingestion and RAG data-flow diagrams.
- Activate/validate project AgentKit paths, privacy block, artifact gate and live routing evidence.

## Planned Files and Ownership

| Action | Path | Owner |
|---|---|---|
| Create | `docs/project-assessment.md` | Planner |
| Create | `docs/architecture/system-overview.md` | Architect |
| Create | `docs/implementation-plan.md` or links to this canonical plan | Planner |
| Create | `docs/security/threat-model.md` | Security reviewer |
| Create | `docs/decisions/*.md` | Controller for non-activating architecture ADRs only; any activation-decision change returns to C0 re-pin |
| Update | `.agentkit/config.yaml` | AgentKit configuration owner after approval |

Forbidden: product code, Git reinitialization, origin rewrite, pinned Decision Log/requirement-catalog mutation, Goal recreation, remote writes, credential inspection/output, paid calls or deployment without a separately accepted task.

## Execution Steps

1. Verify the active Goal pins the final C0 main SHA, semantic/file-list/manifest/source/parent/child-catalog digests, accepted Decision Log revision, final ledger-binding event and same-candidate activation receipts.
2. Run read-only scouts across workspace, remote metadata, toolchain and prompt requirements.
3. Verify every existing-file claim; mark absent targets as `[PLANNED]` and treat any C0 identity drift as `HOLD`, never as an invitation to edit the pin in M0.
4. Verify accepted `DEC-001` is propagated everywhere: finite M0-M4 Goal, M5-M8 retained as later Goals, and no stale completion wording.
5. Write architecture and threat boundaries that conform to the pinned outcome/decisions; record alternatives and consequences without changing activation scope.
6. Build dependency, shared-file collision, ownership and validation matrices.
7. Have Advisor and Kongming independently review the same M0 baseline/workflow candidate and record Controller disposition.
8. Run whole-plan consistency sweep and produce the post-activation M0 handoff packet for Prompt Phase 1. If a material decision or Goal contract must change, stop and route a separately approved C0 re-pin instead of presenting a new activation packet from M0.

## Validation Matrix

| Check | Evidence |
|---|---|
| Repository truth | Root/origin/visibility/branch/status inventory |
| Toolchain truth | Version and compatibility report with primary-source links |
| Plan coverage | Prompt Phase 0-43 matrix has no missing phase |
| Runtime truth | Live agent/runtime/model/control inventory or explicit gaps |
| Secret safety | No values in plan/reports/logs; env referenced by name only |
| Contradictions | Whole-plan sweep reports zero unresolved conflicts or lists blockers |

## Commit Plan

- `docs(architecture): assess Nexora baseline and boundaries`
- `docs(plan): add approved production delivery contract`
- `chore(agentkit): enable approved project workflow guards`

Commits occur only after Git bootstrap is separately approved; until then, these are proposed clusters.

## Acceptance

- [ ] All blocking decisions required for the chosen Goal scope were already accepted and their exact revision/digests match the active Goal; M0 did not mutate them.
- [ ] Baseline documents cite actual evidence and label unverified claims.
- [ ] Architecture, security, workflow, release and cost boundaries conform to the approved Goal or the result is `HOLD` with a C0 re-pin recommendation.
- [ ] AgentKit configuration validates without secrets.
- [ ] Same-candidate Advisor and Kongming M0 baseline verdicts pass, every finding has Controller disposition and unresolved contradictions are zero; the existing C0 activation receipts are verified, not regenerated here.

## Stop Conditions

Repository or C0 identity differs from the active Goal pin; secret exposure; unclear tenant authority; a supposedly accepted completion line is absent/changed; runtime controls cannot satisfy required risk tier; any request to resolve an activation decision inside M0.

## Handoff

Provide the post-activation M0 acceptance/resume packet and exact first Prompt Phase 1 task. Do not create, recreate or silently re-pin the Goal; route material drift back through the approved C0 change path.
