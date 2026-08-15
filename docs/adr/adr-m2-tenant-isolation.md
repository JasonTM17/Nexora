# ADR — M2 Tenant Isolation and CMS Aggregate Design

> Status: `ACCEPTED` — verified against implemented codebase.

## Context

Nexora's CMS core (pages, publishing, themes) and identity/RBAC system required
a tenant isolation model that is impossible to bypass — even if the application
layer has a bug. Cross-tenant data leakage is the highest-risk threat.

## Decision

### Three-layer tenant isolation

1. **Database (RLS)**: Every application relation has row-level security with
   `USING (tenant_id = current_setting('nexora.tenant_id')::uuid)`. The runtime
   role is a non-owner role with explicit grants only.

2. **Service (tenant context)**: `TenantContext` is resolved from JWT subject +
   organization header. Every query sets the tenant context before touching data.

3. **API (JWT + header)**: The `X-Nexora-Organization-Id` header is a
   *candidate* — the server resolves a current active membership and evaluates
   authorization before using it.

### CMS aggregate design

Pages are versioned aggregates with immutable publishing:
- `pages` (current state) + `page_versions` (immutable history)
- Workflow: DRAFT → IN_REVIEW → APPROVED → PUBLISHED (terminal)
- Publishing creates a new immutable version; rollback creates another
- Typed SEO validation (title, description, locale, canonical path, Open Graph, Twitter)

### Membership authority

Membership-derived tenant authority: a subject receives authority only from
current membership in the requested tenant. Deny by default. Last-owner
protection prevents orphaning a tenant.

## Consequences

**Positive**:
- Cross-tenant access impossible by default (RLS + service filter + API check)
- Immutable publishing gives full audit trail
- Rollback = new version (no data loss)

**Negative**:
- RLS adds migration complexity (every table needs policies)
- Optimistic concurrency on aggregates requires version tracking

**Neutral**:
- `nexora_runtime` role has minimal grants (application tables only)

## Alternatives Considered

| Alternative | Why rejected |
|---|---|
| Application-only authorization (no RLS) | Single point of failure; RLS is defense-in-depth |
| Soft delete with resurrectable rows | Terminal states (DELETED/SUPERSEDED) are guarded by DB functions |
| Single table per tenant | Schema explosion; migrations unmanageable |

## References

- Migrations: `V002`–`V013` (identity, profiles, memberships, CMS)
- Threat model: `docs/security/threat-model.md` (tenant isolation section)
- CMS service: `apps/platform-api/src/main/java/com/nexora/platform/cms/`
