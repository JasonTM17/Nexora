# System Context (C4 Level 1)

```mermaid
flowchart LR
    U["<b>Users</b><br/>Editors, admins,<br/>public visitors"] -->|"same-origin"| W["<b>Nexora Web</b><br/>Next.js 16<br/>same-origin BFF"]
    U -->|"Auth only"| S["<b>Supabase</b><br/>Auth, Storage,<br/>Realtime"]
    U -->|"signed ops"| S
    W -->|"validated identity"| A["<b>Platform API</b><br/>Spring Boot 4.1<br/>modular monolith"]
    W -->|"Auth, Storage,<br/>Realtime"| S
    A -->|"non-owner<br/>RLS role"| P["<b>PostgreSQL 17</b><br/>+ pgvector<br/>durable truth"]
    A -->|"minimal<br/>context"| AI["<b>AI Provider</b><br/>DeepSeek / TEI<br/>embedding + generation"]
    A -->|"outbox"| N["<b>NATS JetStream</b><br/>event backbone"]
    G["<b>Go Ingestion</b><br/>bounded HTTP<br/>admission edge"] -->|"validated<br/>events"| N
    N -->|"durable<br/>consumers"| A
    X["<b>Untrusted</b><br/>upload / prompt"] -. "must not control<br/>authority" .-> A
    U -. "no direct<br/>domain API" .-> P

    classDef external fill:#f9f,stroke:#333,stroke-width:1px;
    classDef system fill:#bbf,stroke:#333,stroke-width:2px;
    classDef data fill:#bfb,stroke:#333,stroke-width:1px;
    class U,X external;
    class W,A,G system;
    class P,S,N,AI data;
```

## Key decisions

- **Browser → BFF → Spring → PostgreSQL** is the primary product path.
- Direct browser-to-Spring requests are not part of the target path.
- Supabase is intentionally narrow: Auth, signed Storage, private Realtime only.
- PostgreSQL is the durable truth; NATS is delivery; Realtime is advisory.
