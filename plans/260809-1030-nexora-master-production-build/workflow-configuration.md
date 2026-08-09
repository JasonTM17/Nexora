# Nexora Multi-Agent Workflow Configuration

## Review Status

- Document state: `USER-APPROVED FOR C0 CONTROL-PLANE; FORMAL GOAL AND PRODUCT DISPATCH REMAIN BLOCKED`
- Explicitly named C0/pre-Goal bootstrap, packet-overlay, ledger and warmup sections apply before Goal activation; ordinary M0-M8 product dispatch and runtime states apply only after the formal Goal is activated.
- No configuration in this document is proof of live runtime availability.
- Every run must discover and record the actual runtime, agent, model, tools, permissions, isolation, and cancellation behavior.

## Verified Baseline

| Item | Observed state | Consequence |
|---|---|---|
| Workspace | Local `main` seed exists in `D:\Nexora`; C0 writers use isolated worktrees and leases | No product dispatch until C0-06/07 final binding and formal Goal record |
| Remote | `origin` is configured as `https://github.com/JasonTM17/Nexora.git`; no ref has been pushed | First push remains a separate public-content and secret-gated R3 action |
| Root contents | `.agentkit/`, `engineer/`, env template/ignore, this plan | `engineer/` remains local tooling boundary by default |
| AgentKit CLI | `ak 2.7.0`; preferences validation succeeds | Plan CLI is usable; runtime surfaces still need live discovery |
| Root AgentKit prefs | Mostly commented template | Privacy/artifact/project rules must be explicitly proposed and validated |
| Agent definitions | 16 definitions under `engineer/.codex` | On-disk files do not prove dispatch availability |
| Direct named runtime | No verified `agent_<slug>` runtime surface in this task | Use only live collaboration capabilities until activation is proven |
| Stitch | Skill/source present; no verified active Stitch MCP in this task | Preflight key/SDK/quota/tooling; no generated-code claim |
| Collaboration | Controller plus at most three active subagents in current runtime | Proposed concurrency must be clamped to live capacity |

## Operating Model

```text
User
  ↓ approves Outcome Contract, decisions, plan, R3 actions
Controller / Delivery Manager
  ├─ Advisor: one-question product/decision interview
  ├─ Kongming: one-shot C3 counsel and red-team checkpoints
  ├─ Scout / Researcher: read-only evidence
  ├─ Planner / Project Manager: plan and ledger only
  ├─ UI/UX + Stitch: design artifacts and approved UI ownership
  ├─ Implementation Workers: one bounded worktree each
  ├─ Tester: independent verification, read-only by default
  ├─ Code/Security Reviewer: exact-head, read-only verdict
  └─ Git Manager: mechanical explicit-path Git operations after acceptance
```

The Controller owns decomposition, dependency ordering, authority checks, user communication, final synthesis, integration decision, and truthful completion claims. It does not bypass independent testing/review.

## Role and Authority Matrix

