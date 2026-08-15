---
phase: 5
title: "M8 Polish + Final Review"
status: pending
priority: P1
effort: "5-8 days"
dependencies: [4]
---

# Phase 5: M8 Polish + Final Review

## Overview
M8 is product-wide polish: final engineering review across all tracks, documentation completeness, media suite production, and release readiness.

## Requirements
- [ ] Staff-level engineering review (8 tracks)
- [ ] Cross-tenant security STOP tests
- [ ] Accessibility audit (keyboard, 375px, screen reader)
- [ ] Performance budget verification
- [ ] Final documentation sweep
- [ ] Release notes + changelog
- [ ] GitHub About + Releases + media manifest

## Implementation Steps

1. **Engineering review (8 tracks)**
   - Architecture/domain boundaries
   - Data/tenancy/security
   - Frontend/product/accessibility
   - AI/RAG quality/privacy
   - Async/reliability
   - Observability/performance
   - Supply-chain/deployment/DR
   - Developer experience/docs/maintainability

2. **Security final sweep**
   - Cross-tenant: zero unauthorized success
   - Stale membership: no privilege escalation
   - Unsafe page/upload: blocked
   - Prompt injection: contained
   - Unauthorized context: never reaches LLM

3. **Accessibility audit**
   - All surfaces: keyboard navigable
   - 375px mobile + desktop
   - Reduced-motion support
   - Screen reader labels
   - axe-core automated + manual spot check

4. **Performance budget**
   - Lighthouse Performance ≥ 80 mobile / ≥ 90 desktop
   - Accessibility ≥ 90
   - Core Web Vitals: LCP ≤ 2.5s, INP ≤ 200ms, CLS ≤ 0.1
   - Bundle size ≤ 200 KB gzipped per route

5. **Documentation sweep**
   - Every service has README with 6 sections
   - Every public API has OpenAPI docs
   - Architecture diagrams current
   - Decision log reconciled

6. **Release preparation**
   - CHANGELOG.md (Keep a Changelog format)
   - Release notes for v0.1.0
   - GitHub Release with media manifest
   - SBOM + provenance reconcile

## Todo
- [ ] Engineering review all 8 tracks
- [ ] Security STOP tests
- [ ] Accessibility audit
- [ ] Performance budget check
- [ ] Documentation sweep
- [ ] CHANGELOG + release notes
- [ ] GitHub Release

## Success Criteria
- All review tracks: findings fixed or accepted with rationale
- Security STOP tests: 100% pass
- Accessibility: ≥ 90 Lighthouse
- All docs current and complete
- Release ready

## Commit Plan
```
docs(release): add CHANGELOG v0.1.0
docs(release): add release notes
fix(review): [findings from engineering review]
fix(a11y): [accessibility fixes]
perf: [performance optimizations]
docs: final documentation sweep
```
