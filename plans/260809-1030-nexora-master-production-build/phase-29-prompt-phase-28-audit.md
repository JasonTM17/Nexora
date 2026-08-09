---
phase: 29
title: "Prompt Phase 28 — Audit"
status: pending
priority: P1
effort: "5-8 days"
dependencies: [5, 6, 10, 12, 23, 24, 28]
---

# Prompt Phase 28 — Audit

## Outcome

Complete immutable tenant-safe audit events and an authorized admin viewer covering sensitive actions without recording secrets, prompts or raw content.

## Audit Contract

Allowlisted event type, actor, tenant, target type/ID, action, outcome, timestamp, request/trace ID and bounded safe metadata. Never store credentials, request bodies, document text, prompts, responses or sensitive headers.

## Implementation Slices

1. Consolidate early audit interface and versioned schema.
2. Append-only persistence/retention/indexes and integrity posture.
3. Instrument authz, membership, page publish/rollback, workflow, flags/experiments, provider/settings and destructive actions.
4. Tenant-safe filtered/paginated admin API.
5. Accessible viewer/export policy and redaction tests.

## Planned Ownership

Audit migration/domain, producer adapters, admin UI and security fixtures. Canonical event vocabulary has one owner; producers integrate sequentially.

## Validation

- Mutation/delete attempts according to retention policy.
- Cross-tenant enumeration/filter/export denial.
- Sensitive-value fixture never persists/logs.
- Producer failure semantics: critical business action must not silently claim audited if sink fails.
- Pagination/stable ordering/timezone.

## Commit Plan

- `feat(audit): add immutable safe-metadata event store`
- `feat(audit): cover sensitive domain actions`
- `feat(admin): add tenant audit explorer`
- `test(security): reject sensitive audit metadata`

## Acceptance

- [ ] Required sensitive actions have evidence-backed coverage.
- [ ] Viewer is tenant/RBAC safe.
- [ ] Retention/export/deletion exception policy is documented.
- [ ] Audit failure/degradation behavior is explicit.

## Stop Conditions

Raw payload/prompt/response storage, editable audit history, global viewer leakage, action succeeds while falsely claiming durable audit.
