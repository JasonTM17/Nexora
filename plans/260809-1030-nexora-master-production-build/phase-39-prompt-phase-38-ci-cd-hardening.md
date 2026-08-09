---
phase: 39
title: "Prompt Phase 38 — CI/CD Hardening"
status: pending
priority: P1
effort: "6-10 days"
dependencies: [31, 33, 34, 35, 36, 37, 38]
---

# Prompt Phase 38 — CI/CD Hardening

## Outcome

Create fast merge-critical and deeper release/nightly pipelines that connect source SHA to tested/scanned artifacts, Vercel previews, GHCR packages, reviewed release/deployment and rollback.

## Pipeline Tiers

- PR: format/lint/type/build/unit/critical integration/contract/secret scan.
- Merge/release: broader integration/E2E/security, Vercel preview/deployment checks, images, SBOM/provenance/attestation, draft GitHub Release and staging deploy.
- Nightly/manual: heavy SAST/dependency/IaC/container, load, restore and extended RAG evaluation.

## Requirements

- Actions/dependencies pinned and permissions least privilege.
- Cache keys cannot mix untrusted/privileged content.
- Required checks cannot be silently skipped by path/filter mistakes.
- Generated contracts and migrations are reproducible/drift-checked.
- Artifact attestations link SHA, digest, scan/SBOM and deployment.
- GitHub Release notes/assets/checksums and package tags are generated only from the accepted release head; `latest` is stable-only.
- Preview verifies branch UX only. The production candidate is built as a staged Production deployment from the accepted SHA/configuration, checked by exact deployment ID and promoted without rebuild under one deployment authority.
- Production promotion and rollback require accepted authority.

## Planned Ownership

`.github/workflows/**`, CI scripts, required-check docs and provenance artifacts. Workflow files/check names have one owner; domain test commands remain domain-owned.

## Validation

- Pull request from clean branch exercises required gates.
- Intentional failing test/scan/contract prevents merge.
- Fork/untrusted context cannot access secrets/privileged actions.
- Image deploy uses exact digest.
- GHCR pull/attestation verification, Vercel preview promotion, staging rollout and rollback are exercised.

## Commit Plan

- `ci: harden merge-critical quality gates`
- `ci: add reproducible image and provenance pipeline`
- `ci: add controlled staging deployment and rollback`
- `ci(release): publish verified github release and packages`

## Acceptance

- [ ] Green status means every named required gate actually ran.
- [ ] Fast path remains practical; heavy checks are separate without weakening critical coverage.
- [ ] Supply-chain permissions and provenance are reviewed.
- [ ] Deployment evidence ties back to exact main SHA.
- [ ] Release tag/assets, remote main, Vercel deployment and GHCR digests reconcile in one immutable receipt.

## Stop Conditions

Skipped job treated green, unpinned unsafe action, secrets exposed to fork, mutable artifact deployed, package/release identity drift, production deploy without approval/rollback.
