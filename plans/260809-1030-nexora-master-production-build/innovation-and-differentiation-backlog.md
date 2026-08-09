# Nexora Innovation and Differentiation Backlog

## Status and Authority

`DRAFT — PENDING ADVISOR/KONGMING DUAL REVIEW AND USER SELECTION`.

This backlog records important or potentially breakthrough extensions without silently expanding Option A. It is not an implementation order and does not authorize a provider call, product branch, budget, new data collection or completion claim.

The first Goal remains M0-M4 / Prompt Phases 0-21. A proposal may affect that Goal only when it strengthens an already required contract or acceptance test without adding a new headline outcome. Full new product capabilities remain future-Goal candidates until the user accepts a decision, both Advisor and Kongming review the same proposal revision, dependencies/cost are estimated, and the plan/Goal is re-pinned if needed.

The independent innovation review was requested while this plan was under review. If either counsel runtime is unavailable, proposals remain `PENDING_DUAL_REVIEW`; the Controller must not manufacture a verdict.

## What Is Already Covered, Not Missing

- Pragmatic collaboration is already in scope: presence, edit indicators, autosave, optimistic locking, revision/conflict UI. Google-Docs-level CRDT/Yjs remains a measured later option, not a v0.1 requirement.
- Secure RAG already requires permission filtering before context, resolvable citations, injection tests, deletion propagation, redacted traces and evaluation.
- Adaptive intelligence already has future phases for flags, experiments, analytics, personalization and recommendation.
- UI quality already includes a distinctive Stitch-selected design system, AntD Studio boundary, complete states, keyboard operation, 375px/desktop and accessibility evidence.
- Production already has separate deployment, observability, supply-chain and recovery Goals. Innovation cannot bypass those gates.

The important gap is the connective tissue between these capabilities: why an experience changed, which evidence produced an answer or page, how a source change affects published experiences, and how AI can help an editor without gaining uncontrolled write authority.

## Evaluation Rubric

Advisor evaluates user problem severity, workflow improvement, comprehensibility, accessibility, product coherence and total operating cost. Kongming independently challenges tenant/privacy risk, manipulation, prompt/tool injection, provenance overclaim, failure behavior, observability, vendor lock-in and evidence quality.

Each proposal must pass all questions:

1. Does it solve a repeated user job rather than demonstrate technology?
2. Is the difference visible and valuable without a marketing explanation?
3. Can a small experiment disprove it cheaply?
4. Does it preserve tenant, permission, consent, deletion and truthful-claim invariants?
5. Can success be measured without fake vanity metrics or hidden sensitive data?
6. Is its simplest architecture compatible with the modular monolith and PostgreSQL truth?
7. Does it preserve Option A, or explicitly require a future Goal/re-pin?

Verdicts:

- `GO_FOUNDATION`: add only a small contract/evidence hook inside an existing phase.
- `LATER_EXPERIMENT`: retain for a future Goal; no active-Goal completion impact.
- `REJECT_NOW`: do not schedule until its stop condition is removed by new evidence/user need.

## Candidate Portfolio

| ID | Candidate | Proposed disposition | Smallest placement | Full capability placement |
|---|---|---|---|---|
| INN-01 | Explainable Adaptive Decision Receipt | `GO_FOUNDATION` pending dual review | M2 renderer/publication contract | M5 flags/personalization/experiments |
| INN-02 | Grounded Experience Lineage and Impact Radar | `GO_FOUNDATION` pending dual review | M4 source/chunk/citation lineage | M5/M8 impact workflow and final trust UX |
| INN-03 | User-facing Answer and Content Trust Receipt | `GO_FOUNDATION` pending dual review | M4 secure RAG/observability | M8 public trust/evidence polish |
| INN-04 | Continuous Quality Canary and Change-Impact Gate | `GO_FOUNDATION` pending dual review | M4 versioned evaluation metadata | M6/M7 release canary and rollback gate |
| INN-05 | Guarded AI Co-editor via Typed Schema Patches | `LATER_EXPERIMENT` | Contract thought experiment only | M5 dedicated Goal amendment |
| INN-06 | Accessibility-first, User-controlled Adaptation | `LATER_EXPERIMENT` with M1 token hook | M1 preference-safe token semantics | M5 adaptive experience Goal |
| INN-07 | Verifiable Media Content Credentials | `LATER_EXPERIMENT` | Documentation research only | M8 stable publication/export |
| INN-08 | Governed Connector and Action Fabric | `REJECT_NOW` for M0-M8 baseline | No implementation | Post-stable Goal after proven demand/security |

