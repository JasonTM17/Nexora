# Nexora platform API

This module is the Spring Boot modular-monolith boundary for Nexora domain APIs.
It defaults to the deterministic `local` profile: it exposes process health,
readiness, metrics, OpenAPI and a small `/api/v1/platform` vertical slice, but
does not connect to a database.

## Run locally

Use Java 25 (the repository toolchain line) and Maven 3.9+:

```powershell
Set-Location apps/platform-api
mvn spring-boot:run
```

The local endpoints are:

- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`
- `GET /actuator/metrics`
- `GET /v3/api-docs`
- `GET /api/v1/platform`

The server binds to `127.0.0.1` by default. A reviewed deployment that needs a
non-loopback listener must explicitly set `NEXORA_BIND_ADDRESS` (for example,
to its approved private address or `0.0.0.0`). That override alone does not
establish a deployment, network policy, or production-readiness claim.

`X-Trace-Id` is returned on every HTTP response. A caller may supply a
printable 1–128 character value; all other values are replaced with a new
opaque identifier.

## Database profile

The `database` profile is deliberately opt-in. The baseline defines
`nexora_runtime` as `NOLOGIN`, so JDBC must use a separately managed LOGIN
credential that is a member of that non-owner role. Hikari runs `SET ROLE
nexora_runtime` for each connection. Flyway uses a distinct approved migrator
LOGIN to read the authoritative migration train. No credential is stored in
this repository.

```powershell
$env:SPRING_PROFILES_ACTIVE = 'database'
$env:NEXORA_RUNTIME_DATABASE_URL = 'jdbc:postgresql://127.0.0.1:5432/postgres'
$env:NEXORA_RUNTIME_DATABASE_USERNAME = '<LOGIN member of nexora_runtime>'
$env:NEXORA_RUNTIME_DATABASE_PASSWORD = '<runtime LOGIN credential>'
$env:NEXORA_MIGRATION_DATABASE_URL = $env:NEXORA_RUNTIME_DATABASE_URL
$env:NEXORA_MIGRATION_DATABASE_USERNAME = '<approved migrator login>'
$env:NEXORA_MIGRATION_DATABASE_PASSWORD = '<migrator credential>'
$env:NEXORA_MIGRATIONS_LOCATION = 'C:/absolute/path/to/Nexora/database/migrations'
mvn spring-boot:run
```

In this profile, readiness includes the database check. The integration test
uses a disposable local PostgreSQL container to prove this configuration only;
it is not provider or production evidence. The local profile does not claim
database availability, migration application, tenant authorization, or
production readiness.
