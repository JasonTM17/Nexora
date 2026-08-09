---
phase: 43
title: "Prompt Phase 42 — Final Red Team"
status: pending
priority: P1
effort: "8-12 days plus remediation"
dependencies: [31, 39, 40, 41, 42]
---

# Prompt Phase 42 — Final Red Team

## Outcome

Independently attack the release candidate across authorization, tenancy, RAG, uploads, URL ingestion if enabled, builder, flags, admin APIs, event ingestion and webhooks; fix findings and rerun regressions.

## Governance

- Freeze exact candidate HEAD and environment.
- Independent C3 reviewers are read-only; same-model-family fallback is disclosed.
- Tests remain within approved non-destructive scope; production attack requires explicit R3 approval.
- Each finding includes severity, evidence, reproduction, impact, affected head and recommendation.
- Fixes are assigned to original bounded owners and invalidate prior review head.

## Attack Tracks

1. Auth/session/RBAC/admin privilege escalation.
2. Tenant enumeration and page/document/object/vector/chat/audit/analytics leakage.
3. Builder schema bypass, stored/reflected XSS and content/CSP escape.
4. Upload/parser/decompression/malware posture and URL SSRF/redirect/DNS if enabled.
5. RAG injection, unauthorized context, citation spoofing and provider fallback.
6. Flag/experiment/personalization/recommendation manipulation.
7. Event/webhook ingestion spoofing, replay, amplification and poison messages.
8. Secrets/supply-chain/CI/deployment/rollback/recovery controls.
9. README/media/diagram/release/GHCR claim provenance and production-continuity/SLO evidence.

## Planned Artifacts

`plans/.../reports/final-red-team-*.md`, sanitized reproductions, regression tests, residual-risk decisions. Sensitive exploit data is controlled and never blindly published.

## Commit Plan

- `test(red-team): add final adversarial regressions`
- Scoped `fix(security): remediate <finding>` commits.
- `docs(security): record final red-team disposition`

## Acceptance

- [ ] All Critical/High findings fixed or explicitly risk-accepted by authorized user.
- [ ] Regression suite passes on final exact HEAD.
- [ ] Tenant/RAG/secret STOP matrix has zero success.
- [ ] Environment and limitations are documented honestly.
- [ ] Whole-plan consistency sweep is clean after remediation.
- [ ] No stale media, mutable package, identity mismatch, ping-based resilience claim or unmeasured uptime claim survives review.

## Stop Conditions

Critical exploit, secret exposure, test against unapproved target/data, stale review head, suppressed finding, reviewer fixes/accepts its own work.
