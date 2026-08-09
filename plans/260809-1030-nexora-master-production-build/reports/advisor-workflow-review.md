---
type: advisor
date: 2026-08-09
status: fit-for-user-discussion-goal-hold
---

# Advisor Review — Nexora Workflow

## Summary

Advisor recommends one durable outcome authority, a traceable phased plan, integration milestones, bounded tasks, explicit role separation and user approval at material/R3 boundaries.

## Incorporated Findings

- Controller owns outcome, decomposition, user communication and final claims.
- Project Manager owns plan/task ledger; Git Manager performs accepted mechanical Git actions.
- Advisor interviews one decision at a time; Kongming gives one-shot counsel.
- Tester and reviewer are independent and read-only by default.
- Stitch output is design input; production UI is hand-built Next.js/React.
- Static AgentKit model definitions are not live route proof.
- Task states distinguish worker finish from merged/verified DONE.
- Timeout/retry, keepalive/resume and stale-owner replacement are bounded.
- External writes, spending, credentials, release, deployment and destruction require explicit authority.

## Important Current Gaps

- Workspace is not a Git repository, so worktrees/branches/merges are unavailable.
- Root AgentKit preferences are not fully active.
- Advisor/UI model routes are not explicitly pinned in current outer config.
- Direct named AgentKit runtime and Stitch surface are not live-verified.

## Disposition

All applicable findings were incorporated into `workflow-configuration.md`. Material user choices remain in `decision-log.md`.

## Unresolved Questions

- See open `DEC-*` entries; Advisor should ask one at a time after plan review.

## Review 2 — Detailed Team/UI/Continuity Candidate

Advisor issued `HOLD` with four non-P0 findings on the then-current candidate:

1. `DEC-A10` dual supervision was not fully represented in control-plane and Go/NATS dispatch rows.
2. The new thread runbook used dispatch/liveness labels as if they were canonical task states.
3. One team-flow line incorrectly gave `MERGE_READY` to Controller rather than Project Manager.
4. Parallel Stitch direction writers had broad intersecting output roots and inconsistent branch names.

The Controller applied bounded dispositions:

- C0 and Go/NATS now require same-candidate Advisor/Kongming receipts and Controller disposition; every C3/R3 task packet carries `material_gate.required: true`.
- Canonical task states remain those in `workflow-configuration.md`; `DISPATCHED` and `SUSPECTED_STALE` are event/flag values only.
- Project Manager alone marks `MERGE_READY`; Controller disposes material gates and confirms `ACCEPTED` after combined-main evidence.
- Stitch directions now use `design/m1-*` branches and disjoint per-direction directories; only the design-system owner writes canonical `.stitch/DESIGN.md`.

The required Advisor exact-candidate recheck and innovation review could not complete because the subagent runtime reported a revoked refresh token. No `FIT` was invented. The plan and [innovation backlog](../innovation-and-differentiation-backlog.md) remain pending Advisor re-authentication and same-revision review.

## Review 3 — Exact Execution Identity and Canonical States

Advisor access later recovered and the same-revision read-only review resumed. Advisor issued `HOLD` findings as the candidate changed, each against an identified current snapshot:

1. The runbook example incorrectly combined the design-authority branch with frontend product-code paths; it now mirrors M1-D02 design-only followed by M1-T03 `feature/web-design-foundation`.
2. M3 integration used an ambiguous `R01` shorthand and then exposed a T04/T05 acceptance cycle. IDs are fully qualified; T04 freezes an exact interface head, T05 supplies the real consumer, head movement blocks both, and paired evidence makes both `MERGE_READY` before integration.
3. Worker heads were incorrectly called `ACCEPTED` before reaching main. The canonical dependency schema is now `accepted_main`, `integrated`, `frozen_interfaces`, `dispatch_after`; `MERGE_READY`, `INTEGRATED` and `ACCEPTED` are not interchangeable.
4. The R00 evidence train used the same premature state language. R00A/B now become `MERGE_READY`; R00I-A integrates them; R00C branches from that exact evidence head; R00I-B integrates C and alone may reach accepted main before R01.
5. The team task example allowed one identity worker to write migrations and domain code, while the workflow task schema used an overloaded dependency list. The example now consumes M2-C01/M2-DB01 `INTEGRATED` heads, forbids migrations, and both canonical packet definitions share the state classes.
6. Residual no-active-Goal wording was corrected to future/activated Goal language.

