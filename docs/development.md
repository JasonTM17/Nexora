# Development baseline

This document records repository-level compatibility constraints only. It does
not install dependencies or imply that an application runtime exists yet.

## Toolchain pins

| Tool | Pin | Owner | Evidence boundary |
|---|---|---|---|
| Node.js | 24.12.0 | M1-T01 declaration; M1-DW01 manifests | Frozen install and workspace compatibility |
| pnpm | 11.0.9 | M1-T01 declaration; M1-DW01 dependency window | Lockfile and package provenance |
| Java | 25.0.1 (LTS-compatible) | M1-T01 declaration; M1-T02 runtime | Spring/Maven compatibility and tests |
| Go | 1.26.5 | M1-T01 declaration; M3-T04 service | Module and event-ingestion validation |

## Framework boundary

The approved v0.1 direction is Next.js 16 with React 19 and strict TypeScript,
Spring Boot 4.1 on Java 25, PostgreSQL 17 compatibility, and Go 1.26 for the
later bounded event-ingestion service. M1-T01 records the compatibility line;
the owning implementation tasks must verify exact patch versions, dependency
licenses, security advisories, and runtime behavior.

## Ownership seams

M1-T01 owns non-Node governance, Compose, Makefile, CI, tool pins, the license,
provenance boundary, and directory skeleton. M1-DW01 alone owns the Node
dependency window: root `package.json`, `pnpm-workspace.yaml`,
`pnpm-lock.yaml`, `.npmrc`, and workspace `package.json` files. Application
workers own source, migrations, and runtime configuration in later tasks.
