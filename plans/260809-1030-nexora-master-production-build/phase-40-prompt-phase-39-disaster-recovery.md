---
phase: 40
title: "Prompt Phase 39 — Disaster Recovery"
status: pending
priority: P1
effort: "5-9 days"
dependencies: [37, 38, 39]
---

# Prompt Phase 39 — Disaster Recovery

## Outcome

Define and test backup, restore and service recovery for Supabase PostgreSQL/pgvector, object storage, Vercel/application deployment, messaging, configuration and infrastructure state against accepted RPO/RTO.

## Preconditions

Actual deployment/storage target, data classification, retention, encryption/key ownership, RPO/RTO and restoration authority are accepted.

## Recovery Scope

- PostgreSQL schema/data/extensions/vector indexes.
- Object/document/media storage as a state plane separate from database PITR, with versioned export/replication, object hashes and a database/object watermark.
- Supabase PITR/backup freshness, database roles/RLS and post-restore Auth/storage integration boundaries.
- Vercel known-good promotion/rollback plus backend immutable-digest rollback; database compatibility is checked separately.
- Infrastructure/GitOps/config references and required secrets recovery procedure.
- JetStream authoritative/rebuildable classification, outbox replay window, file-backed R3 snapshot/restore where non-rebuildable stream state exists, and duplicate/gap validation.
- Reindex/rebuildable caches/vectors where appropriate; source data and model/dimension version are preserved.
- Service/provider/database/object/NATS/Realtime/partial-deploy outage runbooks, each with a distinct failure-domain receipt.

## Implementation Steps

1. Inventory every state plane as authoritative, reconstructable or ephemeral; assign owner, retention, encryption/key custody, accepted data-loss window and restore dependency.
2. Configure and observe PostgreSQL PITR separately from private-object export/replication and JetStream snapshot/replay controls.
3. Capture backup watermarks, object/hash manifest, schema/migration version, outbox replay horizon, stream configuration and secret-name/config fingerprint without secret values.
4. Document the exact order: infrastructure/config/roles -> database/PITR -> role credential reset -> objects -> streams/outbox replay -> Auth/RLS/Storage/Realtime -> tenant/CMS/RAG reconciliation -> approved cutover.
5. Restore into an isolated environment without production credentials, DNS or writable integration targets.
6. Reset custom-role/runtime credentials from the approved secret authority; never assume database backup retains usable passwords.
7. Run application-level cross-tenant, publish/rollback, object ownership, authorized retrieval/citation and event duplicate/gap verification.
8. Measure PostgreSQL RPO, object RPO, JetStream replay/snapshot result, critical-service RTO and full data-plane RTO separately; record failure-domain gaps and cost.
9. Exercise provider outage, Vercel application rollback and backend immutable-digest rollback without representing them as a full data restore.
10. Seal a drill receipt with exact environment/artifact/migration identities, timestamps, operators, commands, results, limitations and follow-up owners.

## Planned Ownership

`docs/operations/disaster-recovery/**`, backup/restore scripts/config/tests and evidence. Production credentials/data remain outside repository.

## Commit Plan

- `docs(operations): define Nexora disaster recovery plan`
- `test(dr): automate isolated restore validation`
- `chore(operations): monitor backup and recovery readiness`

## Acceptance

- [ ] Backup success alone is not acceptance; isolated restore passes.
- [ ] Restored app validates tenant isolation, publications and authorized knowledge retrieval.
- [ ] PostgreSQL, Storage-object and JetStream recovery objectives/results are measured and reported separately.
- [ ] Critical-service recovery and full data-plane restore times are separate, and any SLO/error-budget impact is reported.
- [ ] Runtime/migration roles and credentials are safely recreated; RLS/ownership behavior is retested after restore.
- [ ] Object hashes/watermark reconcile to restored database metadata; stream replay has no unexplained gaps and duplicates are safely idempotent.
- [ ] RPO/RTO measurement and last drill date are recorded against the accepted target revision.
- [ ] Key-loss and provider-outage limitations are explicit.
- [ ] Vercel rollback, backend rollback and Supabase/data restore receipts remain distinct and identify configuration/migration compatibility.

## Stop Conditions

Documentation-only restore claim, production-target destructive test, database PITR presented as object backup, object manifest/watermark absent, stale PITR assumed current, JetStream/outbox recovery unclassified, app rollback confused with data rollback, role credentials assumed recoverable from backup, keys inaccessible/unplanned, restored cross-tenant inconsistency, or one aggregate RPO/RTO hiding a failed state plane.
