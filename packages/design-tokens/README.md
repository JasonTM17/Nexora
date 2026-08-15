# @nexora/design-tokens

Branded design tokens (colors, typography, spacing) shared across Nexora surfaces.

## Purpose

Single source of truth for visual language. Consumed by all `packages/ui-*` wrappers
and `apps/web` to ensure consistent branding without duplicated CSS.

## API surface

Exported tokens:
- `colors` — primary, secondary, neutral, semantic (success/warning/error/error)
- `typography` — font families, sizes, weights, line heights
- `spacing` — 4px base grid scale
- `radii` — corner roundness tokens
- `shadows` — elevation levels

## Env vars

None (pure TypeScript module).

## Run locally

```powershell
Set-Location packages/design-tokens
pnpm install --frozen-lockfile
pnpm exec tsc --noEmit  # type-check
```

## Test

Type-check only. Visual regression via Storybook (planned).

## Runbook

- **Token drift**: if UI looks inconsistent, check that all surfaces import from this package
- **New tokens**: add here first, then consume in ui-* packages
