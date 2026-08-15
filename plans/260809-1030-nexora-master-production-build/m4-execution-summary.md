# M4 — Secure Knowledge & RAG: Execution Summary

> Generated: 2026-08-14 | Base: M3 ACCEPTED (365783b)

---

## Wave Map

| Wave | Task | Branch | Status | Gate |
|---:|---|---|---|---|
| 1 | M4-C01 | `feature/knowledge-rag-contracts` | IN_PROGRESS | — |
| 2 | M4-DB01 | `feature/m4-schema-train` | PENDING | M4-C01 INTEGRATED |
| 3A | M4-T01 | `feature/knowledge-management` | PENDING | M4-C01 + M4-DB01 INTEGRATED |
| 3B | M4-U01 | `feature/knowledge-workspace-ui` | PENDING | M4-T01 `frozen_interfaces` |
| 4 | M4-T02 | `feature/document-ingestion` | PENDING | M4-T01 INTEGRATED |
| 5 | M4-T03 | `feature/vector-retrieval` | PENDING | M4-T02 + M4-DB01 INTEGRATED |
| 6 | M4-T04 | `feature/hybrid-retrieval` | PENDING | M4-T03 INTEGRATED |
| 7 | M4-T05 | `feature/rag-interaction-contracts` | PENDING | M4-T04 + M4-C01 INTEGRATED |
| 8A | M4-T06 | `feature/secure-rag-api` | PENDING | M4-T05 INTEGRATED |
| 8B | M4-T06B | `feature/rag-conversation-api` | PENDING | M4-T05 + M4-DB01 INTEGRATED |
| 9A | M4-T07 | `feature/rag-chat-ui` | PENDING | M4-T06 + M4-T06B INTEGRATED |
| 9B | M4-S01 | `test/m4-demo-seed-rag` | PENDING | M4-T03 + M4-T07 INTEGRATED |
| 10A | M4-T08 | `feature/rag-quality` | PENDING | M4-T06 + M4-T07 + M4-S01 INTEGRATED |
| 10B | M4-U02 | `feature/rag-quality-ui` | PENDING | M4-T08 `frozen_interfaces` |
| 11 | M4-I01 | `integration/v0.1-m4` | PENDING | All INTEGRATED + security review |

---

## Safe Parallel Windows

- **M4-T06 and M4-T06B** may run in parallel only after the exact M4-T05 contract HEAD is integrated.
  M4-T07 waits for both exact backend heads so persistence and stream contracts cannot diverge.
- **Tester** may prepare read-only hostile/evaluation fixtures while a worker writes, but fixture
  code changes require a separate `test/...` writer branch.
- **M4-DB01** is the only migration writer; **M4-C01 and M4-T05** are the only contract writers for
  their respective frozen paths. M4-T01/M4-T03/M4-T06/M4-T06B/M4-T08 never edit those shared boundaries.
- **M4-S01** and M2-S01 are sequential owners of the same seed roots and never edit product or migration code.
- Every backend/UI pair is separately dispatched and reviewed even when the wave table shows safe
  parallel execution.

---

## Task Packets

---

### M4-C01 — Knowledge & RAG Contracts

**Owner / Thread:** Knowledge/RAG contract owner — sol high R1
**Branch:** `feature/knowledge-rag-contracts`
**Wave:** 1
**Gate:** M3-I01 `ACCEPTED` on main + accepted retention/embedding decisions

#### Exclusive Paths

- Document, job, chunk, vector, access, citation, chat-session, message, and lifecycle **schema and
  canonical fixture contracts only**.
- No migrations, no backend service code, no UI code.

#### Purpose

Establish the single source of truth for every data shape and interface that the rest of M4 will
depend on. All downstream tasks consume these frozen contracts; any breaking change requires this
task's owner to coordinate across all consumers. The contracts must be complete and reviewed before
any storage, schema, or feature work begins.

#### Key Acceptance Criteria

1. **Tenant / access contracts** — Every document and knowledge-base resource carries explicit
   tenant and permission scope. Access rules are defined in the contract, not inferred at query time.
2. **Deletion / export / retention contracts** — The contract explicitly describes how deletion
   propagates from a knowledge base through documents, chunks, vectors, chat history, and citations.
   Export and retention windows are part of the contract, not implementation details.
3. **Provenance contract** — Every chunk record traces back to an authorized document location.
   The chunk's source, page, section, and version fields are defined here, not discovered later.
4. **Chat state lifecycle** — The contract covers session creation, streaming draft states,
   cancellation, timeout, and completed/failed terminal states. A streaming or failed assistant
   draft is never represented as completed.
5. **Idempotency and pagination contracts** — Message send is idempotent (duplicate send does not
   duplicate content). History pagination is stable (cursor-based or keyset; no offset drift).
6. **Citation contract** — Citations resolve to an authorized document/chunk/page identity. A
   citation is never a raw prompt excerpt. The contract defines what a citation carries and how it
   is reauthorized at display time.
7. **Canonical fixture contracts** — A versioned, deterministic set of test fixtures (documents,
   chunks, vectors, sessions, messages) is defined here and shared with all downstream tasks.

#### STOP Conditions

- Tenant/access boundary missing or underdefined in contract.
- Deletion propagation not specified (objects retrievable after deletion).
- Chat state machine absent or ambiguous about streaming/failed states.
- Provenance not traceable from chunk back to authorized document location.

#### Commit Plan

```
feat(contracts): define knowledge-base and document ownership schema
feat(contracts): add chunk/vector/access/provenance contracts
feat(contracts): add citation and lifecycle contracts
feat(contracts): add chat-session/message/pagination/idempotency contracts
feat(contracts): add canonical M4 fixture manifest
```

---

### M4-DB01 — Schema & Migration Train

**Owner / Thread:** Same schema/migration train owner — sol high R2
**Branch:** `feature/m4-schema-train`
**Wave:** 2
**Gate:** M4-C01 `INTEGRATED`

#### Exclusive Paths

