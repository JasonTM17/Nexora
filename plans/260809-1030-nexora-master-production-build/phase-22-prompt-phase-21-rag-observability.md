---
phase: 22
title: "Prompt Phase 21 — RAG Observability"
status: pending
priority: P1
effort: "5-8 days"
dependencies: [20, 21]
---

# Prompt Phase 21 — RAG Observability

## Outcome

Make retrieval behavior inspectable and evaluable through safe traces, score inspection, feedback, admin UX and reproducible scripts without storing sensitive prompts/content by default.

## Trace Contract

Record safe IDs, tenant, query hash/metadata, corpus/model/config version, candidate IDs/scores/stages, latency/token/cost counters, citation IDs and outcome. Raw prompt/context/source text is disabled or separately redacted and retention-controlled.

## Implementation Slices

1. Retrieval-run and feedback schema/retention.
2. Stage-level trace instrumentation.
3. Authorized admin inspection APIs/UI.
4. Feedback capture with abuse/privacy controls.
5. Versioned evaluation CLI/scripts and report format.
6. Alerts/dashboards for failure, latency, no-answer and leakage test status.

## Planned Ownership

RAG trace/feedback migration, platform observability APIs, admin RAG dashboard, evaluation scripts/reports. Trace schema has one owner.

## Validation

- Tenant/admin authorization and cross-tenant denial.
- Redaction and retention/deletion tests.
- Evaluation reproduction from corpus checksum and config.
- Dashboard loading/empty/error/denied states and truthful fixture labels.
- Trace overhead measurement and sampling policy.

## Commit Plan

- `feat(rag): record redacted retrieval traces`
- `feat(admin): add RAG quality inspection`
- `test(rag): add reproducible evaluation suite`

## Acceptance

- [ ] Admin can explain an authorized answer from retrieval to citations.
- [ ] Evaluation reports include Recall@K, citation precision and no-answer behavior.
- [ ] Sensitive content is absent from default telemetry.
- [ ] Retention/deletion and cost are bounded.

## Stop Conditions

Raw sensitive prompt/context logging, cross-tenant trace view, dashboard fabricates quality metrics, evaluation not reproducible.
