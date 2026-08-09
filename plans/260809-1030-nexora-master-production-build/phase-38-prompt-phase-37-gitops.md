---
phase: 38
title: "Prompt Phase 37 — GitOps"
status: pending
priority: P2
effort: "5-8 days"
dependencies: [36, 37]
---

# Prompt Phase 37 — GitOps

## Outcome

Configure Argo CD or accepted GitOps controller so reviewed declarative state promotes immutable artifacts with observable reconciliation and rollback.

## Requirements

- Clear source repository/path, environment promotion and ownership.
- Immutable image digest/version update flow.
- App/project/RBAC and cluster credential boundary.
- Sync policy, health, prune/self-heal and destructive-resource safeguards.
- Secret delivery through approved external mechanism, not Git plaintext.
- Rollback/revert workflow and drift reconciliation runbook.

## Planned Ownership

`infrastructure/argocd/**`, environment deployment references and runbooks. GitOps layout, projects, destinations and value references have one owner.

## Validation

- Rendered application/project/schema checks.
- Reconcile an approved staging revision.
- Observe healthy sync tied to expected digest.
- Bad release/health failure, rollback and manual drift behavior.
- Permission and secret exposure tests.

## Commit Plan

- `feat(gitops): configure Nexora Argo CD applications`
- `test(gitops): verify reconciliation and rollback`
- `docs(operations): document environment promotion`

## Acceptance

- [ ] Git change maps to reviewed deployment revision/digest.
- [ ] Controller access is least privilege.
- [ ] Rollback is tested and observable.
- [ ] No deployment claim from config-only evidence.

## Stop Conditions

Auto-prune dangerous resources without safeguard, mutable tags, plaintext secrets, unreviewed automatic production sync, config called deployed without reconciliation proof.
