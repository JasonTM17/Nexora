---
phase: 31
title: "Prompt Phase 30 — Security Hardening"
status: pending
priority: P1
effort: "10-15 days"
dependencies: [5, 6, 10, 13, 16, 20, 23, 25, 28, 30]
---

# Prompt Phase 30 — Security Hardening

## Outcome

Threat-model and attack current Nexora surfaces, fix evidenced vulnerabilities, add regression tests and record residual risk.

## Attack Tracks

- Authentication/session and authorization bypass.
- Tenant and object/storage/Realtime isolation.
- Page schema, rich content, builder, XSS and CSP.
- Upload/parser, decompression and URL/SSRF if enabled.
- RAG leakage, injection, citations and sensitive telemetry.
- Rate/abuse limits, event ingestion and webhook-style inputs.
- Secrets, dependencies, supply chain and configuration.
- Account/tenant deletion authorization, user export isolation, chat/document purge propagation, analytics/log anonymization and restored-backup resurrection.

## Workflow

1. Update trust/data-flow diagrams and attack inventory.
2. Run parallel read-only adversaries by surface.
3. Require evidence/reproduction for findings.
4. Severity/adjudication by independent C3 reviewer.
5. Route fixes to original bounded owners; no reviewer self-fix.
6. Add regression tests and rerun full security matrix.
7. Record accepted residual risks with owner/deadline.

## Planned Paths

`docs/security/**`, `tests/security/**`, reports; product fixes are separate owned branches. Shared security middleware/config is serialized.

## Validation

- OWASP web/API risks relevant to actual surface.
- Two-tenant automated attack suite.
- Dependency/secret/license scans with severity policy.
- Dynamic/manual browser and API probes where authorized.
- RAG context capture and ingestion hostile fixtures.
- Cross-tenant export, last-owner deletion, partial purge/resume, anonymization/re-identification and purge-on-restore fixtures.

## Commit Plan

- `docs(security): update Nexora threat model`
- `test(security): add adversarial regression suite`
- Scoped `fix(security): ...` commits per finding.

## Acceptance

- [ ] Critical/high findings are fixed or explicitly accepted with rationale/deadline.
- [ ] Exact-head regressions pass after fixes.
- [ ] Evidence and limitations distinguish automated from manual/live coverage.
- [ ] No sensitive attack data is committed.
- [ ] Privacy/lifecycle findings distinguish product correctness from legal-certification claims and verify every active/backup plane named in DEC-016.

## Stop Conditions

Any tenant/RAG/secret/export critical failure, deleted data served after active-plane purge or restored-backup reconciliation, evidence points to stale head, unapproved destructive testing, finding hidden to preserve release schedule.
