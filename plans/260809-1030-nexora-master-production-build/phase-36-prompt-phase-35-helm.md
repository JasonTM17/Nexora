---
phase: 36
title: "Prompt Phase 35 — Helm"
status: pending
priority: P2
effort: "4-6 days"
dependencies: [35]
---

# Prompt Phase 35 — Helm

## Outcome

Package reviewed Kubernetes resources into reusable, schema-validated charts with explicit environment values and safe upgrade/rollback behavior.

## Requirements

- Clear chart boundary, dependencies and versioning.
- `values.schema.json` or equivalent validation for required/allowed settings.
- Dev/staging/production values contain references only, never secrets.
- Common helpers remain minimal; no hidden global magic.
- Hooks used only when idempotent and rollback-safe.
- Rendered output preserves security/resource/probe/network invariants.

## Planned Ownership

`infrastructure/helm/**` and chart CI. Common helpers/global values have one owner; per-chart templates may split.

## Validation

- `helm lint` and `helm template` for every accepted environment.
- Snapshot/policy/schema checks on rendered output.
- Install/upgrade/rollback/uninstall in disposable namespace.
- Missing/invalid values fail early without secret output.

## Commit Plan

- `feat(helm): package Nexora platform charts`
- `test(helm): validate environment rendering and upgrades`

## Acceptance

- [ ] Rendered manifests match Phase 34 accepted controls.
- [ ] Values and chart versioning are documented.
- [ ] Upgrade/rollback evidence exists at approved cluster.
- [ ] Secret references and public configuration are clearly separated.

## Stop Conditions

Secret value committed, rendered security controls lost, unsafe install hooks, environment drift hidden in manual edits.
