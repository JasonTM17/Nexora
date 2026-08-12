# Nexora event ingestion

This service is the narrowly-scoped Go ingress for M3-T04. It owns only
`services/event-ingestion/**`.

## Boundary decision

The service exists to isolate untrusted HTTP admission work from the Spring
modular monolith: bounded request bodies, fixed server timeouts, rate limiting,
NATS publish backpressure, and load measurements can be operated independently.
It does not own database writes, migrations, shared event contracts, root
runtime wiring, or browser authorization.

The current branch validates the frozen event shape, enforces trusted-context
cross-binding, applies bounded per-principal admission, and publishes only
after a JetStream acknowledgement. Its HTTP adapter is dependency-injected and
is not registered by the default process: the backend still owns the missing
short-lived ingestion-credential issuer and runtime wiring owns the NATS
connection/configuration. This is deliberate fail-closed behavior, not a
claim that browser credentials or tenant IDs can be trusted here.

`internal/domain/testdata/v1/publication-invalidated.json` is a local test
fixture pinned to `packages/contracts/domain/v1/event-contract.json` SHA-256
`7954CA52DA41EFD5E434089E2959812AED2FCCBFFA8BF77634591326879B214D` on
the pinned M3 integration base `f6e796f1879a4332c9f954cdd7b798b2cab79e30`.
It is not a second contract source. Any canonical contract change requires
revalidating this fixture before a M3-T05 interface pin.

The Go boundary is retained only if later M3-T04/M3-T05 joint evidence
demonstrates a real idempotent consumer plus a reproducible comparison with
the Spring-only path. It makes no throughput, provider, deployment, or M5
analytics claim.

## Local configuration

All defaults are local-only and bounded:

| Variable | Default | Bounds |
| --- | --- | --- |
| `NEXORA_EVENT_INGESTION_ADDR` | `127.0.0.1:18080` | Literal loopback IP and TCP port 1..65535 |
| `NEXORA_EVENT_INGESTION_BODY_LIMIT_BYTES` | `65536` | 1024..1048576 |
| `NEXORA_EVENT_INGESTION_READ_HEADER_TIMEOUT` | `2s` | positive |
| `NEXORA_EVENT_INGESTION_READ_TIMEOUT` | `5s` | positive |
| `NEXORA_EVENT_INGESTION_WRITE_TIMEOUT` | `5s` | positive |
| `NEXORA_EVENT_INGESTION_IDLE_TIMEOUT` | `30s` | positive |
| `NEXORA_EVENT_INGESTION_SHUTDOWN_TIMEOUT` | `10s` | positive |
| `NEXORA_EVENT_INGESTION_PUBLISH_TIMEOUT` | `2s` | 1ms..30s acknowledgement deadline |
| `NEXORA_EVENT_INGESTION_RATE_LIMIT_PER_MINUTE` | `60` | 1..10000 per trusted organization/subject |
| `NEXORA_EVENT_INGESTION_RATE_LIMIT_KEYS` | `10000` | 1..100000 retained principals; saturation rejects new keys |

Run the local checks from this directory:

```powershell
go test ./...
go vet ./...
```

`GET /healthz` reports process liveness and `GET /readyz` reports service
readiness. `GET /metrics` exposes only bounded aggregate HTTP outcome, in-flight
and duration values in Prometheus text format; it never includes credentials,
tenant/resource IDs, event IDs, trace IDs or payload values. None of these
endpoints implies NATS, persistence, deployment, or provider readiness.
After trusted validation, accepted responses and JetStream messages carry the
server-derived `Nexora-Trace-Id` for bounded correlation; rejected requests do
not echo untrusted trace input.
