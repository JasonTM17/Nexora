# ADR — JWT Signing Approach and Key Management

> Status: `ACCEPTED` — verified against implemented codebase.

## Context

Nexora's authentication uses JWTs issued by an identity provider (Supabase Auth
in v0.1). The platform API verifies these tokens and derives tenant authority
from them. The signing approach must support local development (deterministic
fixtures) and production (managed identity provider) without code changes.

## Decision

### Verification strategy

The platform API uses **JWKS-based verification** — the issuer publishes
`/.well-known/jwks.json` and the API caches the key set. This supports:
- Key rotation without redeployment
- Multiple key slots (PRIMARY for signing, SECONDARY for verifying old tokens)
- Both symmetric (HS256, dev) and asymmetric (RS256/Ed25519, prod) algorithms

### Local development

A `LocalJwtIssuer` generates HS256 tokens with deterministic claims for fixture-based
testing. The `local` profile skips JWKS and uses a shared secret. This enables
CI-gated evidence without live provider credentials.

### Production direction

For production (M6+), migrate to **Ed25519** (preferred) or **RS256** with
≥2048-bit keys:
1. **Dual verify**: new verifier path lands behind `LEGACY_HS256_FALLBACK=true`
2. **Cut signing**: issuer flips to new algo; verifiers accept both
3. **Drop legacy**: remove fallback code path

### Token claims

| Claim | Purpose |
|---|---|
| `sub` | Subject ID (UUID) |
| `session_id` | Session reference (UUID) |
| `role` | Execution role (must be "authenticated") |
| `is_anonymous` | Must be false for domain access |
| `aal` | Assurance level (aal1/aal2) |
| `aud` | Audience (must match API config) |

## Consequences

**Positive**:
- JWKS enables zero-downtime key rotation
- Local issuer enables deterministic CI evidence
- Ed25519 migration path is clear

**Negative**:
- HS256 in dev is symmetric (shared secret) — not production-grade
- JWKS caching adds a small delay on key rotation (30-min TTL acceptable)

**Neutral**:
- Refresh tokens are opaque (random 32-byte base64), not JWTs

## Alternatives Considered

| Alternative | Why rejected |
|---|---|
| Static symmetric secret in production | No key rotation; single point of failure |
| Self-signed JWTs without JWKS | Rotation requires redeployment |
| Opaque tokens only | JWTs enable stateless verification at the edge |

## References

- Validator: `apps/platform-api/src/main/java/com/nexora/platform/auth/NexoraJwtValidator.java`
- Local issuer: `apps/platform-api/src/test/java/com/nexora/platform/auth/LocalJwtIssuer.java`
- Config: `apps/platform-api/src/main/resources/application.yml` (nexora.auth)