The Controller then normalized every dependency cell in the M0-M4 execution ledger to an explicit canonical state or identified read-only receipt. Advisor is withholding the final same-revision `FIT/HOLD/STOP` receipt until Validation Session 2 records the measured current package, so validation cannot invalidate a just-issued receipt. No final `FIT` is claimed in advance.

## Final Exact-Current Advisor Receipt

- Verdict: `FIT` for user discussion.
- Candidate identity: semantic plan bundle SHA-256 `3ee8d0da9bf9e8033861006f022910e0ebcb890824efcc1766cf19ea528ca4b9` across the 63 root plan Markdown files, excluding append-only validation/review evidence.
- Residual material Advisor findings: none.
- Authority limit: this receipt does not authorize Goal creation, Git mutation, provider calls, external publication or deployment.

Advisor's innovation disposition is `GO_FOUNDATION` for INN-01 through INN-04 only through the explicit hook-admission rule, `LATER_EXPERIMENT` for INN-05 through INN-07 and `REJECT_NOW` for INN-08. Advisor recommends no ninth feature; the coherent differentiation is the combined trust spine of decision receipt, lineage, layered answer receipt and versioned quality canary.

Formal Goal activation remains `HOLD`: no Git SHA/control-ledger warmup exists, 3 numeric decisions are `OPEN`, 17 are `PROPOSED`, and Kongming still lacks a final same-revision verdict.

## Review 4 — Final Exact-Candidate Advisor Receipt

This receipt supersedes every earlier Advisor disposition for current-discussion purposes while preserving them as history.

- Verdict: `FIT_FOR_USER_DISCUSSION`.
- Algorithm: `NEXORA-SEMANTIC-DIGEST-1`.
- Semantic files: `69`.
- File-list SHA-256: `2b668f880511c079d3b8597cf236e0df519e8cf9219cfd8366686ec70f6fbdec`.
- Candidate SHA-256: `ce0767c910a4c42c4d554c6649eec48fa7556b2d8796fdf7f6b217d864c3ab0a`.
- Sanitized manifest raw SHA-256: `5871e6508e870f64b35d8312d454414efdf27cbb3e6437e69f5fbf7d671c2429`.
- Master-source SHA-256: `98716a1c79cd0f82a20888249a9d1d70482f13da10effea741bd246dde988b4a`; source lines: `5169`.
- Independent reproduction: Node.js and PowerShell matched the ordered file list, per-file normalized bytes/hashes, file-list digest and candidate digest.

Advisor verified that C0-05 precedes both semantic writers; canonical-authority conflicts fail closed; REQ-S002 is mixed v0.1/future; UREQ-013 prevents silent innovation scope expansion; the Stitch budget remains 36 operations; and M1-D01A/B/C, M3 anti-cycle, branch/worktree/lease and Goal boundaries remain coherent. The former Node lockfile collision is closed: M1-DW01 exclusively owns the enumerated Node dependency controls, T01/T03/T04 exclude them, and conditional M1-DW01R1 invalidates/re-dispatches a blocked requester from the newly integrated exact head. TD-002/TD-019 now distinguish M0 candidate recording from M1-T01 artifact pinning and M3-T04 Go verification. Residual C1/C2/C3 findings: none.

Authority limit: this receipt authorizes discussion only. Formal Goal creation, Git mutation, provider calls, spending, publication, provisioning, release and deployment remain unauthorized. Formal Goal status is `HOLD` pending user decisions and the actual C0/Git/catalog/ledger/warmup/activation receipts.
