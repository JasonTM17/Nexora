# Future v0.1 Goal Contract Template

## Purpose

This file defines how the future finite M0-M4 Goal binds to the approved master plan. It is not an active Goal. `DEC-001` is accepted; the scope placeholder below must resolve to M0-M4 / Prompt Phases 0-21, not M0-M8.

## Template

```text
Objective:
Deliver the user-approved Nexora v0.1.0-alpha.1 release, covering M0-M4 and Prompt
Phases 0-21, described by the canonical plan at
D:\Nexora\plans\260809-1030-nexora-master-production-build\plan.md.

Pinned authority:
- Approved plan Git SHA: <sha>
- Semantic digest algorithm: NEXORA-SEMANTIC-DIGEST-1
- Plan semantic digest, file-list digest and evidence-manifest digest: <digests>
- Master-prompt source SHA-256 plus parent and expanded child requirement-catalog digests: <digests>
- Outcome Contract version: <version>
- Included phase range: Prompt Phases 0-21 / execution files 01-22
- Included milestones: M0-M4
- Excluded from this Goal but retained in master plan: M5-M8 / Prompt Phases 22-43
- Accepted Decision Log revision: <revision>
- Canonical control-ledger path: <absolute path derived from git common dir>
- Control-ledger schema/genesis receipt: <receipt and genesis hash>
- Accepted innovation hooks: []

Execution contract:
1. Treat the pinned plan, source-hashed parent catalog, expanded child requirements
   and unchecked phase items as canonical; a phase checkbox cannot erase a child ID.
2. Execute dependency-ready work only.
3. Use separate worktrees under ignored D:\Nexora\.worktrees and disjoint ownership
   for concurrent writers.
4. Apply the role, model, permission, timeout, review, merge, and keepalive
   rules in workflow-configuration.md.
5. Require the evidence in acceptance-and-evidence.md before changing durable
   status to complete.
6. Never omit, silently defer, weaken, or reorder dependency-bound work.
7. Never expand Goal authority to credentials, spending, deployment, release,
   deletion, or destructive actions without exact user approval.
8. Resume from recorded state and the first incomplete accepted item.
9. Stop on STOP conditions; use HOLD for missing evidence; ask the user for
   unresolved material decisions.
10. Treat M5-M8 as FUTURE_GOAL, never as missing v0.1 work or completed work.
11. Do not describe v0.1 as fully production-ready unless an accepted amendment
    adds and passes the required M6/M7 security, deploy, rollback and restore gates.
12. Produce the alpha documentation/media evidence required by DEC-022 from a
    running exact product head; do not imply that future M7 GHCR/production gates passed.
13. Follow documentation-media-and-github-release.md and
    production-continuity-and-hosting.md for every public/reliability claim.
14. Require independent Advisor and Kongming receipts on the same exact candidate
    for every material C3/R3 decision, milestone, release or production action;
    record Controller disposition and invalidate both receipts if identity changes.
15. Treat innovation-and-differentiation-backlog.md as non-executable research;
    no INN-* capability enters this Goal without an accepted decision, dual review
    and a re-pinned plan/Goal when the public outcome or estimate changes.
16. Keep `accepted_innovation_hooks` as an empty list by default; each non-empty
    entry requires its own Decision ID, requirement/task IDs, estimate, same-revision
    Advisor/Kongming receipts and a Goal re-pin whenever scope or material contracts change.

Completion:
The Goal completes only when every included M0-M4 parent and child requirement has an accepted
receipt, all included phases are integrated on the approved main head, the
v0.1 end-to-end/security gates and all required dual-supervision gates pass, whole-plan consistency is clean, the
accepted remote-publication evidence exists, and no M0-M4 blocker remains.
```

Remote publication is a required completion result because the user requested the public repository push. It remains R3: if exact-scope approval is not granted at publication time, the Goal becomes `NEEDS_USER`; it is not marked complete locally.

## Why the Goal References the Plan

Duplicating the plan inside the Goal creates two authorities that can drift. The Goal therefore pins a plan path, Git SHA, semantic/file-list/manifest identity, master-source and parent/child-catalog digests, scope and Decision Log revision. Plan or requirement changes require explicit approval and a new pin.

## Pre-Goal SHA Bootstrap

The workspace currently has no Git metadata, so a valid plan SHA cannot be invented. After user approval and before Goal creation, perform the separately approved minimal control-plane bootstrap in [v0.1 Release Contract](./release-v0.1-contract.md): initialize the intended repository and create one tightly allowlisted seed commit under the sole pre-ledger exception; create the durable ledger; ratify user decisions and generate/review the expanded child catalog through separate branch/worktree/lease writers; mechanically integrate both; then record the final main SHA and independently reproduced public-safe semantic identity. The seed SHA is not the final Goal pin. A digest-only Goal pin is a fallback requiring an explicit accepted exception and immediate re-pin after Git initialization.

## Goal Warmup Gate

Before Goal creation, the Controller must report:

- Repository root, origin, visibility, default branch, current HEAD, cleanliness.
- Approved Goal scope M0-M4 and v0.1.0-alpha.1 completion line.
- Active AgentKit surfaces and live route inventory.
- Plan validation and contradiction count.
- External actions allowed and forbidden.
- Credential references by variable name only.
- First dependency-ready task and its acceptance criteria.
- Final control-plane main SHA after decision/catalog integration, semantic/file-list/manifest digests, source/parent/child-catalog digests and accepted merge method; include the seed-exception receipt separately.
- Canonical ledger path resolved from `git rev-parse --path-format=absolute --git-common-dir`; every linked worktree resolves the same file.
- Versioned ledger schema and genesis event bound to baseline SHA, plan digest and Decision Log revision.
- C0-05 pre-writer two-process/two-worktree contention receipt showing exactly one active lease for the same normalized boundary.
- The same C0-05 pre-writer read-only task-graph receipt proving `M3-T04` cannot enter `READY` before `M3-T02` is `INTEGRATED` and same-revision Advisor/Kongming Go/NATS approval exists; M3-T05 consumes only a pinned M3-T04 `frozen_interfaces` head while M3-T04 remains non-`MERGE_READY`; head movement blocks both and both reach `MERGE_READY` only after joint evidence; any ignored/deadlocked dependency is `STOP`.
- `accepted_innovation_hooks: []`, unless every non-empty hook has an accepted decision, requirement/task IDs, estimate, same-revision dual receipts and required Goal re-pin.
- Advisor and Kongming activation receipts for the same plan digest, with Controller disposition.
- Two-implementation semantic-digest reproduction receipt with identical ordered paths, per-file hashes, file-list digest and candidate digest.
- Accepted DEC-028 receipt plus a read-only Supabase boundary preflight proving non-exposed domain schemas, explicit grant/RLS semantics, documented managed-table policy ownership and observed—not requested—extension compatibility.

Verdict must be `READY`; otherwise the Goal is not created.
