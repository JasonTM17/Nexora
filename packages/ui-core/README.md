# @nexora/ui-core

Core UI primitives and app shell for Nexora surfaces.

## Purpose

Foundational layout components (`AppShell`, `PageGrid`), action buttons, status
labels, and information cards. Owns the Ant Design wrapper boundary and the
visual vocabulary (`gradient-text`, `bg-grid`, `shadow-glow`, `animate-fade-in-up`).

## API surface

| Export | Purpose |
|---|---|
| `AppShell` | Root layout wrapper with navbar + footer |
| `PageGrid` | Content grid container |
| `ActionButton` | Primary/secondary action button with loading state |
| `StatusLabel` | Status badge (loading/planned/denied/error/success) |
| `InformationCard` | Bounded content card with heading |

## Env vars

None (pure React component library).

## Run locally

```powershell
Set-Location packages/ui-core
pnpm install --frozen-lockfile
pnpm exec tsc --noEmit
```

## Test

Type-check only. Visual regression via Storybook (planned).

## Runbook

- **Style inconsistency**: verify all surfaces use these primitives, not bespoke CSS
- **Ant Design override**: wrap here, not in consumer code