| Role | Effect | Owns | May write | Forbidden |
|---|---|---|---|---|
| User | Decision authority | Outcome, scope, cost, release, destructive/external approval | Accepted decisions | Secret sharing in plan/commit; implicit approval assumptions |
| Controller | Coordination/C3 | Goal contract, delegation, gates, user reporting | Plan/ledger; exceptional bounded integration files only | Broad implementation while reviewing itself; silent scope change |
| Project Manager | Plan/report | Task graph, dependencies, receipts, resume packet | `plans/**`, approved project-management docs | Product code, Git mutation, semantic conflict resolution |
| Advisor | R0 observe | One material interview question and reframing | None | Implementation, merge, fake native relay claims |
| Kongming | R0 C3 observe | Hard-fork counsel, go/no-go, next risk | None | Interviewing, implementing, overriding user decision |
| Scout/Researcher | R0 observe | Current source/config/test evidence | Reports only when assigned | Source edits, commit, unsupported inference |
| Planner | R1 plan write | Traceable plan and ownership map | `plans/<active-plan>/**` | Product code, weakened outcome |
| UI/UX Designer | R1/R2 design write | IA, tokens, screens, Stitch handoff | `assets/designs/**`, `DESIGN.md`, explicitly owned UI paths | Treat Stitch HTML as production, domain/API changes |
| Worker | R1/R2 scoped write | One task and exclusive paths | Assigned worktree paths | Merge, main push, scope widening, shared-file overlap |
| Tester | R0 by default | Test execution and evidence | None; missing tests become separate writer task | Product repairs, acceptance based on worker report |
| Reviewer/Arbiter | R0 C3 | Exact-head contract/security/quality verdict | Review report only | Implement fixes, rubber-stamp, review another HEAD |
| Debugger | R0/R1 | Evidence-backed root cause after failure | Diagnostic report; fixes only through new owned task | Repeating same hypothesis unchanged |
| Git Manager | R1/R3 mechanical | Exact-path staging, accepted commits, approved merge/push | Git metadata and explicitly accepted paths | Decide scope, resolve semantic conflicts, force push without approval |
| Release Manager | R1/R3 external | GitHub About/Release/Packages, Vercel promotion and release receipt | Approved release metadata/workflows/external state | Invent evidence, publish stale head, deploy mutable artifact, bypass user authority |

## Capability and Risk Tiers

| Tier | Meaning | Nexora use |
|---|---|---|
| C1 | Search, extraction, bounded mechanical work | Scout, docs inventory, Git mechanics |
| C2 | Multi-file implementation and test judgment | Normal bounded worker and tester |
| C3 | Architecture, security, review, arbitration | Controller, Advisor, Kongming, planner, reviewer |
| R0 | Observe/report | Advice, scouting, read-only review |
| R1 | Reversible scoped write | One owned component/package/doc task |
| R2 | Isolated high-impact/parallel write | Auth, tenancy, publishing, RAG, migrations, infrastructure |
| R3 | External/destructive | Push/release/deploy/provision/delete/credentialed actions |

R3 always requires fresh exact-scope approval even while the Goal is active.

## Model Routing

### Current Static Definitions

| Agents | Current outer configuration |
|---|---|
| `kongming` | `gpt-5.6-sol`, high reasoning |
| `brainstormer`, `planner`, `code-reviewer`, `code-simplifier` | `gpt-5.5` |
| `debugger`, `docs-manager`, `fullstack-developer`, `journal-writer`, `project-manager`, `researcher` | `gpt-5.4` |
| `explore`, `tester`, `git-manager` | `gpt-5.4-mini` |
| `advisor`, `ui-ux-designer` | No explicit outer model pin |

These values are inventory evidence, not the final Nexora routing policy.

### Proposed Routing Policy

| Work class | Preferred live route | Reasoning | Floor |
|---|---|---|---|
| Controller/governance | `gpt-5.6-sol` | high | C3/R0-R3 coordination |
| Advisor | `gpt-5.6-sol` | high | C3/R0 |
| Kongming | `gpt-5.6-sol` | high; xhigh/max only for hardest fork if live-supported | C3/R0 |
| Architecture/planning | `gpt-5.6-sol` | high | C3/R0-R1 |
| Security/exact-head review | `gpt-5.6-sol` or independently configured qualified C3 | high | C3/R0 |
| Auth/tenancy/publishing/RAG worker | strongest qualified route, normally `gpt-5.6-sol` | high | C3/R2 |
| Normal bounded worker | `gpt-5.6-terra` | medium/high | C2/R1-R2 |
| UI/UX direction | qualified `gpt-5.6-sol` or `terra` with high reasoning plus Stitch | high | C2/C3 |
| Project management | `gpt-5.6-terra` | high | C2/R1 |
| Debugger | `gpt-5.6-terra` or escalation to sol | high | C2/C3 |
| Scout/test/Git mechanics | `gpt-5.6-terra` | low/medium | C1/C2 |

