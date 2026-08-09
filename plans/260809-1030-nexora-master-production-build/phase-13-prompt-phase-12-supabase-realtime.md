---
phase: 13
title: "Prompt Phase 12 — Supabase Realtime"
status: pending
priority: P2
effort: "4-7 days"
dependencies: [5, 10, 12]
---

# Prompt Phase 12 — Supabase Realtime

## Outcome

Add authorized private Realtime channels for publication invalidation, job progress, notifications and builder presence without making Realtime authoritative.

## Channel Contract

- Topics derive from server-authorized tenant/resource identity.
- Clients cannot subscribe by guessing organization/resource IDs.
- Payloads are minimal, versioned and contain no secrets/content bodies unless explicitly required and safe.
- On disconnect/missed event, clients refetch durable state.
- Presence is ephemeral and never required for data correctness.

## Implementation Slices

1. Channel/event vocabulary and authorization policy contract.
2. Provider-documented RLS policies on `realtime.messages` land only through the ordered M3 migration owner before adapter dispatch; no custom object, function, index or destructive operation enters a managed schema.
3. Server publisher/adapter with safe payloads; no SQL or migration edits.
4. Web subscription lifecycle, reconnect and refetch.
5. Publication and job-progress integration.
6. Notification/presence scaffolding completed by their owning phases.
7. Authorization, policy-conformance and failure-mode tests.

## Planned Ownership

The M3 migration train exclusively owns the provider-supported `realtime.messages` policy DDL under `database/migrations/**`; this is policy authority, not ownership of the managed schema. Policy helper functions live in an application-owned schema with hardened `search_path`, fully qualified names and minimum grants. The Realtime delivery branch owns the platform service adapter plus web Realtime client/hooks and policy-conformance fixtures only; it must not create or edit SQL/migrations. `packages/contracts/realtime/**` has one frozen channel-vocabulary owner before either implementation slice.

## Validation

- Same-tenant authorized subscribe succeeds; cross-tenant/anonymous guesses fail.
- Adapter and browser tests run against the exact same-milestone `INTEGRATED` migration-policy head; no SQL/migration path appears in the delivery-branch diff. Only the later milestone merge on approved `main` becomes `ACCEPTED`.
- Token expiry/membership removal and reconnect behavior.
- Duplicate/out-of-order/missed event behavior.
- Realtime outage does not lose publish/job/notification state.
- No unbounded reconnect storm.
- Migration inspection finds no custom table/function/index or migration-history write in `auth`, `storage` or `realtime`; hosted evidence, when authorized, records the target project/config and current platform behavior separately from local emulation.

## Commit Plan

- `feat(realtime): define private channel contracts`
- `feat(realtime): broadcast durable state changes`
- `test(security): verify Realtime tenant authorization`

## Acceptance

- [ ] All channels are private and policy-tested.
- [ ] Durable refetch proves correctness after missed events.
- [ ] Payload/log redaction and rate controls exist.
- [ ] Browser reconnect UX is explicit.

## Stop Conditions

Public tenant topics, business truth stored only in Realtime, sensitive payload broadcast, infinite reconnect/retry, implicit Data API exposure, or unsupported custom/destructive DDL in a managed Supabase schema.
