---
title: "Phase 2: Protocol implementation and immutable capture"
status: todo
---

# Phase 2: Protocol implementation and immutable capture

## Overview

Implement the replacement command as a reviewed patch against the exact live
control CLI digest, without executing it against the live SQLite database.
The patch becomes a Git-tracked review artifact; the live CLI can only be
materialized after the reviewed patch base, expected post-patch CLI bytes and
live files are all hash-equal.

## Requirements

- [ ] Add a separate `goal-rebaseline-*` command family rather than loosening
      legacy `goal-repin-begin` checks.
- [ ] Require the exact active C0-08 lease, current C0-05 receipt, current
      hardening/code digest, a clean dedicated integration checkout and a
      strict Git descendant target.
- [ ] Atomically append `GOAL_REBASELINE_STARTED` then `GOAL_REBASELINED`,
      preserve all prior events, and permit exactly one subsequent close of the
      old C0-08 lease.
- [ ] Mark both events `control_lineage_only: true`; they must not update or
      supersede `final_binding`, `final_manifest`, `goal_contract`,
      `execution_baseline` or `execution_allowances`, and must not record a
      task/milestone/range acceptance.
- [ ] Leave the existing `product_dispatch` artifact byte-for-byte unchanged.
      While the new lifecycle is pending, normal product-dispatch acquisition
      must fail because `goal_rebaseline` is pending; it resumes only when the
      same artifact is complete. No dispatch suspension or restoration event is
      permitted.
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
- [ ] Define exact event/projection contract before code:

      | Event/projection | Required immutable identity | Permitted effect |
      |---|---|---|
      | `GOAL_REBASELINE_STARTED` | protocol version; `control_lineage_only=true`; exact C0-08 tuple; seq-60/61 hashes; seq-81 hardening and seq-84 C0-05; pre/post CLI SHA-256; approval/packet hashes; old/target tree and range inventories | create the sole `goal_rebaseline=PENDING` artifact; no other artifact or projection changes |
      | `GOAL_REBASELINED` | exact start seq/hash; same C0-08 tuple; same Goal/candidate/file list; target SHA/tree/range/manifest; `control_lineage_only=true` | change only that artifact to `COMPLETE`; product dispatch remains byte-identical |
      | exact C0-08 `LEASE_RELEASED` | completed rebaseline seq/hash and same lease id/generation/base/head/owner/worktree | one close only; zero active C0 leases |

      The verifier rejects synthetic seq-60/61 replacement, `goal_repin`
      fabrication, a second lifecycle, every artifact delta other than the
      declared `goal_rebaseline` state, changed final/Goal/execution projection,
      parallel C0 lease or a completion without its matching close.

## Implementation Steps

1. Produce a minimal unified patch from the exact pre-patch CLI SHA and commit
   the patch plus protocol receipt schema in this isolated branch. Record the
   expected post-patch CLI SHA-256 before clone replay.
2. Add verifier rules for ordering, predecessor hashes, lease succession/close,
   control-only artifacts and forbidden acceptance projections.
3. Capture source inventory canonically: commit digest is SHA-256 of lowercase
   `git rev-list --reverse OLD..TARGET` IDs joined with ASCII LF plus final LF;
   changed-path digest is SHA-256 of bytewise-sorted repository-relative UTF-8
   path byte strings joined with NUL plus final NUL, derived without rename
   detection from `git -c diff.renames=false diff --no-renames --name-only -z
   --diff-filter=ACDMRT OLD..TARGET`. The procedure must not use host-locale
   collation; `LC_ALL=C` is set where supported, while the raw NUL bytes and
   bytewise ordering are authoritative on every host. Target tree is
   `git rev-parse TARGET^{tree}` plus SHA-256 of raw
   `git ls-tree -r -z --full-tree TARGET` output.
4. Reject changed target state, non-descendants, dirty integration checkout,
   stale proof, wrong lease, duplicate lifecycle, patch byte mismatch and R3
   flags before any append.

## Todo

- [ ] Code and packet each receive a small Conventional Commit after their own
      focused validation.
- [ ] No patch is copied to `D:\\Nexora\\.git\\agentkit\\ledger_cli.py` in this
      phase.

## Success Criteria

- The complete patch is exact-byte reviewable and applies only to declared
  base and expected post-patch CLI digests.
- A patched copy validates a cloned ledger without modifying live state or
  treating source provenance as source acceptance.

## Risk Assessment

The current executable control CLI is under Git common metadata rather than
the source tree. A tracked patch and exact before/after SHA pair prevent an
unreviewed local edit from becoming the effective protocol.
