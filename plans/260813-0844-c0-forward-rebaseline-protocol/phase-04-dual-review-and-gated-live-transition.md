---
title: "Phase 4: Dual review and gated live transition"
status: todo
---

# Phase 4: Dual review and gated live transition

## Overview

Obtain exact-head Advisor and Kongming dispositions, then stop for a second
user decision before materializing the reviewed CLI patch or appending to the
live ledger.

## Requirements

- [ ] Advisor FIT and Kongming PASS independently name the same packet commit,
      patch SHA, old/new source SHA and replay receipts.
- [ ] The Controller records any reviewer limitation and rejects a candidate
      with missing/fallback/identity-drift evidence.
- [ ] Live execution has a second explicit user confirmation whose scope is
      limited to materializing the reviewed patch, `harden → C0-05 → verify`,
      then one rebaseline lifecycle and exact old-lease close.

## Implementation Steps

1. Run exact-head independent reviews after the packet and replay commit is
   frozen. Do not multiply reviewer calls; record a single disclosed fallback
   only if capacity requires it.
2. Recheck live DB/CLI hashes, main/source target ancestry and dedicated
   checkout cleanliness immediately before requesting the second approval.
3. After that approval only, materialize the reviewed CLI patch, re-harden,
   run real C0-05 contention, verify, execute one rebaseline lifecycle,
   release the exact old lease and verify again.
4. Record redacted receipts. Source-range reconciliation then follows Phase 5;
   no source delivery, task acceptance, milestone, production, push, provider,
   release or deploy claim follows automatically.

## Todo

- [ ] Any mismatch after preflight is STOP; no retry with relaxed predicates.
- [ ] A live transition is not executed in the same approval that authorized
      this design/replay plan.

## Success Criteria

- The protocol is either independently accepted and awaiting its explicitly
  bounded live confirmation, or is rejected with the old C0 state intact.

## Risk Assessment

This phase is intentionally a gate, not a rollout. The only permissible live
state transition is control-plane succession; it cannot imply product,
operational or production readiness.
