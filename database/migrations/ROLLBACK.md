# M1-DB01 rollback notes

`V001` is an initial baseline and must not be edited once applied. Before any
environment where it has been applied is reset, capture the exact migration
checksum and determine whether later migrations have introduced dependencies.

For a disposable local verification database, the supported rollback is to
remove the `nexora-migration-verify` Compose project and its named volumes; the
verification script does this automatically after a successful run.

For a shared or hosted database, do not drop `nexora`, `rag`, `audit`, or either
Nexora role as an ad-hoc rollback. Later migrations may own tables, policies,
or grants in those schemas. Create a new reviewed, ordered forward migration
only after confirming no dependent objects and satisfying the applicable
backup/restore and platform-change authority. Hosted rollback or provisioning
is outside M1-DB01.

## M2-DB02 and M2-DB01 ordered rollback notes

`V007` through `V012` are append-only once applied. Do not edit immutable CMS
history, disable forced RLS, weaken the operation-aware CMS audit insert policy,
weaken the publish-only workflow guard, or grant Data API roles as a rollback
shortcut. `V011` binds audit `actor_id` to the transaction subject and maps
PAGE_CREATE/PAGE_UPDATE to their respective permissions. `V012` keeps draft
edits under `page.update` and restricts `page.publish` transitions to the
frozen lifecycle with no payload mutation. Any corrective change must be a
reviewed forward migration after dependency and backup/restore assessment.

`V002`, `V003`, `V004`, and `V005` are append-only once applied. Do not edit their SQL,
drop the private schema, disable or relax forced RLS, remove the active-owner
reference, or grant Data API roles as an ad-hoc rollback.

Before a shared-environment corrective migration, record the exact checksums
and dependency order:

1. `V005` depends on the membership relation, forced RLS, role matrix, and
   mutation guard from `V003`/`V004`. It owns the private synchronized
   `membership_authorizations` projection and bounded target ID/version policy;
   rollback must be a new forward migration and must preserve the existing
   target-operation transaction contract.
2. `V004` depends on the role/status types and tenant relations from `V003` and
   the version trigger from `V002`.
3. `V003` depends on the private schema and roles from `V001`; organizations and
   memberships have mutually deferred foreign keys, so partial table removal is
   unsupported.
4. `V002` depends on the `V001` schema/role boundary and owns the shared version
   trigger used by `V003`.

For the supported local rollback, remove the disposable
`nexora-m2-db01-verify` Compose project with its volumes. The verification
script performs this teardown after success or failure unless
`-KeepOnFailure` was explicitly supplied.

For a shared database, use a new reviewed forward migration after backup/restore
authority and dependent-object inspection. Preserve membership history and the
last-owner invariant during any expand/contract sequence. Hosted Supabase
rollback, provider configuration, and deployment remain outside M2-DB01.
