---
phase: 17
title: "Prompt Phase 16 — Document Ingestion"
status: pending
priority: P1
effort: "6-10 days"
dependencies: [16]
---

# Prompt Phase 16 — Document Ingestion

## Outcome

Safely extract, normalize and chunk hostile documents into versioned metadata with durable indexing state and reproducible output.

## Initial Scope

Start with PDF, Markdown and plain text only unless the accepted decision expands formats. URL ingestion remains disabled until independent SSRF controls pass.

## Security Limits

File bytes/pages/text/chunks/tokens, decompression ratio, parser CPU/time/memory, nested content and batch concurrency all have ceilings. Parser runs with least privilege and no network where possible.

## Implementation Slices

1. Extractor interface and format-specific adapters.
2. Normalization preserving source/page/section provenance.
3. Deterministic chunking strategy and version identifier.
4. Chunk/document indexing-state transitions and checksums.
5. Retry/resume/cancel/terminal failure behavior.
6. Hostile/corrupt/large/duplicate fixtures and deletion propagation hooks.

## Planned Ownership

Platform `knowledge/ingestion/**`, chunk migration/metadata, parser fixtures and worker configuration. Chunk schema and document state machine each have one owner.

## Validation

- Deterministic chunks/checksums from known corpus.
- Corrupt/encrypted/empty/oversized/decompression-bomb fixtures.
- Parser timeout/crash and process restart.
- Duplicate job does not create duplicate active chunks.
- Source provenance maps every chunk to authorized document location.

## Commit Plan

- `feat(rag): add bounded document extraction`
- `feat(rag): normalize and chunk source documents`
- `test(security): add hostile ingestion fixtures`

## Acceptance

- [ ] Ingestion is bounded and observable.
- [ ] State transitions are durable and recoverable.
- [ ] Chunk strategy/version and source checksum are recorded.
- [ ] Failed document cannot become partially searchable as complete.

## Stop Conditions

Unbounded parser/network access, URL ingestion without SSRF gate, missing provenance, duplicate active chunks, sensitive raw content in logs.
