---
phase: 8
title: "Cross-cutting: ADRs + READMEs"
status: pending
priority: P2
effort: "2-3 days"
dependencies: [1]
---

# Phase 8: Cross-cutting ADRs + READMEs

## Overview
Write missing ADRs (M1, M2, M4) and per-service READMEs for all services/packages. Foundation documentation completeness.

## Requirements
- [ ] ADR M1: Why Spring Boot + Next.js + Go + PostgreSQL + NATS
- [ ] ADR M2: Tenant isolation strategy (RLS + application-layer)
- [ ] ADR M4: Knowledge + RAG architecture (pgvector, hybrid retrieval)
- [ ] ADR: JWT signing approach (Ed25519 + JWKS)
- [ ] Per-service README: apps/web, apps/platform-api, services/event-ingestion
- [ ] Per-package README: packages/contracts, ui-core, ui-studio, ui-ai, ui-builder, design-tokens
- [ ] Root README updated with M4-M5 surfaces

## Implementation Steps

1. **ADRs** (template: docs/adr/template.md)
   - Each ADR: Context → Decision → Consequences → Alternatives
   - Status: Accepted
   - Append-only (never edit accepted ADRs)

2. **Per-service README** (6 sections each)
   - Purpose (1-2 sentences, who calls it, who it calls)
   - API surface (bullet endpoints or link to OpenAPI)
   - Env vars (table: name | required | default | description)
   - Run locally (exact commands)
   - Test (exact commands + coverage threshold)
   - Runbook (common ops)

3. **Root README update**
   - Add M4 surfaces to product preview
   - Update "Implemented today" section
   - Update limitations section

## Todo
- [ ] ADR M1 platform choices
- [ ] ADR M2 tenant isolation
- [ ] ADR M4 knowledge + RAG
- [ ] ADR JWT signing
- [ ] README apps/web
- [ ] README apps/platform-api
- [ ] README services/event-ingestion
- [ ] README packages (6 packages)
- [ ] Update root README

## Success Criteria
- 4 new ADRs written and accepted
- All services/packages have README
- Root README accurate and current

## Commit Plan
```
docs(adr): add ADR M1 platform technology choices
docs(adr): add ADR M2 tenant isolation strategy
docs(adr): add ADR M4 knowledge + RAG architecture
docs(adr): add ADR JWT signing approach
docs(readme): add per-service README for web
docs(readme): add per-service README for platform-api
docs(readme): add per-service README for event-ingestion
docs(readme): add per-package READMEs
docs(readme): update root README with M4-M5 surfaces
```