Execution rule: do not write a model into an internal job when its live agent definition owns the model. Record the resolved route in the run receipt. If no route meets capability and risk floors, mark `BLOCKED`; never silently downgrade.

## Concurrency Policy

- Runtime maximum is discovered per run.
- Current session supports four total slots, so proposed maximum is Controller plus three subagents.
- Early foundation/auth/contracts: maximum one writer plus up to two read-only test/review agents.
- Mature disjoint scopes: maximum two writers plus one tester/reviewer.
- Never use three concurrent writers on root config, lockfiles, generated contracts, migrations, tenant/auth middleware, page schema, event vocabulary, telemetry naming, or infrastructure values.
- Same-stage jobs run concurrently only when dependencies are satisfied, paths are disjoint, and each writer has an isolated worktree.

## Task Assignment Contract

`NEXORA-TASK-PACKET-1` below is the single canonical machine-consumed schema. Runbook and team-model examples are conforming projections and never define alternate field names.

```yaml
schema: NEXORA-TASK-PACKET-1
id: <stable-task-id>
title: <short task title>
packet_revision: <positive integer>
ledger_seq: <authoritative control-ledger sequence>
control_program_id: <nexora-pre-goal-c0 or null after Goal activation>
goal_id: <active finite Goal ID; null only for PRE_GOAL_C0>
goal_scope: <PRE_GOAL_C0, v0.1-M0-M4 or later Goal>
pre_goal_constraints:
  allowed_task_ids: <[C0-01D, C0-01R] only for PRE_GOAL_C0; [] after Goal>
  product_dispatch_allowed: <false for PRE_GOAL_C0; true after Goal>
plan_path: <absolute local plan path; never copied to public evidence>
plan_sha: <exact seed/current-main/approved plan commit>
semantic_digest_algorithm: NEXORA-SEMANTIC-DIGEST-1
plan_semantic_digest: <approved digest>
plan_file_list_digest: <approved digest>
master_source_sha256: <approved digest>
parent_requirement_catalog_digest: <approved digest>
child_requirement_catalog_digest: <approved digest; PENDING_C0_01R only for the seed-locked C0-01D/C0-01R packets>
milestone: <C0 or M0-M8>
prompt_phase: <null only when goal_scope is PRE_GOAL_C0; otherwise integer 0-43>
requirements: [<stable REQ-Pxxx-yyy, REQ-Sxxx-yyy and UREQ-xxx IDs>]
outcome: <one concrete outcome>
non_goals: [<explicit exclusions>]
importance: normal|high
risk_class: C1|C2|C3
effect_tier: R0|R1|R2|R3
effect: observe|scoped-write|high-impact-write|external-destructive
cwd: <exact workspace/worktree>
base_ref: <accepted main or milestone integration ref>
base_sha: <exact SHA matching the dependency-state packet>
branch: <intent-based branch>
worktree: <absolute path or null>
allowed_paths: [<exclusive paths>]
forbidden_paths: [<shared/unrelated paths>]
shared_boundaries:
  consumed: [<owner@exact head/digest or empty>]
  produced: [<bounded output contract or empty>]
dependencies:
  accepted_main: [<prior milestone main identities or accepted decision revisions>]
  integrated: [<same-milestone task@exact integration head>]
  frozen_interfaces: [<producer task@exact head/digest and bounded interface>]
  dispatch_after: [<ordering-only task IDs>]
required_skills: [<skills>]
expected_output: <artifact or behavior>
acceptance: [<objective checks>]
checks: [<commands>]
commit_intent: [<small Conventional Commit clusters or empty for R0>]
timeout: <bounded duration>
writer_lease_generation: <generation or null>
writer_lease_id: <atomic lease record or null>
keepalive_due: <event checkpoint plus bounded liveness interval or null for R0>
material_gate:
  required: <true when risk_class is C3 or effect_tier is R3; otherwise true|false>
  candidate_identity: <contract SHA/digest/deployment or not-applicable>
  advisor_receipt: <same-candidate FIT receipt or not-applicable>
  kongming_receipt: <same-candidate PASS receipt or not-applicable>
  controller_disposition: <linked disposition or not-applicable>
resolved_route:
  role: <resolved AgentKit role>
  model: <actual model>
  reasoning_effort: <actual effort>
  tools: [<material tools>]
merge_method: <approved method>
commit_authority: none|scoped
merge_authority: none|mechanical-approved-head-only
push_authority: none|approved-branch-only
destructive: false
stop_conditions: [<fail-closed conditions>]
external_authority: none|<exact separate R3 receipt>
```

