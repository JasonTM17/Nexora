---
title: "Phase 5: Historical source-range reconciliation"
status: todo
---

# Phase 5: Historical source-range reconciliation

## Overview

Treat `0373..0c326` as an historical, unaccepted implementation range after
control provenance is repaired. It is a separate delivery program, not a
rebaseline side effect: it maps all 127 commits/224 paths to requirements,
exact heads, local evidence and unresolved risks before acceptance.

## Requirements

- [ ] Produce range inventory grouped by requirement/task/owned paths and
      distinguish integrated source from local-only or superseded branch work.
- [ ] Re-run exact-head Advisor FIT + Kongming PASS for each material packet;
      old chat summary or local green test is not a substitute.
- [ ] Reconcile 91 `apps/`, 43 `packages/`, 29 `database/`, one `services/`
      and remaining paths against the M0-M4 execution ledger and evidence
      contract.
- [ ] Keep security, hosted-provider, browser, performance, release and
      production limitations unmet until directly evidenced by their owner.

## Implementation Steps

1. Build an auditable dependency/ownership map from canonical plan/catalog to
   every commit/path in the captured range.
2. Independently verify source SHA, tests, database/tenant evidence, security
   findings and limitations per task; repair through small focused branches.
3. Mechanically integrate only accepted exact heads under restored C0 authority
   and record the correct milestone evidence.

## Todo

- [ ] Do not mark a commit, task, milestone or Goal accepted merely because it
      is included in the rebaseline range.
- [ ] Do not push, provision, call a credentialed provider, release or deploy
      without separate user authority.

## Success Criteria

- Every included M0-M4 requirement is accepted with current scoped evidence or
  explicitly STOP/HOLD with owner and next evidence gate.
- Eventual integration baseline advances only by canonical integration tasks,
  never by rebaseline.
