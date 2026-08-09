# Nexora Thread, Branch and Worktree Runbook

## Purpose

This is the executable operating contract for running Nexora as a supervised agent team. It converts the high-level role model into precise dispatch, branch, worktree, review, integration, keepalive, recovery and audit rules.

The core invariant is:

```text
one write task
+ one active writer agent
+ one intent-based branch
+ one isolated worktree
+ one atomic writer lease
+ one frozen acceptance contract
+ one independent exact-head review
= one auditable integration candidate
```

Read-only Advisor, Kongming, scout, tester and arbiter threads do not create implementation branches. Their independence is evidence, not a violation of the one-writer-thread/one-branch rule. If a read-only finding requires a code or document change, the Project Manager opens a new bounded writer task and branch.

## Authority Layers

| Layer | Role | Decides | Writes | Cannot do |
|---|---|---|---|---|
| Product authority | User | Scope, cost, external publication, destructive work, major trade-offs | User decisions | Be assumed to have approved silence |
| Delivery authority | Controller / Team Lead | Decomposition, dispatch, risk escalation, final acceptance recommendation | Plan/control documents only when explicitly owned | Treat a worker claim as evidence |
| Decision counsel | Advisor | Frames one material decision and exposes trade-offs | Nothing | Implement, merge, or override the user |
| Adversarial counsel | Kongming | C3 threat, contradiction, architecture and claims review | Nothing | Implement or silently widen scope |
| Runtime coordination | Project Manager | Dependencies, leases, state transitions, receipt completeness | Control ledger/task projection | Change product code or resolve semantic conflicts |
| Implementation | Writer agent | Implementation choices inside a frozen contract | Its branch/worktree and owned paths only | Merge, push `main`, widen paths, accept itself |
| Verification | Tester / exact-head reviewer | Reproduce behavior and issue PASS/HOLD/STOP for an exact head | Receipt only; test repairs are a new writer task | Review a different head or repair while reviewing |
| Git mechanics | Git Manager | Mechanical execution of an already approved merge/push instruction | Git refs and approved metadata | Decide semantics, resolve conflicts by invention, publish without R3 authority |
| Release mechanics | Release Manager | Assemble evidence and execute approved publication/promotion | Assigned release/docs/workflow paths and approved external state | Publish stale or unreconciled artifacts |

The Controller, Project Manager and Git Manager are intentionally different responsibilities. In a small runtime they may be represented by the same outer system, but every action must still carry the relevant role, authority class and separate receipt. A single opaque “lead merged it” event is invalid.

## Material-Matter Dual Gate

Every important matter is supervised by both Advisor and Kongming. The gate is mandatory when any answer below is yes:

- Does it change Goal scope, product outcome, user journey or a previously accepted decision?
- Does it establish or change architecture, trust/data boundary, shared schema/contract, migration order, authentication, tenancy, permission, publishing, event or RAG behavior?
- Does it select a visual direction, global design system, accessibility policy or public product claim?
- Does it select a model/provider, expose data externally, spend money, accept a license, or change a retention/privacy policy?
- Does it merge a milestone, accept a risk exception, publish docs/media/About/Release/Package, deploy/promote/rollback, mutate external state, or claim SLO/recovery/security evidence?
- Is it C3/R3, destructive, hard to reverse, disputed, based on contradictory evidence, or the third attempt at the same failed hypothesis?

The Project Manager records `material_gate_id`, candidate SHA/digest/deployment identity, Advisor receipt, Kongming receipt and Controller disposition. Both receipts must target the same candidate. Candidate change invalidates both. Advisor assesses outcome/UX/operability/cost fit; Kongming challenges architecture/security/failure/evidence. Either may HOLD/STOP. Neither implements or merges; the user retains material scope/cost/external authority.

Routine C1/C2 work is not important merely because it is part of a large project. It may execute under the already dual-approved contract, but any boundary or risk change automatically pauses and escalates it.

## Thread Classes