Prompts contain variable names only, never credential values or raw environment dumps.

`PRE_GOAL_C0` is not a formal Goal and cannot dispatch product work. It is valid only after the C0-03 seed commit and canonical ledger exist, and only for C0-01D/C0-01R plus their mechanical integration/review tasks. Those packets use `control_program_id: nexora-pre-goal-c0`, `goal_id: null`, `prompt_phase: null`, the exact seed/current-main SHA, explicit allowed paths and an active ledger lease. Any other null Goal/prompt-phase combination is invalid. C0-01R pins source/parent identities and `PENDING_C0_01R` as its input status, then emits the child-catalog digest as reviewed output. C0-06 replaces the pending value with the actual integrated digest before any M0 packet or formal Goal can exist.

This dependency schema is canonical and is expanded with examples and invariants in [Thread, Branch and Worktree Runbook](./thread-branch-worktree-runbook.md). `accepted_main`, `integrated` and `frozen_interfaces` are distinct scheduler predicates; a bare task ID or state substitution is invalid.

No packet with `risk_class: C3` or `effect_tier: R3` may enter `IN_PROGRESS` with `material_gate.required: false`, missing receipts, mismatched candidate identities or an undisposed finding. Routine C1/C2 work may record `not-applicable` only when it remains inside an already dual-approved material contract.

## Runtime Task States

```text
BACKLOG
  → READY
  → IN_PROGRESS
  → IMPLEMENTED
  → VERIFYING
  → MERGE_READY
  → INTEGRATING
  → INTEGRATED
  → ACCEPTED

IN_PROGRESS/IMPLEMENTED/VERIFYING/INTEGRATED → CHANGES_REQUIRED → IN_PROGRESS
any non-terminal → BLOCKED | NEEDS_USER | FAILED | SUPERSEDED
```

Definitions:

- `READY`: dependencies, ownership, route and approvals verified.
- `IN_PROGRESS`: one owner actively executes the accepted task.
- `IMPLEMENTED`: worker branch is frozen and reported; no acceptance claim yet.
- `VERIFYING`: exact HEAD frozen for tester/reviewer.
- `CHANGES_REQUIRED`: verdict is not PASS; prior review invalid after changes.
- `MERGE_READY`: exact head has PASS receipts and clean scope.
- `INTEGRATED`: reviewed worker head is present on the milestone integration branch and combined integration checks pass; still provisional.
- `ACCEPTED`: milestone integration is on approved `main`, combined-main checks pass, and required remote evidence is recorded.
- `NEEDS_USER`: material decision or R3 approval required.

Plan checkboxes are durable. Runtime state is a projection and must sync back before completion claims.

## Branch and Worktree Topology

Approved topology after C0-03 Git bootstrap:

```text
main                              # protected accepted release line; M0 baseline is reviewed directly here
integration/v0.1-m1
integration/v0.1-m2
integration/v0.1-m3
integration/v0.1-m4
feature/page-schema
feature/adaptive-publishing
feature/secure-rag
fix/tenant-isolation
infra/observability
release/v0.1.0-alpha.1
```

Rules:

