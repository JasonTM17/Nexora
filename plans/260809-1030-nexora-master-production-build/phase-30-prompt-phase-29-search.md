---
phase: 30
title: "Prompt Phase 29 — Search"
status: pending
priority: P2
effort: "6-9 days"
dependencies: [7, 18, 19, 20]
---

# Prompt Phase 29 — Search

## Outcome

Provide authorized hybrid global search over approved page and knowledge sources with filters, stable pagination and safe snippets.

## Requirements

- Search adapters share a versioned result contract but retain source-specific authorization.
- Tenant/permission predicates apply before candidates and counts.
- Filters, sort/cursor and result-type semantics are stable.
- Snippets/highlights are sanitized and cannot reveal unauthorized neighboring text.
- Deleted/unpublished/revoked content leaves future eligibility promptly.

## Planned Ownership

Platform `search/**`, indexes/migrations, web global search UI, shared result contract. Result/cursor contract has one owner.

## Validation

- Exact, lexical, semantic and no-match queries.
- Cross-tenant/role/unpublished/deleted content exclusion.
- Filter/pagination stability under concurrent updates.
- Malicious rich text/snippet XSS.
- Search dependency failure and honest partial/no-result UX.

## Commit Plan

- `feat(search): define authorized global search contract`
- `feat(search): combine page and knowledge retrieval`
- `feat(web): add accessible filtered search experience`
- `test(security): prevent search result leakage`

## Acceptance

- [ ] Counts/results/snippets contain no unauthorized information.
- [ ] Pagination and filters are reproducible.
- [ ] Result source and permission context are traceable.
- [ ] Performance/query-plan evidence exists for representative corpus.

## Stop Conditions

Post-result permission filtering, leaked counts/snippets, unstable offset paging at scale, unsafe highlight HTML.
