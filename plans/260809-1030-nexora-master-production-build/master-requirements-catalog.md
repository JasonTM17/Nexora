# Nexora Master Requirement Catalog

## Source Identity

- Source ID: `NEXORA-MASTER-PROMPT-V1`; logical source name: `pasted-text.txt`. Machine-local attachment locations belong only in untracked local receipts and never in the public plan or evidence manifest.
- Source SHA-256: `98716a1c79cd0f82a20888249a9d1d70482f13da10effea741bd246dde988b4a`.
- Source lines: 5169.
- Catalog schema: `NEXORA-REQCAT-1`.
- Coverage unit: the source preamble plus every numbered source section `0..139`; each row inherits every normative bullet, table row, scenario and checklist item inside its recorded line span. The 141 parent rows cover lines `1..5169` contiguously.

## Binding Coverage Rule

A parent row is not complete when only its headline outcome exists. After the Git seed and durable control ledger exist, pre-Goal writer task C0-01R expands every normative statement in every preamble/section span into stable child IDs such as `REQ-P000-001` or `REQ-S038-001`, records source line, modality, milestone/task owner, acceptance test/evidence and disposition, then proves no normative source line is unclassified. Structural, blank and separator lines are explicitly classified as non-normative rather than silently skipped. Child requirements may be `INCLUDED_V0.1`, `FUTURE_GOAL`, an explicitly accepted `DEFER`, or a documented duplicate of another ID; they may never disappear silently.

The future Goal pins the source hash, this catalog digest and the expanded child-catalog digest. Any source/catalog semantic change invalidates Advisor/Kongming receipts and requires a re-pin. M0-T02 rechecks coverage against the pinned artifacts but cannot weaken them after Goal activation.

## User Conversation Overlay

| ID | Accepted/requested constraint | Canonical plan authority |
|---|---|---|
| UREQ-001 | Discuss and approve the detailed plan before formal Goal | `plan.md`; `decision-log.md` |
| UREQ-002 | Option A: first Goal M0-M4/Prompt 0-21, then later Goals M5-M8 | DEC-001; `release-v0.1-contract.md` |
| UREQ-003 | Each writing task is one agent/thread, intent branch, isolated worktree and writer lease | `thread-branch-worktree-runbook.md` |
| UREQ-004 | Every material matter has independent Advisor and Kongming supervision | DEC-A10; `workflow-configuration.md` |
| UREQ-005 | Stitch plus Ant Design, distinctive professional UI informed by leading products without copying | DEC-012/025; UI/UX strategy |
| UREQ-006 | Real images/GIF/architecture diagrams plus GitHub About, Releases, Docker/GHCR, SBOM/provenance | DEC-022; documentation/distribution contract |
| UREQ-007 | Vercel/Supabase/backend continuity with no ping-hack claim | production continuity contract |
| UREQ-008 | DeepSeek RAG secret stays env-only; pasted key rotates before live use | DEC-A05/010; secret gate |
| UREQ-009 | Small complete Conventional Commits and reviewed push | DEC-A03; workflow/runbook |
| UREQ-010 | Project work and worktrees remain under `D:\Nexora` | workspace/runbook boundary |
| UREQ-011 | Apache-2.0 repository license | accepted DEC-015 |
| UREQ-012 | Formal Goal must mirror and pin the approved plan, not replace it | Goal contract template |
| UREQ-013 | Advisor and Kongming independently identify missing, important and breakthrough opportunities, while preserving Option A and preventing silent Goal expansion | `innovation-and-differentiation-backlog.md`; DEC-026; same-candidate dual-review and hook-admission gates |

## Numbered Master-Prompt Coverage

