# @nexora/ui-builder

Page builder UI components for the Nexora Builder surface.

## Purpose

Visual page builder with clear ownership and selection geometry: navigator (block
tree), canvas (visual editor), and inspector (property panel). Supports keyboard
toggle semantics and responsive preview (desktop + 375px mobile frame).

## API surface

| Export | Purpose |
|---|---|
| `BuilderNavigator` | Block tree with drag-and-drop reordering |
| `BuilderCanvas` | Visual editing canvas with selection |
| `BuilderInspector` | Property panel for selected block |
| `ResponsivePreview` | Desktop/mobile viewport toggle |

## Env vars

None (pure React component library).

## Run locally

```powershell
Set-Location packages/ui-builder
pnpm install --frozen-lockfile
pnpm exec tsc --noEmit
```

## Test

Type-check only.

## Runbook

- **Selection state lost**: verify selection geometry is owned by parent, not blocks
- **Mobile preview**: canvas does not claim full desktop editing at 375px
