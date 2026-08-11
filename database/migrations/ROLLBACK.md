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

`V007` through `V013` are append-only once applied. Do not edit immutable CMS
history, disable forced RLS, weaken the operation-aware CMS audit insert policy,
weaken the draft-only update or publish-only workflow guards, or grant Data API
roles as a rollback shortcut. `V011` binds audit `actor_id` to the transaction
subject and maps PAGE_CREATE/PAGE_UPDATE to their respective permissions.
`V012` restricts `page.publish` transitions to the frozen lifecycle with no
payload mutation. `V013` limits `page.update` to DRAFT rows and prevents its
use from changing a published-version pointer. Any corrective change must be a
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

## M3-DB01 transactional outbox and private Realtime notes

`V014` through `V020` are append-only once applied. Do not remove the outbox table, helper
functions, safe-payload check, idempotency contract, claim/lease semantics, or
the scoped `realtime.messages` policy DDL by editing an applied migration in
place. In particular, do not reintroduce browser direct writes, fall back to a
normal session JWT, stop epoch bumps on membership mutation, or weaken the
topic/event/epoch descriptor binding as a rollback shortcut. Any corrective
change must be a reviewed forward migration after dependent-object inspection
and the applicable Supabase platform-boundary review. The M3 proof script tears
down its disposable Compose project and volumes automatically after success or
failure unless `-KeepOnFailure` is explicitly supplied.

V016's private page-resource projection is the only reviewed policy-helper
route to Presence resource identity. Do not query CMS pages through a definer
helper, disable page forced RLS, or make the projection browser-readable as an
ad-hoc repair.

V017's projection synchronizers are trigger-only hardened definer functions.
Do not replace them with a runtime `SELECT` grant or direct function execution
as a shortcut for their UPSERT reads.

`V019` is the sole runtime descriptor-epoch route. Do not replace it with a
runtime `SELECT` grant, an API-role grant, a caller-selected subject, or a
browser-callable helper. Any change to its topic and transaction-context check
must be a reviewed forward migration and preserve the private epoch-table
boundary. The function depends on the existing Spring-established runtime
transaction context; do not expose that runtime LOGIN or treat custom GUCs as
browser-provided proof of authority.

`V020` makes the 1.1.0 contract boundary explicit and introduces a private
consumer receipt ledger. Do not restore the 1.0.0 record path, reserialize a
legacy envelope as 1.1.0, delete its operator-visible dead-letter evidence, or
grant runtime/API roles direct ledger access as a rollback shortcut. A future
correction must be another reviewed forward migration and preserve both the
outbox terminal receipt and the event-ledger duplicate/replay contract.

For a shared database, use a new reviewed forward migration only after the
required dependency and rollback assessment. Preserve the outbox receipt and
terminal-state contract during any expand/contract sequence. Hosted Supabase
rollback, provider configuration, and deployment remain outside M3-DB01.
