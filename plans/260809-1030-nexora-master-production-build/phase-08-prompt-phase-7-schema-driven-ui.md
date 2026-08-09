---
phase: 8
title: "Prompt Phase 7 — Schema-Driven UI"
status: pending
priority: P1
effort: "7-10 days"
dependencies: [4, 7]
---

# Prompt Phase 7 — Schema-Driven UI

## Outcome

Define a canonical versioned page/component schema and render five allowlisted blocks consistently across API validation, builder preview and public runtime.

## Initial Registry

`Hero`, `RichText`, `FeatureGrid`, `CTA`, `FAQ`.

Each component defines type/version, prop schema, defaults, validation limits, accessibility rules, editor metadata, renderer, migration path and test fixtures. The page schema includes a versioned block-visibility field: hidden blocks remain editable/versioned but are omitted from public output and accessibility trees; preview exposes their hidden state only inside authorized Studio chrome.

## Security Invariants

- No arbitrary JavaScript/React/CSS or event handlers.
- Rich content uses a constrained format and sanitization policy.
- URLs/assets follow allowlists and ownership rules.
- Unknown component/version/prop fails safely and visibly.

## Implementation Slices

1. Canonical JSON Schema and evolution/migration policy.
2. Shared generated/static types and drift check.
3. Server-side validation at persistence/publish boundary.
4. Frontend registry and exhaustive renderer.
5. Five accessible production components and fixtures.
6. Compatibility/migration/hostile-payload tests.
7. Visible/hidden compatibility fixtures shared by server validation, builder preview and public renderer.

## Planned Ownership

`packages/contracts/page-schema/**`, `packages/ui-core/blocks/**`, platform schema validation and tests. Schema contract is serialized; renderers may split after contract acceptance.

## Commit Plan

- `feat(builder): define versioned component schema`
- `feat(ui): add five schema-driven page blocks`
- `feat(cms): validate page documents server-side`
- `test(security): reject unsafe page payloads`

## Acceptance

- [ ] Persisted document containing all five blocks validates and renders in preview/public paths.
- [ ] Server and client agree on contract fixtures.
- [ ] Unknown/old versions have documented migration/fallback.
- [ ] XSS/unsafe URL/arbitrary style fixtures are rejected or sanitized.
- [ ] Component accessibility and responsive checks pass.
- [ ] Hide/show semantics survive save, preview, publish, rollback and schema migration; a hidden block produces no public DOM, metadata or focus target.

## Stop Conditions

Only client-side validation; raw HTML without sanitization; registry fallthrough executes arbitrary code; schema owner ambiguous; hidden content remains discoverable/focusable in public output.
