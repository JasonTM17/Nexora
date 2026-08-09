---
phase: 3
title: "Prompt Phase 2 — Java Platform Foundation"
status: pending
priority: P1
effort: "3-5 days"
dependencies: [2]
---

# Prompt Phase 2 — Java Platform Foundation

## Outcome

Create the Spring Boot modular-monolith platform boundary with reproducible persistence, stable API errors, health and telemetry foundations.

## Architecture

- `apps/platform-api` owns domain APIs and authorization.
- Package-by-feature/modules; enforce boundaries with architecture tests where practical.
- PostgreSQL/Supabase-compatible SQL and versioned migrations are authoritative.
- Stable error envelope includes safe code/message/details and trace identifier.
- Liveness is process health; readiness checks critical dependencies.

## Implementation Slices

1. Build/toolchain and module/package conventions.
2. Profiles and typed configuration without embedded credentials.
3. Database connection, migration baseline and integration test environment.
4. Global validation/error contract and OpenAPI generation.
5. Structured logging, request correlation, metrics/tracing baseline.
6. Health/readiness and graceful shutdown.

## Planned Paths

The Java platform worker owns `apps/platform-api/**` and its Java-specific CI slice, excluding migration paths. The database foundation owner alone owns `database/migrations/**`. M1-T04 owns generated/shared source under `packages/contracts/**` except `packages/contracts/package.json`; M1-DW01 alone owns that Node dependency manifest and the workspace lockfile. Migrations, generated contracts and dependency controls each have one distinct writer.

## Tests and Evidence

- Context/startup, configuration validation and architecture-boundary tests.
- Migration from empty database plus rollback/forward policy evidence.
- API validation/error contract tests; no stack/internal leakage.
- Health/readiness behavior with dependency available/unavailable.
- Trace/log correlation with safe metadata.
- OpenAPI and generated-contract drift check.

## Commit Plan

- `chore(api): initialize Spring platform application`
- `feat(api): add stable validation and error contract`
- `feat(observability): add platform health and telemetry baseline`

## Acceptance

- [ ] Application starts with documented local profile.
- [ ] Empty database migrates successfully.
- [ ] OpenAPI, health, logs and trace IDs are observable.
- [ ] No service-role/provider secret reaches logs or committed config.
- [ ] Build and integration tests pass at exact head.

## Stop Conditions

Unpinned/incompatible toolchain, destructive baseline migration, sensitive payload logging, readiness that reports healthy while DB is unavailable.