None of these dispositions is accepted until dual review and user disposition are recorded.

## Goal Hook Admission Rule

Every formal Goal starts with `accepted_innovation_hooks: []`. An INN item is not admitted by appearing in this backlog or by receiving a favorable research comment. Each non-empty hook needs all of the following on the same revision: an accepted `DEC-*`, mapped requirement/task IDs, estimate and budget impact, owned paths/dependencies, Advisor receipt, Kongming receipt, Controller disposition and user approval. If it changes a material schema/API, public outcome, trust/data boundary, provider, signal collection or phase estimate, the plan and Goal are re-pinned before dispatch. Absence from the hook list means no implementation, migration, provider call or release claim.

## INN-01 — Explainable Adaptive Decision Receipt

### User problem

Editors and operators cannot trust or debug an adaptive page if they cannot reproduce why visitor A saw variant X while visitor B saw fallback Y. Visitors also need a bounded explanation without exposure of private signals or internal rule details.

### Differentiator

Every adaptive render produces a safe receipt whose stable core is deterministic while operational envelope fields remain explicitly volatile:

```yaml
receipt_id: "opaque-random-id"
decision_core_hash: "sha256:canonical-stable-inputs-and-result"
page_version: 18
renderer_contract: "v1"
theme_version: 4
policy_revision: 9
flag_experiment_rule_ids: ["safe-opaque-id"]
signal_classes: ["explicit_preference", "consented_session"]
signal_freshness: "current|stale|missing"
selected_variant: "hero-compact"
reason_code: "explicit-user-preference"
fallback_used: false
occurred_at: "<timestamp>"
```

`decision_core_hash` is computed from canonical authorized decision inputs, pinned contract/policy versions and result only. It excludes `receipt_id`, timestamp, trace IDs and other volatile metadata. Public `reason_code` values come from a small versioned allowlist and never expose rule secrets or sensitive signal values. Receipt rows, indexes and metrics have explicit tenant retention, cardinality and sampling ceilings.

The Studio adds “Preview as scenario” and “Why this experience?” views. An editor simulates permission, consent, device, preference, flag and experiment inputs with synthetic values; it does not impersonate a real person or reveal sensitive profile data. Public explanation is coarser than the authorized operator receipt.

### Smallest experiment and evidence

- M2 defines an opaque `receipt_id`, deterministic `decision_core_hash`, renderer/policy version, versioned safe reason-code enumeration and fallback contract even before personalization exists.
- Three fixtures reproduce the same selected result and core hash from the same canonical input/version tuple; they do not require random IDs or timestamps to be byte-for-byte identical.
- Negative tests prove no sensitive signal/value or rule secret crosses the visitor boundary.
- Load/cardinality tests enforce accepted receipt retention and metric-label budgets; M5 measures scenario reproduction rate, fallback correctness, operator time-to-diagnose and accessibility of the explanation.

### Kill/stop criteria

Opaque ML-only decisions, protected attributes, inferred disability, exposure of targeting internals, unbounded reason-code/cardinality/retention, non-reproducible core result, or latency/storage cost beyond an accepted budget. Do not market the M2 hook as personalization.

## INN-02 — Grounded Experience Lineage and Impact Radar

### User problem

When a source document changes, expires or is deleted, teams need to know which chunks, answers, page blocks and publications are now stale. Citations alone show where an answer came from, not what downstream experiences are affected.

### Differentiator

Build a tenant-scoped provenance lineage using PostgreSQL relations, not a new graph database by default:

```text
source object/version
-> ingestion job/parser/chunker revision
-> chunk/embedding version
-> retrieval run/citation
-> answer receipt
-> optional page block/publication reference
```

An Impact Radar later opens review tasks for affected pages/answers, distinguishes `fresh`, `changed`, `deleted`, `permission-revoked` and `model-reindexed`, and never silently republishes content.