- Knowledge, document, job, chunk, vector, chat-session, message, and trace **migrations** only.
- RLS policies, indexes, and DB-level fixtures.
- No backend service code, no UI code. One writer; M4-C01 and M4-T05 are the only contract writers
  for disjoint frozen paths — this task never edits those boundaries.

#### Purpose

Materialize the M4-C01 contracts as actual Supabase/PostgreSQL schema objects. This is the only
migration writer for all M4 tables. The pgvector extension is enabled and validated here, as are
all RLS policies, indexes, and retention/deletion schema hooks.

#### Key Acceptance Criteria

1. **pgvector setup** — The `vector` extension is enabled via managed Supabase setup with **no
   explicit SQL version clause**. The actually installed `vector` version is recorded in migration
   metadata. The application blocks startup when the installed version falls outside the tested
   compatibility range.
2. **Vector dimension / index** — Vector rows include: tenant ID, document ID, chunk ID, permission
   scope, embedding model name, model revision, dimension, content checksum, and lifecycle state.
   Index type and parameters are documented for a representative corpus query plan.
3. **Chat schema** — `chat_sessions` and `chat_messages` tables are created by this migration
   alone. Message state column covers draft, streaming, completed, cancelled, and failed.
   Revision lineage for regenerated messages is stored explicitly (not overwritten).
4. **RLS / tenant isolation** — Row-level security policies enforce tenant and user ownership on
   every M4 table. Cross-tenant reads cannot succeed even with a valid JWT.
5. **Rollback evidence** — Each migration has a verified rollback path. The rollback is exercised
   and the result recorded.
6. **Retention / deletion hooks** — Schema includes the deletion-propagation hooks defined in
   M4-C01: cascade or soft-delete from knowledge base to documents, chunks, vectors, and chat
   history.

#### STOP Conditions

- Migration writer is not the sole owner of these tables.
- pgvector extension requested at an explicit pinned version not verified against Supabase managed state.
- RLS absent or bypassable by a non-service-role JWT.
- Dimension or model name not stored on the vector row.
- Rollback not verified.

#### Commit Plan

```
feat(db): create knowledge-base, document, job, and chunk migrations
feat(db): add pgvector extension validation and vector table with RLS
feat(db): add chat-session and chat-message migrations with state/lineage columns
feat(db): add retrieval-trace and feedback schema migrations
feat(db): add indexes and retention/deletion cascade hooks
test(db): verify rollback paths and RLS cross-tenant denial
```

---

### M4-T01 — Knowledge Management Backend

**Owner / Thread:** Critical knowledge backend worker — sol high R2
**Branch:** `feature/knowledge-management`
**Wave:** 3A
**Gate:** M4-C01 + M4-DB01 `INTEGRATED`

#### Exclusive Paths

- Spring Boot `knowledge/**` — knowledge-base CRUD, document CRUD, storage operations, job state.
- Backend tests for the above.
- **No migrations, no UI.**

#### Purpose

Implement tenant-safe knowledge bases and documents with private object storage, durable job state,
signed upload/download, and honest progress reporting. This backend exposes the API that M4-U01
will consume (frozen interface for Wave 3B).

#### Key Acceptance Criteria

1. **Tenant ownership** — Every knowledge base, document, and storage object is scoped to a tenant.
   A user in tenant A cannot read, list, or delete resources belonging to tenant B, enforced at the
   service layer (not only at RLS).
2. **Approved formats and ceilings** — Only PDF, Markdown, and plain text are accepted initially.
   File bytes, page count, and batch size have explicit bounded ceilings. Requests exceeding limits
   are rejected with informative errors.
3. **Content-hash deduplication** — A document upload that matches the SHA-256 of an existing
   document in the same knowledge base is rejected or deduplicated as defined by the contract.
4. **Tenant-derived storage keys** — Object paths in storage are derived from tenant and document
   IDs, never from user-supplied filenames. Signed upload and download URLs enforce tenant
   ownership and expire after a bounded TTL.
5. **MIME sniffing** — MIME type is determined by reading file bytes (magic bytes), not by trusting
   the `Content-Type` header or filename extension.
6. **Durable job state** — Document processing jobs persist state to the database with a bounded
   number of retry attempts. A job in `queued` or `running` state that survives a process restart
   is recoverable to a consistent terminal or retry state.
7. **Cancellation and retry policy** — Jobs can be cancelled and retried through the API. Retry
   count is bounded. Permanently failed jobs transition to a `failed` terminal state.
8. **Progress via durable API** — Job progress is obtained from the durable database state.
   Realtime subscriptions may reduce latency but are not required for correctness; polling/refetch
   always reflects the true state.
9. **Deletion contract** — Deleting a knowledge base or document removes the backing storage object
   and marks all derived artifacts (chunks, vectors, job records) for deletion propagation.

#### STOP Conditions

- Public object bucket without RLS policy.
- MIME type trusted from filename or `Content-Type` header.
- Ephemeral-only (in-memory) job state without database persistence.
- Unbounded upload or batch sizes.
- Deletion leaves storage object or derived chunks retrievable.
- Service-role or storage secret reaches browser.

#### Commit Plan

```
feat(knowledge): add tenant knowledge-base and document CRUD
feat(knowledge): add secure storage upload contract with tenant-derived paths
feat(knowledge): add MIME sniffing and format/size ceiling enforcement
feat(jobs): persist document processing job state with bounded retry
feat(jobs): implement cancellation and restart recovery
test(knowledge): cross-tenant isolation, dedup, and deletion propagation
```

---

### M4-U01 — Knowledge Workspace UI

**Owner / Thread:** Knowledge workspace UI worker — terra high R1
**Branch:** `feature/knowledge-workspace-ui`
**Wave:** 3B
**Gate:** M4-T01 API `frozen_interfaces` exact head (head movement blocks both tasks)

#### Exclusive Paths

- Next.js `admin/knowledge/**` — knowledge list, upload, job progress, and recovery UI only.
- No migrations, no backend code.

