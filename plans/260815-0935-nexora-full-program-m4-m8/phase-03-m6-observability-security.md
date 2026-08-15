---
phase: 3
title: "M6 Observability + Security"
status: pending
priority: P1
effort: "8-12 days"
dependencies: [2]
---

# Phase 3: M6 Observability + Security

## Overview
M6 delivers full observability stack (Prometheus metrics, Loki logs, Tempo traces, Grafana dashboards) and security hardening (JWT Ed25519, rate limiting, account lockout, zod validation).

## Requirements
- [ ] Prometheus `/metrics` endpoint on all 3 services
- [ ] Structured JSON logging (zerolog/pino) with trace correlation
- [ ] OpenTelemetry tracing (W3C Trace Context)
- [ ] Grafana dashboards (local via Compose)
- [ ] JWT signing cutover to Ed25519 + JWKS
- [ ] Redis-backed rate limiter (replace in-memory)
- [ ] Account lockout after N failed logins
- [ ] Zod validation on all mutating API routes
- [ ] Security headers + CSP

## Implementation Steps

1. **Observability stack**
   - Add `observability/` directory with Compose services (Prometheus, Loki, Tempo, Grafana)
   - Spring: Micrometer + OTLP exporter → `/metrics`, structured logging
   - Go: Prometheus handler + OTLP
   - Next.js: Web Vitals + OTLP (server-side)
   - Grafana dashboards: 4 critical dashboards (API, Go, Web, Infrastructure)

2. **JWT Ed25519 cutover**
   - Three-phase protocol: dual verify → cut signing → drop legacy
   - JWKS endpoint at `/.well-known/jwks.json`
   - PRIMARY + SECONDARY key slots
   - 90-day rotation cadence

3. **Redis rate limiter**
   - Replace in-memory limiter in Go ingestion
   - Sliding-window Redis (key: `rl:<route>:<actor>`)
   - Spring: bucket4j or custom Redis limiter
   - Per-IP for unauthed, per-user for authed

4. **Account lockout**
   - Schema: `failed_login_count`, `locked_until` on users
   - 5 failures → 15 min lock → captcha after 2nd strike
   - Reset on successful login

5. **API validation hardening**
   - Audit all POST/PATCH/PUT/DELETE routes for zod/safeParse
   - Add validation where missing
   - CI lint gate: `grep -rn "request.json()" | grep -v "safeParse"` must be empty

6. **Security headers**
   - CSP, HSTS, X-Frame-Options, X-Content-Type-Options
   - Spring Security filter chain
   - Next.js headers config

## Todo
- [ ] Observability Compose stack
- [ ] Prometheus metrics on all services
- [ ] Structured logging + trace correlation
- [ ] Grafana dashboards
- [ ] JWT Ed25519 + JWKS
- [ ] Redis rate limiter
- [ ] Account lockout
- [ ] API validation audit + fixes
- [ ] Security headers

## Success Criteria
- All services expose `/metrics`
- Traces correlate across web → BFF → Spring → DB
- JWT uses Ed25519 in production profile
- Rate limiter survives multi-replica (Redis-backed)
- Account locks after 5 failed attempts
- All mutating routes validate input

## Commit Plan
```
feat(obs): add Prometheus metrics to all services
feat(obs): add structured logging with trace correlation
feat(obs): add OpenTelemetry tracing
feat(obs): add Grafana dashboards
feat(auth): cutover JWT to Ed25519 + JWKS
feat(security): add Redis-backed rate limiter
feat(security): add account lockout
feat(security): add API validation hardening
feat(security): add security headers
```