### Smallest experiment and evidence

- M4 records source version, chunk identity, ingestion revision and citation/answer derivation already needed for secure RAG.
- A controlled source update/deletion returns the exact affected citations and makes ineligible chunks impossible to retrieve.
- Future experiment links one cited claim to one draft page block and measures impact-query precision, false positives and review time.
- Export may map to W3C PROV concepts later, but v0.1 uses the smallest relational vocabulary.

### Kill/stop criteria

Cross-tenant traversal, immutable retention that defeats deletion rights, graph explosion without bounded retention/indexes, inferred causal claims, or introducing Neo4j/another truth store without measured PostgreSQL failure.

## INN-03 — User-facing Answer and Content Trust Receipt

### User problem

A list of links does not tell a user whether every material claim is grounded, whether sources are still valid, or whether the system fell back. Operators need evidence without exposing raw prompts, chain-of-thought or unauthorized metadata.

### Differentiator

Secure RAG returns an expandable, layered trust receipt:

- answer ID and timestamp;
- provider/model route and prompt-policy version at an appropriate disclosure level;
- source versions, citation coverage and freshness status;
- retrieval/no-answer/fallback reason;
- permission-policy revision and safe trace correlation;
- machine-readable authorized admin export;
- no hidden reasoning or raw cross-tenant context.

The UI uses progressive disclosure: a simple grounded/freshness state for ordinary users, source-level evidence on expansion and an authorized operator trace separately. It never labels provenance as “true” merely because a signature or citation exists.

### Smallest experiment and evidence

- Integrate into existing M4 citation and RAG-observability tasks rather than add an agent system.
- Every displayed citation must resolve to an authorized versioned chunk, but an empty citation list is not a pass. Predeclared material-claim coverage measures whether substantive answer claims are actually cited.
- Citation correctness/entailment, source freshness, authorized resolution, no-answer behavior and hostile label/HTML spoof resistance are reported as separate metrics/slices; no single composite “trust score” can hide a failure.
- Hostile fixtures prove source labels/HTML cannot spoof trusted UI.
- Comprehension test checks that users distinguish grounded, stale, unavailable and no-answer states.

### Kill/stop criteria

Chain-of-thought exposure, raw prompt/context logging, misleading confidence score, inaccessible source disclosure, unauthorized filenames/metadata, or a “verified truth” claim unsupported by evidence.

## INN-04 — Continuous Quality Canary and Change-Impact Gate

### User problem

Changing a prompt, model, embedding, parser, chunking strategy, retrieval weight or policy can silently improve one case and regress another. One-time demo evaluation is not enough.

### Differentiator

Treat AI/retrieval behavior as a versioned release artifact:

- immutable evaluation corpus/checksum and tenant-safe synthetic/adversarial cases;
- baseline versus candidate comparison by prompt/model/parser/chunker/embedding/retrieval/policy revision;
- predeclared quality, leakage, citation, latency and cost thresholds;
- canary/shadow evidence only with accepted privacy/retention rules;
- automatic `HOLD`, not automatic promotion, when a material regression or data leak appears;
- rollback identity tied to the exact configuration and index version.

### Smallest experiment and evidence

- M4 extends the existing evaluation report with a version tuple, baseline delta, confidence/limitations and kill criteria. Each gate predeclares corpus slices, minimum sample size, zero-leakage invariant, quality/citation/no-answer thresholds and latency/cost ceilings; results stay disaggregated rather than collapsed into a composite score.
- M6 defines release thresholds and safe telemetry; M7 proves canary/rollback on exact artifacts.
- Pin any adopted OpenTelemetry GenAI semantic-convention revision because current GenAI conventions are evolving; content capture remains disabled by default.

### Kill/stop criteria

Private production prompts copied into a corpus, unplanned/tiny slices, Goodharted single or composite score, any leakage, exceeded latency/cost ceiling, unbounded live-call cost, experimental telemetry schema treated as stable, quality gate bypass, or shadow traffic without consent/policy.

## INN-05 — Guarded AI Co-editor via Typed Schema Patches

### User problem

Content creators want AI acceleration, but allowing a model to emit HTML/React or mutate/publish a page directly breaks the schema-driven safety model.

