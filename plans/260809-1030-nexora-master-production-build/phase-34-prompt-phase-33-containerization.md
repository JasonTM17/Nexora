---
phase: 34
title: "Prompt Phase 33 — Containerization"
status: pending
priority: P1
effort: "4-7 days"
dependencies: [3, 4, 14, 28, 31, 32]
---

# Prompt Phase 33 — Containerization

## Outcome

Produce reproducible minimal non-root production images tied to exact source, health behavior, vulnerability scans, SBOM/provenance and GHCR identities.

## Requirements

- Multi-stage builds, pinned reviewed base images/digests where practical.
- Non-root runtime, read-only/minimal filesystem and explicit writable paths.
- No build secrets copied into layers/history.
- Correct signal handling, graceful shutdown and health endpoints.
- Deterministic image tags plus immutable digest provenance.
- OCI source/description/license/revision labels and repository-linked GHCR packages.
- Tested architecture matrix; never advertise an untested multi-architecture image.
- SPDX SBOM, build provenance, GitHub artifact attestation where supported, and vulnerability/license scan severity policy.
- Build secrets use secret mounts; build arguments and attestation metadata contain no credential.

## Planned Ownership

Per-service Dockerfiles, `.dockerignore`, container test scripts, image CI and provenance manifests. Shared base-image policy and root build scripts have one owner.

## Validation

- Build from clean source and approved toolchain.
- Image history/content secret scan.
- Run as non-root, health/readiness, shutdown and dependency failure.
- Scan and SBOM correspond to exact image digest/commit.
- Push candidate tags to GHCR from least-privilege CI, pull by digest, and verify provenance/attestation.
- Size/runtime-content review and architecture target where relevant.

## Commit Plan

- `build(containers): add production service images`
- `test(containers): verify runtime health and shutdown`
- `chore(security): attach image scans and SBOMs`
- `ci(packages): publish attested images to ghcr`

## Acceptance

- [ ] Every deployable has an exact digest and linked evidence.
- [ ] Critical exploitable findings block or have time-bounded accepted exception.
- [ ] No secret exists in image/layer/SBOM/report.
- [ ] Local container stack runs documented critical scenario.
- [ ] GitHub package metadata links source/license/description and a clean environment can pull and run the documented digest.

## Stop Conditions

Root runtime without justification, floating unreviewed base, secret in layer/build arguments/attestation, scan/SBOM not tied to deployed digest, unverifiable GHCR package, health check masks failure.
