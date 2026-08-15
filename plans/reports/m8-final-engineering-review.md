# M8 Final Engineering Review — Nexora

> Staff Engineer candidate review across 8 tracks.
> Date: 2026-08-15. Scope: M0–M6 implemented, M7 CI prep, M8 in progress.

## Verdict

**Production-shaped preview, ready for hardening.** M0–M5 are genuinely complete
with passing tests, ADRs, and documentation. M6 observability and partial security
hardening are in place. The codebase demonstrates disciplined tenant isolation,
evidence-bounded claims, and honest limitations. Remaining work (M6 JWT cutover,
M8 media suite, infrastructure/K8s) is real but bounded.

---

## Track 1: Architecture / Domain Boundaries

### Findings
- **Modular monolith well-structured**: identity, tenant, RBAC, CMS, events, knowledge,
  RAG, feature flags, analytics, notifications, experiments, search — each in its own
  package with clear ownership.
- **Trust boundary enforced**: browser → BFF → Spring → PostgreSQL. Direct browser-to-Spring
  and browser-held credentials are not in the target path.
- **Go edge isolates untrusted HTTP**: bounded admission with fail-closed semantics.

### Residual risk
- `infrastructure/` is empty (K8s deferred). Acceptable for v0.1 but needs a tracked plan.

### Grade: PASS

---

## Track 2: Data / Tenancy / Security

### Findings
- **Three-layer tenant isolation**: RLS (DB), tenant context (service), JWT + header (API).
  Cross-tenant access impossible by default.
- **RLS on every application relation** with `nexora_runtime` non-owner role.
- **Permission-before-context RAG**: authorization filter before LLM context construction.
- **Redis-backed rate limiter** for multi-replica Go ingestion.
- **Account lockout** schema + service for failed auth tracking.

### Residual risk
- JWT still uses HS256 in dev (Ed25519 migration planned, auth is external Supabase).
- Account lockout is application-layer only (can't enforce at identity provider level).

### Grade: PASS (with noted improvements)

---

## Track 3: Frontend / Product / Accessibility

### Findings
- **UI vocabulary consistent**: `gradient-text`, `bg-grid`, `shadow-glow`, `animate-fade-in-up`,
  `nx-*` prefixed classes across all surfaces.
- **Complete states**: loading, empty, denied, error on every surface.
- **Ant Design wrapper boundary** owned by `packages/ui-core`, not leaked to consumers.
- **Same-origin BFF** with generated client (zero drift).

### Residual risk
- No automated accessibility audit (axe-core in devDeps but no CI gate).
- Web vitest jsdom failures (pre-existing, documented).

### Grade: PASS (a11y audit recommended)

---

## Track 4: AI / RAG Quality / Privacy

### Findings
- **Evidence-before-assertion**: no answer generated without authorized sources.
- **Permission-before-context**: RLS + authorization filter before retrieval candidates
  enter LLM context.
- **Deterministic evaluation mode**: reproducible local evidence without providers.
- **Hybrid retrieval**: lexical (ts_rank) + vector (pgvector) with RRF fusion.
- **Citations resolvable only to authorized sources**.

### Residual risk
- No live provider evaluation (deterministic only). Live quality is a funded decision.

### Grade: PASS

---

## Track 5: Async / Reliability

### Findings
- **Transactional outbox** before NATS publish (durable truth in PostgreSQL).
- **Idempotent persistence consumer** with duplicate/replay handling.
- **Go admission fail-closed** unless both ADMISSION_URL and NATS_URL configured.
- **NATS JetStream** with file-backed stream provisioning.

### Residual risk
- JetStream file-backed (not clustered). Acceptable for local; production needs
  replication factor 3 (deferred to M7 infra).

### Grade: PASS

---

## Track 6: Observability / Performance

### Findings
- **Prometheus metrics** on all 3 services (`/actuator/prometheus`, `/metrics`).
- **Structured ECS JSON logging** for Loki ingestion.
- **Tempo tracing** with OTLP gRPC/HTTP.
- **Grafana dashboards** with datasource auto-provisioning.
- **JaCoCo** coverage for Java, `go -cover` for Go.

### Residual risk
- Observability stack configured but not yet validated with live traffic.
- No performance budget enforcement (LHCI not wired).

### Grade: PASS (validation pending)

---

## Track 7: Supply Chain / Deployment / DR

### Findings
- **4-layer CI security**: Trivy + CodeQL + Gitleaks + SBOM.
- **Docker Hub publish workflow** for 3 images with provenance + SBOM.
- **Dependabot** for npm, Go, Actions, Docker.
- **Multi-stage Dockerfiles** with non-root users and HEALTHCHECK.
- **CODEOWNERS**, **SECURITY.md**, **.gitleaks.toml**.

### Residual risk
- No disaster recovery drill (deferred to M7 infra with real deployment target).
- No Kubernetes manifests (deferred pending hosting decisions).

### Grade: PASS (DR deferred)

---

## Track 8: Developer Experience / Docs / Maintainability

### Findings
- **5 ADRs** covering M1, M2, M3, M4, JWT signing.
- **Per-service READMEs** with 6-section structure (purpose, API, env, run, test, runbook).
- **CHANGELOG** in Keep a Changelog format.
- **Generated client** from OpenAPI (zero drift).
- **Media manifest** mapping captures to release SHA.

### Residual risk
- Web surfaces still use deterministic fixtures (no live provider).

### Grade: PASS

---

## Summary

| Track | Grade | Key improvement |
|---|---|---|
| 1. Architecture | PASS | Track infra plan |
| 2. Data/Security | PASS | JWT Ed25519, a11y audit |
| 3. Frontend | PASS | axe-core CI gate |
| 4. AI/RAG | PASS | Live provider eval (funded) |
| 5. Async | PASS | JetStream clustering (M7) |
| 6. Obs/Perf | PASS | Validate with live traffic |
| 7. Supply-chain | PASS | DR drill (M7) |
| 8. DX/Docs | PASS | — |

## Recommended next steps

1. **M6 finish**: JWT Ed25519 migration plan (3-phase protocol).
2. **M8 finish**: Run accessibility audit, produce media captures.
3. **M7 infra**: Hosting decisions → K8s manifests → DR drill.
4. **Release**: v0.1.0-alpha.1 → v0.1.0 (M5) → v0.2.0 (M6) → production.
