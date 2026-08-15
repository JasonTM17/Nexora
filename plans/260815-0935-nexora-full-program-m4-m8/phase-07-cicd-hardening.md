---
phase: 7
title: "Cross-cutting: CI/CD Hardening"
status: pending
priority: P2
effort: "3-4 days"
dependencies: [1]
---

# Phase 7: Cross-cutting CI/CD Hardening

## Overview
Add security scanning, coverage gates, SBOM generation, and Docker Hub publish to CI. Builds on existing validate.yml.

## Requirements
- [ ] Trivy vulnerability scan (image + filesystem)
- [ ] CodeQL SAST (GitHub Advanced Security)
- [ ] Gitleaks secret scan
- [ ] Coverage gate (≥ 80% lines, ≥ 70% branches)
- [ ] SBOM generation (Syft → spdx.json)
- [ ] Docker Hub publish workflow
- [ ] Dependabot for npm + go + actions + docker
- [ ] Branch protection rules documented

## Implementation Steps

1. **Trivy scan**
   - Add job to validate.yml
   - Scan filesystem + Docker images (after build)
   - CRITICAL + HIGH fail build

2. **CodeQL**
   - `.github/workflows/codeql.yml`
   - Analyze Java, Go, TypeScript
   - Weekly + on PR

3. **Gitleaks**
   - Add step to validate.yml
   - Scan diff for secrets
   - Include patterns: ghp_, github_pat_, sk-, AIza, JWT_SECRET, DATABASE_URL

4. **Coverage gate**
   - Java: JaCoCo threshold in pom.xml
   - Go: `go test -cover` threshold
   - Web: vitest --coverage threshold
   - CI fails if below threshold

5. **SBOM**
   - Syft generates sbom.spdx.json
   - Upload as release artifact
   - Include in Docker Hub release notes

6. **Docker Hub publish**
   - `.github/workflows/docker-publish.yml`
   - Trigger: push to main
   - 3 images, multi-arch, tags: latest + SHA
   - Secrets: DOCKERHUB_USERNAME, DOCKERHUB_TOKEN

7. **Dependabot**
   - `.github/dependabot.yml`
   - npm, gomod, github-actions, docker
   - Auto-merge patch/minor

## Todo
- [ ] Trivy scan
- [ ] CodeQL
- [ ] Gitleaks
- [ ] Coverage gate
- [ ] SBOM
- [ ] Docker Hub publish
- [ ] Dependabot

## Success Criteria
- All security scans pass
- Coveragegate enforced
- SBOM generated per release
- Images push to Docker Hub on main

## Commit Plan
```
ci(security): add Trivy vulnerability scan
ci(security): add CodeQL SAST
ci(security): add Gitleaks secret scan
ci(coverage): add coverage gate
ci(sbom): add SBOM generation
ci(docker): add Docker Hub publish workflow
ci(dependabot): add dependabot config
```
