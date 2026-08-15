# ADR — JWT Ed25519 Migration Protocol

> Status: `ACCEPTED` — migration plan for M6 security hardening.

## Context

Current JWT verification uses JWKS (good foundation) but the local development
issuer uses HS256 (symmetric). Production requires Ed25519 (asymmetric) or
RS256 with ≥2048-bit keys. Since authentication is delegated to an external
provider (Supabase), the migration is primarily on the verification side.

## Decision

### Three-phase cutover protocol

1. **Dual verify (read both, write old)**:
   - New verifier path lands behind `LEGACY_HS256_FALLBACK=true` feature flag.
   - Both old (HS256) and new (Ed25519) code paths verify; signing still uses old algo.
   - Confirm zero verify failures across services for ≥ 7 days.

2. **Cut signing over (read both, write new)**:
   - Issuer flips signing to Ed25519.
   - Verifiers still accept both. Run for ≥ 1 token TTL window so all in-flight
     tokens drain.

3. **Drop legacy verify (read new, write new)**:
   - Remove fallback code path. Set `LEGACY_HS256_FALLBACK=false`.
   - Delete the env flag in the next release.

### Key management

- **JWKS endpoint** at `/.well-known/jwks.json` with two key slots:
  - `PRIMARY`: signs new tokens
  - `SECONDARY`: verifies old tokens during rotation
- **Rotation cadence**: ≤ 90 days
- **Drill**: quarterly rotation on staging

## Consequences

**Positive**:
- Asymmetric keys: private key never leaves the issuer
- JWKS enables zero-downtime rotation
- Three-phase protocol prevents session eviction

**Negative**:
- Requires coordination with external identity provider (Supabase)
- Feature flag adds temporary complexity

## References

- Current validator: `apps/platform-api/src/main/java/com/nexora/platform/auth/NexoraJwtValidator.java`
- JWT ADR: `docs/adr/adr-jwt-signing-approach.md`
- Feature flags: `FeatureFlagService` (M5)
