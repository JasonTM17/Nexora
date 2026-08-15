# Nexora Security

## Reporting a vulnerability

Please report security vulnerabilities privately to the repository maintainer.
Do **not** open public issues for security defects.

## Security posture

Nexora runs a layered CI security gate on every push and pull request:

1. **Trivy** — filesystem + Docker image scan (CRITICAL + HIGH fail build).
2. **CodeQL** — SAST for Java, Go, and TypeScript/JavaScript.
3. **Gitleaks** — secret scan on diff (regex covers `ghp_`, `github_pat_`, `sk-`, `AIza`, `JWT_SECRET=`, `DATABASE_URL=`).
4. **SBOM** — generated per release via Docker Buildx provenance + SBOM attestation.

## Trust boundary

The primary product path is **browser → same-origin Next.js BFF → Spring → PostgreSQL**.
Direct browser-to-Spring requests and browser-held privileged credentials are not part
of the target path. See [docs/security/threat-model.md](docs/security/threat-model.md).

## Tenant isolation

- PostgreSQL row-level security (RLS) on every application relation.
- Server-derived tenant authority: a client-supplied tenant ID never establishes tenant scope.
- Cross-tenant STOP tests gate milestone acceptance.

## Secrets

No credential, token, or connection string with embedded credentials belongs in this
repository. Services fail closed without explicitly configured runtime dependencies.
`.env.example` carries placeholders only.

## License

Apache-2.0 — see [LICENSE](LICENSE).