1. Never use `codex/` prefix.
2. Manager records exact accepted base SHA before branch creation.
3. One concurrent writer per branch/worktree.
4. Worktree path and allowed files are explicit in the task.
5. Shared contracts/migrations/lockfiles are sequenced under one owner.
6. Failed worktrees remain for diagnosis until explicit merge/discard decision.
7. Remove worktree only after process cleanup and verified integration.

## Commit Protocol

Each worker scope produces 1-3 independently understandable commits when practical:

```text
implement or update tests first where risk warrants
→ implement one concern
→ run targeted checks
→ inspect diff and staged paths
→ secret-shaped-value scan
→ Conventional Commit
```

Examples:

- `feat(tenant): derive organization context from membership`
- `test(security): reject cross-tenant document access`
- `feat(publishing): add immutable page version transaction`
- `fix(rag): filter unauthorized chunks before context assembly`
- `chore(ci): add reproducible contract drift gate`

Never manufacture commit count or leave broken intermediate commits.

## Exact-Head Verification

Before `MERGE_READY`, independently verify:

1. Repository root and intended branch.
2. Exact base and head SHA; ancestry is expected.
3. Clean working tree or explicitly accounted changes.
4. Focused commit history.
5. Cumulative changed-path scope.
6. `git diff --check` for incremental and cumulative diff.
7. Required targeted and combined checks.
8. Secret/credential-shaped additions.
9. Content claims, limitations, generated artifact provenance.
10. Reviewer verdict tied to the same head SHA.

Any change after review invalidates the verdict and returns the task to `VERIFYING`.

## Integration and Merge Authority

1. Worker reports base/head/commits/paths/checks/cleanliness.
2. Tester runs exact-head acceptance commands.
3. Reviewer/arbiter issues PASS/HOLD/STOP.
4. Project Manager checks dependencies and marks `MERGE_READY`.
5. Git Manager performs only the approved mechanical merge or cherry-pick.
6. Semantic conflicts stop and return to the owning worker.
7. Merge one branch at a time.
8. Run combined checks after every shared-contract merge.
9. Default to a non-transforming merge that preserves the reviewed worker HEAD; record base/head, diff digest, target-before SHA, merge method and merge SHA.
10. Any squash, cherry-pick, rebase, conflict resolution or generated-file delta requires patch-equivalence proof or a new exact-head review.
11. A task remains `INTEGRATED` until its milestone is merged and verified on approved `main`.
12. Record main and required remote evidence.
13. Push only under the accepted R3 policy.

Workers never push `main`. Tests passing do not authorize automatic merge. Integration branches are temporary test surfaces, never a second product authority. No force push except explicit lease-protected recovery approval.

Legacy local agent text that limits Git Manager to an arbitrary small tool-call count or reviews only `HEAD~1` is not valid for Nexora. Git preflight must be complete, and review always uses the recorded exact `base..head` cumulative range.

## Mandatory Advisor and Kongming Dual Supervision

`DEC-A10` makes dual supervision mandatory for every material matter. A matter is material when it is C3 or R3, changes product outcome/scope/user journey, architecture or trust boundary, data/AI egress, auth/tenant/permission behavior, shared contract/migration strategy, selected UI direction/design-system boundary, model/provider/budget/license, production topology/SLO/recovery, public documentation claim, GitHub Release/Package, deployment, destructive action, risk exception, milestone acceptance or Goal completion.

For each material gate:

1. Advisor independently issues `FIT`, `HOLD` or `STOP` for product value, operability, UX, cost/complexity and alignment with the approved outcome.
2. Kongming independently issues `PASS`, `HOLD` or `STOP` for contradictions, architecture/security, failure modes, evidence quality and claim truthfulness.
3. The Controller records exact candidate identity, both receipt IDs, findings and a disposition for every recommendation.
4. Any `HOLD`, `STOP`, missing receipt or candidate-head change blocks advancement. The user resolves scope/cost/external-authority decisions; neither counsel merges or overrides the user.

