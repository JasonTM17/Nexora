# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

#### Milestone M5 — Adaptive Intelligence
- **Feature flags**: tenant-scoped, deterministic per-subject rollout via SHA-256 hash bucket
- **Product analytics**: event recording with idempotency, aggregation queries, cursor pagination
- **Notifications**: tenant/user-scoped with TTL, multi-channel delivery, read tracking
- **Experiments**: A/B experimentation with deterministic variant assignment
- **Global search**: authorized hybrid search over published pages and active knowledge
- **Admin UI**: feature flags, analytics dashboard, notifications inbox, experiments management

#### Milestone M6 — Observability + Security
- **Observability stack**: Prometheus (metrics), Loki (logs), Tempo (traces), Grafana (dashboards)
- **Service metrics**: Spring `/actuator/prometheus`, Go `/metrics`, Next.js Web Vitals
- **Structured logging**: JSON format for production (Loki ingestion), human-readable for local
- **Redis-backed rate limiter**: sliding-window for multi-replica Go ingestion (fail-closed)
- **JaCoCo coverage**: Java coverage reporting via Maven

#### CI/CD + Security
- **Docker Hub publish**: 3 images (web, platform-api, event-ingestion) with Trivy scan, SBOM, provenance
- **Security pipeline**: CodeQL SAST (Java/Go/TS), Gitleaks secret scan, Trivy filesystem scan
- **Dependabot**: automated updates for npm, Go, GitHub Actions, Docker
- **Web Dockerfile**: multi-stage, non-root, HEALTHCHECK, OCI labels
- **CODEOWNERS**, **SECURITY.md**, **.gitleaks.toml**, **.dockerignore**

#### Documentation
- **ADRs**: M1 platform architecture, M2 tenant isolation, M4 knowledge/RAG, JWT signing approach
- **Per-service READMEs**: apps/web, packages/design-tokens, ui-core, ui-studio, ui-ai, ui-builder
- **Full-program plan**: `plans/260815-0935-nexora-full-program-m4-m8/`

### Database
- Migrations `V025`–`V028`: feature flags, analytics events, notifications, experiments

### Changed
- **README**: updated to reflect M0–M5 implementation, M6 in progress, current limitations
- **next.config.ts**: added `output: "standalone"` for containerized deployment
- **CI**: `validate.yml` now runs the web unit test suite (vitest) and the contracts fixture tests (node --test) on every push and pull request

### Security
- 4-layer CI security pipeline (Trivy + CodeQL + Gitleaks + SBOM)
- Redis-backed rate limiter replaces single-instance in-memory when configured
- Cleared blocking Trivy findings: event-ingestion golang.org/x/crypto v0.52.0 / x/sys v0.45.0 / go-redis v9.7.3 (CVE-2025-29923, CVE-2026-398xx, CVE-2026-42508, CVE-2026-465xx), platform-api Spring Boot 4.0.4 (CVE-2026-22731/22733) and PostgreSQL driver 42.7.12 (CVE-2026-42198/54291); GO-2026-5932 suppressed in `.trivyignore` as an unimported openpgp module-level advisory
- Re-pinned CI actions to Node 24 runtimes (checkout v7.0.1, setup-buildx v4.2.0, build-push v7.3.0, CodeQL v4.37.7, gitleaks v3.0.0) ahead of the September 2026 Node 20 runner removal
- Fixed the CodeQL implicit-cast-in-compound-assignment finding in `DeterministicEmbeddingProvider`; dismissed the two `spring-disabled-csrf-protection` findings as not applicable (stateless JWT bearer API, no cookie-based authentication)

## [v0.1.0-alpha.1] — 2026-08-15

### Added
- Milestones M0–M4: repository foundation, platform API, web foundation, tenant CMS core,
  durable event outbox, bounded Go ingress, private Realtime descriptors, knowledge management
  and secure RAG
