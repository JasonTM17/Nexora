# @nexora/ui-ai

AI and knowledge workspace UI components for the Nexora AI surface.

## Purpose

Components for the evidence-before-assertion RAG interface: chat frames, citation
cards, source authorization indicators, and deterministic response states.
Enforces the contract that no answer is generated and no source is cited
without authorization.

## API surface

| Export | Purpose |
|---|---|
| `ChatFrame` | Conversation container with message list |
| `CitationCard` | Resolvable citation with source preview |
| `SourceAuthIndicator` | Authorization state for a source |
| `NoAnswerFrame` | Honest no-answer/denied response state |

## Env vars

None (pure React component library).

## Run locally

```powershell
Set-Location packages/ui-ai
pnpm install --frozen-lockfile
pnpm exec tsc --noEmit
```

## Test

Type-check only.

## Runbook

- **Citation not resolving**: verify source authorization before rendering
- **No-answer state**: always show honest "no answer" frame, never fabricate