#### Purpose

Provide the admin-facing workspace for managing knowledge bases and documents. The UI surfaces
upload progress, job status, error states, and retry/cancel controls. It is built against the exact
frozen API surface exported by M4-T01.

#### Key Acceptance Criteria

1. **Upload flow** — Authorized users can upload documents; unauthorized users and cross-tenant
   attempts are denied with visible error states (not silent failures).
2. **Progress and polling** — Job progress is always obtainable through polling/refetch. Realtime
   subscriptions may improve latency but are not required. The UI accurately reflects the durable
   job state returned by the API.
3. **Job lifecycle UI states** — The UI covers: queued, running, completed, failed, cancelled, and
   retry. State labels are truthful — no fixture state is labeled as live progress.
4. **Error and empty states** — Upload rejection (wrong type, too large, duplicate), empty
   knowledge-base list, and API failure states all have explicit, informative UI treatments.
5. **Retry and cancel controls** — Users can retry a failed job and cancel a running job through
   the UI, with optimistic update and confirmation feedback.
6. **Responsive and accessible** — All states render correctly at 375 px viewport width. Keyboard
   navigation and accessible labels are verified for the file-upload control and job-state indicators.

#### STOP Conditions

- Progress state only sourced from Realtime without polling fallback.
- Live job state labeled as fixture/mock.
- Upload control inaccessible by keyboard.
- Cross-tenant denial not shown as an explicit error state.

#### Commit Plan

```
feat(admin): add knowledge management workspace list and base detail views
feat(admin): add document upload flow with progress and error states
feat(admin): add job lifecycle state display with retry and cancel controls
test(ui): keyboard, 375px, and fixture/live label accuracy checks
```

---

### M4-T02 — Document Ingestion

**Owner / Thread:** Ingestion worker — terra high R2
**Branch:** `feature/document-ingestion`
**Wave:** 4
**Gate:** M4-T01 `INTEGRATED`

#### Exclusive Paths

- Platform `knowledge/ingestion/**` — parser adapters, chunker, job worker.
- Chunk migration metadata and parser fixtures.
- Worker configuration.
- **No migrations (schema owned by M4-DB01), no UI.**

#### Purpose

Safely extract, normalize, and chunk documents into versioned metadata with durable indexing state
and reproducible output. The ingestion pipeline is the critical security boundary between raw
uploaded bytes and the searchable knowledge corpus.

#### Key Acceptance Criteria

1. **Format scope** — Only PDF, Markdown, and plain text are supported. URL ingestion is disabled
   until independent SSRF controls pass review.
2. **Extraction bounds** — Each parser runs under explicit limits: max file bytes, max pages, max
   extracted text length, max chunks, max tokens per chunk, decompression ratio ceiling, parser CPU
   time, and memory. Any limit breach terminates parsing and transitions the job to a failed state.
3. **Least privilege** — The parser process runs with no network access where possible. No
   outbound calls are made during ingestion.
4. **Normalization and provenance** — Extracted text is normalized and each chunk carries: source
   document ID, source storage path, page number, section identifier, and checksum of the source
   page/range. Provenance is non-optional; a chunk cannot exist without a verifiable source.
5. **Deterministic chunking** — The chunking strategy and version identifier are stored alongside
   each chunk. The same input always produces the same chunks and checksums.
6. **Durable indexing state** — Chunk indexing-state transitions (pending, indexed, failed,
   tombstoned) are durable. A restart mid-ingestion resumes to a consistent state without creating
   duplicate active chunks.
7. **Hostile fixture coverage** — Tests include: corrupt/encrypted files, empty files, oversized
   files, decompression bombs, parser timeout, parser crash, and duplicate job submission.
8. **Deletion propagation** — When a document is deleted, all derived chunks are tombstoned and
   removed from future retrieval eligibility.

#### STOP Conditions

- Parser has unrestricted network access (SSRF vector).
- URL ingestion enabled without SSRF gate.
- Chunk exists without source provenance mapping.
- Duplicate active chunks created by duplicate job submission.
- Sensitive raw content (document text) appears in logs or traces.
- Ingestion limits absent or unbounded.

#### Commit Plan

```
feat(rag): add bounded document extraction with format-specific adapters
feat(rag): normalize and chunk source documents with deterministic versioning
feat(rag): add durable chunk indexing state with restart recovery
feat(rag): add deletion propagation hooks for derived chunks
test(security): add hostile ingestion fixtures (corrupt, bomb, timeout, duplicate)
```

---

### M4-T03 — Vector Retrieval (PGVector)

**Owner / Thread:** Critical vector worker — sol high R2
**Branch:** `feature/vector-retrieval`
**Wave:** 5
**Gate:** M4-T02 + M4-DB01 `INTEGRATED`

#### Exclusive Paths

- Platform `rag/embedding/**` and `rag/vector/**` — embedding provider adapter, vector query,
  index-plan tests.
- Provider configuration by variable name.
- Fixtures.
- **No migrations (owned by M4-DB01).**

#### Purpose

Generate and persist embeddings with explicit model and dimension provenance, enforce tenant and
permission constraints before candidate output, and provide bounded, safe similarity queries backed
by pgvector indexes.

#### Key Acceptance Criteria

1. **Embedding provider abstraction** — The provider is behind a formal abstraction with explicit
   timeout, per-request concurrency limit, per-call token limit, and a documented cost bound per
   batch. The application does not call any provider directly.
2. **Deterministic CI provider** — A fake/local embedding provider produces deterministic vectors
   for CI without requiring a paid API. Live-provider smoke tests are separated and not required to
   pass in CI.
3. **Vector row metadata** — Each persisted vector row includes: tenant ID, document ID, chunk ID,
   permission scope, model name, model revision, vector dimension, content checksum, and lifecycle
   state. No field is optional.
4. **Pre-candidate authorization** — Query predicates enforce tenant and permission constraints
   **before** returning any candidate. Unauthorized chunks are absent from the candidate set, not
   merely filtered from the final result.