| Thread class | Typical output | Branch/worktree | Writer lease | Review requirement |
|---|---|---|---|---|
| `decision` | User decision packet, ADR recommendation | None | None | Advisor/Kongming as risk requires |
| `scout` | Read-only repository/runtime evidence | None | None | Controller verifies cited evidence |
| `design-exploration` | Stitch direction or design evidence | `design/...` only if artifacts are saved | Required when writing | UI lead plus user direction approval |
| `contract` | OpenAPI/event/schema contract | `feature/...-contract` | Required | Consumer/provider contract review |
| `migration` | One ordered Flyway train | `feature/...-migration` | Exclusive global migration lease | DB/security exact-head review |
| `implementation` | One bounded feature slice | `feature/...` or `fix/...` | Required | Tester plus risk-matched reviewer |
| `test-evidence` | Reproducible exact-head receipt | None by default | None | Independent by definition |
| `debug` | Root-cause report | None by default | New `fix/...` only after diagnosis is accepted | Different reviewer for fix |
| `integration` | Mechanical merge and combined checks | `integration/...` | No product writer lease | Git Manager plus combined tester |
| `release` | Docs/media/package/deployment reconciliation | `release/...` | Required for repository writes | Release reviewer plus R3 approval |

## Durable Control Plane

The Project Manager is the single logical writer of one SQLite ledger shared by all linked worktrees. After Git exists, resolve it from every worktree with `git rev-parse --path-format=absolute --git-common-dir`, validate the common directory belongs to the intended repository, and use `<absolute-git-common-dir>\agentkit\nexora-control-ledger.sqlite`. A normal checkout therefore uses `D:\Nexora\.git\agentkit\nexora-control-ledger.sqlite`; a linked worktree must resolve the identical file. The database is local Git administrative state, untracked and never a release artifact. Worktree-local `.agentkit/state/**` contains ignored projections/cache only and has no lease authority.

Control-plane bootstrap first creates a schema-version record and genesis event bound to the seed SHA, parent semantic/source/catalog identity, user-decision receipt and an explicit `child_requirement_catalog_digest: PENDING_C0_01R`. Before the final Goal exists, that seed ledger may dispatch only the two allowlisted semantic writers C0-01D and C0-01R; no product task can become `READY`. After their reviewed heads are mechanically integrated, a hash-chained final-binding event records the final main SHA, accepted Decision Log revision and actual parent/child catalog digests. A unique active-lease constraint is enforced over the normalized ownership boundary, with monotonically increasing generations and atomic event/lease changes.

### One-time seed-bootstrap exception

Because an empty directory has no Git HEAD, Git common directory, branch base, worktree or ledger, C0-03 is the sole exception to the ordinary branch/worktree/lease rule. C0-02 is strictly read-only. After its PASS and explicit user plan approval, one Git Manager performs the indivisible C0-03 operation: initialize exact `D:\Nexora` on `main`, configure the supplied origin without pushing, write only the approved parent plan/governance/ignore/env-template allowlist, prove the project-local worktree root is ignored, and create one root seed commit directly on `main`. The receipt pins the user-approved candidate digest, staged path list, secret/provenance scan and no-remote-write result. No other writer may run, no product code may enter, and the exception ends as soon as the seed commit exists. Decision ratification, child-catalog generation and every later semantic change require their own intent branch, project-local worktree, active ledger lease and exact-head review.

Every state-changing event records:

```yaml
ledger_seq: 184
previous_event_hash: "sha256:..."
event_hash: "sha256:..."
goal_id: "nexora-v0.1-m0-m4"
plan_sha: "<approved Git SHA>"
semantic_digest_algorithm: "NEXORA-SEMANTIC-DIGEST-1"
plan_semantic_digest: "sha256:..."
plan_file_list_digest: "sha256:..."
master_source_sha256: "sha256:..."
parent_requirement_catalog_digest: "sha256:..."
child_requirement_catalog_digest: "sha256:..."
task_id: "M2-DB01"
old_state: "READY"
new_state: "IN_PROGRESS"
event_type: "DISPATCHED"
branch: "feature/m2-schema-auth"
worktree: "D:\\Nexora\\.worktrees\\Nexora-m2-schema-auth"
base_sha: "<exact integration SHA>"
head_sha: "<exact observed head or null>"
lease_id: "lease-M2-DB01-g1"
lease_generation: 1
lease_owner: "<agent/task identity>"
process_fingerprint: "<safe digest, no env or secret>"
actor_role: "project-manager"
occurred_at_utc: "<RFC3339>"
reason: "dependencies and path lock verified"
```

Database constraints must enforce one active writer lease per normalized ownership boundary and monotonically increasing lease generation. Chat, task UI, heartbeats and Markdown ledgers are projections. If projection, SQLite, Git or observed process state disagree, the task becomes `BLOCKED` and no replacement, merge or acceptance is allowed.

