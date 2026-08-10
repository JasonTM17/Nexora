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
