# @nexora/ui-studio

Dense-workflow UI components for the Nexora Studio surface.

## Purpose

Owned Ant Design wrapper boundary for complex workflows (page builder, CMS editor,
media management). Provides table-heavy layouts, form workflows, and multi-step
processes with explicit loading/denied/error states.

## API surface

| Export | Purpose |
|---|---|
| `StudioTable` | Accessible data table with sorting + pagination |
| `StudioForm` | Multi-section form with validation |
| `WorkflowStepper` | Multi-step workflow progress indicator |

## Env vars

None (pure React component library).

## Run locally

```powershell
Set-Location packages/ui-studio
pnpm install --frozen-lockfile
pnpm exec tsc --noEmit
```

## Test

Type-check only.

## Runbook

- **Dense workflow issues**: check `StudioTable` virtualization for large datasets
- **Form validation**: all forms use Zod schemas via `react-hook-form`