### Differentiator

The model can only propose typed, allowlisted operations against an exact draft/version:

```text
authorized brief + selected sources
-> structured patch proposal
-> schema/policy/permission validation
-> visual diff and citations
-> explicit human accept/reject per operation
-> server reauthorization and exact-draft precondition at apply time
-> deterministic diff and per-operation audit
-> normal optimistic-lock save
-> normal review/publish workflow
```

Possible operations are bounded (`insertBlock`, `updateProps`, `moveBlock`, `suggestThemeToken`) and never contain JavaScript, arbitrary HTML/CSS, credentials or publish authority. The patch includes source citations, confidence limitations, cost and model identity. A human click is necessary but insufficient: apply rechecks current membership/permission, exact draft/version and schema/policy on the server, generates a deterministic before/after diff and writes an audit event for each accepted/rejected operation.

### Smallest experiment and evidence

- Future M5-only experiment on one page type and two safe operations using deterministic fixtures first; stale draft, revoked membership and tampered-patch apply attempts must fail.
- Measure time-to-first-valid-draft, schema rejection rate, human acceptance/edit distance, accessibility regression and cost per accepted patch.
- Require meaningful workflow improvement; novelty is not success.

### Kill/stop criteria

Direct writes/publish, client-only authorization, human-click-only trust, hidden or non-deterministic patch operations, missing per-operation audit, stale-version overwrite, prompt-injected sources controlling tools, unsupported claims, inaccessible output, weak editor value or provider cost above the accepted ceiling.

## INN-06 — Accessibility-first, User-controlled Adaptation

### User problem

Personalization systems commonly optimize engagement for the business while making interfaces less predictable or usable. Nexora can make adaptation user-controlled and accessibility-preserving.

### Differentiator

Explicit preferences such as calm mode, density, motion, help level, reading width and content simplification take precedence over experiments. Users can inspect/reset them. The system never infers disability or uses accessibility preferences for marketing segmentation. Default schema remains fully usable without personalization.

### Smallest experiment and evidence

- M1 tokens/components retain semantic support for reduced motion, density and readable layout; no user profiling is added.
- M5 evaluates two explicit, reversible preferences with local or consented tenant storage.
- Test preference precedence, keyboard/screen-reader stability, reset/delete, cross-device policy and no experiment override.
- W3C WAI-Adapt informs exploration but its draft modules are not presented as a conformance claim.

### Kill/stop criteria

Inferred disability, fingerprinting, dark patterns, preference used as ad/experiment signal, inaccessible variant, irreversible layout, or standards-draft marketing claim.

## INN-07 — Verifiable Media Content Credentials

### User problem

Organizations publishing human- and AI-edited media may need portable evidence of asset origin and edit history beyond an internal audit log.

### Differentiator

For selected exported media, evaluate opt-in C2PA Content Credentials tied to the asset version, permitted authorship/edit assertions and signing authority. The Nexora trust UI explains validation states without claiming that provenance proves truth or safety.

### Smallest experiment and evidence

- M8 research/spike on one supported format after asset export, key custody and privacy policy exist. At experiment start, live-verify the current C2PA specification index, pin the exact adopted revision, and use UX guidance from that same revision; this plan does not label a hard-coded UX 2.2 URL as current.
- Verify credential after download/re-upload and document which transformations preserve or remove it.
- User research tests layered disclosure and failure wording.

### Kill/stop criteria

Weak signing-key custody, privacy/identity over-disclosure, unsupported format pipeline, false authenticity claim, confusing UX or insufficient customer need. It is never an M4 or M7 blocker unless the user later expands scope.

## INN-08 — Governed Connector and Action Fabric

### User problem

Knowledge eventually comes from external repositories, and users may ask AI to perform actions. A broad connector/tool ecosystem is attractive but creates a major tenant, OAuth, prompt-injection and destructive-action surface.

### Proposed rejection boundary

Do not add a generic action agent or arbitrary MCP/tool execution to the current M0-M8 baseline. After stable production and proven demand, a new Goal may test one read-only ingestion connector with:

- tenant-scoped OAuth and least privilege;
- explicit allowlisted resource boundary;
- content quarantine and injection handling;
- immutable provenance/sync cursor/deletion behavior;
- per-action human approval, idempotency and audit if writes are ever considered;
- egress, rate, cost and revocation controls.

### Kill/stop criteria

Generic bearer-token forwarding, confused-deputy authorization, arbitrary URL/tool registration, source content controlling actions, cross-tenant cache, hidden background writes, weak revocation or no end-to-end audit. A connector demo is not a moat if it weakens the trust model.

## Recommended Product Narrative if Approved

The defensible story is not “a CMS with AI.” It is:

> Nexora is an explainable adaptive experience system: every published experience is schema-safe, every AI answer is permission-grounded, every important decision has a receipt, and every source change can be traced to the experiences it affects.

This narrative is permitted only after the corresponding behavior is implemented and evidenced. Before then, it remains a product direction.

## Proposed Plan Integration Without Scope Creep

| Existing work | Permitted foundation refinement | Explicitly deferred |
|---|---|---|
| M2 schema/publish | Opaque decision ID, renderer/policy version, safe fallback/reason contract | Actual targeting/personalization/simulator |
| M4 citations/observability | Source/version lineage, layered answer receipt, versioned evaluation tuple | Page-wide impact automation and AI co-editor |
| M6 observability/security | Candidate/baseline quality gate, privacy-safe GenAI telemetry revision | Unbounded production trace capture |
| M5 adaptive intelligence | Scenario simulator, explicit preferences and typed co-editor experiment after a new decision | Opaque autonomous personalization |
| M8 documentation/trust | Impact Radar polish and optional Content Credentials spike | Authenticity/truth claims unsupported by provenance |

Any foundation refinement estimated above the accepted phase envelope, changing public API/schema materially, collecting a new signal, or requiring a new provider becomes a separate future Goal amendment.

## Proposed Thread/Branch Experiments After Approval

| Candidate | Read-only discovery | Writer branch if later authorized | Exact gate |
|---|---|---|---|
| INN-01 | `research/adaptive-decision-receipts` report only | `feature/m5-experience-simulator` | Core-hash/result determinism, privacy, bounded retention/cardinality, performance, dual review |
| INN-02 | lineage model/retention ADR | `feature/m5-impact-radar` | Cross-tenant/deletion/query-bound evidence |
| INN-03 | source disclosure usability study | Fold bounded contract into M4 owned tasks | Material-claim coverage, correctness, freshness, no-answer, spoof and comprehension tests |
| INN-04 | eval metric/retention review | `feature/m6-ai-quality-gate` | Predeclared slices/minimum sample, zero leakage, disaggregated thresholds, cost/latency ceilings and rollback proof |
| INN-05 | threat model + workflow prototype | `feature/m5-guarded-ai-coeditor` | Typed patch, apply-time reauthorization, exact draft, deterministic diff, per-op audit, no direct write |
| INN-06 | accessibility preference research | `feature/m5-user-controlled-adaptation` | User control, precedence, no inference |
| INN-07 | format/key-custody compatibility spike | `feature/m8-content-credentials` | Verify round trip, privacy and honest UX |
| INN-08 | demand/security research only | none in current program | New Goal and security architecture required |

Every writer experiment follows the normal one-agent/one-branch/one-worktree/one-lease rule. Advisor and Kongming review the same experiment contract before dispatch and the same exact result before any promotion into the roadmap.

## Primary Reference Anchors

- [W3C WAI-Adapt overview](https://www.w3.org/WAI/adapt/)
- [W3C PROV-O Recommendation](https://www.w3.org/TR/prov-o/)
- [C2PA current specification index](https://spec.c2pa.org/about/)
- [NIST AI Risk Management Framework resources](https://airc.nist.gov/)
- [OpenTelemetry semantic conventions](https://github.com/open-telemetry/semantic-conventions)
- [Vercel AI SDK generative UI](https://ai-sdk.dev/docs/ai-sdk-ui/generative-user-interfaces)

These sources inform an experiment or data model; they do not prove Nexora implements the capability. WAI-Adapt, C2PA guidance and GenAI semantic conventions include evolving material, so the owning experiment must live-verify the current index, pin an exact revision, use version-matched guidance and label stability before adoption.
