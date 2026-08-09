---
type: kongming-red-team
date: 2026-08-09
status: exact-recheck-incomplete-usage-limit
---

# Kongming Red-Team Review — Nexora Plan

## Verdict

`HOLD BEFORE IMPLEMENTATION`.

The master plan may preserve all Prompt Phases 0-43, but a formal Goal needs an explicit finite completion line. Kongming recommends v0.1 ending after secure adaptive publishing and secure RAG; later distributed/growth/infrastructure capabilities should receive new Goals.

## Accepted Findings

- Add PASS/HOLD/STOP/DEFER semantics.
- Require exact-head milestone receipts.
- Treat tenant or RAG leakage and secret exposure as unconditional STOP.
- Add early data/tenant/RLS/storage/deletion gates.
- Add hostile ingestion, prompt injection, citation and provider-egress gates.
- Add supply-chain, artifact provenance, deployment/rollback and restore gates.
- Add budget, token, concurrency, retry and kill-switch gates.
- Make UI accessibility and complete states phase-local, not final polish only.
- Keep external/destructive R3 actions outside implicit Goal authority.
- Make keepalive state-backed and prevent replacement-writer overlap.

## Decision Resolution

- Advisor originally supported one Goal with multiple integrated milestones.
- Kongming recommended one finite v0.1 Goal, followed by separate Goals.
- The user accepted Option A on 2026-08-09: `DEC-001` now binds the first Goal to M0-M4 and retains M5-M8 for later Goals.

## Disposition

Hard gates are incorporated in `acceptance-and-evidence.md`, `workflow-configuration.md`, `risk-register.md`, and individual phase files. This historical HOLD is superseded only for DEC-001; implementation still remains blocked pending review and all remaining activation decisions.

## Review 2 — Control Plane, M3, Frontend and Release Evidence

Kongming re-reviewed the expanded candidate and returned `HOLD` with five concrete control gaps plus innovation refinements:

1. A worktree-relative SQLite path could create multiple control authorities; C0 lacked schema/genesis and real cross-worktree lease contention proof.
2. The validation log still described the superseded DEC-001 state and old counts.
3. M3 Realtime ownership blurred SQL/RLS migration authority, and the scheduler dependency needed an executable dry-run proof.
4. A blanket nonce CSP could silently force dynamic public routes, while Stitch HTML/assets lacked an explicit untrusted-input quarantine.
5. The M4 release-evidence row combined architecture, media and docs/index writers on one branch despite separate subject ownership.

Kongming also required narrower innovation evidence: deterministic decision core rather than byte-identical volatile receipts; material-claim citation coverage and disaggregated trust metrics; predeclared evaluation slices/minimum samples/cost-latency ceilings/zero leakage; server reauthorization and per-operation audit for AI patches; and live-pinned, version-matched C2PA guidance.

## Controller Disposition for Review 2

- The canonical ledger now derives from the absolute Git common directory and is shared across all worktrees. C0 adds versioned schema/genesis, unique active-boundary constraint, a two-process/two-worktree contention race and a task-graph dry-run before warmup/Goal creation. Worktree-local `.agentkit/state/**` is projection/cache only.
- M3-DB01 exclusively owns all SQL/RLS policies. M3-T03 owns only service adapter, web hooks and policy-conformance tests, with no migration paths. M3-T04 requires accepted T02 plus same-revision dual ADR and Controller disposition.
- `DEC-027` now requires a tested CSP/cache ADR by surface before frontend dispatch; Studio/auth and public cacheable pages are evidenced separately. Stitch artifacts enter an offline/no-network quarantine and gain no code/dependency/asset authority.
- R00 is split into R00A architecture, R00B media, R00C docs/index and R00I mechanical evidence integration; R01 is a separate one-writer release branch.
- The innovation backlog and Goal template now default to `accepted_innovation_hooks: []` and apply the requested metric, authorization, retention/cardinality and revision-pinning controls.
- Validation Session 2 is written only after these files and the final same-candidate recheck are measured, so historical Session 1 remains labeled as superseded rather than silently edited into present truth.

Final Kongming recheck is pending on this exact revised candidate. No `PASS` is inferred from incorporation alone.

## Review 3 — Partial Exact-Current Recheck

Kongming began an exact-current pass and confirmed that the canonical ledger, M3 SQL/RLS ownership, T04/T05 anti-cycle concept and fully qualified M3-R01 identity were substantively corrected. It then found and the Controller fixed these additional load-bearing residues:

- duplicate numbering in the pre-Goal bootstrap;
- the graph receipt did not yet prove T05 `frozen_interfaces`, head-movement blocking and joint `MERGE_READY` behavior;
- the team topology retained `feature/web-foundation` and omitted the persistence consumer, M3 root wiring and R00 evidence branches;
- the tracked media manifest attempted to contain a future/final SHA, creating self-reference;
- one sentence implied the formal Goal was already accepted instead of only accepting its future completion boundary.

The resulting release evidence identity is now `product_sha -> evidence_sha -> release_sha`: the tracked manifest pins product/capture data only, R00I-B records `evidence_sha` externally, and the external tag/GitHub Release receipt maps final `release_sha` to the evidence head plus manifest digest. The branch topology and no-active-Goal language were also reconciled.

Kongming's agent then hit its runtime usage limit before it could read the final post-normalization candidate and issue a same-revision `PASS/HOLD/STOP`. Therefore this report is a truthful partial recheck, not a `PASS`; formal dual approval remains pending even if local structural validation is clean.

## Review 4 — Final Exact-Candidate Kongming Receipt

This receipt supersedes the earlier partial current verdict while retaining all earlier findings as historical evidence.

- Verdict: `PASS_FOR_USER_DISCUSSION`.
- Algorithm: `NEXORA-SEMANTIC-DIGEST-1`.
- Semantic files: `69`.
- File-list SHA-256: `2b668f880511c079d3b8597cf236e0df519e8cf9219cfd8366686ec70f6fbdec`.
- Candidate SHA-256: `ce0767c910a4c42c4d554c6649eec48fa7556b2d8796fdf7f6b217d864c3ab0a`.
- Sanitized manifest raw SHA-256: `5871e6508e870f64b35d8312d454414efdf27cbb3e6437e69f5fbf7d671c2429`.
- Master-source SHA-256: `98716a1c79cd0f82a20888249a9d1d70482f13da10effea741bd246dde988b4a`; source lines: `5169`.
- Independent reproduction: Node.js and PowerShell matched every manifest entry and both candidate identities.

Kongming red-teamed the M1-DW01/R1 request-block-integrate-invalidate-re-dispatch protocol, Java/Go/contracts exclusions, C0 order, M3 T04/T05 anti-cycle, Supabase/RLS/tenant/privacy, secure RAG, Stitch/quarantine, media/GHCR/SBOM and continuity/recovery claim boundaries. TD-002 and TD-019 now assign M0 evidence-only candidate selection, M1-T01 exact repository toolchain pins and M3-T04 exact Go service/runtime verification without overlapping M1-DW01 dependency ownership. Residual C1/C2/C3 findings: none.

Formal Goal status is `HOLD`: no Git baseline, shared ledger/genesis, expanded child catalog, completed C0, final binding, warmup, accepted activation decisions or same-final-candidate activation receipts exist. This discussion receipt authorizes no external or product action.
