# Contributing to Nexora

Nexora is a solo-contributor project. This document records the conventions
and quality gates that govern contributions.

## Commit conventions

All commits follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `build`,
`ci`, `chore`, `revert`.

Scopes: `web`, `api`, `contracts`, `docker`, `ci`, `docs`, `security`, `obs`,
`flags`, `analytics`, `notification`, `experiment`, `search`, `rag`, `knowledge`.

## Quality gates

### Before every commit

1. **No secrets**: scan diff for credential-shaped values (`ghp_`, `github_pat_`,
   `sk-`, `AIza`, `JWT_SECRET=`, `DATABASE_URL=`).
2. **No AI co-author**: commits must not contain `Co-Authored-By: Claude` or
   similar AI tooling markers.
3. **Focused**: one logical change per commit. Split large changes into small,
   reviewable commits.

### Before every PR

1. **CI green**: `validate.yml` (foundation + Go + Java), `security-scan.yml`
   (CodeQL + Gitleaks + Trivy).
2. **Tests pass**: unit tests for touched behavior; integration tests where
   shared contracts changed.
3. **Coverage**: Java ≥ 80% lines (JaCoCo), Go ≥ 70% lines.

## Code style

- **Java**: follow existing patterns (JdbcTemplate, plain carriers, `@Profile("database")`).
- **TypeScript**: strict mode, generated client for API calls (no hand-written fetch).
- **Go**: `gofmt`, `vet`, bounded error handling.
- **SQL**: migrations are append-only; never edit an accepted migration.

## Testing

```powershell
# Java
cd apps/platform-api && ./mvnw test

# Go
cd services/event-ingestion && go test ./...

# Web
cd apps/web && pnpm exec vitest run

# Contracts
cd packages/contracts && node scripts/check-generated.mjs
```

## Documentation

- Update `README.md` when user-facing behavior, setup, or commands change.
- Write an ADR for architecture decisions (template: `docs/adr/template.md`).
- Update `CHANGELOG.md` for user-visible changes.

## Security

- Report vulnerabilities privately (do not open public issues).
- Never commit secrets, tokens, or credentials.
- Services fail closed without configured runtime dependencies.
