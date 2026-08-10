# Nexora

Nexora is a planned, polyglot monorepo for a tenant-aware CMS and knowledge
workspace. This branch implements the M1-T01 repository foundation only.

## Implemented now

- Apache-2.0 repository license and third-party provenance boundary.
- Non-Node monorepo skeleton under `apps/`, `services/`, `packages/`,
  `database/`, `infrastructure/`, `observability/`, and `docs/`.
- Reproducible toolchain declarations for Node, pnpm, Java, and Go.
- Loopback-only local PostgreSQL and NATS dependency Compose with health checks
  and named volumes.
- Cross-platform validation entrypoints and a pinned GitHub Actions baseline.
- `.env.local`, `engineer/`, `.worktrees/`, and AgentKit runtime state remain
  ignored.

## Planned, not implemented

Application modules, Node package manifests and lockfiles, database migrations,
provider configuration, secrets, deployment, production observability, and
runtime readiness belong to later owned tasks. The empty directories are
layout markers, not working application features.

## Local commands

Run `make help` (or inspect `Makefile` on systems without Make) to see the
canonical commands. `pwsh ./tools/validate-repo.ps1` performs deterministic
repository checks, and `docker compose config --quiet` validates the local
dependency definition without starting services. `make compose-health` starts
the loopback-only dependencies and waits for their health checks.

The first public push, remote configuration changes, provider calls, release,
provisioning, and deployment are intentionally outside this branch.
