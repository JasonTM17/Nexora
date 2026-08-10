# Nexora application migration train

`database/migrations/` is the single Flyway-style, ordered migration authority
for application-owned `nexora`, `rag`, and `audit` schemas.  Files are named
`VNNN__description.sql` and are applied in lexical order.  Supabase CLI
migrations, dashboard SQL, and ad-hoc console changes are not an alternative
history.

## M1 baseline

`V001__application_schema_security_baseline.sql` intentionally creates no
domain tables. M2, M3, and M4 own their respective tables, constraints, RLS
policies, and per-object grants. The baseline establishes:

- `nexora_migrator` as a non-login owner role and `nexora_runtime` as a
  separate non-login, non-owner runtime role; both are `NOBYPASSRLS`.
- Application-owned, non-public `nexora`, `rag`, and `audit` schemas.
- Explicit `PUBLIC`, `anon`, `authenticated`, and `service_role` revokes where
  those platform roles exist, plus default-privilege revokes for future
  relations.
- Runtime `USAGE` only. A future migration must first enable and force RLS for
  every tenant-scoped table, then explicitly grant only the needed table,
  sequence, or function privilege to `nexora_runtime`.

The migration neither installs extensions nor specifies an extension version.
It does not create or alter `auth`, `storage`, `realtime`, or `public` objects,
does not expose a Data API schema, and does not use `BYPASSRLS`.

## Local proof

From the repository root, run:

```powershell
pwsh -NoProfile -File database/migrations/scripts/verify-foundation.ps1
```

The script starts the existing foundation Compose definition as the isolated
`nexora-migration-verify` project on loopback-only high ports (15433, 14333,
18333 by default), creates disposable local fixtures for the three Data API
role names, applies every ordered migration through container `psql`, and runs
the privilege/ownership fixture. It then removes only that disposable Compose
project and its volumes. No provider endpoint, Supabase project, or hosted
credential is used.

For a failure that needs interactive inspection, preserve the disposable stack:

```powershell
pwsh -NoProfile -File database/migrations/scripts/verify-foundation.ps1 -KeepOnFailure
```

Use the cleanup command emitted by the script afterwards.

## M2 identity, tenant, profile, and RBAC train

M2-DB01 consumes the frozen
`packages/contracts/domain/v1/identity-tenant-permission.json` vocabulary
without modifying that contract:

1. `V002__identity_profiles.sql` creates the allowlisted, versioned profile
   lifecycle fields and subject-scoped forced RLS.
2. `V003__tenant_memberships.sql` creates organizations and the authoritative
   membership rows, exact status/role enums, composite tenant keys, forced RLS,
   and the subject-only resolution versus full tenant-context boundary.
3. `V004__tenant_rbac.sql` seeds the exact 51-row v1 permission matrix, adds
   guarded membership/organization mutations, and enforces a deferred
   composite reference to one `ACTIVE` `OWNER` per organization.
4. `V005__membership_management_target_rls.sql` adds a private, forced-RLS
   membership authorization projection and permits a manager to read or mutate
   one explicitly named target membership only. It does not add tenant member
   enumeration or any `SECURITY DEFINER`/`BYPASSRLS` path.
5. `V006__manager_membership_enumeration_rls.sql` adds a separate forced-RLS
   read policy for a current `ACTIVE` actor holding both `user.manage` and
   `role.manage`. It exposes only membership id, organization id, subject id,
   status, role, and version for the selected organization; actor-only and
   exact-target behavior remains unchanged for all other contexts.

Every M2 object is owned by `nexora_migrator`. `nexora_runtime` remains a
non-owner `NOBYPASSRLS` group role and receives explicit object/column grants
only. Policies name `nexora_runtime`; no policy targets `PUBLIC`, `anon`,
`authenticated`, or `service_role`. No M2 function uses `SECURITY DEFINER`.

Tenant resolution and domain access use separate transaction-local phases:

- A resolution transaction sets only `nexora.subject_id` and may read that
  subject's current `ACTIVE` memberships.
- A domain transaction sets `nexora.subject_id`, `nexora.organization_id`, and
  `nexora.membership_id` with `set_config(..., true)` before protected queries.
  The organization policy rechecks that exact current `ACTIVE` membership.
- Commit or rollback clears all three local settings. A pooled connection must
  start its next transaction without retained values and set a fresh context.

Direct membership reads under a resolved runtime context expose only the
actor's exact membership unless the current actor is `ACTIVE` and holds both
`user.manage` and `role.manage`; that manager-only RLS policy may enumerate
only the selected organization and only the explicitly granted list columns.
Cross-member status/role changes remain a backend authorization-operation
concern. For the bounded M2-T02
management operation, the backend must, inside the same transaction and only
after `TenantContextService.withFreshTenant` has revalidated/promoted the
actor, call `set_config(..., true)` for both `nexora.target_membership_id` and
`nexora.target_membership_version`. Immediately around the one expected-version
`UPDATE`, it must set `nexora.target_membership_mutation` to `true` and clear it
again before any subsequent query; this lets PostgreSQL validate the incremented
post-update image without turning a stale target read into a success. The target
policy requires the exact target ID/version, resolved organization equality, and
a current ACTIVE actor with both `user.manage` and `role.manage`; normal
membership reads still expose only the actor. Commit or rollback clears all six
settings. Broad cross-tenant visibility and a `SECURITY DEFINER` mutation
shortcut remain absent. The
database fixes the role matrix, guards assignment/escalation, and enforces the
last-owner reference.

