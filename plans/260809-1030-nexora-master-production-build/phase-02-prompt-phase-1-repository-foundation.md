---
phase: 2
title: "Prompt Phase 1 — Repository Foundation"
status: pending
priority: P1
effort: "2-4 days"
dependencies: [1]
---

# Prompt Phase 1 — Repository Foundation

## Outcome

Create a reproducible, public-safe monorepo foundation with one-command local validation and focused bootstrap commits.

## Requirements

- Decide/init repository boundary, `main`, origin and license.
- Create `apps/{web,platform-api}`, `services/`, `packages/`, `database/`, `infrastructure/`, `observability/`, `docs/` layout only where justified.
- Pin Node/pnpm/Java/Go toolchains and record framework compatibility constraints; the serialized M1-DW01 dependency window owns exact Node dependency manifests and lockfile pins.
- Add `.env.example` without values, expanded `.gitignore` including project-local `.worktrees/`, Makefile/task entrypoint, Compose dependency stack and base CI.
- Enable secret, dependency and formatting gates appropriate to an empty foundation.

## Planned Ownership

M1-T01 controls non-Node root governance/build paths: Makefile, Compose, `.github/workflows/**`, `.gitignore`, license/provenance, tool-version files, directory skeleton and root documentation. It explicitly excludes `package.json`, `pnpm-workspace.yaml`, `pnpm-lock.yaml`, `.npmrc` and every Node workspace `package.json`. The serialized M1-DW01 owner alone creates/changes that dependency declaration/control set. No concurrent root or dependency writer.

## Implementation Slices

1. Repository/license/governance and ignore policy.
2. Monorepo directories and package/build conventions.
3. Toolchain pins, editor/formatter/linter baseline.
4. Local dependency Compose with health checks and named volumes.
5. Base CI matching local commands.
6. Public-content secret/provenance scan and initial push receipt.

## Validation

- Fresh-clone setup documented and run in a clean environment.
- `make help` or equivalent exposes canonical commands.
- Format/lint/config validation passes without application placeholders pretending to be features.
- Compose config renders and approved dependencies become healthy.
- Full tracked/staged secret scan passes.
- `git check-ignore .worktrees` passes and every linked worktree target is a strict descendant of `D:\Nexora\.worktrees\`.
- CI uses pinned/reviewed actions and produces required check names.
- M1-DW01 records the exact Node dependency-manifest allowlist, package/version rationale and license/security review; `pnpm install --frozen-lockfile` succeeds with zero manifest/lockfile diff.

## Commit Plan

- `chore(repo): initialize Nexora monorepo governance`
- `chore(dev): add reproducible local dependency stack`
- `chore(ci): add baseline validation workflow`
- `chore(deps): pin M1 Node dependency window`

## Acceptance

- [ ] `main` is clean and public-safe; origin and visibility match contract.
- [ ] `engineer/` and env secrets are excluded.
- [ ] License/provenance decision is recorded.
- [ ] Local and CI baseline commands agree.
- [ ] Receipt records initial main and remote SHA when push is approved.

## Risks and Rollback

Root and dependency-control paths are separately serialized. M1-T01 never writes the Node manifest/lockfile set, and application workers never borrow it from M1-DW01. Bootstrap commits remain independently revertible. No broad staging or history rewrite.
