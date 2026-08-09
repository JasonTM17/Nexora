---
phase: 11
title: "Prompt Phase 10 — Theme Engine"
status: pending
priority: P2
effort: "4-6 days"
dependencies: [4, 7, 10]
---

# Prompt Phase 10 — Theme Engine

## Outcome

Provide versioned constrained theme tokens, preview and publication that generate safe dynamic CSS variables without arbitrary stylesheet execution.

## Token Contract

Color roles, typography scale, spacing/radius, container widths, surface/border/focus/motion tokens. Values use schema constraints, accessible defaults and documented fallback.

## Implementation Slices

1. Canonical token schema/version/evolution rules.
2. Theme persistence and tenant/site ownership.
3. Accessible editor with validation and contrast feedback.
4. Preview isolated from published theme.
5. Publish/rollback integrated with page version resolution.
6. Safe CSS variable serializer and CSP-compatible delivery.

## Validation

- Reject arbitrary CSS, invalid units, unsafe URLs and pathological values.
- Light/dark/high-contrast checks where approved.
- Preview versus published isolation and concurrent edit conflict.
- Public render applies latest authorized theme with safe fallback.
- 375px/desktop visual regression and focus visibility.

## Commit Plan

- `feat(theme): define versioned design tokens`
- `feat(theme): add accessible theme editor and preview`
- `feat(theme): publish safe CSS variables`

## Acceptance

- [ ] Theme changes do not require frontend deployment.
- [ ] Invalid/unsafe tokens never reach public CSS.
- [ ] Publication/rollback is auditable and tenant-safe.
- [ ] Design system and public/admin surfaces remain coherent.

## Risks

Arbitrary CSS injection, inaccessible contrast, token drift from UI package, unbounded font/asset URLs, theme/page version mismatch.