5. **Dimension / model version safety** — A dimension or model change does not silently mix
   incompatible vectors in a query. New model/dimension uses separate storage or a complete
   versioned reindex. Dimension mismatch is rejected at insert and query time.
6. **Reindex idempotency** — A reindex operation run twice produces the same result as running it
   once. Partial reindex does not leave orphaned or duplicate active vectors.
7. **Source deletion / tombstone** — When a source chunk is tombstoned, its vector is removed from
   future retrieval eligibility within a bounded propagation window.
8. **Query plan documentation** — Index type, parameters, and representative query plan output are
   recorded for a defined corpus size.

#### STOP Conditions

- Authorization applied after candidate set is produced.
- Provider output dimension untracked or mismatched vectors mixed silently.
- Unbounded reindex or unbounded cost.
- Mock/fake provider result presented as a live provider result.
- Explicit pgvector extension version requested in SQL without verification against the managed
  Supabase installed version.
- Provider secret or raw document body appears in logs or traces.

#### Commit Plan

```
feat(rag): add embedding provider abstraction with timeout and cost bounds
feat(rag): add deterministic CI provider and live-provider smoke separation
feat(rag): persist tenant-scoped vectors with full provenance metadata
feat(rag): enforce pre-candidate tenant/permission predicates on similarity queries
test(rag): verify similarity, index plan, permission predicates, and reindex idempotency
```

---

### M4-T04 — Hybrid Retrieval

**Owner / Thread:** Retrieval worker — sol high R2
**Branch:** `feature/hybrid-retrieval`
**Wave:** 6
**Gate:** M4-T03 `INTEGRATED`

#### Exclusive Paths

- Platform `rag/retrieval/**` — full-text search adapter, vector adapter, RRF/fusion logic.
- Lexical indexes and migration additions (coordinated with M4-DB01 owner).
- Evaluation corpus fixtures.
- Retrieval result contract and fusion logic have one owner.

#### Purpose

Combine authorized lexical (full-text) and vector retrieval through a deterministic fusion
contract with configurable, bounded top-K and measurable quality. Both retrieval branches apply
equivalent tenant and permission predicates.

#### Key Acceptance Criteria

1. **Lexical adapter** — Full-text search with a declared language/tokenization configuration.
   The adapter returns normalized result objects with: document ID, chunk ID, source provenance,
   BM25 or equivalent score, and any ranking metadata.
2. **Vector adapter** — Wraps M4-T03 retrieval and returns the same normalized result object shape
   as the lexical adapter.
3. **Predicate parity** — Both lexical and vector paths apply **equivalent** tenant and permission
   predicates. An authorized chunk that appears in one branch's candidate set must apply the same
   authorization rules in the other branch.
4. **Fusion algorithm** — Reciprocal-rank fusion (RRF) or the accepted alternative is implemented
   as a deterministic, versioned function. The same query, corpus, and configuration always produce
   the same ranking.
5. **Bounded top-K** — The maximum number of candidates and the maximum final context size are
   declared in configuration with safe defaults. Queries cannot produce an unbounded candidate set.
6. **Fallback behavior** — If one retrieval branch is unavailable or returns zero results, the
   fusion layer degrades gracefully to the available branch without weakening authorization.
7. **Evaluation corpus** — A versioned evaluation corpus with declared Recall@K, MRR, or equivalent
   metrics covers: exact-match queries, semantic queries, rare-term queries, and no-match queries.
   Corpus provenance (source documents and version) is recorded.
8. **Latency and query plan** — Per-query latency under representative load and the query execution
   plan are documented. Limitations are stated explicitly.

#### STOP Conditions

- Different authorization predicates applied by lexical versus vector paths.
- Fusion ranking not reproducible (non-deterministic).
- Candidate set unbounded.
- Evaluation corpus or its provenance not recorded or private data leaked into fixtures.

#### Commit Plan

```
feat(rag): add authorized lexical retrieval adapter with language configuration
feat(rag): add vector retrieval adapter with normalized result contract
feat(rag): implement deterministic hybrid result fusion (RRF)
feat(rag): add query configuration with bounded top-K defaults
test(rag): add retrieval evaluation corpus with Recall@K and MRR baselines
```

---

### M4-T05 — RAG Interaction Contracts

**Owner / Thread:** RAG interaction contract owner — sol high R1
**Branch:** `feature/rag-interaction-contracts`
**Wave:** 7
**Gate:** M4-T04 + M4-C01 `INTEGRATED`

#### Exclusive Paths

- Stream protocol, citation, no-answer, provider, and trace contracts.
- Conversation/history contracts: session, message, pagination, idempotency, lifecycle, and
  regeneration lineage.
- **No backend implementation, no migrations, no UI.**

#### Purpose

Freeze the interaction contract that separates the Wave 8A backend (M4-T06) from the Wave 8B
backend (M4-T06B) and the Wave 9A frontend (M4-T07). No feature work in those three tasks may
begin until this contract is reviewed and the exact HEAD is integrated.

#### Key Acceptance Criteria

1. **Stream protocol contract** — Defines the SSE event types, payload shapes, and sequence for:
   start, delta, citation, no-answer, error, and done events. Stream delivery is same-origin only.
2. **Citation contract** — A citation event carries: chunk ID, document ID, page reference,
   display title, and the URL pattern for source-open. Citations are never raw prompt text.
3. **No-answer / low-confidence policy** — The contract defines the explicit response behavior when
   no authorized context meets the confidence threshold: a declared no-answer state, not a
   fabricated answer.
4. **Provider contract** — Defines the provider interface: inputs (authorized context only), output
   (stream deltas), and required behaviors (timeout, cancellation, rate-limit propagation). Provider
   credentials are never exposed to the frontend.
5. **Conversation / history contract** — Defines session creation and lookup, message send
   (idempotent by client-generated idempotency key), stable cursor-based pagination, resume,
   cancel, and regenerate-as-new-revision semantics.
