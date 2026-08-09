---
phase: 20
title: "Prompt Phase 19 — Secure RAG"
status: pending
priority: P1
effort: "9-14 days"
dependencies: [6, 19]
---

# Prompt Phase 19 — Secure RAG

## Outcome

Deliver permission-aware context construction, tenant-scoped persistent conversations, bounded provider abstraction, streaming answer UX, real citations and explicit no-answer behavior with zero tenant leakage.

## Security Invariants

- Authorization filters occur during retrieval and are rechecked before context assembly.
- Unauthorized candidates/context are a STOP even when final text omits them.
- Sources are untrusted data, never higher-priority instructions.
- Provider receives minimal authorized context; logs/traces redact prompt and source text by default.
- Citations resolve to authorized document/chunk/page evidence.
- Session/message ownership is tenant plus subject scoped; history, export, resume and source-open paths reauthorize current membership and source access.
- Persist user intent and assistant lifecycle explicitly. A streaming, canceled or failed assistant draft is never mislabeled as a completed answer.

## Implementation Slices

1. User/tenant/permission retrieval context.
2. Context builder with token and source limits.
3. Provider abstraction and deterministic CI provider.
4. Tenant-scoped chat session/message history with stable pagination, idempotent send, resume, cancel and regenerate-as-new-revision semantics.
5. Stream protocol, persisted lifecycle, cancellation, timeout and safe partial failure.
6. Citation contract/resolver, historical source reauthorization and sanitized renderer.
7. No-answer/low-confidence policy plus chat-history/export/deletion propagation.
8. Leakage, injection, XSS, provider failure, duplicate-send, pagination and deletion tests.
9. Evaluate Ant Design X conversation/sender/source components behind owned adapters; keep authorization, persistence, SSE, citation and sanitization contracts outside the library.

## Planned Ownership

Platform `rag/query/**`, `rag/provider/**`, dedicated conversation backend paths, web `knowledge/chat/**`, `packages/ui-ai/**`, citation/chat contract and security/evaluation fixtures. The migration train alone owns `chat_sessions`, `chat_messages` and retrieval/feedback schema. Retrieval/provider API, conversation persistence API and UI writers are separate as defined by the execution ledger.

## Commit Plan

- `feat(rag): filter retrieval by effective permissions`
- `feat(rag): build bounded grounded context`
- `feat(rag): stream answers with resolvable citations`
- `feat(chat): persist tenant-scoped conversation history`
- `test(security): prevent RAG tenant and prompt leakage`

## Acceptance

- [ ] Captured test context contains zero unauthorized chunk IDs/content.
- [ ] Every displayed citation resolves and is authorized at access time.
- [ ] No-match/low-confidence query returns honest no-answer.
- [ ] Provider timeout/cancel/rate/failure is bounded and visible.
- [ ] Deterministic and live evidence are reported separately.
- [ ] AI UI exposes safe stage metadata and real sources, never hidden chain-of-thought or browser provider credentials.
- [ ] History list/detail pagination, reload/resume, duplicate retry, cancel, regenerate and deletion preserve explicit message revisions/states and never cross tenant/user boundaries.
- [ ] Historical citations are reauthorized when opened; lost access shows an unavailable-source state rather than content.

## Stop Conditions

Unauthorized candidate/context/history, fabricated citation, raw prompt/source logging, provider fallback weakens policy, overwritten regeneration lineage, streaming draft labeled complete, deleted chat still served, or live claim from mock only.