Mandatory paired checkpoints include plan/Outcome Contract activation; each material `DEC-*`; architecture/workflow baselines; selected Stitch direction and design-system foundation; auth/tenancy, publishing, event boundary, secure RAG and destructive migrations; every milestone acceptance; public media/claims; GitHub About/Release/GHCR; SLOs; production promotion/rollback/restore; and final Staff-level acceptance.

Routine, reversible C1/C2 implementation within an already approved contract does not require both agents on every cosmetic commit. It is automatically escalated to the paired gate when scope, shared boundary, risk, claim, budget or external state changes. After repeated failures or contradictory evidence, the Controller pauses the writer and triggers both reviews before a new approach.

## Frontend and Stitch Workflow

```text
Approved journeys and information architecture
→ bounded Stitch prompt for one screen family
→ three comparable visual directions
→ user selects direction
→ export screenshots, HTML reference, DESIGN.md
→ normalize Nexora tokens and component contracts
→ map selected tokens into owned AntD wrappers/custom public primitives
→ hand-build Next.js/React/Tailwind/AntD implementation
→ responsive, keyboard, reduced-motion, error/empty/loading work
→ 375px + desktop browser evidence
→ accessibility and visual fidelity review
```

Nexora explicitly excludes incompatible Vite/MUI assumptions, a parallel full shadcn system, and generic TanStack defaults from AgentKit guidance. Stitch output is design evidence, not production code authority. The complete reference/design/thread contract lives in [UI/UX, Stitch and Ant Design Strategy](./ui-ux-stitch-ant-design-strategy.md). Dispatch, branch, worktree, lease, timeout and exact-head integration mechanics are expanded in [Thread, Branch and Worktree Runbook](./thread-branch-worktree-runbook.md).

## Timeout, Retry and Escalation

Suggested defaults, clamped to the live runtime:

| Work | Checkpoint/timeout |
|---|---|
| Read-only scout | 15 minutes |
| Planner/design task | 30 minutes |
| Worker slice | 45-60 minutes before progress checkpoint |
| Focused test/review | 30 minutes |
| Mechanical merge | 15 minutes |
| CI/deployment | Bounded status waits; no blind retry loop |

Rules:

- One automatic retry only for clearly transient network/process failure.
- Deterministic failure is never retried unchanged.
- After two failed approaches, dispatch Debugger with evidence.
- Architecture/security contradiction or repeated hypothesis escalates to the Advisor/Kongming dual gate.
- Missing credential, budget, approval, destructive action, or outcome change becomes `NEEDS_USER`.
- One corrective message to a stalled owner; then preserve handoff and reassign.
- Every writer owns a lease generation. Heartbeat expiry alone never transfers ownership.
- Interrupt the prior writer, verify its agent/process has stopped, inspect the worktree and revoke its lease before issuing a higher generation.
- Do not assign a replacement writer while a timed-out writer may still mutate the same ownership boundary.

## Keepalive and Resume

Keepalive is event-driven status continuity, not an immortal polling agent.

Agent keepalive is not product uptime. Runtime continuity, health, redundancy, monitoring and recovery are governed separately by [Production Continuity and Hosting](./production-continuity-and-hosting.md).

### Durable control-ledger authority