The frozen database context contains subject, organization, and membership IDs
but no membership-version setting. M2-DB01 therefore proves that the referenced
membership row is currently `ACTIVE`, including denial after `REMOVED` or
`SUSPENDED`; it does not claim to detect a version-only mismatch while the same
membership remains active. The later backend freshness gate must compare the
resolved `membershipId` and `membershipVersion` against the current row before
setting the database context.

## M2-DB02 CMS and publishing train

`V007` creates tenant-scoped sites, themes, immutable page/publication history,
workflow reviews and typed SEO snapshots. `V008` adds only explicit
`nexora_runtime` forced-RLS policies; its helper is SECURITY INVOKER and
rechecks the current ACTIVE actor plus frozen permission matrix. `V011` makes
CMS audit inserts operation-aware: PAGE_CREATE/PAGE_UPDATE require their
matching permission, publication-related operations require `page.publish`,
and the audit actor must match the transaction subject. No API role is granted
schema or table access.

`V012` adds the forced-RLS `page.publish` path for server workflow transitions.
`V013` narrows the older `page.update` policy to state-stable `DRAFT` rows only
and replaces the guard function accordingly. Its SECURITY INVOKER guard permits
only the frozen C02 state graph and automatic row version/timestamp changes;
content, SEO, theme, site, slug, title, publication pointer, and draft version
remain immutable during a transition. A normal draft edit must advance the
draft version exactly once and cannot install a published-version pointer.

## M3 transactional outbox and private Realtime policy train

`V014__outbox_events_and_private_realtime_policy.sql` creates the
application-owned transactional outbox/event-ledger table, versioned event and
state types, safe payload validation, function-only record/publisher access,
bounded lease/retry transitions, and a guarded `realtime.messages` policy block
for the provider schema. The no-login migration owner is used by narrowly
granted, hardened `SECURITY DEFINER` functions so tenant producers can record an
event atomically while a publisher can claim across tenants without receiving
direct table DML or `BYPASSRLS`. The provider policy reads only the trusted
`auth.uid()` and `realtime.topic()` helpers; Data API roles retain no `USAGE` on
the application schema.

`V015__scoped_realtime_channel_authorization.sql` adds an application-owned
subject authorization epoch and replaces V014's broad channel policy with a
read-only descriptor policy. A normal browser session JWT is denied: the
provider-validated Realtime JWT must bind `sub`, the exact topic, event route,
event version and the current subject epoch. Membership changes synchronously
bump that epoch, so an old descriptor fails on a subsequent join or token
refresh. Browser code receives no `INSERT` policy on `realtime.messages`, even
for Presence; M3-T03 must validate a bounded same-origin intent and emit the
canonical ephemeral state server-side. V015 does not configure a hosted Auth
Hook, signing key or Supabase project, and it makes no hosted-provider claim.

`V016__realtime_presence_resource_projection.sql` keeps page-resource identity
in a private trigger-maintained projection so the scoped Realtime helper can
authorize Presence without bypassing or relaxing the CMS page table's forced
RLS. The projection contains only resource and organization identifiers, never
page content; it has no browser or direct runtime read path.

`V017__realtime_projection_trigger_privileges.sql` keeps both private
projections unreadable by `nexora_runtime` while allowing their trigger-only
UPSERT paths to run under a fixed-search-path, migrator-owned definer boundary.
Neither synchronizer is directly executable by application or API roles.

`V018__realtime_descriptor_event_versions.sql` pins every active v0.1 private
Realtime route to its exact descriptor event version. The policy now rejects a
numerically valid but unsupported event version; a version bump must be an
explicit reviewed contract and forward-migration change.

The M3 proof script creates a disposable local stand-in for `auth.uid()`,
`auth.jwt()`, `realtime.topic()`, and `realtime.messages`, applies `V001`
through `V017`, reruns the M2 tenant/CMS fixtures, then executes the actual
private-channel RLS expression plus the outbox fixture. The stand-in is torn
down with the Compose project; it is policy-conformance evidence, not hosted
Supabase proof.

```powershell
pwsh -NoProfile -File database/migrations/scripts/verify-m3-schema-events.ps1
```

Run the complete disposable PostgreSQL 17.5 proof from the repository root:

```powershell
pwsh -NoProfile -File database/migrations/scripts/verify-m2-schema-auth.ps1
```

The script applies `V001` through `V006`, reruns the M1 boundary checks, proves
M2 role/grant/forced-RLS behavior, two-tenant isolation, `REMOVED` membership
denial, target ID/version management scope, transaction-local reset, matrix
assignment denials, profile versioning, and the last-owner invariant, then
removes only its Compose project and volumes. It does not prove version-only
actor freshness and does not use a Supabase project, provider API, provider
credential, or Supabase CLI/MCP call.
