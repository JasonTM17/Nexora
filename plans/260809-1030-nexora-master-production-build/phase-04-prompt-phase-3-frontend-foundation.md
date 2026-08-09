---
phase: 4
title: "Prompt Phase 3 — Frontend Foundation"
status: pending
priority: P1
effort: "5-7 days"
dependencies: [2]
---

# Prompt Phase 3 — Frontend Foundation

## Outcome

Create the real Next.js product shell and Nexora-owned design foundation, ready for authenticated admin and expressive public experiences.

## Requirements

- Next.js/React with strict TypeScript; Tailwind/custom primitives for branded public surfaces; Ant Design 6.x through owned Studio wrappers; evaluated Ant Design X adapters for RAG.
- Public, auth and admin route groups; application/auth shell.
- Server/client boundary documented; query cache, forms and schema validation selected intentionally.
- Root and route error boundaries, not-found, loading and empty primitives.
- Route-aware navigation primitives include semantic breadcrumbs, skip links, command/search entry and permission-aware contextual help. Breadcrumbs describe information hierarchy rather than browser history and never replace authorization.
- Token system, typography, spacing, focus, motion and responsive breakpoints from the user-selected three-direction Stitch/DESIGN.md process.
- `@ant-design/nextjs-registry`/current official SSR integration, `ConfigProvider`/`App`, context-safe feedback APIs, CSP/hydration and bundle strategy.
- Accepted per-surface CSP/cache ADR before frontend dispatch: dynamic Studio/auth may use strict request nonces; cacheable public schema pages use a tested external/static CSS, hash-compatible or other documented strategy without default `unsafe-inline`.
- Stitch HTML/assets are quarantined as untrusted design input: offline URL/script/event-handler/font/image/dependency scan, sanitize or no-network sandbox, and no generated dependency/code/remote asset copied into production.
- No MUI/shadcn dual-system, Vite or generic TanStack assumptions from AgentKit guidance.

## Design Workflow

1. M1-D00 locks journeys, IA, route/state inventory and wireflows under `docs/ux/architecture/**`; its exact reviewed head is mechanically integrated before any direction writer dispatches.
2. M1-D01A through M1-D01C generate exactly three comparable Stitch directions from that pinned `INTEGRATED` M1-D00 head, maximum two disjoint writers at once, and obtain explicit user choice.
3. M1-D02 writes the selected canonical `.stitch/DESIGN.md`, selected evidence and AntD/token/component contract only after all direction scorecards and the user decision exist.
4. M1-DW01 consumes the exact `INTEGRATED` M1-T01 and M1-D02 heads, writes only the approved Node dependency-control files and proves the frozen install/import/SSR probe.
5. M1-T03 consumes the exact `INTEGRATED` M1-D02 and M1-DW01 heads, maps semantic tokens into AntD wrappers/custom primitives and hand-builds production React components without editing design authority or any dependency declaration/lockfile.
6. Independent UI review verifies browser, responsive, accessibility, CSP/cache, provenance, frozen dependency state and bundle evidence on exact heads.

## Planned Paths

`docs/ux/architecture/**` belongs only to M1-D00. `.stitch/directions/**` and direction-specific `assets/designs/**` belong only to M1-D01A/B/C. Canonical `.stitch/DESIGN.md`, `assets/designs/selected/**` and the design decision record belong only to M1-D02. M1-DW01 alone owns root `package.json`, `pnpm-workspace.yaml`, `pnpm-lock.yaml`, `.npmrc` and the `package.json` files under `apps/web`, `packages/design-tokens`, `packages/ui-core`, `packages/ui-studio`, `packages/ui-ai`, `packages/ui-builder` and `packages/contracts`. M1-T03 owns all other approved files under `apps/web/**`, `packages/design-tokens/**` and `packages/ui-{core,studio,ai,builder}/**`; every listed dependency-control file is explicitly excluded. One owner controls global styles/tokens/providers/root layout; feature workers consume frozen exports. No task crosses these writer boundaries.

## Validation

- Lint, strict typecheck, unit/component tests and production build.
- 375px and desktop shell screenshots with no overflow.
- Keyboard navigation, visible focus, reduced motion and contrast checks.
- Breadcrumb/current-page announcements, skip-link order and contextual-help trigger/content work with keyboard and screen reader at 375px/desktop; help never leaks unauthorized names/actions.
- Route-level loading/error/not-found/unauthenticated states.
- CSP/header baseline and no invented metrics/live content.
- Production build plus public/Studio header inspection, browser CSP-violation capture, hydration-console check, FOUC evidence and public cache hit/miss/control behavior.
- App Router SSR has no first-screen style flash/hydration mismatch; bundle report attributes AntD/Ant Design X/icons/locales.
- Stitch quarantine report has no unapproved executable/network/dependency input and records provenance for any retained local asset.
- M1-D00 route/state inventory covers every included v0.1 persona/journey plus loading, empty, denied, error, offline/degraded and responsive states; each of M1-D01A, M1-D01B and M1-D01C pins the exact integrated inventory head in its own packet.
- M1-T03 validation starts with `pnpm install --frozen-lockfile` and ends with an exact zero diff for every M1-DW01-owned manifest plus `pnpm-lock.yaml`; a dependency need becomes a ledger request, never an application-worker edit.

## Commit Plan

- `chore(web): initialize Next.js product shell`
- `feat(ui): add Nexora design tokens and primitives`
- `feat(web): add resilient route and error boundaries`

## Acceptance

- [ ] Approved design direction is linked.
- [ ] Public/admin/auth visual registers are consistent but purpose-specific.
- [ ] Critical shell states pass automated and manual accessibility smoke.
- [ ] Production build and browser checks pass.
- [ ] Public cache semantics and Studio/auth CSP semantics match the accepted route-level ADR; neither blanket nonce nor default `unsafe-inline` is used.
- [ ] Stitch reference artifacts passed offline quarantine and no generated code/dependency/remote asset gained production authority.
- [ ] Anchor screens are recognizably Nexora and do not retain default AntD showcase styling.
- [ ] Public/Studio route families have consistent hierarchy breadcrumbs and accessible contextual help with route-specific ownership and content review.

## Risks

Generic or executable Stitch output, default AntD template look, two full component systems, blanket nonce destroying public caching, `unsafe-inline`, CSS-in-JS/CSP or hydration drift, over-centralized global state, inaccessible navigation, premature abstraction.
