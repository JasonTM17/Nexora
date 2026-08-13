---
title: "C0 Forward Rebaseline Protocol"
description: "Forward-only replacement for the structurally inapplicable C0-08 re-pin begin gate; design/replay first, ledger mutation only after a second approval."
status: in-progress
priority: P1
effort: "bounded control recovery"
tags: ["nexora", "control-ledger", "c0", "rebaseline", "security"]
created: 2026-08-13
---

# C0 Forward Rebaseline Protocol

## Overview

The legacy `goal-repin-begin` command requires the canonical root checkout to
remain clean at the old binding SHA. The legitimate source history has advanced
from `0373ecfe2fc11ae6c7799131073036aa586c4d66` to
`0c326c9b05f08c020995f10d59f087030349ba03`, so the command is structurally
inapplicable. Resetting or bypassing the gate would discard source history or
weaken the control plane.

This plan defines a new, forward-only C0 rebaseline protocol. It is limited to
reconstructing an auditable control baseline for the already-existing Git
history. It does **not** accept every source change as product-complete, erase
the prior binding, authorize R3 actions, push, provision a provider, release,
or deploy.

The protocol also records an existing historical inconsistency instead of
normalizing it away: seq 60's immutable final-binding file-list digest differs
from the reproducible semantic file list observed in both the old and target
clean checkouts. The successor must preserve the predecessor value, bind both
observed identities and state exactly why the later binding supersedes it.

User approval was recorded in this task conversation on 2026-08-13 for the
design packet, disposable replay, and dual exact-head review. A separate,
explicit approval remains required immediately before any live SQLite control
ledger mutation.

## Goals

| # | Goal | Priority |
|---|------|----------|
| 1 | Preserve the old C0 lineage while binding an independently captured, descendant source target. | P1 |
| 2 | Make every live transition replayable, fail closed, and independently reviewable. | P1 |
| 3 | Resume source delivery only after the new baseline is proven; retain all R3 STOP conditions. | P1 |

## Phases

| # | Phase | Status |
|---|-------|--------|
| 1 | [Authority and protocol specification](./phase-01-start.md) | In progress |
| 2 | [Protocol implementation and immutable capture](./phase-02-protocol-implementation-and-immutable-capture.md) | Pending |
| 3 | [Disposable clone replay](./phase-03-disposable-clone-replay.md) | Pending |
| 4 | [Dual review and gated live transition](./phase-04-dual-review-and-gated-live-transition.md) | Pending |

## Success Criteria

- [ ] The new target is a captured descendant of the old baseline and has a
      reproducible commit/path/manifest inventory.
- [ ] The protocol preserves immutable predecessor events, requires the one
      active C0-08 lease and fresh C0-05/hardening evidence, and cannot create
      a parallel active C0 lease.
- [ ] Positive and adversarial clone replays pass before a live transition is
      considered.
- [ ] Advisor FIT and Kongming PASS name the same protocol bytes, packet and
      replay receipt.
- [ ] A second user confirmation precedes any live ledger write; first push,
      paid provision, credentialed provider call, release and deploy remain
      forbidden.

<!-- slug: c0-forward-rebaseline-protocol -->