- The Project Manager is the single logical writer to one local SQLite control ledger shared by every worktree. Its authority path is resolved only after Git initialization by running `git rev-parse --path-format=absolute --git-common-dir`, validating that the returned absolute directory belongs to the intended `D:\Nexora` repository, and appending `agentkit\nexora-control-ledger.sqlite`. For example, a normal checkout resolves to `D:\Nexora\.git\agentkit\nexora-control-ledger.sqlite`; linked worktrees must resolve to that same file, never to a worktree-relative `.agentkit` path.
- `.agentkit/state/**` inside a worktree is an ignored projection/cache location only. It is not authoritative, cannot grant or revoke a lease, and cannot be used as a fallback when the common-directory ledger is unavailable.
- Bootstrap creates a versioned schema and a hash-chained genesis event bound to the seed SHA, approved parent candidate/source/catalog identity, user-decision receipt and explicit `child_requirement_catalog_digest: PENDING_C0_01R`. Until a final-binding event exists, only C0-01D and C0-01R may be dispatched; product tasks fail closed. After their reviewed heads are integrated, the final-binding event records the final main SHA, `NEXORA-SEMANTIC-DIGEST-1` candidate/file-list/manifest identity, accepted Decision Log revision and actual parent/child catalog digests. Database constraints include a unique active lease per normalized ownership boundary, monotonically increasing lease generations and atomic event/lease transitions.
- State/lease transitions are atomic transactions. Each append event records monotonic sequence, previous-event hash, Goal/plan revision, task, old/new state, branch/worktree/head, lease ID/generation/owner, process fingerprint, timestamp and event hash.
- Process fingerprint contains observed agent/task ID, PID or session ID when applicable, safe command digest, worktree and start time; it contains no secret-bearing command or environment value.
- A uniqueness rule prevents more than one active writer lease for an ownership boundary. Reassignment transactionally revokes the prior lease only after observed agent/process/worktree reconciliation.
- Chat messages, heartbeats, UI task state and plan checkboxes are projections. If they disagree with the control ledger plus observed Git/process state, dispatch and merge fail closed.
- At milestone acceptance, the Manager exports a redacted hash-chained receipt to the plan reports; the live SQLite file itself is never committed or treated as release evidence.
- Ledger corruption/unavailability produces `BLOCKED`; no in-memory replacement writer or merge is authorized.

Before Goal creation, the sole seed exception initializes Git and the canonical ledger/genesis is created. Before either semantic C0 writer dispatches, C0-05 runs against the exact C0-01 user-decision receipts. Two independent processes launched from temporary linked worktrees under ignored `D:\Nexora\.worktrees\` must resolve the same canonical ledger path and race for the same test ownership boundary; exactly one active-lease transaction succeeds and the loser receives the defined contention result without replacing or corrupting the winner. After verified temporary-worktree cleanup, the same C0-05 gate runs a read-only task-graph simulation proving `M3-T04` cannot enter `READY` until `M3-T02` is `INTEGRATED` and the same-revision Go/NATS boundary ADR has Advisor `FIT`, Kongming `PASS` and Controller disposition. It also proves M3-T05 may consume only a pinned M3-T04 `frozen_interfaces` head while M3-T04 remains `IMPLEMENTED/VERIFYING`; head movement blocks both, and both reach `MERGE_READY` only after joint consumer evidence. A failed proof or a scheduler that ignores `dispatch_after`, `accepted_main`/`integrated`/`frozen_interfaces` or material-gate dependencies is a `STOP`.

Only after the combined C0-05 PASS does the ledger govern the separate decision-ratification writer and then the requirement-catalog writer; the latter expands the preamble plus numbered master-prompt spans to bullet-level requirements with zero unclassified normative lines and dual review on one catalog digest. After both exact `MERGE_READY` heads are mechanically integrated, two independent digest implementations reproduce the same ordered semantic paths/content hashes/file-list/candidate digest under [Semantic Digest Contract](./semantic-digest-contract.md), using a public-safe manifest with logical/relative paths only. C0-06 verifies the unchanged C0-05 receipt identities, writes the final binding, inventories runtime surfaces and performs same-candidate dual activation review plus Goal warmup.

At every task boundary persist:

- Goal, baseline Git SHA, semantic/file-list/manifest/source/catalog identities and Decision Log revision.
- Control-ledger sequence/event hash and writer lease ID.
- Milestone, Prompt Phase and completed/remaining stable parent/child requirement IDs.
- Task state, owner, resolved route.
- Writer lease generation and expiry/revocation state.
- Branch/worktree, base/head, cleanliness.
- Commands, exit codes, environment/evidence paths.
- Running process PID/port/command/worktree.
- Blocker, attempts, last activity, next executable action.

Resume procedure:

1. Read pinned Goal and plan revisions.
2. Inspect current repository, origin, branch, worktrees, heads and status.
3. Rehydrate tasks from unchecked plan items.
4. Compare last receipt against current Git evidence.
5. Reconcile or stop orphaned processes.
6. Verify active writer lease generations; interrupt/revoke stale owners before reassignment.
7. Live-revalidate runtimes/models/agents/controls.
8. Reuse successful accepted outputs.
9. Restart only the next incomplete bounded task with a valid lease.
10. Rerun arbiter if any reviewed job reruns.

Idle means waiting, not success. A fresh heartbeat never authorizes merge.

## External and Destructive Actions

Fresh user approval required for:

- First remote initialization/push and any force-with-lease recovery.
- PR merge policy changes or branch protection changes.
- Supabase/cloud resource creation, paid AI calls beyond approved smoke, Stitch paid quota.
- Production deployment, custom domain, registry publication.
- GitHub About/topics/social preview changes, public Release publication and package visibility changes.
- Database reset/drop, destructive migration, object deletion, infrastructure destroy.
- Public issue/wiki/site publication containing plan details.

Any future activated Goal does not broaden these permissions.

## Proposed Root AgentKit Preferences

Review only; do not apply until accepted:

```yaml
coding_level: 5
paths:
  docs: docs
  plans: plans
