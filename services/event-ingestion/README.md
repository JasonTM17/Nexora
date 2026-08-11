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
`D367422D60EFFEA6B0CCE7EBC4A4ABE557D8F613A3E048E5AC914C04FB686337` on
the current main. It is not a second contract source. Any canonical contract
change requires revalidating this fixture before a M3-T05 interface pin.

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
| `NEXORA_EVENT_INGESTION_RATE_LIMIT_PER_MINUTE` | `60` | 1..10000 per trusted organization/subject |
| `NEXORA_EVENT_INGESTION_RATE_LIMIT_KEYS` | `10000` | 1..100000 retained principals; saturation rejects new keys |

Run the local checks from this directory:

```powershell
go test ./...
go vet ./...
```

`GET /healthz` reports process liveness and `GET /readyz` reports service
readiness. Neither endpoint implies NATS, persistence, deployment, or provider
readiness.
