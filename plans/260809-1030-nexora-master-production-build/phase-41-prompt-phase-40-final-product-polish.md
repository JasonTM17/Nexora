---
phase: 41
title: "Prompt Phase 40 — Final Product Polish"
status: pending
priority: P1
effort: "6-10 days"
dependencies: [4, 9, 10, 11, 12, 16, 20, 23, 24, 25, 26, 27, 28, 30]
---

# Prompt Phase 40 — Final Product Polish

## Outcome

Audit every user-facing route and state for responsive behavior, accessibility, visual consistency, truthful copy/data and production interaction quality.

## Audit Inventory

Public pages, authentication/onboarding, organization/member/RBAC, page list/builder/preview/review/history/theme, knowledge/upload/jobs/chat/citations, flags/experiments/analytics/personalization/recommendations, notifications/audit/search, error/system states.

## Required States

Loading, skeleton/progress, empty, error/retry, denied, not found, conflict, offline/reconnect, saving/saved/failed, destructive confirm, partial degradation, disabled/kill-switch and successful completion.

## Workflow

1. Inventory routes/states and link existing evidence.
2. Compare real implementation against approved Stitch/DESIGN.md and token contract.
3. Test 375px, tablet if accepted, desktop and zoom/reflow.
4. Keyboard/screen-reader/focus/reduced-motion/contrast review.
5. Fix component/token/copy/interaction issues in bounded branches.
6. Run visual/E2E/a11y regressions and truthfulness audit.
7. Freeze accepted routes/states and hand them to the media owner for exact-SHA screenshot/GIF capture.

## Planned Ownership

Read-only audits may parallelize by route. Global tokens/styles/shared components and copy index are serialized; fixes return to owning domains.

## Commit Plan

- Scoped `fix(ui): complete <flow> production states`
- `fix(a11y): resolve critical interaction barriers`
- `docs(ui): record final visual and accessibility evidence`
- `docs(media): capture verified product walkthrough`

## Acceptance

- [ ] No lorem ipsum, fake live metric, unsupported capability or unlabeled fixture.
- [ ] No horizontal overflow at 375px; critical flows keyboard complete.
- [ ] Zero serious/critical automated a11y findings plus manual smoke evidence.
- [ ] Public/admin/knowledge visual systems are coherent.
- [ ] Error/degraded states preserve trust and recovery action.
- [ ] Approved desktop/375px capture set and walkthrough storyboard cover real critical states without overstating speed or capability.

## Stop Conditions

Final polish used to hide missing functional/security work, inaccessible builder, visual evidence from fixtures presented as live, media captured from stale/unreviewed HEAD, unreviewed global style regression.
