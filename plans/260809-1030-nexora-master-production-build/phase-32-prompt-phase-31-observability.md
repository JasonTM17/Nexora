---
phase: 32
title: "Prompt Phase 31 — Observability"
status: pending
priority: P1
effort: "6-10 days"
dependencies: [3, 14, 15, 20, 25, 28]
---

# Prompt Phase 31 — Observability

## Outcome

Complete safe correlated metrics, traces, logs, dashboards and actionable alerts across browser, Java, PostgreSQL, Go, queues/jobs and RAG.

## Telemetry Contract

One owner defines resource attributes, environment/service/version, trace propagation, metric names/units/cardinality, safe log fields, sampling, retention and redaction. Payloads/prompts/document text remain excluded by default.

## Implementation Slices

1. OpenTelemetry collector/export pipeline and local observability stack.
2. Browser/API/DB/Go/NATS/job trace propagation.
3. RED/USE/business-safe metrics and structured logs.
4. Grafana dashboards for platform, DB, events, jobs, publishing and RAG.
5. SLO candidates, alert rules and owned runbooks.
6. Outage/redaction/cardinality/overhead validation.

## Planned Ownership

`observability/**`, shared telemetry config, service instrumentation, `docs/operations/runbooks/**`. Naming/collector config has one owner; per-service instrumentation may split.

## Validation

- One end-to-end request/job trace across applicable components.
- Provider/DB/NATS/Realtime/job failures visible with correlation.
- No secrets/prompts/raw document bodies in telemetry.
- Cardinality and sampling budgets.
- Alerts fire from controlled failure and link to actionable runbook.

## Commit Plan

- `feat(observability): standardize OpenTelemetry resources`
- `feat(observability): add platform and workflow dashboards`
- `feat(operations): add actionable alerts and runbooks`
- `test(observability): verify correlation and redaction`

## Acceptance

- [ ] Operators can follow critical flows and failures.
- [ ] Dashboard panels use real query sources and provenance.
- [ ] Alert ownership/severity/silence/recovery are documented.
- [ ] Telemetry retention and cost controls exist.

## Stop Conditions

Sensitive telemetry, unbounded metric labels, dashboards with fake values, alerts without owner/runbook, health status contradicts dependency reality.