6. **Message state lifecycle** — Message states (draft, streaming, completed, cancelled, failed)
   are enumerated. Regenerated messages create a new revision; the prior revision is retained, not
   overwritten.
7. **Trace / observability contract** — Defines the fields in a retrieval run trace: safe IDs,
   tenant, query hash, corpus/model/config version, candidate IDs and scores, latency/token/cost
   counters, citation IDs, and outcome. Raw prompt or source text is excluded from the default
   trace.

#### STOP Conditions

- Backend or UI work begins before this contract is reviewed and the exact HEAD integrated.
- Stream protocol allows cross-origin delivery.
- No-answer policy undefined (fabricated answer substituted).
- Provider contract exposes credentials or raw context to the frontend.
- Regeneration overwrites prior message revision.

#### Commit Plan

```
feat(contracts): define stream protocol and citation event contracts
feat(contracts): add no-answer and low-confidence policy contract
feat(contracts): add provider abstraction interface contract
feat(contracts): add conversation/session/message/pagination/idempotency contracts
feat(contracts): add retrieval-trace observability contract
```

---

### M4-T06 — Secure RAG API

**Owner / Thread:** Critical RAG API worker — sol high R2
**Branch:** `feature/secure-rag-api`
**Wave:** 8A (parallel with M4-T06B after M4-T05 integrated)
**Gate:** M4-T05 `INTEGRATED`

#### Exclusive Paths

- Platform `rag/query/**` and `rag/provider/**` — context construction, provider call, stream
  delivery, and tests.
- **No migration, no conversation persistence, no UI.**

#### Purpose

Implement the permission-aware context construction, provider call, and streaming answer delivery
defined by the M4-T05 contracts. This task owns the retrieval-to-answer path: enforcing
authorization both at retrieval and at context assembly, calling the provider with minimal
authorized context, and streaming results back to the client.

#### Key Acceptance Criteria

1. **Double authorization** — Authorization is enforced during retrieval (M4-T04 predicates) and
   rechecked before context assembly. An unauthorized candidate that passes retrieval filters cannot
   enter the context window.
2. **Injection resistance** — Sources and retrieved chunks are treated as untrusted data. The
   prompt construction does not allow source text to act as higher-priority instructions. Tests
   demonstrate injection resistance with adversarial fixtures.
3. **Context bounds** — Token and source count limits are enforced at context construction time.
   The provider never receives more context than the declared maximum.
4. **Provider minimal context** — The provider receives only the authorized, bounded context. Raw
   document bodies, user identity details, and system internals are not sent to the provider.
5. **Streaming delivery** — The SSE stream follows the M4-T05 protocol. Cancellation and timeout
   are handled: a cancelled or timed-out stream transitions to a cancelled or failed terminal state,
   never to a completed state.
6. **Citation fidelity** — Every citation in the stream resolves to a real authorized chunk/page.
   No fabricated or hallucinated citations are accepted.
7. **Provider failure bounds** — Rate-limit, timeout, and transient provider failures are bounded
   with explicit retry policy and backoff. Unbounded retries or silent failure are not permitted.
8. **Log / trace safety** — Prompt text, source text, and provider responses are redacted from
   default logs and traces. Only safe identifiers and counters appear in telemetry.

#### STOP Conditions

- Unauthorized candidate or context in any tested scenario.
- Provider receives raw document body or user credentials.
- Prompt injection succeeds in adversarial tests.
- Streaming draft labeled as completed on cancellation or timeout.
- Provider fallback weakens authorization or context policy.
- Live provider claim from a deterministic mock.

#### Commit Plan

```
feat(rag): filter retrieval candidates by effective tenant/permission predicates
feat(rag): build bounded grounded context with injection resistance
feat(rag): stream authorized answers with resolvable citations via SSE
feat(rag): add provider abstraction with cancellation, timeout, and rate-limit bounds
test(security): injection, cross-tenant, fabricated-citation, and log-redaction tests
```

---

### M4-T06B — Conversation Persistence API

**Owner / Thread:** Conversation persistence API worker — sol high R2
**Branch:** `feature/rag-conversation-api`
**Wave:** 8B (parallel with M4-T06 after M4-T05 integrated)
**Gate:** M4-T05 + M4-DB01 `INTEGRATED`

#### Exclusive Paths

- Chat session, message, history, export, and delete **backend paths and tests only**.
- **No retrieval provider code, no migrations, no UI.**

#### Purpose

Implement the persistent, tenant-scoped conversation layer as defined by M4-T05 contracts. This
task owns session and message CRUD, history pagination, idempotent message send, stream state
persistence, resume/regenerate lineage, deletion, and historical source reauthorization. It is
intentionally separated from the retrieval provider path (M4-T06) to enable parallel execution
without a shared contract owner conflict.

#### Key Acceptance Criteria

1. **Tenant / user ownership** — Every chat session and message is scoped to a tenant and a
   subject (user). A user cannot read, list, or delete sessions or messages belonging to another
   tenant or another user within the same tenant.
2. **Stable pagination** — History listing uses cursor-based or keyset pagination. Page boundaries
   do not drift when new messages are inserted during pagination.
3. **Idempotent message send** — A message send with a client-generated idempotency key is safe to
   retry. A duplicate send does not create a duplicate message record or trigger a duplicate
   provider call.
4. **Persisted stream state** — Each assistant message has an explicit persisted state: draft,
   streaming, completed, cancelled, or failed. State transitions are durable: a process restart
   does not reset a streaming message to an incorrect state.
5. **Resume and regenerate** — A session can be resumed after disconnection. Regeneration creates a
   new message revision linked to the prior revision; the prior revision is retained with its
   original state and content.
6. **Deletion propagation** — Session deletion cascades to messages, and message deletion is
   propagated to associated citation records. Deleted sessions and messages are no longer served.
7. **Historical source reauthorization** — When a user opens a citation from a historical message,
   the source access is reauthorized at the time of access. If the user no longer has access, an
   `unavailable-source` state is returned rather than the document content.
