---
phase: 9
title: "Prompt Phase 8 — Page Builder"
status: pending
priority: P1
effort: "9-13 days"
dependencies: [7, 8]
---

# Prompt Phase 8 — Page Builder

## Outcome

Deliver an accessible page composition workspace with component library, canvas, properties, responsive preview, autosave and explicit recovery/conflict behavior.

## UX Contract

- Library/search panel, canvas/outline, property inspector and preview controls.
- Branded AntD Studio wrappers may own tree/forms/menus/drawers; the canvas, selection geometry and adaptive-signal state remain Nexora custom components.
- Drag/drop plus keyboard move/add/reorder alternative.
- Duplicate/delete with undo or explicit recovery policy.
- Hide/show is a first-class keyboard/pointer command with visible navigator/canvas state, undo and version/audit semantics; hidden never means deleted.
- Autosave states: unsaved, saving, saved, failed, offline, reconnecting, conflict.
- Responsive preview does not pretend to be the final browser test.

## Implementation Slices

1. Builder state/document command model.
2. Component library and insert operation.
3. Canvas selection/reorder and accessible alternative.
4. Schema-derived property editor and validation.
5. Duplicate/delete/hide/show/undo and responsive preview.
6. Debounced/cancel-safe autosave with optimistic concurrency.
7. Browser/E2E/a11y/recovery tests.

## Planned Ownership

Web `builder/{state,canvas,inspector,library,preview}/**`; platform draft-save endpoint only through a separately owned contract. Global DnD and builder state have one owner.

## Commit Plan

- `feat(builder): add composition workspace shell`
- `feat(builder): add accessible canvas commands`
- `feat(builder): add schema property inspector`
- `feat(builder): add conflict-safe autosave`

## Acceptance

- [ ] Creator builds a page with all five blocks without raw code.
- [ ] Keyboard user can perform every critical canvas action.
- [ ] Keyboard and pointer users can hide/show a block, undo it, save/reload it and verify preview/public behavior without losing content or focus.
- [ ] Autosave survives restart/reconnect and never silently overwrites conflict.
- [ ] Loading/empty/error/denied/offline/conflict/destructive states are visible.
- [ ] 375px/desktop admin QA and zero serious/critical a11y findings.
- [ ] Desktop builder follows the selected Stitch direction; mobile capability is documented honestly rather than compressing an unusable canvas.

## Risks

Pointer-only DnD, large rerender cost, stale debounced save after navigation, loss of selection/focus, client/server schema drift, or UI-only hidden state that leaks into public DOM/SEO.
