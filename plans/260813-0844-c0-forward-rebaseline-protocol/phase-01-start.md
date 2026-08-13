---
title: "Phase 1: Start"
status: in-progress
---

# Phase 1: Authority and protocol specification

## Overview

Freeze the replacement protocol's authority, terminology and preconditions
without changing the live ledger, the active C0-08 lease, the root checkout or
any product source. The protocol must treat the current source target as
unaccepted until the explicit rebaseline event succeeds.

## Requirements

- [ ] Bind the predecessor lineage: final binding event seq 60, Goal creation
      seq 61, active C0-08 lease `C0-08-g1-1dcf780dd4`, rehardening seq 81 and
      C0-05 pass seq 84.
- [ ] Capture the target source identity, ancestry proof from old to new SHA,
      ordered commit list, changed-path list, and deterministic per-file
      manifest from a dedicated clean target checkout.
- [ ] Define `GOAL_REBASELINE_STARTED` and `GOAL_REBASELINED` as distinct
      forward-only events; never reinterpret them as the existing
      `GOAL_REPIN_*` lifecycle.
- [ ] Require a user-approval receipt digest, target inventory digest and
      code-patch digest before the start event. The approval must state that
      the range is a control-baseline transition, not blanket product
      acceptance.
- [ ] Retain `first_push`, `paid_provision`, `credentialed_provider_call`,
      `release`, and `deploy` as hard prohibitions.

## Implementation Steps

1. Recompute and record predecessor/target Git identity in a redacted packet.
2. Specify the immutable event bodies, artifact projection changes and exact
   close rule for the existing C0-08 lease.
3. Specify all fail-closed preconditions and negative replay cases.
4. Obtain advisory review of this specification before writing a runtime patch.

## Todo

- [ ] No live ledger event, active-lease mutation, source integration, push or
      provider action occurs in this phase.
- [ ] The source target is named but not claimed as accepted implementation.

## Success Criteria

- The packet identifies one unambiguous successor lifecycle and a clean target
  checkout, with no reset/bypass/parallel C0 alternative.
- Reviewers can determine from the packet alone exactly what must be replayed
  and what remains forbidden.

## Risk Assessment

The protocol changes control semantics, so a natural-language approval cannot
replace byte-level review and clone replay. The live root is intentionally not
used as a cleanliness predicate because it contains preserved unrelated local
configuration; the dedicated target checkout becomes the observed source
identity instead.

## Captured Pre-Implementation Evidence

Captured read-only on 2026-08-13. These values are protocol inputs only and
must be recomputed immediately before clone replay and again before any
separately approved live action.

| Item | Captured value |
|---|---|
| Old control main | `0373ecfe2fc11ae6c7799131073036aa586c4d66` |
| Proposed source target | `0c326c9b05f08c020995f10d59f087030349ba03` |
| Ancestry | old is an ancestor of target |
| Ordered commits in range | 127; SHA-256 `6dd3c6cbfd005bbd1f034cb5b9c526b1b2d500645fa86d808e71d5a37e89d983` |
| Sorted changed paths | 224; SHA-256 `6eafcb57851bdcc823c7658fbfffd26cb625965920b20c664a8b0edc10ceb766` |
| First / final range commit | `4848e4c99a8cbe68ce54ae8dea93314a41a810e7` / `0c326c9b05f08c020995f10d59f087030349ba03` |
| Dedicated target checkout | `C:\\Users\\Admin\\.codex\\worktrees\\m3t01eventcontracts\\Nexora` on `feature/event-contracts`, observed clean at target |
| Live executable CLI SHA-256 | `b4840e59379374f800d429c36d70359f1d210891f855ac826ac16125f7fe923f` |
| Live ledger SHA-256 | `10f2211828666ca64bdd7568e8adcfe831b09fd11d57e9d885d7be4a0bf377a7` |
| Verified ledger tip | seq 84, `C0_05_VERIFIED_CONTENTION_PASS`, hash `13c7ab18d896ce8c4213718aff6c91ee75d8b1e29aed6c1039d1618c80c868a3` |
| Active lease | `C0-08-g1-1dcf780dd4`, generation 1, `control/goal-repin`, `chore/goal-repin-control`, old base/head |

The active final binding remains candidate digest
`91c16ea317b856060ed34eb7464e72ac8e496620c6aa0679ec9fc9dfe3a31246` at
event seq 60. This plan does not alter that fact; the replacement protocol
must make any successor relationship explicit and hash-bound.
