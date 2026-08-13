# Nexora event ingestion

This service is the narrowly-scoped Go ingress for M3-T04. It owns only
`services/event-ingestion/**`.

## Boundary decision

The service exists to isolate untrusted HTTP admission work from the Spring
modular monolith: bounded request bodies, fixed server timeouts, rate limiting,
NATS publish backpressure, and load measurements can be operated independently.
It does not own database writes, migrations, shared event contracts, root
runtime wiring, or browser authorization.

The current branch validates the frozen event shape, forwards the caller's
existing bearer only to the Spring-owned admission boundary, exact-compares its
fresh RLS decision, applies bounded per-principal admission, and publishes only
after a JetStream acknowledgement. It does not mint, cache or parse a second
credential type. Spring remains the JWT, current-membership and page authority;
the Go process rejects the route unless both explicit runtime dependencies are
configured. This is deliberate fail-closed behavior, not a claim that browser
credentials or tenant IDs can be trusted here.

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
| `NEXORA_EVENT_INGESTION_MAX_CONCURRENCY` | `128` | 1..10000 aggregate in-flight ingestion requests; excess requests receive `503 INGESTION_OVERLOADED` |
| `NEXORA_EVENT_INGESTION_ADMISSION_URL` | unset | Exact `http(s)://host/api/v1/internal/event-admission`; paired with NATS URL |
| `NEXORA_EVENT_INGESTION_NATS_URL` | unset | Credential-free `nats://host:port`; paired with admission URL |

The rate limiter is in-memory, fixed-window and single-instance: per-principal
limits are per-process, so horizontal scaling multiplies the effective ceiling.
M3-R01 therefore wires exactly one ingestion replica locally; a multi-replica
topology needs a shared or externally-coordinated limiter plus its own review.

Run the local checks from this directory:

```powershell
go test ./...
go vet ./...
```

Build and run the isolated container with Docker Compose:

```powershell
docker compose up --build --wait -d
docker compose ps
docker compose down --volumes --remove-orphans
```

The service-only Compose file keeps the listener on container loopback and
uses an in-container healthcheck, so it does not publish an unauthenticated
port to the host. It runs the read-only, capability-dropped image. This is
disposable local evidence only. The default process still has no NATS
connection or event route until both URLs are configured. The admission URL is
a private service-to-service boundary, excluded from OpenAPI/generated browser
clients; the later runtime-wiring packet owns its network placement and any
separate credential transport.

`GET /healthz` reports process liveness and `GET /readyz` reports this
process's local serve state only: it turns unready during shutdown and stays
ready through a NATS outage, because publish failures surface as bounded 503
responses instead of silent loss. `GET /metrics` exposes only bounded aggregate
HTTP outcome (including the `overloaded` outcome), in-flight and duration values
in Prometheus text format; it never includes credentials, tenant/resource IDs,
event IDs, trace IDs or payload values. None of these endpoints implies NATS,
persistence, deployment, or provider readiness.
After trusted validation, accepted responses and JetStream messages carry the
validated envelope `Nexora-Trace-Id` for bounded correlation. The trusted
backend/runtime boundary remains responsible for deriving and binding that
trace value before this adapter accepts the event; rejected requests do not
echo untrusted trace input.
