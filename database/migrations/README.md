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
