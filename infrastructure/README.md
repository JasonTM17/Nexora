# Infrastructure (placeholder)

This directory is a layout marker for M7 production deployment infrastructure.

## Planned contents (deferred pending hosting decisions)

- `kubernetes/` — Kubernetes manifests (Deployments, Services, Ingress, HPA)
- `helm/` — Helm charts for the 3 services
- `terraform/` — Infrastructure-as-code for cloud provisioning
- `gitops/` — Argo CD / Flux configurations

## Hosting decisions required (per production-continuity-and-hosting.md)

| Decision | Status |
|---|---|
| Cloud provider / region | OPEN |
| Kubernetes distribution (managed vs self-hosted) | OPEN |
| NATS clustering strategy | OPEN |
| GPU embedding route | OPEN |
| Vercel/Supabase paid tiers | OPEN |
| SLO/RPO/RTO targets | OPEN |
| DNS/email configuration | OPEN |
| Data retention policy | OPEN |
| Cost envelope | OPEN |

## See also

- `docs/architecture/production-continuity-and-hosting.md` — candidate topology
- `plans/260809-1030-nexora-master-production-build/production-continuity-and-hosting.md`
