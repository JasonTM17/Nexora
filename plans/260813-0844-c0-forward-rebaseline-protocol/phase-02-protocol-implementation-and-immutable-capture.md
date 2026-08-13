---
title: "Phase 2: Protocol implementation and immutable capture"
status: todo
---

# Phase 2: Protocol implementation and immutable capture

## Overview

Implement the replacement command as a reviewed patch against the exact live
control CLI digest, without executing it against the live SQLite database.
The patch becomes a Git-tracked review artifact; the live CLI can only be
materialized after the patch, source and live-file digests are equal.

## Requirements

- [ ] Add a separate `goal-rebaseline-*` command family rather than loosening
      legacy `goal-repin-begin` checks.
- [ ] Require the exact active C0-08 lease, current C0-05 receipt, current
      hardening/code digest, a clean dedicated integration checkout and a
      strict Git descendant target.
- [ ] Atomically append `GOAL_REBASELINE_STARTED` then `GOAL_REBASELINED`,
      revise the Goal/binding/dispatch/execution projections, preserve all
      prior events, and permit exactly one subsequent close of the old C0-08
      lease.
- [ ] Add a distinct `goal_rebaseline` artifact; do not overwrite or fabricate
      a `goal_repin` artifact/history.
- [ ] Bind a reproducible manifest, range inventory, user approval, source
      target SHA, patch SHA and old lineage event hashes into immutable event
      bodies.
- [ ] Bind the active final-binding file-list digest separately from the
      genesis-only artifact and both observed clean-checkout semantic
      identities. Completion may use the observed target value only if the
      old/target semantic identities are equivalent and equal the active final
      binding.

## Implementation Steps

1. Produce a minimal unified patch from the exact pre-patch CLI SHA and commit
   the patch plus protocol receipt schema in this isolated branch.
2. Add verifier rules for rebaseline ordering, predecessor hashes, lease
   succession/closure and artifact projections.
3. Make the command reject changed target state, non-descendants, dirty
   integration checkout, stale proof, wrong lease, duplicate lifecycle and R3
   flags before any append.

## Todo

- [ ] Code and packet each receive a small Conventional Commit after their own
      focused validation.
- [ ] No patch is copied to `D:\\Nexora\\.git\\agentkit\\ledger_cli.py` in this
      phase.

## Success Criteria

- The complete patch is exact-byte reviewable and applies only to the declared
  CLI base digest.
- A patched copy can validate a cloned ledger without modifying live state.

## Risk Assessment

The current executable control CLI is under Git common metadata rather than
the source tree. A tracked patch and exact before/after SHA pair prevent an
unreviewed local edit from becoming the effective protocol.