Pre-Goal activation creates the sole Git seed exception, then the canonical ledger/genesis. Before either semantic C0 writer dispatches, C0-05 runs both proofs against the exact C0-01 user-decision receipts. Its contention proof uses two temporary linked worktrees under ignored `D:\Nexora\.worktrees\` and two independent processes. Both must report the exact same canonical ledger path; when they request the same test boundary concurrently, exactly one lease succeeds and the other returns the specified contention result. The proof records process identities, transaction results, schema version, genesis hash and cleanup validation without recording environment values. The same C0-05 gate runs a read-only graph dry-run proving `M3-T04` remains non-`READY` until `M3-T02` is `INTEGRATED` and the Go/NATS ADR has same-revision Advisor/Kongming receipts plus Controller disposition. It must also prove M3-T05 can consume only a pinned M3-T04 `frozen_interfaces` head while M3-T04 remains non-`MERGE_READY`; head movement blocks both, and joint evidence later makes both heads `MERGE_READY`. Any failed proof, or any dispatch engine that bypasses `dispatch_after`, `accepted_main`/`integrated`/`frozen_interfaces` dependency semantics, decision or material-gate edges, is a `STOP`. Only after the combined C0-05 PASS may the ledger dispatch and integrate the decision-ratification writer, then dispatch and integrate the requirement-catalog writer. Two implementations then reproduce the final `NEXORA-SEMANTIC-DIGEST-1` identity during C0-06.

### Pre-Goal C0 packet overlay

The normal packet below describes product work under an active Goal. The only valid pre-Goal writer packets are C0-01D and C0-01R, and they additionally/exceptionally carry:

```yaml
control_program_id: nexora-pre-goal-c0
goal_id: null
goal_scope: PRE_GOAL_C0
milestone: C0
prompt_phase: null
plan_sha: "<seed or current exact main SHA>"
master_source_sha256: "<approved source digest>"
parent_requirement_catalog_digest: "<approved parent digest>"
child_requirement_catalog_digest: PENDING_C0_01R
pre_goal_constraints:
  allowed_task_ids: [C0-01D, C0-01R]
  product_dispatch_allowed: false
```

All ordinary `NEXORA-TASK-PACKET-1` writer fields still apply: exact base, intent branch, project-local worktree, allowed/forbidden paths, active lease, dependency class, acceptance, stop conditions and exact-head review. `prompt_phase: null` is accepted only together with `goal_scope: PRE_GOAL_C0`, `goal_id: null` and an allowlisted C0 task ID. C0-01D cannot write the expanded catalog. C0-01R owns only `master-requirements-catalog-expanded.md`; its reviewed output receipt supplies the actual digest. C0-06 records that digest in the final-binding event, and any later packet with a pending child digest is invalid.

## Conforming Task-Packet Example

No agent is spawned with a prose-only request. The Controller and Project Manager freeze a complete `NEXORA-TASK-PACKET-1` as defined in [Workflow Configuration](./workflow-configuration.md). The following is a conforming product-task projection for explanation; it does not rename fields or create a second validator schema:

```yaml
schema: NEXORA-TASK-PACKET-1
id: M2-T03
title: "Tenant CMS domain and API"
packet_revision: 1
ledger_seq: "<authoritative sequence>"
control_program_id: null
goal_id: nexora-v0.1-m0-m4
goal_scope: v0.1-M0-M4
pre_goal_constraints:
  allowed_task_ids: []
  product_dispatch_allowed: true
milestone: M2
prompt_phase: 6
plan_path: "D:\\Nexora\\plans\\260809-1030-nexora-master-production-build\\plan.md"
plan_sha: "<approved plan commit>"
semantic_digest_algorithm: "NEXORA-SEMANTIC-DIGEST-1"
plan_semantic_digest: "sha256:..."
plan_file_list_digest: "sha256:..."
master_source_sha256: "sha256:..."
parent_requirement_catalog_digest: "sha256:..."
child_requirement_catalog_digest: "sha256:..."
requirements: [REQ-S006-001, REQ-S053-001, UREQ-003]
outcome: "Tenant-scoped content model and authorized CRUD API"
non_goals:
  - page builder interaction UI
  - publishing workflow
  - analytics
dependencies:
  accepted_main:
    - M1-I01@<accepted-main-head>
  integrated:
    - M2-DB01@<milestone-integration-head>
    - M2-C02@<milestone-integration-head>
  frozen_interfaces: []
  dispatch_after: []
