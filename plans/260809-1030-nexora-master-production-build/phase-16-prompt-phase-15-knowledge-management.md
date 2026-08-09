---
phase: 16
title: "Prompt Phase 15 — Knowledge Management"
status: pending
priority: P1
effort: "6-9 days"
dependencies: [5, 6, 3, 4]
---

# Prompt Phase 15 — Knowledge Management

## Outcome

Provide tenant-safe knowledge bases, documents, storage/upload, durable jobs and honest progress UI that survives process and Realtime failure.

## Requirements

- Knowledge base/document ownership and permissions.
- Approved initial formats, size/page/batch ceilings and content-hash deduplication.
- Tenant-derived storage keys, signed upload/download, MIME sniffing and metadata validation.
- Durable job states with bounded attempts, cancellation/retry policy and restart recovery.
- Progress obtained from durable API; Realtime only improves latency.
- Deletion/retention contract covers objects and later derived artifacts.

## Planned Ownership

Knowledge/document/job migrations; platform `knowledge/**`; approved Storage policies; web `admin/knowledge/**`. Storage path and job state contracts are serialized.

## Validation

- Authorized upload/list/view/delete; cross-tenant IDs/objects/signatures denied.
- Duplicate upload and size/type limits.
- Process restart during queued/running job.
- Realtime disconnected progress via polling/refetch.
- Failed/cancelled/retry UI and accessible file upload.

## Commit Plan

- `feat(knowledge): add tenant knowledge bases and documents`
- `feat(knowledge): add secure storage upload contract`
- `feat(jobs): persist document processing state`
- `feat(admin): add knowledge management workspace`

## Acceptance

- [ ] Document and job truth survives restart.
- [ ] Service-role/storage secret never reaches browser.
- [ ] Object paths and signed URLs enforce tenant ownership.
- [ ] UI labels live versus fixture/progress states honestly.

## Stop Conditions

Public object bucket without policy, trusting filename MIME, ephemeral-only job state, unbounded upload/batch, deletion leaves retrievable object.
