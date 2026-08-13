---
title: "Phase 3: Disposable clone replay"
status: todo
---

# Phase 3: Disposable clone replay

## Overview

Prove the complete protocol on disposable SQLite and Git/CLI copies. Replays
must verify the positive lifecycle and demonstrate that each integrity seam
fails before an append or lease mutation.

## Requirements

- [ ] Positive replay advances the cloned ledger from seq 84 to one completed
      control-lineage rebaseline and exact released old C0-08 lease.
- [ ] Recomputed projections bind the observed source target as unaccepted
      provenance without accepting a commit, task or milestone.
- [ ] `verify` succeeds only after the expected final event/order/projections.

## Implementation Steps

1. Produce the SQLite clone through a read-only source connection using the
   SQLite backup API into a new destination; verify source DB SHA-256 and WAL/
   SHM presence+hash before and after backup, and record clone DB SHA-256.
2. Copy the CLI to the disposable directory, verify reviewed base SHA-256,
   apply the tracked patch, then verify expected post-patch SHA-256 before run.
3. Capture target only after `HEAD == TARGET`, named branch, empty porcelain,
   locally present target/tree and matching canonical inventory digests.
4. Run the positive lifecycle and capture redacted machine-readable receipts.
5. Run one isolated negative per case: non-descendant/wrong target SHA, changed
   target after capture, dirty integration checkout, wrong or additional active
   C0 lease, stale C0-05/hardening/code digest, malformed approval/inventory,
   attempted genesis-file-list substitution, non-equivalent old/target
   semantic identities, patch byte mismatch, forbidden acceptance-projection
   mutation, premature close and duplicate rebaseline.
6. Assert every negative leaves the cloned event count and active lease state
   unchanged; assert no forbidden R3 event type or external action is invoked.

## Todo

- [ ] All temporary databases/checkouts are removed or retained only as
      redacted receipts; the live DB and live executable CLI remain unchanged.
- [ ] Replay scripts use no provider credentials, paid calls, push, release or
      deployment action.

## Success Criteria

- Positive and negative replay outputs are bound to the exact patch and packet
  digests, and no negative can mutate a clone past its initial state.
- Positive replay leaves final binding/manifest/Goal/execution projections
  byte-for-byte equal to pre-replay values; `product_dispatch` is also
  byte-for-byte equal. Only the declared `goal_rebaseline` artifact state and
  the final C0-08 close may change.

## Risk Assessment

Passing a happy-path clone alone is insufficient: target substitution, stale
hardening and lease confusion are the threats this replacement exists to stop.
