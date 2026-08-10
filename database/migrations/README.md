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

Direct membership reads under a resolved runtime context intentionally expose
only the actor's exact membership. Cross-member status/role changes therefore
remain a backend authorization-operation concern; broad tenant membership
visibility and a `SECURITY DEFINER` mutation shortcut are deliberately absent.
The database still fixes the role matrix, guards inserts/self-transitions, and
enforces the last-owner reference. A later accepted contract is required before
expanding the direct table mutation surface.

Run the complete disposable PostgreSQL 17.5 proof from the repository root:

```powershell
pwsh -NoProfile -File database/migrations/scripts/verify-m2-schema-auth.ps1
```

The script applies `V001` through `V004`, reruns the M1 boundary checks, proves
M2 role/grant/forced-RLS behavior, two-tenant isolation, removed/stale denial,
transaction-local reset, matrix assignment denials, profile versioning, and the
last-owner invariant, then removes only its Compose project and volumes. It
does not use a Supabase project, provider API, provider credential, or Supabase
CLI/MCP call.