8. **Export** — Session export produces a complete, sanitized record including messages, states,
   and citation identifiers. Raw source text is not included by default.

#### STOP Conditions

- Session or message readable across tenant or user boundaries.
- Duplicate send creates duplicate message or duplicate provider call.
- Stream state reverts incorrectly after restart.
- Regeneration overwrites prior message revision.
- Deleted session or messages still served.
- Historical citation returns source content without reauthorizing current access.

#### Commit Plan

```
feat(chat): add tenant-scoped chat session creation and lookup
feat(chat): add idempotent message send with state lifecycle
feat(chat): add stable cursor-based history pagination
feat(chat): add resume, cancel, and regenerate-as-new-revision semantics
feat(chat): add session/message deletion with cascade propagation
feat(chat): add historical source reauthorization on citation open
test(chat): cross-tenant denial, idempotency, pagination stability, and deletion tests
```

---

### M4-T07 — RAG Chat UI

**Owner / Thread:** Frontend RAG worker — terra high R2
**Branch:** `feature/rag-chat-ui`
**Wave:** 9A
**Gate:** M4-T06 + M4-T06B `INTEGRATED`

#### Exclusive Paths

- Next.js `knowledge/chat/**` — chat interface and history views.
- `packages/ui-ai/**` — owned adapters wrapping any AI component library.
- Citation renderer.
- **No backend code, no migrations.**

#### Purpose

Deliver the user-facing RAG chat experience including streaming answer display, citation rendering,
conversation history, and full lifecycle controls (cancel, regenerate, delete). The UI is built
against the exact M4-T05 stream and conversation contracts. AI component library components are
used only behind owned adapters; authorization, persistence, SSE, citation, and sanitization
contracts remain outside the library boundary.

#### Key Acceptance Criteria

1. **Same-origin SSE** — The streaming connection is same-origin only. Cross-origin SSE is not
   accepted.
2. **Stream state display** — The UI correctly represents: streaming (in-progress indicator),
   completed, cancelled (explicit user-visible state), and failed (error state with retry
   affordance). A streaming message is never displayed as completed until the server sends the done
   event.
3. **History load and reload** — Conversation history loads correctly on page load and after
   reconnection. Cursor-based pagination is supported without drift.
4. **Citation renderer** — Citations are displayed with display title and a safe source-open link.
   The renderer sanitizes citation content before display (XSS prevention). A citation that cannot
   be reauthorized is shown as an unavailable-source state, not as document content.
5. **Cancel and regenerate** — The user can cancel an in-progress stream. Regeneration produces a
   new revision; the prior revision remains accessible in history.
6. **Delete** — The user can delete a session or individual messages. Deleted items are removed
   from the UI and confirmed by subsequent API calls.
7. **Partial / no-answer states** — The UI explicitly displays the no-answer state returned by the
   API. Low-confidence is not silently converted to a confident answer.
8. **Error and denied states** — Provider failure, rate-limit, timeout, and authorization-denied
   states each have explicit UI treatments.
9. **Responsive and accessible** — All states render correctly at 375 px viewport width. Keyboard
   navigation, focus management, and accessible labels are verified.
10. **No hidden chain-of-thought or browser credentials** — The UI never exposes internal reasoning
    chains, raw prompt text, or provider API credentials via DevTools, network inspector, or DOM.

#### STOP Conditions

- Cross-origin SSE accepted.
- Streaming message displayed as completed before the done event.
- Citation renderer does not sanitize content (XSS).
- No-answer state converted to a confident answer.
- Provider credentials or raw prompt text visible in browser.

#### Commit Plan

```
feat(ui): add RAG chat interface with SSE streaming display
feat(ui): add conversation history with cursor pagination and reload
feat(ui): add citation renderer with sanitization and unavailable-source state
feat(ui): add cancel, regenerate, and delete lifecycle controls
feat(ui): add partial/no-answer/error/denied explicit UI states
test(ui): 375px, keyboard, a11y, XSS, and fixture/live label checks
```

---

### M4-S01 — Demo Seed (RAG)

**Owner / Thread:** RAG demo seed writer — terra high R1
**Branch:** `test/m4-demo-seed-rag`
**Wave:** 9B (parallel with M4-T07)
**Gate:** M4-T03 + M4-T07 `INTEGRATED`; M2-S01 accepted seed manifest

#### Exclusive Paths

- Sequential extension of `test-fixtures/demo/**` and `database/seed/**` only.
- **No product code, no migrations, no retrieval/embedding implementation.**

#### Purpose

Provide a named, versioned, idempotent synthetic dataset that supports end-to-end RAG demo and
evaluation scenarios. The seed supplies the documents, users, expected chunks, citation fixtures,
and allow/deny query pairs consumed by the integration gate (M4-I01) and the quality evaluation
task (M4-T08).

#### Key Acceptance Criteria

1. **Named synthetic documents** — A declared set of synthetic documents with known content,
   expected chunk count, and expected chunk checksums. All documents are synthetic; no real or
   proprietary content is included.
2. **Named users with defined access** — Seed users have explicit knowledge-base and document
   access grants and denials declared in the seed manifest.
3. **Expected citations** — For a declared set of seed queries, expected citation IDs are listed.
   The evaluation suite asserts these citations appear in the actual response.
4. **Allow / deny query pairs** — For each query, there is an explicit list of chunk IDs that
   should appear (allow) and chunk IDs that should never appear for an unauthorized user (deny).
5. **Idempotent reset** — Running the seed script twice produces the same state as running it once.
   Partial seed state from a prior run is cleaned up before insertion.
6. **No secret or production target** — The seed script does not reference any production
   environment variable, real API key, or production database connection. It is safe to run in CI.
7. **Manifest compatibility** — The seed extends the M2-S01 accepted seed manifest without
   breaking it. Sequential ownership is maintained.

#### STOP Conditions

