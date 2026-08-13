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

- [ ] Positive replay advances the cloned ledger from the seq-84 state to one
      completed rebaseline and the exact released old C0-08 lease.
- [ ] Recomputed projections bind the new source target without claiming every
      included commit is a product acceptance.
- [ ] `verify` succeeds only after the expected final event/order/projections.

## Implementation Steps

1. Copy the DB, executable CLI and target checkout metadata to a disposable
   directory; verify original hashes before applying the reviewed patch.
2. Run the positive lifecycle and capture redacted machine-readable receipts.
3. Run one isolated negative per case: non-descendant/wrong target SHA, changed
   target after capture, dirty integration checkout, wrong or additional active
   C0 lease, stale C0-05/hardening/code digest, malformed approval/inventory,
   premature close and duplicate rebaseline.
4. Assert every negative leaves the cloned event count and active lease state
   unchanged; assert no forbidden R3 event type or external action is invoked.

## Todo

- [ ] All temporary databases/checkouts are removed or retained only as
      redacted receipts; the live DB and live executable CLI remain unchanged.
- [ ] Replay scripts use no provider credentials, paid calls, push, release or
      deployment action.

## Success Criteria

- Positive and negative replay outputs are bound to the exact patch and packet
  digests, and no negative can mutate a clone past its initial state.

## Risk Assessment

Passing a happy-path clone alone is insufficient: target substitution, stale
hardening and lease confusion are the threats this replacement exists to stop.