docs:
  max_loc: 800
plan:
  naming_format: "{date}-{slug}"
  date_format: YYMMDD-HHmm
  reports_dir: reports
project:
  type: monorepo
  package_manager: pnpm
  framework: nextjs
locale:
  response_language: vi
  thinking_language: null
assertions:
  - pattern: "apps/web/**/*.{ts,tsx}"
    rule: "strict TypeScript, accessible complete states, no invented live data"
  - pattern: "apps/platform-api/**/*.java"
    rule: "tenant authorization, stable errors, tests, telemetry, migration review"
  - pattern: "services/**/*.go"
    rule: "bounded retries, graceful shutdown, table-driven tests, telemetry"
  - pattern: "database/migrations/**"
    rule: "one migration owner, tenant/RLS/index/rollback review"
privacy_block: true
workflow_artifact_gate:
  enabled: true
simplify:
  gate:
    enabled: true
```

Framework detection for a polyglot monorepo may require adjustment after Phase 0; validation must decide rather than guessing.

## Preflight Before Every Milestone

- Repository/main/origin/visibility/cleanliness verified.
- Current plan and accepted decisions resolved.
- Runtime/agent/model/control inventory refreshed.
- Dependencies and shared-file ownership checked.
- Runtime task-ledger dependencies and any phase `dispatch_after` safety gates checked; coarse phase frontmatter alone never authorizes dispatch.
- Credentials referenced by name only.
- Budget/external authority checked.
- Worktree capacity and running processes inspected.
- Stop/HOLD conditions reviewed.

## Workflow Approval Checklist

- [ ] Role authorities accepted.
- [ ] Manager versus Git Manager split accepted.
- [ ] Model policy accepted subject to live routing.
- [ ] Maximum concurrency and writer limits accepted.
- [ ] Task state machine accepted.
- [ ] Worktree/branch/commit/merge policy accepted.
- [ ] Mandatory Advisor/Kongming dual-supervision classification, receipts and stale-candidate invalidation accepted.
- [ ] Timeout/retry/escalation policy accepted.
- [ ] Keepalive/resume semantics accepted.
- [ ] R3 approval boundary accepted.
- [ ] Documentation/media/GitHub distribution and exact-artifact release gates accepted.
- [ ] Agent keepalive versus product-continuity boundary accepted.
- [ ] Proposed AgentKit root preferences accepted or amended.