| Requirement | Source span | Source section | Goal disposition | Canonical implementation contract | Minimum evidence class |
|---|---:|---|---|---|---|
| REQ-P000 | 1-80 | NEXORA mission, required disciplines and five-star production bar | `MASTER_PROGRAM` | `outcome-contract.md`; `technology-decisions.md`; `acceptance-and-evidence.md`; all milestone quality gates | architecture-rationale, no-demo-quality and production-evidence receipt |
| REQ-S000 | 81-138 | CRITICAL EXECUTION RULES | `GLOBAL_GATE` | `phase-01-start.md`; `workflow-configuration.md` | baseline plus control-plane receipt |
| REQ-S001 | 139-173 | AUTONOMOUS ENGINEERING BEHAVIOR | `GLOBAL_GATE` | `phase-01-start.md`; `workflow-configuration.md` | baseline plus control-plane receipt |
| REQ-S002 | 174-211 | PRODUCT VISION | `MIXED_V0.1_FUTURE` | `outcome-contract.md`; `release-v0.1-contract.md`; M1-M4 core adaptive publishing/RAG; M5 flags, experiments, personalization, announcements, analytics and audit | child-level capability disposition plus M1-M4 journey evidence or later-goal task/evidence mapping |
| REQ-S003 | 212-273 | PRODUCT DIFFERENTIATOR | `INCLUDED_V0.1` | `outcome-contract.md`; `release-v0.1-contract.md`; M1-M4 UI/domain contracts | journey and persona acceptance |
| REQ-S004 | 274-345 | PRIMARY PERSONAS | `MIXED_V0.1_FUTURE` | M2 profile/CMS, M4 assistant and M5 personalization/bookmarks/topics/notifications | child-level persona capability acceptance |
| REQ-S005 | 346-397 | MULTI-TENANCY | `INCLUDED_V0.1` | `phase-05-prompt-phase-4-identity-and-tenancy.md`; `phase-06-prompt-phase-5-rbac.md` | two-tenant allow/deny receipt |
| REQ-S006 | 398-465 | HIGH-LEVEL ARCHITECTURE | `INCLUDED_V0.1` | `technology-decisions.md`; M1 foundation phases | architecture/build compatibility receipt |
| REQ-S007 | 466-513 | ARCHITECTURAL STYLE | `INCLUDED_V0.1` | `technology-decisions.md`; M1 foundation phases | architecture/build compatibility receipt |
| REQ-S008 | 514-555 | WHY GO EXISTS | `INCLUDED_V0.1` | `technology-decisions.md`; M1 foundation phases | architecture/build compatibility receipt |
| REQ-S009 | 556-620 | REPOSITORY STRUCTURE | `INCLUDED_V0.1` | `technology-decisions.md`; M1 foundation phases | architecture/build compatibility receipt |
| REQ-S010 | 621-650 | FRONTEND STACK | `INCLUDED_V0.1` | `technology-decisions.md`; M1 foundation phases | architecture/build compatibility receipt |
| REQ-S011 | 651-697 | FRONTEND APPLICATIONS | `MIXED_V0.1_FUTURE` | M1 route shell; M2/M4 included routes; M5 later routes; UI/UX strategy | child-level route ownership/auth/a11y receipt |
| REQ-S012 | 698-756 | UI/UX QUALITY BAR | `INCLUDED_V0.1` | `phase-04-prompt-phase-3-frontend-foundation.md`; `ui-ux-stitch-ant-design-strategy.md` | responsive/a11y/design-system receipt |
| REQ-S013 | 757-802 | DESIGN TOKEN SYSTEM | `INCLUDED_V0.1` | `phase-04-prompt-phase-3-frontend-foundation.md`; `ui-ux-stitch-ant-design-strategy.md` | responsive/a11y/design-system receipt |
| REQ-S014 | 803-845 | VISUAL PAGE BUILDER | `INCLUDED_V0.1` | `phase-09-prompt-phase-8-page-builder.md` | builder operation and browser receipt |
| REQ-S015 | 846-904 | COMPONENT REGISTRY | `INCLUDED_V0.1` | `phase-08-prompt-phase-7-schema-driven-ui.md` | registry/schema hostile-fixture receipt |
| REQ-S016 | 905-941 | PAGE VERSIONING | `INCLUDED_V0.1` | `phase-10-prompt-phase-9-versioning-and-publishing.md` | immutable-version/rollback receipt |
| REQ-S017 | 942-972 | CONTENT WORKFLOW | `INCLUDED_V0.1` | `phase-12-prompt-phase-11-workflow.md` | transition/permission/audit receipt |
| REQ-S018 | 973-1022 | REALTIME ARCHITECTURE | `INCLUDED_V0.1` | `phase-13-prompt-phase-12-supabase-realtime.md`; `supabase-platform-boundary.md` | private-channel/fallback receipt |
| REQ-S019 | 1023-1054 | COLLABORATIVE EDITING | `INCLUDED_V0.1` | `phase-09-prompt-phase-8-page-builder.md`; `phase-13-prompt-phase-12-supabase-realtime.md` | ephemeral-collaboration/durable-state receipt |
| REQ-S020 | 1055-1074 | DATABASE | `INCLUDED_V0.1` | M1-M4 database train; `supabase-platform-boundary.md` | migration/grant/RLS/query-plan receipt |
| REQ-S021 | 1075-1133 | CORE DATABASE ENTITIES | `MIXED_V0.1_FUTURE` | M1-M4 database train plus M5 analytics/personalization/notification/audit schemas; Supabase boundary | child-level entity/migration/RLS/query-plan receipt |
| REQ-S022 | 1134-1176 | DATABASE INDEXING | `INCLUDED_V0.1` | M1-M4 database train; `supabase-platform-boundary.md` | migration/grant/RLS/query-plan receipt |
| REQ-S023 | 1177-1204 | ROW LEVEL SECURITY | `INCLUDED_V0.1` | M1-M4 database train; `supabase-platform-boundary.md` | migration/grant/RLS/query-plan receipt |
| REQ-S024 | 1205-1230 | AUTHENTICATION | `INCLUDED_V0.1` | `phase-05-prompt-phase-4-identity-and-tenancy.md` | auth lifecycle and negative receipt |
| REQ-S025 | 1231-1292 | AUTHORIZATION | `INCLUDED_V0.1` | `phase-06-prompt-phase-5-rbac.md` | permission matrix and escalation denial |
| REQ-S026 | 1293-1326 | AUDIT LOG | `FUTURE_GOAL_WITH_EARLY_HOOK` | `phase-29-prompt-phase-28-audit.md`; early audit contracts in M2-M4 | safe-metadata audit coverage |
| REQ-S027 | 1327-1371 | KNOWLEDGE PLATFORM | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S028 | 1372-1422 | RAG PIPELINE | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S029 | 1423-1469 | DOCUMENT INGESTION | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S030 | 1470-1497 | CHUNKING STRATEGY | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S031 | 1498-1526 | EMBEDDINGS | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S032 | 1527-1555 | HYBRID SEARCH | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S033 | 1556-1579 | RERANKING | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S034 | 1580-1609 | PERMISSION-AWARE RAG | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S035 | 1610-1644 | RAG CITATIONS | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S036 | 1645-1664 | RAG FAILURE BEHAVIOR | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S037 | 1665-1691 | PROMPT INJECTION DEFENSE | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S038 | 1692-1713 | CHATBOT | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S039 | 1714-1755 | AI OBSERVABILITY | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S040 | 1756-1782 | RAG EVALUATION | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S041 | 1783-1811 | SEMANTIC SEARCH | `INCLUDED_V0.1` | `phase-16-prompt-phase-15-knowledge-management.md` through `phase-22-prompt-phase-21-rag-observability.md` | secure-RAG quality/security receipt |
| REQ-S042 | 1812-1850 | FEATURE FLAGS | `FUTURE_GOAL` | `phase-23-prompt-phase-22-feature-flags.md` through `phase-27-prompt-phase-26-recommendation.md` | M5 feature/experiment/analytics receipt |
| REQ-S043 | 1851-1876 | PROGRESSIVE ROLLOUT | `FUTURE_GOAL` | `phase-23-prompt-phase-22-feature-flags.md` through `phase-27-prompt-phase-26-recommendation.md` | M5 feature/experiment/analytics receipt |
| REQ-S044 | 1877-1919 | A/B TESTING | `FUTURE_GOAL` | `phase-23-prompt-phase-22-feature-flags.md` through `phase-27-prompt-phase-26-recommendation.md` | M5 feature/experiment/analytics receipt |
| REQ-S045 | 1920-1944 | PERSONALIZATION | `FUTURE_GOAL` | `phase-23-prompt-phase-22-feature-flags.md` through `phase-27-prompt-phase-26-recommendation.md` | M5 feature/experiment/analytics receipt |
| REQ-S046 | 1945-1984 | RECOMMENDATION ENGINE | `FUTURE_GOAL` | `phase-23-prompt-phase-22-feature-flags.md` through `phase-27-prompt-phase-26-recommendation.md` | M5 feature/experiment/analytics receipt |
| REQ-S047 | 1985-2009 | EVENT ANALYTICS | `FUTURE_GOAL` | `phase-23-prompt-phase-22-feature-flags.md` through `phase-27-prompt-phase-26-recommendation.md` | M5 feature/experiment/analytics receipt |
| REQ-S048 | 2010-2045 | EVENT PIPELINE | `FUTURE_GOAL_WITH_EARLY_HOOK` | `phase-25-prompt-phase-24-analytics.md`; M3 event contract | end-to-end event provenance |
| REQ-S049 | 2046-2084 | TRANSACTIONAL OUTBOX | `INCLUDED_V0.1` | `phase-15-prompt-phase-14-transactional-outbox.md`; M3 consumer | atomicity/idempotency/replay receipt |
| REQ-S050 | 2085-2099 | IDEMPOTENCY | `INCLUDED_V0.1` | `phase-15-prompt-phase-14-transactional-outbox.md`; M3 consumer | atomicity/idempotency/replay receipt |
| REQ-S051 | 2100-2137 | NOTIFICATIONS | `FUTURE_GOAL_WITH_EARLY_HOOK` | `phase-28-prompt-phase-27-notifications.md`; M3 vocabulary only | persist-before-deliver/DLQ receipt |
| REQ-S052 | 2138-2162 | EMAIL / WEBHOOK WORKER | `FUTURE_GOAL_WITH_EARLY_HOOK` | `phase-28-prompt-phase-27-notifications.md`; M3 vocabulary only | persist-before-deliver/DLQ receipt |
| REQ-S053 | 2163-2198 | API DESIGN | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S054 | 2199-2224 | API RESPONSE MODEL | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S055 | 2225-2240 | PAGINATION | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S056 | 2241-2257 | INPUT VALIDATION | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S057 | 2258-2279 | CONCURRENCY CONTROL | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S058 | 2280-2307 | CACHING | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S059 | 2308-2334 | RATE LIMITING | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S060 | 2335-2360 | SECURITY BASELINE | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S061 | 2361-2381 | XSS | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S062 | 2382-2401 | SSRF | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S063 | 2402-2421 | FILE UPLOAD SECURITY | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S064 | 2422-2447 | SECRETS | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S065 | 2448-2465 | DATABASE MIGRATIONS | `GLOBAL_GATE` | `requirements-matrix.md`; phase-local API/security/migration gates | phase-local negative and contract tests |
| REQ-S066 | 2466-2555 | OBSERVABILITY | `FUTURE_GOAL_WITH_EARLY_BASELINE` | `phase-32-prompt-phase-31-observability.md`; `phase-33-prompt-phase-32-performance.md`; service-local baselines | measured telemetry/SLO/load receipt |
| REQ-S067 | 2556-2593 | GRAFANA | `FUTURE_GOAL_WITH_EARLY_BASELINE` | `phase-32-prompt-phase-31-observability.md`; `phase-33-prompt-phase-32-performance.md`; service-local baselines | measured telemetry/SLO/load receipt |
| REQ-S068 | 2594-2616 | HEALTH CHECKS | `FUTURE_GOAL_WITH_EARLY_BASELINE` | `phase-32-prompt-phase-31-observability.md`; `phase-33-prompt-phase-32-performance.md`; service-local baselines | measured telemetry/SLO/load receipt |
| REQ-S069 | 2617-2654 | SLOS | `FUTURE_GOAL_WITH_EARLY_BASELINE` | `phase-32-prompt-phase-31-observability.md`; `phase-33-prompt-phase-32-performance.md`; service-local baselines | measured telemetry/SLO/load receipt |
| REQ-S070 | 2655-2680 | PERFORMANCE TESTING | `FUTURE_GOAL_WITH_EARLY_BASELINE` | `phase-32-prompt-phase-31-observability.md`; `phase-33-prompt-phase-32-performance.md`; service-local baselines | measured telemetry/SLO/load receipt |
| REQ-S071 | 2681-2714 | JAVA CODING STANDARDS | `INCLUDED_V0.1` | `phase-03-prompt-phase-2-java-platform-foundation.md`; Java feature phases | build/static/integration receipt |
| REQ-S072 | 2715-2740 | JAVA PERSISTENCE | `INCLUDED_V0.1` | `phase-03-prompt-phase-2-java-platform-foundation.md`; Java feature phases | build/static/integration receipt |
| REQ-S073 | 2741-2770 | GO CODING STANDARDS | `INCLUDED_V0.1` | `phase-14-prompt-phase-13-go-event-ingestion.md` | Go correctness/load/failure receipt |
| REQ-S074 | 2771-2794 | GO HTTP SERVICE | `INCLUDED_V0.1` | `phase-14-prompt-phase-13-go-event-ingestion.md` | Go correctness/load/failure receipt |
| REQ-S075 | 2795-2815 | FRONTEND PERFORMANCE | `INCLUDED_V0.1` | `phase-04-prompt-phase-3-frontend-foundation.md`; `ui-ux-stitch-ant-design-strategy.md` | bundle/CWV/cache receipt |
| REQ-S076 | 2816-2832 | SEO | `INCLUDED_V0.1` | CMS/publishing SEO contract in M2 | metadata/canonical/sitemap/structured-data E2E |
| REQ-S077 | 2833-2845 | CONTENT PREVIEW | `INCLUDED_V0.1` | `phase-10-prompt-phase-9-versioning-and-publishing.md`; workflow | preview/publish/schedule/rollback receipt |
| REQ-S078 | 2846-2873 | PUBLISHING | `INCLUDED_V0.1` | `phase-10-prompt-phase-9-versioning-and-publishing.md`; workflow | preview/publish/schedule/rollback receipt |
| REQ-S079 | 2874-2890 | SCHEDULED PUBLISHING | `INCLUDED_V0.1` | `phase-10-prompt-phase-9-versioning-and-publishing.md`; workflow | preview/publish/schedule/rollback receipt |
| REQ-S080 | 2891-2906 | FEATURE FLAG + PAGE BUILDER INTEGRATION | `FUTURE_GOAL` | M5 flags/personalization/analytics phase contracts | M5 deterministic assignment/dashboard receipt |
| REQ-S081 | 2907-2924 | PERSONALIZATION + PAGE BUILDER | `FUTURE_GOAL` | M5 flags/personalization/analytics phase contracts | M5 deterministic assignment/dashboard receipt |
| REQ-S082 | 2925-2947 | ADMIN ANALYTICS | `FUTURE_GOAL` | M5 flags/personalization/analytics phase contracts | M5 deterministic assignment/dashboard receipt |
| REQ-S083 | 2948-2983 | BACKGROUND JOBS | `GLOBAL_GATE` | service/job/error/resilience/provider/cost phase-local contracts | fault/budget/degraded-state receipt |
| REQ-S084 | 2984-3001 | ERROR HANDLING | `GLOBAL_GATE` | service/job/error/resilience/provider/cost phase-local contracts | fault/budget/degraded-state receipt |
| REQ-S085 | 3002-3019 | RESILIENCE | `GLOBAL_GATE` | service/job/error/resilience/provider/cost phase-local contracts | fault/budget/degraded-state receipt |
| REQ-S086 | 3020-3037 | AI PROVIDER ABSTRACTION | `GLOBAL_GATE` | service/job/error/resilience/provider/cost phase-local contracts | fault/budget/degraded-state receipt |
| REQ-S087 | 3038-3056 | COST CONTROLS | `GLOBAL_GATE` | service/job/error/resilience/provider/cost phase-local contracts | fault/budget/degraded-state receipt |
| REQ-S088 | 3057-3122 | TESTING STRATEGY | `GLOBAL_GATE` | `acceptance-and-evidence.md`; phase-local test matrices | unit/integration/API/E2E/security/architecture/contract receipt |
| REQ-S089 | 3123-3141 | SECURITY TESTS | `GLOBAL_GATE` | `acceptance-and-evidence.md`; phase-local test matrices | unit/integration/API/E2E/security/architecture/contract receipt |
| REQ-S090 | 3142-3159 | ARCHITECTURE TESTS | `GLOBAL_GATE` | `acceptance-and-evidence.md`; phase-local test matrices | unit/integration/API/E2E/security/architecture/contract receipt |
| REQ-S091 | 3160-3181 | CONTRACT TESTING | `GLOBAL_GATE` | `acceptance-and-evidence.md`; phase-local test matrices | unit/integration/API/E2E/security/architecture/contract receipt |
| REQ-S092 | 3182-3208 | LOCAL DEVELOPMENT | `INCLUDED_V0.1` | `phase-02-prompt-phase-1-repository-foundation.md` | fresh-clone/local parity receipt |
| REQ-S093 | 3209-3226 | DOCKER | `INCLUDED_V0.1_AND_M7` | M1 Compose baseline; `phase-34-prompt-phase-33-containerization.md` | local health plus immutable-image receipt |
| REQ-S094 | 3227-3256 | KUBERNETES | `FUTURE_GOAL` | `phase-35-prompt-phase-34-kubernetes.md` through `phase-40-prompt-phase-39-disaster-recovery.md` | render/deploy/provenance/restore receipt |
| REQ-S095 | 3257-3270 | HELM | `FUTURE_GOAL` | `phase-35-prompt-phase-34-kubernetes.md` through `phase-40-prompt-phase-39-disaster-recovery.md` | render/deploy/provenance/restore receipt |
| REQ-S096 | 3271-3291 | TERRAFORM | `FUTURE_GOAL` | `phase-35-prompt-phase-34-kubernetes.md` through `phase-40-prompt-phase-39-disaster-recovery.md` | render/deploy/provenance/restore receipt |
| REQ-S097 | 3292-3319 | GITOPS | `FUTURE_GOAL` | `phase-35-prompt-phase-34-kubernetes.md` through `phase-40-prompt-phase-39-disaster-recovery.md` | render/deploy/provenance/restore receipt |
| REQ-S098 | 3320-3354 | CI PIPELINE | `FUTURE_GOAL` | `phase-35-prompt-phase-34-kubernetes.md` through `phase-40-prompt-phase-39-disaster-recovery.md` | render/deploy/provenance/restore receipt |
| REQ-S099 | 3355-3368 | SUPPLY CHAIN SECURITY | `FUTURE_GOAL` | `phase-35-prompt-phase-34-kubernetes.md` through `phase-40-prompt-phase-39-disaster-recovery.md` | render/deploy/provenance/restore receipt |
| REQ-S100 | 3369-3385 | ENVIRONMENTS | `FUTURE_GOAL` | `phase-35-prompt-phase-34-kubernetes.md` through `phase-40-prompt-phase-39-disaster-recovery.md` | render/deploy/provenance/restore receipt |
| REQ-S101 | 3386-3401 | BACKUP AND RECOVERY | `FUTURE_GOAL` | `phase-35-prompt-phase-34-kubernetes.md` through `phase-40-prompt-phase-39-disaster-recovery.md` | render/deploy/provenance/restore receipt |
| REQ-S102 | 3402-3415 | DATA RETENTION | `GLOBAL_GATE` | DEC-016 lifecycle/privacy contract; security/DR phases | export/deletion/anonymization/retention receipt |
| REQ-S103 | 3416-3430 | PRIVACY | `GLOBAL_GATE` | DEC-016 lifecycle/privacy contract; security/DR phases | export/deletion/anonymization/retention receipt |
| REQ-S104 | 3431-3463 | DOCUMENTATION | `FUTURE_GOAL_WITH_RELEASE_EVIDENCE` | `documentation-media-and-github-release.md`; M4 prerelease evidence; M8 final docs | exact-SHA reproducibility/media receipt |
| REQ-S105 | 3464-3493 | ADRS | `FUTURE_GOAL_WITH_RELEASE_EVIDENCE` | `documentation-media-and-github-release.md`; M4 prerelease evidence; M8 final docs | exact-SHA reproducibility/media receipt |
| REQ-S106 | 3494-3528 | DIAGRAMS | `FUTURE_GOAL_WITH_RELEASE_EVIDENCE` | `documentation-media-and-github-release.md`; M4 prerelease evidence; M8 final docs | exact-SHA reproducibility/media receipt |
| REQ-S107 | 3529-3578 | README QUALITY | `FUTURE_GOAL_WITH_RELEASE_EVIDENCE` | `documentation-media-and-github-release.md`; M4 prerelease evidence; M8 final docs | exact-SHA reproducibility/media receipt |
| REQ-S108 | 3579-3622 | DEMO DATA | `INCLUDED_V0.1` | deterministic demo seed pack in M1-M4 | seed checksum/reset/no-secret receipt |
| REQ-S109 | 3623-3646 | DEMO FLOW #1 — PAGE BUILDER | `INCLUDED_V0.1` | M2 builder/theme plus M4 RAG demo journeys | Playwright/API exact-head journey receipt |
| REQ-S110 | 3647-3662 | DEMO FLOW #2 — THEME | `INCLUDED_V0.1` | M2 builder/theme plus M4 RAG demo journeys | Playwright/API exact-head journey receipt |
| REQ-S111 | 3663-3690 | DEMO FLOW #3 — RAG | `INCLUDED_V0.1` | M2 builder/theme plus M4 RAG demo journeys | Playwright/API exact-head journey receipt |
| REQ-S112 | 3691-3716 | DEMO FLOW #4 — SECURE RAG | `INCLUDED_V0.1` | M2 builder/theme plus M4 RAG demo journeys | Playwright/API exact-head journey receipt |
| REQ-S113 | 3717-3738 | DEMO FLOW #5 — FEATURE FLAG | `FUTURE_GOAL` | M5/M6 demo journeys | flags/experiment/observability evidence |
| REQ-S114 | 3739-3759 | DEMO FLOW #6 — A/B TEST | `FUTURE_GOAL` | M5/M6 demo journeys | flags/experiment/observability evidence |
| REQ-S115 | 3760-3776 | DEMO FLOW #7 — OBSERVABILITY | `FUTURE_GOAL` | M5/M6 demo journeys | flags/experiment/observability evidence |
| REQ-S116 | 3777-3797 | NON-FUNCTIONAL REQUIREMENTS | `GLOBAL_GATE` | `outcome-contract.md`; `workflow-configuration.md`; `acceptance-and-evidence.md` | quality/commit/usable-mainline receipt |
| REQ-S117 | 3798-3820 | ANTI-GOALS | `GLOBAL_GATE` | `outcome-contract.md`; `workflow-configuration.md`; `acceptance-and-evidence.md` | quality/commit/usable-mainline receipt |
| REQ-S118 | 3821-3860 | ENGINEERING PRINCIPLE | `GLOBAL_GATE` | `outcome-contract.md`; `workflow-configuration.md`; `acceptance-and-evidence.md` | quality/commit/usable-mainline receipt |
| REQ-S119 | 3861-3885 | GIT WORKFLOW | `GLOBAL_GATE` | `outcome-contract.md`; `workflow-configuration.md`; `acceptance-and-evidence.md` | quality/commit/usable-mainline receipt |
| REQ-S120 | 3886-3959 | COMMIT RULES — ABSOLUTELY IMPORTANT | `GLOBAL_GATE` | `outcome-contract.md`; `workflow-configuration.md`; `acceptance-and-evidence.md` | quality/commit/usable-mainline receipt |
| REQ-S121 | 3960-3985 | COMMIT SIZE | `GLOBAL_GATE` | `outcome-contract.md`; `workflow-configuration.md`; `acceptance-and-evidence.md` | quality/commit/usable-mainline receipt |
| REQ-S122 | 3986-4001 | COMMIT QUALITY | `GLOBAL_GATE` | `outcome-contract.md`; `workflow-configuration.md`; `acceptance-and-evidence.md` | quality/commit/usable-mainline receipt |
| REQ-S123 | 4002-4015 | DO NOT COMMIT BROKEN MAINLINE | `GLOBAL_GATE` | `outcome-contract.md`; `workflow-configuration.md`; `acceptance-and-evidence.md` | quality/commit/usable-mainline receipt |
| REQ-S124 | 4016-4625 | PHASED IMPLEMENTATION PLAN | `MASTER_PROGRAM` | `requirements-matrix.md`; all `phase-*.md` files | phase coverage and dependency receipt |
| REQ-S125 | 4626-4659 | DEFINITION OF DONE FOR EVERY FEATURE | `GLOBAL_GATE` | `acceptance-and-evidence.md`; phase-local review checklists | independent exact-head review receipt |
| REQ-S126 | 4660-4703 | CODE REVIEW SELF-CHECK | `GLOBAL_GATE` | `acceptance-and-evidence.md`; phase-local review checklists | independent exact-head review receipt |
| REQ-S127 | 4704-4721 | DATABASE REVIEW SELF-CHECK | `GLOBAL_GATE` | `acceptance-and-evidence.md`; phase-local review checklists | independent exact-head review receipt |
| REQ-S128 | 4722-4738 | API REVIEW SELF-CHECK | `GLOBAL_GATE` | `acceptance-and-evidence.md`; phase-local review checklists | independent exact-head review receipt |
| REQ-S129 | 4739-4753 | RAG REVIEW SELF-CHECK | `GLOBAL_GATE` | `acceptance-and-evidence.md`; phase-local review checklists | independent exact-head review receipt |
| REQ-S130 | 4754-4770 | FRONTEND REVIEW SELF-CHECK | `GLOBAL_GATE` | `acceptance-and-evidence.md`; phase-local review checklists | independent exact-head review receipt |
| REQ-S131 | 4771-4788 | DEVOPS REVIEW SELF-CHECK | `GLOBAL_GATE` | `acceptance-and-evidence.md`; phase-local review checklists | independent exact-head review receipt |
| REQ-S132 | 4789-4903 | FINAL ACCEPTANCE CRITERIA | `MASTER_PROGRAM` | `plan.md`; `release-v0.1-contract.md`; M5-M8 contracts | scenario/failure/source-of-truth/final acceptance |
| REQ-S133 | 4904-4955 | PORTFOLIO QUALITY | `MASTER_PROGRAM` | `plan.md`; `release-v0.1-contract.md`; M5-M8 contracts | scenario/failure/source-of-truth/final acceptance |
| REQ-S134 | 4956-4985 | SCALING DISCUSSION TO DOCUMENT | `MASTER_PROGRAM` | `plan.md`; `release-v0.1-contract.md`; M5-M8 contracts | scenario/failure/source-of-truth/final acceptance |
| REQ-S135 | 4986-5002 | POSSIBLE FUTURE SERVICE EXTRACTION | `MASTER_PROGRAM` | `plan.md`; `release-v0.1-contract.md`; M5-M8 contracts | scenario/failure/source-of-truth/final acceptance |
| REQ-S136 | 5003-5035 | PRODUCTION FAILURE SCENARIOS | `MASTER_PROGRAM` | `plan.md`; `release-v0.1-contract.md`; M5-M8 contracts | scenario/failure/source-of-truth/final acceptance |
| REQ-S137 | 5036-5056 | SOURCE OF TRUTH PRINCIPLE | `MASTER_PROGRAM` | `plan.md`; `release-v0.1-contract.md`; M5-M8 contracts | scenario/failure/source-of-truth/final acceptance |
| REQ-S138 | 5057-5081 | FINAL BUILD STANDARD | `MASTER_PROGRAM` | `plan.md`; `release-v0.1-contract.md`; M5-M8 contracts | scenario/failure/source-of-truth/final acceptance |
| REQ-S139 | 5082-5169 | FINAL INSTRUCTION TO THE AGENT | `MASTER_PROGRAM` | `plan.md`; `release-v0.1-contract.md`; M5-M8 contracts | scenario/failure/source-of-truth/final acceptance |

