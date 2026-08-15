# Nexora Web

Next.js 16 / React 19 product shell with same-origin BFF routes.

## Purpose

Browser-facing surface for the tenant-aware CMS and knowledge workspace. Handles
public pages (home, studio, AI, builder, search), admin surfaces (feature flags,
analytics, notifications, experiments), and proxies API calls to the Spring
platform API through same-origin BFF routes.

## API surface

BFF routes under `app/api/bff/` proxy to `apps/platform-api`:
- `/api/bff/access-context`, `/api/bff/tenant-context`
- `/api/bff/memberships`, `/api/bff/profile`
- `/api/bff/knowledge`, `/api/bff/rag`, `/api/bff/conversations`
- `/api/bff/feature-flags`, `/api/bff/analytics`, `/api/bff/notifications`
- `/api/bff/experiments`, `/api/bff/search`

## Env vars

| Name | Required | Default | Description |
|---|---|---|---|
| `NEXORA_PLATFORM_API_URL` | No | `http://127.0.0.1:8080` | Backend API base URL |
| `NEXORA_BFF_SECRET` | No | — | Session cookie signing secret |

## Run locally

```powershell
Set-Location apps/web
pnpm install --frozen-lockfile
pnpm exec next dev -p 3000
# or production build:
pnpm exec next build && pnpm exec next start -p 3000
```

## Test

```powershell
pnpm exec vitest run
```

Coverage threshold: ≥ 80% lines (not yet enforced in CI).

## Runbook

- **Build fails**: check Node 24.12 + pnpm 11.0.9 toolchain
- **BFF proxy errors**: verify `NEXORA_PLATFORM_API_URL` and backend health
- **Cookie issues**: set `NEXORA_BFF_SECRET` to a random 32-byte value