- Real or proprietary document content in seed.
- Seed not idempotent (duplicate records on second run).
- Secret or production connection string present.
- Seed breaks M2-S01 accepted seed manifest.
- Allow/deny query pairs absent or untested.

#### Commit Plan

```
feat(seed): add M4 RAG synthetic document corpus with expected chunks and checksums
feat(seed): add seed users with explicit allow/deny knowledge-base access
feat(seed): add expected citation fixtures and allow/deny query pairs
test(seed): verify idempotent reset and no-secret scan
```

---

### M4-T08 — RAG Quality (Reranking & Observability)

**Owner / Thread:** RAG quality backend worker — terra high R2
**Branch:** `feature/rag-quality`
**Wave:** 10A
**Gate:** M4-T06 + M4-T07 + M4-S01 `INTEGRATED`

#### Exclusive Paths

- Platform `rag/rerank/**` — optional reranker abstraction and provider adapter.
- Redacted retrieval trace and feedback API.
- Evaluation scripts and reports.
- **No migrations (owned by M4-DB01), no UI (owned by M4-U02).**

#### Purpose

Add an optional reranking layer behind an abstraction and make retrieval behavior inspectable and
evaluable through redacted traces, score inspection, feedback capture, and reproducible evaluation
scripts. The reranker is enabled by default only if reproducible evaluation demonstrates a useful
quality/cost trade-off on the M4-S01 seed corpus.

#### Key Acceptance Criteria — Reranking

1. **Authorization before reranking** — The reranker receives only already-authorized candidates
   from M4-T04. Reranking does not expand or bypass the authorized candidate set.
2. **Provider abstraction** — Model name, model version, input token limit, timeout, and per-call
   cost are explicit in the provider adapter configuration.
3. **Stable fallback** — If the reranker times out, errors, or is disabled, the original hybrid
   fusion ranking from M4-T04 is returned unchanged. Authorization and citation identity are
   preserved in fallback.
4. **Measurable benefit** — A reproducible evaluation comparing Recall@K, MRR, citation precision,
   latency, and cost before/after reranking on the M4-S01 seed corpus. The reranker is enabled by
   default only if the evaluation shows a positive trade-off. A decision to disable by default is
   acceptable and must be recorded.
5. **Long-text truncation** — Inputs exceeding the reranker's token limit are truncated
   deterministically before submission, not silently dropped.

#### Key Acceptance Criteria — Observability

6. **Safe trace fields** — Traces record: safe IDs, tenant ID, query hash (not raw query text),
   corpus version, model/config version, candidate IDs and scores at each stage, stage-level
   latency, token and cost counters, citation IDs, and outcome. Raw prompt text, context text, and
   source text are disabled from the default trace.
7. **Redaction and retention** — Traces have a declared retention window. A delete request removes
   the trace record. Tests verify that raw sensitive content is absent from default telemetry.
8. **Feedback capture** — Feedback can be attached to a retrieval run by an authorized user.
   Feedback is rate-limited and abuse-controlled. Feedback cannot be submitted for a retrieval run
   belonging to another user.
9. **Evaluation reproducibility** — The evaluation script can be run against the M4-S01 seed
   corpus checksum and a declared config version and produce the same Recall@K, citation precision,
   and no-answer rate. Results include model, parameters, date, and corpus digest.
10. **Cost and latency bounded** — Trace overhead and reranker cost are measured and declared.
    Sampling policy is explicit.

#### STOP Conditions — Reranking

- Reranking applied before authorization.
- Silent fallback changes ranking policy without recording the change.
- Quality claim made without fixed corpus and reproducible script.
- Reranker cost or latency unbounded.

#### STOP Conditions — Observability

- Raw sensitive prompt or context text in default logs or traces.
- Cross-tenant trace readable.
- Evaluation not reproducible from corpus checksum and config.
- Dashboard or report fabricates quality metrics.

#### Commit Plan

```
feat(rag): add optional reranker abstraction with provider adapter and fallback
feat(rag): record redacted retrieval-run traces with stage-level instrumentation
feat(rag): add feedback capture API with rate limiting and ownership checks
feat(rag): add versioned evaluation CLI with Recall@K and citation precision
test(rag): benchmark reranking quality, cost, and latency vs baseline
test(rag): verify trace redaction, retention, and cross-tenant denial
```

---

### M4-U02 — RAG Quality UI

**Owner / Thread:** RAG quality UI worker — terra high R1
**Branch:** `feature/rag-quality-ui`
**Wave:** 10B
**Gate:** M4-T08 API / fixture `frozen_interfaces` exact head (head movement blocks both tasks)

#### Exclusive Paths

- Next.js trace inspection, feedback, and evaluation admin UI only.
- **No backend code, no migrations.**

#### Purpose

Provide admin-facing visibility into RAG retrieval quality: trace inspection per retrieval run,
feedback review, and evaluation report display. The dashboard is built against the exact frozen API
and fixture surface exported by M4-T08.

#### Key Acceptance Criteria

1. **Trace inspection** — Authorized admins can view a retrieval run trace: candidate IDs and
   scores at each stage, latency breakdown, citation IDs, and outcome. Cross-tenant traces are
   denied with an explicit error state.
2. **Redacted content** — The dashboard never displays raw prompt text, raw context text, or raw
   source document content. Only safe identifiers, scores, and counters are shown.
3. **Truthful labels** — Fixture data and live data are explicitly labeled. A dashboard populated
   from seed fixtures is labeled as such. A live trace is labeled with its actual retrieval date
   and config version.
4. **Evaluation report display** — The dashboard can display a saved evaluation report including
   Recall@K, citation precision, no-answer rate, model name, parameters, corpus digest, and date.
5. **Feedback review** — Admins can view feedback submitted for a retrieval run and can delete
   feedback within their tenant scope.
6. **Complete UI states** — The dashboard covers: loading, empty (no traces yet), populated, error,
   and access-denied states. None default to a fabricated or cached state.
7. **Responsive and accessible** — All states render correctly at 375 px viewport width. Keyboard
   navigation and accessible labels are verified.