## Pre-Goal Expansion Receipt — C0-01R

The branch-owned semantic artifact is `master-requirements-catalog-expanded.md`. It is created only by C0-01R on `docs/c0-requirements-catalog` in an ignored project-local worktree with an active ledger lease; the seed-bootstrap exception cannot create or edit it. The generated child catalog must include: logical source ID/hash; parser/tool version; exact line classifier over lines `1..5169`; stable parent/child IDs; duplicate/defer rationale; phase/task/owned-path mapping; positive, negative, failure and evidence tests; decision dependencies; Goal disposition; and an unclassified-line report. Advisor reviews outcome completeness and usability; Kongming independently attacks omission, ambiguous modality, circular evidence and scope laundering. Controller may mark the exact catalog head `MERGE_READY` only when both review the same digest and every finding is disposed; Git Manager then integrates it mechanically before the final Goal pin.

## Change Control

- Editing the master prompt, this parent catalog, a child mapping or any acceptance meaning is semantic.
- Append-only validation/reviewer receipts are evidence and stay outside the semantic digest, but each receipt pins the semantic candidate.
- A new user request is first entered as `UREQ-*`, then mapped to decision/task/evidence before dispatch.
- A requirement outside the current finite Goal remains visible as `FUTURE_GOAL`; it is not counted complete.
- A requirement without owner, evidence and disposition blocks the dependent task; any silent omission is `STOP`.
