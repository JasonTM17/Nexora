# Nexora Risk Register

## Rating

- Likelihood/Impact: Low, Medium, High, Critical.
- Owner is a delivery role, not necessarily the code author.
- A risk closes only with linked evidence.

| ID | Risk | Likelihood | Impact | Leading indicator | Mitigation / gate | Owner |
|---|---|---|---|---|---|---|
| R-001 | Full roadmap becomes endless Goal | Low | Critical | v0.1 criteria begin requiring M5-M8 | Accepted DEC-001; separate v0.1 contract; later phases marked FUTURE_GOAL | Controller |
| R-002 | Cross-tenant data disclosure | Medium | Critical | Tenant ID accepted from request without membership proof | Auth matrix, RLS, cross-tenant STOP tests | Security reviewer |
| R-003 | Unauthorized chunks reach LLM | Medium | Critical | Permission filtering after retrieval/context | Retrieval-time predicates; captured context leakage tests | RAG security owner |
| R-004 | Prompt injection or XSS through sources/citations | High | High | Raw source content rendered or instructions followed | Hostile fixtures, sanitization, CSP, no tool authority | Security reviewer |
| R-005 | Unsafe file/URL ingestion | High | Critical | Unbounded parsers, redirects, private network access | Size/type/time limits; URL ingestion disabled until SSRF gate | Knowledge owner |
| R-006 | Premature distributed complexity | High | High | NATS/Go/Redis added before measured need | DEC-014; modular monolith and DB outbox first | Architect |
| R-007 | Publish/cache/realtime inconsistency | Medium | High | UI says published but durable version differs | Transactional idempotent publish; refetch fallback | Publishing owner |
| R-008 | Shared-file agent collision | High | High | Concurrent edits to migrations/lockfiles/contracts | Ownership matrix, worktrees, sequential integration | Manager |
| R-009 | Reviewer checks stale HEAD | Medium | Critical | Review SHA differs from merge SHA | Exact-head receipt and re-review on head change | Arbiter |
| R-010 | Secret enters public history | Medium | Critical | Credential-shaped staged diff or logs | Ignore rules, staged/history scan, STOP and rotate | Git Manager |
| R-011 | Stitch output treated as production | Medium | High | Export copied without responsive/a11y work | DESIGN.md handoff; hand-built React; visual QA | UI owner |
| R-012 | Fake metrics/AI/benchmark claims | High | High | Dashboard uses fixtures without label | Provenance labels; evidence classes; claim audit | Reviewer |
| R-013 | AI/cloud retry cost storm | Medium | High | Nested retries and no tenant ceilings | Budget/attempt/token/concurrency limits, kill switch | Platform owner |
| R-014 | Provider/model drift | High | Medium | Static model names fail at runtime | Live inventory before each run; record resolved route | Controller |
| R-015 | Migration causes outage/data loss | Medium | Critical | Destructive schema change without compatibility | Expand/contract, backup, restore, rollback gate | DB owner |
| R-016 | Backup exists but restore fails | Medium | Critical | Only documentation or provider status | Isolated application-level restore drill | Ops owner |
| R-017 | Accessibility deferred to final polish | High | High | Builder pointer-only or missing states | Phase-local a11y acceptance, final audit as regression | UI reviewer |
| R-018 | Tests prove mocks, not live services | High | High | Live claims cite only unit tests | Evidence class separation and explicit limitations | Tester |
| R-019 | Manager becomes implementer/reviewer/merger | Medium | High | Same context owns all gates | Role separation and immutable receipts | User/Controller |
| R-020 | Keepalive causes duplicate/stale writes | Medium | High | Replacement writer starts before old writer stops | Event-driven state, handoff, no overlap after timeout | Manager |
| R-021 | Public repo licensing/provenance gap | Medium | High | Source pushed without license/asset provenance | DEC-015, dependency/license/asset manifest | Docs/Legal owner |
| R-022 | Observability leaks prompts/PII | Medium | Critical | Raw payloads in traces/logs | Safe metadata allowlist, redaction tests, retention | Observability owner |
| R-023 | Experiment results statistically misleading | Medium | High | Winner labels with insufficient exposure | Exposure integrity, sample rules, honest confidence | Analytics owner |
| R-024 | Recommendation/personalization violates consent | Medium | Critical | Profiles built without purpose/opt-out | Consent, explainability, fallback, deletion | Product/security |
| R-025 | Infrastructure added without deploy evidence | High | Medium | Manifests exist but never render/apply | Render/validate/deploy/rollback receipts | Ops reviewer |
| R-026 | Goal requires a plan Git SHA before Git exists | High | Critical | Goal template contains `TBD` or invented SHA | Approved pre-Goal control-plane commit; pin real SHA plus digest | Controller/Git Manager |
| R-027 | Spring runtime role silently bypasses RLS | Medium | Critical | Runtime role owns tables, has `BYPASSRLS`, or pool leaks tenant context | Separate migration/runtime roles, forced RLS, transaction-local context and pool leakage tests | DB/security owner |
| R-028 | Integration branch becomes a second source of truth | Medium | High | Task marked DONE before accepted main merge | `INTEGRATED` versus `ACCEPTED` states; main exact-head receipt required | Project Manager |
| R-029 | Merge transforms reviewed patch | Medium | Critical | Squash/cherry-pick/conflict changes reviewed diff | Fixed merge method, base/head/diff digest, ancestry/patch equivalence; re-review any delta | Git Manager/arbiter |
| R-030 | Timed-out writer and replacement mutate concurrently | Medium | Critical | New writer starts from heartbeat expiry alone | Lease generation, explicit interrupt/process stop and worktree verification before reassignment | Controller |
| R-031 | M0-M4 is marketed as fully production-ready | High | High | Release claims omit M6/M7 security/deploy/DR gaps | `v0.1.0-alpha.1` classification and claims audit; amend Goal if real production launch is required | Controller/release reviewer |
| R-032 | Stale/concept media is presented as the running release | Medium | High | Screenshot/GIF lacks source SHA, route/state or fixture label | Capture pipeline, media manifest, exact-head truthfulness review and recapture on material change | Docs/UI evidence owner |
| R-033 | Ping loop is mistaken for production resilience | Medium | Critical | Free/sleeping tier or single replica is kept warm by cron | Paid non-pausing data tier, min capacity, probes, redundancy, SLOs and synthetic detection only | Ops owner |
| R-034 | Release/package/deployment identities diverge | Medium | Critical | Tag, remote SHA, GHCR digest or deployed digest differs | Immutable tags/digests, SBOM/provenance/attestation and release reconciliation gate | Release/supply-chain owner |
| R-035 | Chat-exposed credential is reused in production | Medium | Critical | Previously shared key appears in live env or evidence | Rotate before live use; new secret manager/env value; secret/history/artifact scans without printing value | Security owner/user |
| R-036 | AntD and another full component system create conflicting semantics/tokens/CSS | Medium | High | Duplicate Button/Form/Dialog primitives or two theme authorities appear | AntD 6 Studio boundary, custom/Tailwind public boundary, no full shadcn; ADR for any exception | UI platform owner |
| R-037 | Nexora looks like default AntD or an imitation of a reference product | High | High | Default blue tokens, copied layout/wording, generic dashboard cards | Signal Atelier direction, owned token/wrapper layer, do-not-copy ledger and user-approved Stitch evidence | UI/UX lead |
| R-038 | Next.js/AntD SSR, CSP, hydration or bundle behavior degrades production UX | Medium | High | FOUC, hydration warnings, broad client boundary, large first route chunk | Next registry, server/client boundary tests, per-surface CSP/cache ADR, bundle budgets and exact browser evidence | Frontend reviewer |
| R-039 | Dynamic AI-rendered UI exposes unsafe content or hidden reasoning | Medium | Critical | Raw model HTML/tool payload or chain-of-thought reaches browser | Typed allowlisted message parts, sanitization, owned AntD X adapters, safe citations and hostile fixtures | RAG/UI security owner |
| R-040 | Control-ledger or stale lease permits concurrent writers/false acceptance | Medium | Critical | Worktrees resolve different SQLite files, chat state differs from ledger/Git/process state or two leases overlap | Git-common-dir canonical ledger, versioned genesis, unique active-boundary constraint, two-worktree contention proof, fail-closed reconciliation and verified revocation | Project Manager/Controller |
| R-041 | Adaptive experience becomes opaque or manipulative | Medium | Critical | Visitor cannot explain/reset a variant or experiments override user/accessibility preference | Versioned decision receipt, explicit consent/preference precedence, safe fallback and scenario tests | Personalization/product security |
| R-042 | Provenance/impact graph leaks tenants or defeats deletion | Medium | Critical | Cross-tenant lineage edge, tombstoned source remains traversable or retention grows unbounded | Tenant keys/predicates on every edge, deletion propagation, bounded retention/indexes and negative traversal tests | Knowledge/security owner |
| R-043 | AI co-editor gains unsafe write/publish authority | High | Critical | Model emits raw HTML/code, hidden patch or direct write/publish; client click bypasses revoked permission/stale draft | Typed allowlisted patches, apply-time server reauthorization, exact draft precondition, deterministic diff, per-operation audit, human review and normal workflow | AI/editor security owner |
| R-044 | Continuous evaluation becomes a privacy leak or vanity gate | Medium | Critical | Real prompts copied to fixtures, one/composite score hides a failed slice, tiny samples, leakage or unbounded provider spend | Synthetic/consented corpora, predeclared slices/minimum samples, disaggregated metrics, zero-leakage invariant, retention/redaction, latency/cost ceilings and kill switch | RAG quality/privacy owner |
| R-045 | Provenance credential is misrepresented as truth or safety | Medium | High | Signed asset shown as factually correct or signer/key state omitted | Layered validation UX, key custody, limitations and separate content/security review | Release/trust owner |
| R-046 | Connector/tool fabric creates confused-deputy or prompt-action compromise | High | Critical | Generic token forwarding, arbitrary tools/URLs or source text triggers writes | REJECT_NOW baseline; later least-privilege connector, quarantine, human approval, revocation and audit architecture | Security/architecture owner |
| R-047 | Global nonce CSP silently disables public static/cache optimization | Medium | High | Public routes become dynamic, CDN hit rate disappears or latency/cost rises after CSP middleware | DEC-027 route-level ADR; public cache and Studio nonce strategies tested separately with build/header/browser/cache receipts | Frontend/platform owner |
| R-048 | Stitch reference artifact executes or imports untrusted content | Medium | Critical | Generated HTML loads remote scripts/fonts/images, event handlers or dependencies in a trusted origin | Offline quarantine scan, no-network sandbox/sanitization, owned local assets and hand-built production code only | UI/security reviewer |
| R-049 | Phase-level traceability hides a dropped master-prompt bullet | High | Critical | 44/44 phases pass while preamble/persona/SEO/privacy/demo behavior has no owner/test | Source SHA, 141 contiguous parent spans over lines 1-5169, pre-Goal bullet child catalog, zero-unclassified report and dual review | Requirements Controller |
| R-050 | Advisor and Kongming review different semantic revisions | Medium | Critical | Receipt says “latest” or hash cannot be reproduced across machines | NEXORA-SEMANTIC-DIGEST-1, two implementations, ordered per-file manifest and stale-receipt invalidation | Controller/reviewers |
| R-051 | Supabase platform drift breaks security or migrations | Medium | Critical | Implicit Data API grants, custom managed-schema objects or ignored extension version request | DEC-028, explicit exposure/grants, documented policy DDL only, observed-version compatibility and current-changelog preflight | DB/platform reviewer |
| R-052 | Persistent chat leaks tenant history or resurrects deleted sources | Medium | Critical | Session lookup lacks tenant/user predicate, regeneration overwrites lineage or deleted citation still opens | Chat schema/RLS, stable history API, source reauthorization, idempotent lifecycle and deletion tests | RAG/security owner |
| R-053 | Privacy workflow reports success while copies survive | Medium | Critical | DB row deleted but object/vector/cache/export/provider/backup copy is served | DEC-016 plane inventory, durable purge state, per-plane manifest and purge-on-restore reconciliation | Data Steward/security |
| R-054 | Demo seed leaks credentials or becomes fake production evidence | Medium | Critical | Public password, real data, broad reset or fixture metric labeled live | Deterministic manifest, ephemeral credentials, environment refusal, tenant allowlist and exact-seed media labels | Test/data evidence owner |

## Review Cadence

- Reassess at every milestone start and close.
- Any Critical risk without an effective gate keeps dependent work on `HOLD`.
- New evidence may lower likelihood/impact only through a documented decision.
