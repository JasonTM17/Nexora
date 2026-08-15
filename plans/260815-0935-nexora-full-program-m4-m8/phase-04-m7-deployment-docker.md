---
phase: 4
title: "M7 Deployment + Docker Hub"
status: pending
priority: P1
effort: "7-10 days"
dependencies: [3]
---

# Phase 4: M7 Deployment + Docker Hub

## Overview
M7 delivers production deployment pipeline: Docker Hub images, GitHub Actions CI/CD, GitOps, disaster recovery drills, and production readiness verification.

## Requirements
- [ ] Web Dockerfile (multi-stage, non-root, distroless)
- [ ] Docker Hub publish workflow (3 images: web, platform-api, event-ingestion)
- [ ] Full CI pipeline (security scan, coverage gate, SBOM)
- [ ] GitOps deployment manifests (K8s/Helm or Compose for staging)
- [ ] Disaster recovery drill (backup/restore for PostgreSQL, NATS, Storage)
- [ ] Production runbook
- [ ] Staging environment deploy

## Implementation Steps

1. **Web Dockerfile**
   - Multi-stage: build → runtime
   - Runtime: distroless or alpine, non-root (uid 65532)
   - HEALTHCHECK `/healthz`
   - LABEL org.opencontainers.image.*

2. **Docker Hub publish**
   - Workflow: `.github/workflows/docker-publish.yml`
   - Trigger: push to main
   - Images: `nguyenson1710/nexora-web`, `nexora-platform-api`, `nexora-event-ingestion`
   - Tags: `latest` + git SHA
   - Multi-arch: amd64 + arm64

3. **CI/CD hardening**
   - Add Trivy (image + filesystem scan)
   - Add CodeQL (SAST)
   - Add Gitleaks (secret scan)
   - Add coverage gate (≥ 80% lines)
   - Add SBOM generation (Syft)
   - Update `validate.yml` with new jobs

4. **Deployment manifests**
   - Kubernetes manifests OR Compose for staging
   - Health/readiness/liveness probes
   - Resource limits, disruption budget
   - Rolling update strategy
   - DNS/TLS/ingress config

5. **Disaster recovery**
   - PostgreSQL PITR procedure
   - NATS JetStream snapshot/restore
   - Object storage export/replication
   - DR drill script + verification
   - RPO/RTO measurement

6. **Production runbook**
   - Common ops: rotate secret, reset DB, drain & restart
   - Incident response
   - Escalation paths

## Todo
- [ ] Web Dockerfile
- [ ] Docker Hub publish workflow
- [ ] Trivy/CodeQL/Gitleaks CI
- [ ] Coverage gate
- [ ] SBOM generation
- [ ] K8s/Compose manifests
- [ ] DR drill
- [ ] Production runbook

## Success Criteria
- All 3 images build and push to Docker Hub
- CI passes all security gates
- DR drill completes within RPO/RTO targets
- Staging deploy works end-to-end

## Commit Plan
```
feat(docker): add web Dockerfile multi-stage non-root
ci(docker): add Docker Hub publish workflow
ci(security): add Trivy vulnerability scan
ci(security): add CodeQL SAST
ci(security): add Gitleaks secret scan
ci(coverage): add coverage gate
ci(sbom): add SBOM generation
feat(k8s): add deployment manifests
docs(dr): add disaster recovery drill
docs(runbook): add production runbook
```