cwd: "D:\\Nexora\\.worktrees\\Nexora-m2-cms-domain-api"
base_ref: integration/v0.1-m2
base_sha: "<exact SHA>"
branch: feature/m2-cms-domain-api
worktree: "D:\\Nexora\\.worktrees\\Nexora-m2-cms-domain-api"
allowed_paths:
  - apps/platform-api/src/main/java/com/nexora/cms/**
  - apps/platform-api/src/test/java/com/nexora/cms/**
forbidden_paths:
  - database/migrations/**
  - apps/web/**
  - pom.xml
  - pnpm-lock.yaml
shared_boundaries:
  consumed:
    - packages/contracts/openapi/cms.yaml@<digest>
    - database/schema-contract.json@<digest>
  produced: []
acceptance:
  - "membership-derived tenant context; no request-supplied trust"
  - "two-tenant allow/deny integration tests"
  - "OpenAPI conformance"
checks:
  - "./mvnw -pl apps/platform-api -Dtest=<bounded suite> test"
  - "./mvnw -pl apps/platform-api verify"
commit_intent:
  - "feat(cms): add tenant scoped content domain"
  - "test(cms): prove tenant isolation"
risk_class: C3
effect_tier: R2
effect: high-impact-write
material_gate:
  required: true
  candidate_identity: "<task-contract revision/digest before dispatch>"
  advisor_receipt: "<FIT receipt for same identity>"
  kongming_receipt: "<PASS receipt for same identity>"
  controller_disposition: "<all recommendations disposed>"
resolved_route:
  role: critical-worker
  model: "<observed supported model>"
  reasoning_effort: high
  tools: ["<material tools>"]
writer_lease_id: "<issued atomically at dispatch>"
writer_lease_generation: 1
keepalive_due: "event-based plus bounded liveness interval"
stop_conditions:
  - contract digest changed
  - migration needed outside allowed paths
  - cross-tenant success observed
  - secret-shaped content observed
  - dependency head moved
external_authority: none
```

Dependency classes align exactly with the canonical state machine:

- `accepted_main` is reserved for a prior milestone/task identity already present on approved `main`, or an accepted decision with its revision. It never describes an unmerged same-milestone worker.
- `integrated` pins a same-milestone exact head already mechanically present on the named integration branch with combined checks appropriate to that incremental merge. It is provisional and not `ACCEPTED`.
- `frozen_interfaces` is a narrower exceptional edge for a downstream task that must help produce joint evidence: it pins an exact producer head/digest while that producer remains `IMPLEMENTED/VERIFYING`, never implies PASS, and blocks both tasks if the producer head moves.
- `dispatch_after` adds ordering but never overrides a state dependency or material gate.

For M3 specifically, M3-T05 may consume the frozen M3-T04 event interface/fixtures; M3-T04 cannot become `MERGE_READY` until the real M3-T05 persistence consumer and joint failure evidence pass. Both exact heads then become `MERGE_READY` and are mechanically integrated. A scheduler that treats `accepted_main`, `MERGE_READY`, `INTEGRATED` or `ACCEPTED` as synonyms is invalid.

Any missing `base_sha`, allowed/forbidden path, acceptance test, risk class, effect tier, lease or stop condition keeps the task in `BACKLOG` or `READY`; it cannot enter `IN_PROGRESS`. `DISPATCHED` is an event, not a durable task state. Every packet with `risk_class: C3` or `effect_tier: R3` must set `material_gate.required: true`; `false`/`not-applicable` is valid only for bounded C1/C2 work under an already dual-approved contract.

## Branch and Worktree Naming

Branches never use a `codex/` prefix.

| Purpose | Pattern | Example |
|---|---|---|
| Feature | `feature/<milestone>-<intent>` | `feature/m4-hybrid-retrieval` |
| Fix | `fix/<milestone>-<defect>` | `fix/m2-tenant-context-leak` |
| Tests | `test/<milestone>-<scope>` | `test/m4-rag-adversarial` |
| Design | `design/<milestone>-<direction>` | `design/m1-signal-atelier` |
| Operations | `infra/<milestone>-<intent>` | `infra/m3-local-jetstream` |
| Documentation | `docs/<milestone>-<intent>` | `docs/m4-alpha-evidence` |
| Integration | `integration/v0.1-m<milestone>` | `integration/v0.1-m4` |
| Release | `release/v0.1.0-alpha.1` | `release/v0.1.0-alpha.1` |

Worktrees use explicit paths under the ignored project-local root `D:\Nexora\.worktrees`, for example `D:\Nexora\.worktrees\Nexora-m4-hybrid-retrieval`. Before any worktree exists, the bootstrap owner adds `.worktrees/` to the canonical ignore policy and proves it with `git check-ignore`. The Manager resolves and records the absolute root/target before creation and rejects a target that is not a strict descendant. No destructive cleanup may target a computed, unresolved, root or broad directory.

## Planned Creation Sequence

These commands are examples for the future approved execution; this planning review does not run them.

```powershell
git status --short --branch
git remote -v
git worktree list --porcelain
git rev-parse integration/v0.1-m4
$nexoraWorktreeRoot = [IO.Path]::GetFullPath('D:\Nexora\.worktrees')
$nexoraWorktreeTarget = [IO.Path]::GetFullPath((Join-Path $nexoraWorktreeRoot 'Nexora-m4-hybrid-retrieval'))
if (-not $nexoraWorktreeTarget.StartsWith($nexoraWorktreeRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unsafe worktree target' }
New-Item -ItemType Directory -Force -Path $nexoraWorktreeRoot
git check-ignore --quiet -- .worktrees
if ($LASTEXITCODE -ne 0) { throw '.worktrees must be ignored before dispatch' }
git worktree add -b feature/m4-hybrid-retrieval $nexoraWorktreeTarget <exact-base-sha>
git -C $nexoraWorktreeTarget status --short --branch
git -C $nexoraWorktreeTarget rev-parse HEAD
```

The Project Manager writes the lease only after the branch, worktree, base SHA, clean state, dependencies and path lock have been observed. If worktree creation succeeds but ledger issuance fails, the branch stays quarantined and no writer is dispatched.

## Work Package Sizing

A writer task should normally satisfy all of these:

- One coherent outcome explainable in one sentence.
- One primary subsystem and bounded tests.
- One to three logical Conventional Commits when the work naturally separates; never manufacture commit count.
- Target completion within one focused agent run or a resumable series of checkpoints.
- No shared migration, root manifest, lockfile, generated contract, global tokens or infrastructure value overlap with another writer.
- Acceptance independently runnable from the task packet.

Split a task before dispatch when it owns both migration and domain implementation, backend and UI, contract and two consumers, feature and broad refactor, or code and public release state. The M0-M4 execution ledger is a wave ledger; each broad work package must materialize into these bounded child tasks before agents are spawned.

## Concurrency and Path Locks

| Risk/scope | Active writers | Additional read-only agents |
|---|---:|---:|
| Bootstrap, root config, lockfiles | 1 | Up to 2 |
| Migration train | 1 globally | DB reviewer plus tester |
| Auth/tenant/publish/secure RAG | 1 | Tester plus C3 reviewer |
| Mature, disjoint backend and UI slices | 2 maximum | One shared contract tester |
| Integration/release | 0 product writers | Git Manager, tester, reviewer |

Two writers may run only when all owned-path intersections are empty and consumed shared contracts are frozen at digests. A codeowner label is insufficient; the Manager computes normalized path-lock intersections before dispatch.

Root manifests, lockfiles, global design tokens, OpenAPI/event vocabulary, Flyway numbering, telemetry naming and deployment values are serialized “railway tracks.” A task needing one of these files stops and requests a new boundary-owner task; it does not casually edit the shared file.

### Node dependency-window protocol

The Node dependency boundary is the exact set of root `package.json`, `pnpm-workspace.yaml`, `pnpm-lock.yaml`, `.npmrc` and every workspace `package.json`. In M1 the roster enumerates those workspace manifests explicitly and M1-DW01 is their sole writer; M1-T01 owns non-Node repository governance, while M1-T03/M1-T04 exclude dependency manifests and the lockfile even when they own the surrounding directory.

Every consumer packet pins the exact integrated dependency-window head and begins/ends with frozen-lockfile plus changed-path checks. If a package change is necessary, the consumer records a value-free ledger request containing exact package/version constraint, reason, compatibility evidence and affected owner; it becomes `BLOCKED` without editing any dependency-control file. A serialized dependency owner then writes only the enumerated boundary. For M1 the single conditional packet is M1-DW01R1 on `chore/m1-node-dependency-revision`. After exact-head review, Git Manager integrates it, revokes the requesting task's old lease and receipts, and re-dispatches that task from the new exact integration head. A second M1 request is `STOP` for replan. Later milestones materialize their own stable `<milestone>-DW*` task IDs before dispatch; no generic application worker inherits lockfile authority.

## Agent Work Loop

1. Read the complete task packet, linked phase contract, repository instructions and relevant skill instructions.
2. Verify exact cwd, branch, base/head, status, lease ID/generation and canonical allowed/forbidden paths.
3. Inspect existing implementation and user changes before editing.
4. Reproduce or define the smallest behavior slice.
5. Implement only the bounded outcome.
6. Run targeted checks early; record commands, exit codes and limitations.
7. Run required scope checks, `git diff --check`, changed-path audit and secret-shaped-content scan.
8. Review its own diff for accidental generated files, fixtures presented as live data, and out-of-scope changes.
9. Create small logical Conventional Commits using explicit paths.
10. Freeze the branch, record exact head and produce a worker receipt. After `IMPLEMENTED`, the writer does not mutate the branch without a new lease generation and review cycle.

## Keepalive and Resume Contract

Keepalive is checkpointed state, not a forever loop and not proof of progress. At meaningful boundaries and before a long-running process, the writer reports:

```yaml
task_id: M4-T04
ledger_seq: 241
lease_id: lease-M4-T04-g1
lease_generation: 1
branch: feature/m4-hybrid-retrieval
worktree: "D:\\Nexora\\.worktrees\\Nexora-m4-hybrid-retrieval"
head_sha: "<observed SHA>"
status: IN_PROGRESS
completed_slice: "vector and FTS candidate fusion"
dirty_paths: []
last_check:
  command_digest: "sha256:..."
  exit_code: 0
running_process:
  session_or_pid: "<safe id or null>"
  port: "<port or null>"
  cwd: "<worktree>"
evidence: "<receipt path or safe summary>"
limitation: "live provider evaluation not run"
next_action: "add authorization predicate regression"
stop_condition: null
```

No key, token, environment value, prompt containing private data or secret-bearing command is stored. On resume, the Controller reconciles SQLite sequence/hash, agent/task state, process existence, Git branch/head/status and worktree path before continuing.

## Worker Receipt

The immutable handoff must include:

- Task/Goal/plan identity and ledger event hash.
- Exact base/head, branch, worktree and cleanliness.
- Commit list with purpose; changed paths; diff digest.
- Owned-path compliance and any generated artifacts.
- Checks executed with command, environment class, result and evidence path.
- Acceptance items proven, failed or unverified.
- Secret scan result without echoing matched values.
- Known limitations and live-service claims explicitly not proven.
- Recommended reviewer risk route.

`IMPLEMENTED` means only this receipt exists and the branch is frozen. It is not PASS, merged, accepted, released or deployed.

## Exact-Head Review

The Project Manager dispatches a reviewer with the worker receipt but requires independent observation:

1. Resolve the task branch and observed head; reject if it differs from the receipt.
2. Verify accepted base, ancestry, commit list, worktree cleanliness and changed-path allowlist.
3. Inspect the actual diff and security/trust-boundary behavior.
4. Run the acceptance checks independently where practical.
5. Separate deterministic, integration, browser, live-provider, deployment and restore evidence classes.
6. Issue one verdict for the exact head:
   - `PASS`: all required evidence for this gate is observed.
   - `HOLD`: repairable missing/failed evidence; no merge.
   - `STOP`: security, tenant, secret, destructive, artifact-identity or authority violation.
   - `DEFER`: user-approved move to a future Goal with impact recorded.
7. Record reviewer identity, model route, commands, exact head, findings and verdict.

Any commit, amend, rebase, generated-file change or conflict resolution after review invalidates the verdict. The changed head returns to `IMPLEMENTED` and must be reviewed again.

## HOLD Repair Cycle

The reviewer never edits the reviewed branch. The Controller creates a repair packet for the original writer or a replacement:

```text
REVIEW_HOLD
-> freeze exact finding and evidence
-> issue new/higher writer lease
-> repair only finding scope
-> run targeted and regression checks
-> freeze new HEAD
-> independent full/delta review as risk requires
```

Three repetitions of the same failed hypothesis trigger debugger plus Advisor/Kongming dual escalation. They do not justify broader blind edits.

## Mechanical Integration

After `REVIEW_PASS`, the Project Manager validates dependency heads and marks `MERGE_READY`. The Git Manager then:

1. Verifies integration branch exact head and cleanliness.
2. Verifies worker exact reviewed head and PASS receipt.
3. Rechecks path scope, diff digest and dependency order.
4. Merges one branch at a time using the milestone’s fixed method.
5. Performs no semantic conflict resolution. A conflict yields `HOLD` and a dedicated integration-repair task.
6. Records pre-merge integration SHA, worker SHA, resulting SHA, merge method and ancestry/patch-equivalence evidence.
7. Runs the branch-specific smoke gate before the next merge.

Cherry-pick or squash is allowed only if the plan fixes that method before review and the resulting patch identity is reconciled. Interactive history rewriting and unapproved force push are forbidden.

## Milestone Combined Gate

When all scheduled branches are integrated, no product writers remain active. The tester runs the milestone’s combined scenarios from `integration/v0.1-mN`:

- Build/lint/unit/integration contracts for all affected stacks.
- Migration-from-previous-accepted-head and clean-bootstrap paths.
- Browser journeys at desktop and 375px where UI exists.
- Keyboard, focus, loading, empty, error, denied and degraded states.
- Cross-tenant and unauthorized-context negative tests.
- Event/outbox/retry/idempotency or RAG evaluation as applicable.
- Docs/evidence/claim and artifact-identity checks.

Advisor reviews material product/UX/cost trade-offs. Kongming reviews C3 trust boundaries, architecture contradictions and release claims. Their recommendations receive a disposition; they do not mechanically merge.

For every material milestone candidate, both reviews are mandatory and target the same integration head. A missing or stale dual receipt blocks merge to `main`.

Only after the combined PASS does the Git Manager receive a bounded merge-to-`main` instruction. The Controller observes the new main head, required checks and remote equality before marking the milestone `ACCEPTED`.

## Main and Remote Semantics

- Local worker completion is not remote completion.
- Integration-branch PASS is provisional.
- `main` is the accepted product authority.
- For the v0.1 release Goal, public `origin/main`, tag, release, artifact and deployment identities must reconcile to the reviewed main SHA.
- Workers never push `main` and never publish releases/packages/deployments.
- First remote push, protection/settings mutation, PR merge-policy change, Release/GHCR publication, Vercel production promotion, paid provisioning and destructive data operations are R3 and require explicit user authority.
- If required R3 authority is withheld, the task is `NEEDS_USER`; it is not falsely completed.

## Conflict Protocol

| Conflict | Required action |
|---|---|
| Worker touches forbidden/shared path | Freeze; HOLD; restore only with user-preserving scoped repair task |
| Two active leases overlap | STOP both writers; reconcile ledger/process/worktrees; never choose by latest heartbeat |
| Integration Git conflict | Git Manager stops; Controller opens semantic conflict-resolution task from current integration head |
| Contract changed under consumer | Invalidate consumer task; rebase only through new base and re-review |
| Migration number collision | Stop; DB owner reorders one migration train; replay upgrade and clean bootstrap |
| User has unrelated dirty changes | Preserve; exclude from scope; stop if exact paths overlap |
| Reviewer and worker disagree | Evidence and files outrank claims; Advisor and Kongming independently review a material contradiction, user decides scope/trade-off |

## Timeout, Crash and Reassignment

Heartbeat expiry alone never transfers ownership.

1. Project Manager sets liveness flag `SUSPECTED_STALE` while retaining the canonical task state; this flag is an event/condition, not a task state, and no new writer starts.
2. Controller sends one bounded status/handoff request.
3. Observe agent/task status and any PID/session in the recorded worktree.
4. Interrupt the prior agent and terminate only the exact owned process when authorized and necessary.
5. Inspect branch/head/status/diff and save a handoff receipt.
6. Atomically revoke the lease with reason and observed state.
7. Issue a higher generation to a replacement only after path ownership is clean.
8. Replacement starts from observed Git state, not chat summary, and re-runs relevant checks.

Never delete a failed worktree for tidiness. Retain it until the Manager decides integrate, salvage or discard and validates exact absolute targets. Material deletion requires explicit scope and recoverability reporting.

## Three Concrete Dispatch Examples

### UI direction and foundation

```text
design/m1-signal-atelier
  owner: UI/UX direction agent + Stitch
  writes: .stitch direction artifacts only
  gate: UI lead + user selects one direction

design/m1-nexora-design-system
  owner: design-system authority writer
  consumes: selected Stitch direction + Advisor/Kongming scorecards + explicit user selection
  writes: canonical .stitch/DESIGN.md + assets/designs/selected/** + design decision record only
  gate: exact-head Advisor/Kongming review; no product code

chore/m1-node-dependency-window
  owner: serialized M1-DW01 dependency owner
  consumes: exact `INTEGRATED` repository constraints + selected design/component contract
  writes: enumerated Node package manifests + pnpm-workspace.yaml + pnpm-lock.yaml + .npmrc only
  gate: provenance/license/security + frozen install/import/SSR probe; no product source

feature/web-design-foundation
  owner: frontend foundation writer
  consumes: exact same-milestone `INTEGRATED` canonical DESIGN.md/token/component-contract and M1-DW01 heads
  writes: apps/web/** + packages/design-tokens/** + packages/ui-{core,studio,ai,builder}/**, excluding every package.json and all root dependency controls/lockfile
  gate: frozen install, zero dependency-control diff, SSR/hydration/a11y/bundle exact-head review

test/m1-ui-states (only if test code is needed)
  owner: separate test writer
  gate: desktop/375px/keyboard/visual evidence
```

Stitch concept output never lands directly as production React. AntD owns dense Studio primitives, custom/Tailwind owns branded public composition, and Ant Design X is evaluated behind owned RAG adapters.

### Ordered migration plus CMS domain

```text
feature/m2-domain-migrations  -- exclusive DB train first
feature/m2-cms-contract       -- contract can proceed only on frozen schema vocabulary
feature/m2-cms-domain-api     -- consumes exact same-milestone `INTEGRATED` migration/contract heads
feature/m2-cms-studio         -- may run beside API only after contract freeze and disjoint paths
integration/v0.1-m2           -- one reviewed branch merged at a time
```

No Java worker creates a Flyway file. No UI worker changes the OpenAPI contract. Required changes become separate owner tasks.

### Outbox, NATS and Go ingestion

```text
feature/event-contracts
  -> feature/m3-schema-events
  -> feature/transactional-outbox
  -> feature/go-event-ingestion
  -> feature/event-persistence-consumer
  -> chore/m3-runtime-wiring
```

The durable database outbox precedes Go/NATS consumers. Go does not become transaction authority; JetStream receipt and idempotent processing are validated before M3 acceptance.

## Milestone Staffing Waves

| Milestone | Serial railway tracks | Safe parallel pair examples | Mandatory supervision |
|---|---|---|---|
| M0 | Decisions, secret/license gate, initial plan commit | Read-only scouts only | Advisor and Kongming dual review for every material choice and activation candidate |
| M1 | Non-Node root foundation, M1-DW01 Node dependency window, DB baseline | Java foundation + selected UI direction only after their consumed boundaries freeze | UI lead, dependency reviewer, architecture reviewer, combined build tester |
| M2 | Migration train, shared contracts, publishing state machine | Backend slice + disjoint Studio slice | Advisor journey checks; Kongming tenant/publish C3 gates |
| M3 | Event contract, outbox migration, root wiring | Realtime UI + isolated ingestion after contracts | Advisor architecture/operability fit plus Kongming durability/ordering dual review |
| M4 | Knowledge migration, retrieval contract, RAG policy | Parser pipeline + disjoint RAG UI after contract freeze | Advisor RAG UX; Kongming unauthorized-context/security gate |
| Release | Main/tag/artifact identity, migration/rollback notes | Docs capture and package assembly only from frozen candidate | Advisor claims; Kongming release/supply-chain review |

## Manager Dashboard Projection

The human-readable dashboard must show at minimum:

| Field | Why |
|---|---|
| Goal/plan SHA/digest | Prevents executing stale plan text |
| Task/state/risk | Shows outcome and gate level |
| Dependency heads | Prevents consumer drift |
| Branch/worktree/base/head | Establishes exact code identity |
| Owned/shared/forbidden paths | Prevents collisions |
| Lease ID/generation/owner | Prevents concurrent stale writers |
| Agent/model/effort | Records actual route, not assumed config |
| Last ledger seq/hash | Reconciles projections |
| Check/evidence class | Prevents mock evidence from proving live behavior |
| Review verdict/head | Prevents stale PASS reuse |
| Merge/main/remote SHA | Separates implementation, integration and acceptance |
| Blocker/next action/authority | Makes handoff and user decisions explicit |

## Definition of Managed-Team Completion

A task is complete only as `ACCEPTED`, with:

- Frozen task contract and fulfilled dependencies.
- One writer lease and no unresolved ownership collision.
- Small, coherent commits on the intended branch.
- Independent exact-head PASS at the required risk level.
- Mechanical merge receipt and combined milestone PASS.
- Accepted `main` SHA and, where required, matching remote/artifact/deployment evidence.
- Ledger, Git, process state and evidence index reconciled.
- Limitations and deferred work recorded truthfully.

Anything less is progress, not completion.
