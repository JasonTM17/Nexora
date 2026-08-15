# ADR — M4 Knowledge Management and Secure RAG Architecture

> Status: `ACCEPTED` — verified against implemented codebase (36/36 M4 tests
> pass, migrations V022-V024 applied). This ADR records the as-built architecture
> for the knowledge and RAG subsystem.

## Context

Nexora M4 extends the tenant CMS (M2) and event spine (M3) with a knowledge
management and retrieval-augmented generation (RAG) subsystem. Organizations need
to ingest documents, extract and embed their content, retrieve relevant passages
using hybrid lexical + vector search, and generate answers using only
tenant-authorized context — with durable job progress, persistent conversation
history, and verifiable citations.

The core tension: vector retrieval and LLM generation are powerful but
inherently cross-tenant dangerous. A naive implementation could leak one
tenant's documents into another's RAG context. The architecture must enforce
**permission-before-context**: no chunk, vector, or citation enters the LLM
prompt unless the requesting subject has active read permission on the owning
tenant.

## Decision

### 1. Document Ingestion Pipeline

Documents enter through a bounded, tenant-scoped ingestion pipeline:

- **Upload → authorized Storage** (private, tenant-keyed)
- **Job tracking** (`knowledge_jobs` table) with durable progress events
  published to the M3 outbox/NATS spine
- **Chunking** with pluggable `ChunkingStrategy` (paragraph, fixed-window,
  semantic-boundary)
- **Extraction** via `DocumentExtractor` (PDF, DOCX, Markdown, plain text)
- **Embedding** via pluggable `EmbeddingProvider` (deterministic for dev,
  TEI/remote for prod)
- **Persistence** to `knowledge_chunks` with pgvector `vector(1024)`

### 2. Hybrid Retrieval

Retrieval combines two paths with a pluggable `Reranker`:

- **Lexical** (PostgreSQL full-text) — exact term matching, language-aware
- **Vector** (pgvector cosine distance `<=>` operator) — semantic similarity
- **Reranker** merges and re-scores candidates before authorization filtering

### 3. Permission-Before-Context RAG

The secure query path enforces authorization at every transition:

1. Resolve subject + tenant from JWT + `X-Nexora-Organization-Id` header
2. Retrieve candidates (hybrid) — **before** authorization filter
3. **Authorization filter**: drop chunks where subject lacks `knowledge.read`
   on the owning tenant
4. Build LLM `Context` from authorized chunks only
5. Generate via pluggable `ChatProvider` (deterministic for dev, DeepSeek for
   prod)
6. Return answer with **citations resolvable only to authorized sources**

### 4. Persistent Conversation History

Chat sessions and messages are tenant-scoped and durable:

- `conversation` + `conversation_message` tables with RLS
- Operations: create, append, regenerate, delete, reload — all tenant-bound
- Raw prompts excluded from traces and logs (privacy-by-default)

### 5. Deterministic Evaluation

A bounded deterministic mode (`DeterministicEmbeddingProvider`,
`DeterministicChatProvider`) enables reproducible local evidence without
remote providers. Evaluation runs measure retrieval quality, citation
accuracy, and latency — but never store raw prompts or source text.

## Consequences

**Positive**:
- Tenant isolation is enforced at the database (RLS), service (authorization
  filter), and API (JWT + organization header) layers
- Pluggable providers allow dev/prod parity swaps without architecture changes
- Durable job progress via existing event spine (no new infrastructure)
- Deterministic mode enables CI-gated evidence without provider credentials

**Negative**:
- pgvector `vector(1024)` requires the `pgvector` extension; not available on
  all managed PostgreSQL tiers without approval
- Hybrid retrieval adds latency vs single-path; requires reranker tuning
- Deterministic embeddings don't reflect real semantic quality — remote
  provider evaluation needed for production confidence

**Neutral**:
- 1024-dimension vector chosen for TEI compatibility; dimension change
  requires re-embedding migration
- Conversation history grows unbounded; retention policy needed in M6

## Alternatives Considered

| Alternative | Why rejected |
|---|---|
| External vector DB (Pinecone, Weaviate) | Adds operational complexity, egress cost, and a second tenant-isolation surface. pgvector keeps truth in PostgreSQL. |
| Application-only authorization (no RLS) | Single point of failure; RLS provides defense-in-depth even if app layer has a bug. |
| Direct LLM calls from browser | Violates trust boundary; secrets (API keys) would cross client boundary. |
| Symmetric embedding only (no lexical) | Misses exact-term matches; hybrid gives better recall for code/technical content. |

## References

- Implementation: `apps/platform-api/src/main/java/com/nexora/platform/knowledge/`
- RAG subsystem: `apps/platform-api/src/main/java/com/nexora/platform/rag/`
- Migrations: `database/migrations/V022__knowledge_documents_jobs_chunks_chat.sql`,
  `V023__knowledge_rag_tenant_rls.sql`,
  `V024__knowledge_terminal_guards_and_vector_plane.sql`
- Tests: 36 tests across knowledge + rag packages (all pass)
- Threat model: `docs/security/threat-model.md` (RAG privacy + upload threats)
