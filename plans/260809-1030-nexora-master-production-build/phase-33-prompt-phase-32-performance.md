---
phase: 33
title: "Prompt Phase 32 — Performance"
status: pending
priority: P2
effort: "6-10 days"
dependencies: [32]
---

# Prompt Phase 32 — Performance

## Outcome

Measure critical paths, identify real bottlenecks, optimize only from evidence and publish reproducible before/after results.

## Workloads

Public page read/publish invalidation, builder save, tenant API, document ingestion, hybrid retrieval/RAG, event ingestion, flag evaluation, analytics query and notification backlog as applicable.

## Method

1. Declare workload/SLO hypothesis and environment.
2. Record SHA, hardware, service versions, dataset, warmup, duration and concurrency.
3. Capture raw latency/throughput/error/resource/query-plan evidence.
4. Identify bottleneck from traces/profiles, not intuition.
5. Change one bounded variable and protect correctness.
6. Rerun identical workload and add regression budget.

## Planned Ownership

`tests/performance/**`, performance reports, scoped product optimizations. Shared test environment/data is controlled; each optimization returns to owning component.

## Validation

- Correctness and tenant/security invariants hold under load.
- Cold/warm cache effects labeled.
- Failure/backpressure and saturation points captured.
- Results include variance/percentiles, not only averages.
- External provider limits excluded or clearly separated.

## Commit Plan

- `test(perf): add reproducible critical-path workloads`
- Scoped `perf(<domain>): optimize measured bottleneck`
- `docs(perf): record before and after evidence`

## Acceptance

- [ ] Every performance claim links to raw artifacts and exact SHA.
- [ ] Optimization improves declared metric without breaking security/correctness.
- [ ] Regression threshold runs at appropriate CI tier.
- [ ] Remaining bottlenecks/limits are explicit.

## Stop Conditions

Fabricated/round-number claims, incomparable environments, optimization before baseline, security disabled for benchmark, average-only misleading report.
