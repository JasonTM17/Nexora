---
title: "Nexora Full Program M4-M8"
description: "Complete Nexora milestone M4-M8: verify/finish M4, implement M5 analytics/experiments/notifications, M6 observability/security, M7 deployment/Docker Hub/GitOps, M8 polish/final review, full media suite."
status: pending
priority: P1
effort: "Multi-wave program; ~200+ engineer days estimated"
tags: [feature, full-program, m4, m5, m6, m7, m8, media, deployment]
created: 2026-08-15
---

# Nexora Full Program M4-M8

## Outcome Contract (LOCKED)

- **Intended result**: Hoàn thành Full Program Nexora M4–M8 — code, tests, docs, media, deployment pipeline
- **In scope**: (1) M4 verify & hoàn thiện, (2) M5 analytics/experiments/notifications, (3) M6 observability + security hardening, (4) M7 deployment + Docker Hub + GitOps, (5) M8 polish + final review, (6) Full media suite (ảnh + GIF cho README + docs)
- **Out of scope**: Production cloud provisioning thật (Vercel/Supabase/NATS cloud paid tiers), live provider accounts, compliance certification
- **Acceptance signals**: code+tests pass locally CI green; media hoàn chỉnh (ảnh + GIF trong docs); Dockerfile + CI đầy đủ (security scan, coverage gate, Docker Hub publish)
- **Constraints**: Docker Hub push (public) + staging deploy được phép; không provisioning production thật
- **Allowed substitutions**: Không phải là milestone hoặc production target
- **Decision owner**: user

## Contract Traceability

| Phase | Contract items | Acceptance signals | Facts / assumptions / prereqs / user decisions |
|---|---|---|---|
| 1 M4 Verify & Finish | M4 verify, README fix, ADRs | CI green, README accurate | FACT: M4 code exists (~1,428 lines). ASSUME: tests pass. PREREQ: fix README inconsistency |
| 2 M5 Adaptive Intelligence | analytics, experiments, notifications | unit+integration tests pass | FACT: M5 not started. ASSUME: event backbone (M3) ready |
| 3 M6 Observability + Security | Prometheus/Loki/Tempo, JWT Ed25519, rate limit | dashboards work, security scan clean | FACT: observability/ empty. ASSUME: local stack sufficient |
| 4 M7 Deployment + Docker Hub | Dockerfiles, CI/CD, GitOps, DR drill | images published, deploy works | FACT: web Dockerfile missing. DECISION: Docker Hub allowed |
| 5 M8 Polish + Final Review | docs polish, media suite, final review | media in docs, review complete | FACT: capture script exists. ASSUME: local next build works |
| 6 Cross-cutting: Media + Docs | ảnh + GIF cho README + docs | all docs have media | FACT: README has GIF, docs don't |
| 7 Cross-cutting: CI/CD hardening | security scan, coverage gate, SBOM | CI passes all gates | FACT: only validate.yml exists |
| 8 Cross-cutting: ADRs + README per service | ADRs for M1-M4, per-service READMEs | all services documented | FACT: only 1 ADR exists |

## Phases

| # | Phase | Status |
|---|-------|--------|
| 1 | [Phase 1: M4 Verify & Finish](./phase-01-m4-verify-finish.md) | Pending |
| 2 | [Phase 2: M5 Adaptive Intelligence](./phase-02-m5-adaptive-intelligence.md) | Pending |
| 3 | [Phase 3: M6 Observability + Security](./phase-03-m6-observability-security.md) | Pending |
| 4 | [Phase 4: M7 Deployment + Docker Hub](./phase-04-m7-deployment-docker.md) | Pending |
| 5 | [Phase 5: M8 Polish + Final Review](./phase-05-m8-polish-review.md) | Pending |
| 6 | [Phase 6: Cross-cutting Media + Docs](./phase-06-media-docs.md) | Pending |
| 7 | [Phase 7: Cross-cutting CI/CD Hardening](./phase-07-cicd-hardening.md) | Pending |
| 8 | [Phase 8: Cross-cutting ADRs + READMEs](./phase-08-adrs-readmes.md) | Pending |

## Success Criteria

- [ ] M4 verified: README accurate, all M4 tests pass, ADR written
- [ ] M5 implemented: analytics pipeline, experiments, notifications — tests pass
- [ ] M6 implemented: observability stack local, security hardening, coverage gate
- [ ] M7 implemented: 3 Dockerfiles, CI/CD full, Docker Hub images, DR drill doc
- [ ] M8 complete: media suite, final review, docs polished
- [ ] CI green on main: foundation + go + java + security + coverage
- [ ] All docs have media (ảnh + GIF)
- [ ] Per-service READMEs + ADRs for M1-M4

<!-- slug: nexora-full-program-m4-m8 -->
