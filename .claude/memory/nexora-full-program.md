---
name: nexora-full-program
description: Nexora v0.1.0 RELEASED — bilingual, permissions, Docker, all branches merged
metadata:
  type: project
---

Nexora full-program Goal: HOÀN THÀNH — v0.1.1 released with all branches merged, bilingual UI, comprehensive permissions, and Docker images published.

## Status: v0.1.1 RELEASE CANDIDATE

### Completed Milestones
1. M4 Verify & Finish ✅
2. M5 Adaptive Intelligence ✅ (17 tests, full UI, contracts)
3. M6 Observability ✅ (Prom/Loki/Tempo/Grafana + metrics + ECS logging)
4. M6 Security ✅ (Redis rate limiter + account lockout + JWT dual-verify + K8s manifests)
5. M7 Prep ✅ (Dockerfiles + Docker Hub publish + 4-layer security + K8s manifests + production compose)
6. M8 Review ✅ (8-track review + media manifest + 7 screenshots)
7. Bilingual i18n ✅ (VI/EN infrastructure + translated surfaces)
8. Comprehensive Permissions ✅ (16 permissions, 5 roles, RBAC enforcement)
9. ADRs ✅ (6 ADRs: M1, M2, M3, M4, JWT signing, JWT migration)
10. READMEs ✅ (7 per-service + CHANGELOG + CONTRIBUTING)
11. Media captures ✅ (7 screenshots from live build)
12. Docker images ✅ (3 images on Docker Hub + GHCR)
13. GitHub release ✅ (v0.1.0 → v0.1.1)
14. GitHub About ✅ (description + topics updated)
15. Branch cleanup ✅ (0 unmerged branches)

## Docker Hub (nguyenson1710)
- nexora-web:latest + :c8b0393 (81MB)
- nexora-platform-api:latest + :c8b0393 (120MB)
- nexora-event-ingestion:latest + :c8b0393 (3.4MB)

## GitHub Release
- v0.1.1 — Nexora M0-M6 + Bilingual + Permissions + Site Editor
- URL: https://github.com/JasonTM17/Nexora/releases/tag/v0.1.1

## Test Status
- 68/68 platform-api unit tests pass
- 17 M5 unit tests pass
- 6 Redis limiter tests pass
- 29 Go tests pass
- 60 contracts tests pass
- Web build succeeds with all i18n + permissions

## Remaining (requires external coordination)
- M7 cloud provisioning — needs 9 hosting decisions + budget
- M6 JWT Ed25519 actual cutover — decoder ready, needs Supabase coordination

## Global Rules Added
- Nexora project rules added to C:/Users/Admin/.claude/CLAUDE.md