#### STOP Conditions

- Raw prompt or source text visible in the dashboard.
- Cross-tenant trace view not denied.
- Fixture-populated dashboard not labeled as fixture.
- Evaluation report fabricated or not sourced from actual evaluation script output.

#### Commit Plan

```
feat(admin): add RAG trace inspection dashboard with per-stage score display
feat(admin): add feedback review and deletion for authorized admin
feat(admin): add evaluation report display with corpus and config provenance
test(ui): 375px, keyboard, fixture/live label, and cross-tenant denial checks
```

---

### M4-I01 — M4 Integration Gate

**Owner / Thread:** Tester + dedicated security reviewer + Git Manager
**Branch:** `integration/v0.1-m4`
**Wave:** 11
**Gate:** M4-T08 + M4-U01 + M4-U02 + M4-S01 all `INTEGRATED` + security review

#### Exclusive Paths

- Mechanical `MERGE_READY` heads only — no semantic edits.
- Integration branch output only.

#### Purpose

Close the M4 milestone by mechanically integrating all verified worker heads, running the complete
end-to-end test suite against the M4-S01 seed corpus, and obtaining the mandatory security review
and dual milestone receipts before merging to main.

#### Key Acceptance Criteria

1. **Full end-to-end scenario** — The integration gate must pass the following sequence on the same
   exact integrated heads:
   - Upload a seed document → observe ingestion job progress → confirm chunk/vector creation.
   - Execute a hybrid retrieval query → confirm authorized candidates only.
   - Start a chat session → stream a grounded answer → verify citation IDs resolve.
   - Resume a session after disconnect → verify history loads and stream state is correct.
   - Cancel a streaming message → verify terminal cancelled state, not completed.
   - Regenerate a message → verify new revision created, prior revision retained.
   - Delete a session → verify messages no longer served.
2. **Unauthorized context is STOP** — Any retrieval test in which an unauthorized chunk ID appears
   in context, history, or citation output causes the gate to halt immediately. This is a
   non-negotiable stopping condition.
3. **Security review** — A dedicated security reviewer inspects: cross-tenant isolation, prompt
   injection resistance, citation fabrication, log/trace redaction, and provider credential
   handling. The security review produces an identified PASS receipt.
4. **Same-head dual receipts** — The Advisor FIT receipt and the Kongming PASS receipt are
   produced against the exact same integrated candidate HEAD. No semantic edits are permitted
   after the receipts are issued.
5. **No semantic edits** — The Git Manager performs only mechanical merge operations. No code
   changes are authored during integration.

#### STOP Conditions

- Unauthorized candidate, context, or history appears in any test scenario.
- Security review not completed before merge to main.
- Any worker head moves after dual receipts are issued.
- Semantic edits made during the integration merge.
- End-to-end scenario does not complete successfully on the exact integrated heads.

#### Commit Plan

Integration branch only — no authored commits. The Git Manager records the `INTEGRATED` evidence
head SHA. The final merge commit to main includes the security review receipt and dual milestone
receipt identifiers in the merge commit message.

```
merge(m4): integrate all M4 MERGE_READY heads
# merge commit authored by Git Manager with security + dual receipt reference
```

---

## M4 Governance Notes

### Owner Boundaries Summary

| Role | Tasks | What they never touch |
|---|---|---|
| Contract owner (sol R1) | M4-C01, M4-T05 | Migrations, backend impl, UI |
| Migration train (sol R2) | M4-DB01 | Backend impl, UI, contracts |
| Backend workers (sol R2) | M4-T01, M4-T02, M4-T03, M4-T04, M4-T06, M4-T06B, M4-T08 | Each other's exclusive paths, migrations |
| UI workers (terra R1/R2) | M4-U01, M4-U02, M4-T07 | Backend impl, migrations |
| Seed writer (terra R1) | M4-S01 | Product code, migrations, retrieval impl |
| Integration (Git Manager) | M4-I01 | Semantic edits of any kind |

### Dependency Spine

```
M3-I01 ACCEPTED
    └─ M4-C01 (contracts)
           └─ M4-DB01 (migrations)
                  ├─ M4-T01 (knowledge backend) ─── [frozen_interfaces] ─── M4-U01 (knowledge UI)
                  │       └─ M4-T02 (ingestion)
                  │               └─ M4-T03 (vector retrieval) ─────────────────────────────────┐
                  │                       └─ M4-T04 (hybrid retrieval)                          │
                  │                               └─ M4-T05 (RAG contracts) ◄─ M4-C01           │
                  │                                   ├─ M4-T06 (RAG API) ──────────────────── ─┤
                  └─ M4-T06B (conversation API) ──────┘                                         │
                                                          └─ M4-T07 (chat UI) ◄─────────────────┘
                                                                  └─ M4-S01 (seed) ◄─ M4-T03
                                                                          └─ M4-T08 (quality)
                                                                              ├─ M4-U02 (quality UI)
                                                                              └─ M4-I01 → main
```

### Key Cross-Cutting Rules

1. **Authorization is pre-candidate, always.** No task may apply permission filters after
   candidates are produced. Any authorization applied post-retrieval is a STOP.
2. **Secrets never reach the browser.** Service-role credentials, storage secrets, and provider
   API keys are backend-only. This is a STOP if violated anywhere in M4.
3. **Frozen interfaces block both owners.** When a task is consumed under `frozen_interfaces`,
   the producer task cannot move its HEAD until the consumer task completes its review. Both tasks
   coordinate on any interface change.
4. **One migration writer.** M4-DB01 is the sole author of all M4 migrations. No other task may
   create or modify migration files.
5. **Deterministic CI.** No task may require a paid external provider to pass CI. Deterministic
   stubs are mandatory for all external dependencies (embedding, reranking, LLM).
6. **Sensitive content out of logs.** Raw prompt text, source document text, and provider
   responses are redacted from default telemetry across all tasks. This is audited at the M4-I01
   gate.
