---
phase: 35
title: "Prompt Phase 34 — Kubernetes"
status: pending
priority: P2
effort: "6-10 days"
dependencies: [34, 32]
---

# Prompt Phase 34 — Kubernetes

## Outcome

Deploy the approved backend stack with correct probes, resources, disruption/network controls and only evidence-justified autoscaling.

## Requirements

- Namespace/service-account/RBAC and workload security context.
- Startup/liveness/readiness probes aligned with real health semantics.
- Requests/limits based on measured workloads; graceful termination budget.
- PDB and topology rules based on replica/availability target.
- Default-deny NetworkPolicy plus required flows.
- HPA only with suitable metric and measured behavior.
- Config/secret references contain no committed values.

## Planned Ownership

`infrastructure/kubernetes/**`, environment overlays and validation tests. Namespace, shared config, ingress, network policy and secret vocabulary are serialized.

## Validation

- Schema/render validation and policy/security scan.
- Apply to approved disposable/staging cluster.
- Bad config, dependency failure, rollout, pod termination and node disruption behavior.
- Network allow/deny tests.
- Resource pressure and HPA evidence if enabled.

## Commit Plan

- `feat(k8s): add secure platform workload manifests`
- `feat(k8s): add network and disruption policies`
- `test(k8s): verify rollout and failure behavior`

## Acceptance

- [ ] Running pods use exact accepted image digests.
- [ ] Probes and termination avoid traffic to unready/terminating instances.
- [ ] NetworkPolicy and service account least privilege are verified.
- [ ] Live/staging evidence is separate from render-only evidence.

## Stop Conditions

Plain secret values committed, privileged/root workload without decision, fake readiness, unjustified HPA/PDB, manifests never applied but called deployed.
