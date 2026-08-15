---
phase: 1
title: "M4 Verify & Finish"
status: pending
priority: P1
effort: "2-3 days"
dependencies: []
---

# Phase 1: M4 Verify & Finish

## Overview
M4 (Knowledge + RAG) đã implement (~1,428 lines main, ~1,355 lines tests, migrations V022-V024) nhưng README claim "planned, not implemented." Phase này verify toàn bộ M4, fix inconsistency, và hoàn thiện phần còn thiếu.

## Requirements
- [ ] Verify M4 code compiles and all tests pass
- [ ] Fix README: M4 is implemented, not planned
- [ ] Write ADR for M4 (knowledge + RAG architecture)
- [ ] Verify RAG permission-before-context contract works
- [ ] Verify document ingestion pipeline end-to-end
- [ ] Verify hybrid retrieval (lexical + vector)
- [ ] Verify citation + chat persistence
- [ ] Add missing unit tests if coverage < 80%

## Implementation Steps

1. **Audit M4 code completeness**
   - Read all `knowledge/` and `rag/` packages
   - Verify controllers → services → repositories chain complete
   - Check for stubs, TODO, UnsupportedOperationException

2. **Run M4 tests**
   - `cd apps/platform-api && ./mvnw test -Dtest='Knowledge*Test,Rag*Test,SecureRag*Test,Hybrid*Test,Vector*Test,Conversation*Test'`
   - Verify all pass

3. **Fix README inconsistency**
   - Update "Planned, not implemented" → accurate M4 status
   - Add M4 surfaces to product preview table
   - Update "Implemented today" section

4. **Write ADR-M4-knowledge-rag.md**
   - Context: knowledge + RAG architecture
   - Decision: pgvector, hybrid retrieval, permission-before-context
   - Consequences: positive/negative/neutral

5. **Verify cross-tenant isolation**
   - Run cross-tenant STOP tests for knowledge/RAG
   - Verify RLS policies in V022-V024

## Todo
- [ ] Audit M4 code
- [ ] Run M4 tests
- [ ] Fix README
- [ ] Write ADR M4
- [ ] Verify tenant isolation

## Success Criteria
- M4 tests all pass
- README accurately reflects M4 implementation
- ADR M4 written and accepted
- No stubs/unimplemented methods in M4 code

## Commit Plan
```
feat(m4): verify knowledge + RAG implementation completeness
docs: update README M4 status from planned to implemented
docs(adr): add ADR-M4 knowledge + RAG architecture
fix(m4): [any fixes found during verification]
```
