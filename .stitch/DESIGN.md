# Nexora design system — Luminous Grid

**Status:** canonical selected-direction contract for M1-D02.
**Selected direction:** M1-D01B — Luminous Grid.
**Authority:** this document defines semantic design intent for Nexora. It does
not implement components, select packages, or prove a running interface.

## 1. Intent and visual character

Luminous Grid makes a content experience feel calm, modular, and trustworthy.
It uses a cool white-blue field, graphite reading text, cobalt structural
anchors, and a narrow amber provenance cue. The grid should clarify hierarchy
and ownership; it must never become a decorative lattice, a dashboard of
invented metrics, or an imitation of another product.

The direction is deliberately light-first. A dark Studio theme is not selected
or evidenced in M1-D02 and must not be inferred from this contract; it requires
a separately reviewed contrast and visual-regression decision.

## 2. Platform-agnostic semantic tokens

These names are the source contract. Implementations may generate platform
variables from them, but feature code must consume semantic names rather than
raw values.

### Color

| Token | Value | Intended use |
| --- | --- | --- |
| `color.canvas` | `#F7F9FC` | Page background and quiet grid field. |
| `color.surface.base` | `#FFFFFF` | Primary cards, panels, menus, and readable content. |
| `color.surface.subtle` | `#F2F4F7` | Quiet grouping and non-interactive supporting areas. |
| `color.surface.raised` | `#E0E3E6` | Rare elevated or selected background, never a dominant panel. |
| `color.text.strong` | `#182233` | Titles, body copy, and high-emphasis controls. |
| `color.text.muted` | `#434654` | Supporting copy and metadata after contrast verification. |
| `color.border.default` | `#D6DFEE` | Card boundaries and non-dominant separators. |
| `color.border.strong` | `#C3C5D7` | Deliberate grouping or active structural edge. |
| `color.action.primary` | `#2457D6` | Primary actions, active navigation, and grid anchors. |
| `color.action.primary-hover` | `#003FB3` | Hover/pressed treatment for the primary action. |
| `color.provenance` | `#C97916` | Source-aware marker only; not a general accent or fill. |
| `color.focus` | `#C97916` | Visible keyboard focus outline paired with shape/offset, never color alone. |

Success, warning, error, information, data-visualization, high-contrast, and
dark-mode values are intentionally not invented from a concept screenshot.
Their semantic aliases must be set only with component-level contrast,
non-color-state, and user-test evidence.

### Type, spacing, geometry, depth, and layers

| Family | Contract |
| --- | --- |
| `font.display` | Approved sans display face; strong but compact headlines. The concept's Hanken Grotesk is a visual reference, not an approved runtime dependency. |
| `font.body` / `font.ui` | Approved legible sans face with Vietnamese coverage, predictable fallback, and no layout shift. The concept's Work Sans is reference-only. |
| `font.code` | Approved monospace for short resource IDs, state labels, and technical metadata only. The concept's JetBrains Mono is reference-only. |
| `space.*` | 4px base scale: 4, 8, 12, 16, 20, 24, 32, 40, 48, 64px. Use tokens; do not introduce feature-local gaps. |
| `radius.control` / `radius.card` | 8px / 10px. Controls may use a full pill only when the interaction pattern requires it. |
| `border.default` | 1px. Use boundaries to explain structure, not to add noise. |
| `shadow.raised` | One restrained tonal elevation: `0 2px 4px` at low opacity. No stacked, glass, or floating-card effects. |
| `layer.*` | Base < sticky chrome < dropdown < drawer < modal < toast < drag overlay < command palette. Exact numeric z-index values belong to the implementation owner. |

## 3. Layout and responsiveness

- Desktop uses a 12-column grid with 20px gutters and 40px outer margins;
  content is constrained to a readable maximum rather than stretched by the
  viewport.
- Tablet uses eight columns. Mobile uses four columns, 12px gutters, and 16px
  margins below 768px.
- At 375px, public content becomes one vertical scroll column. Navigation
  reduces to wordmark plus menu control; no desktop control strip, card row, or
  horizontal overflow survives.
- Interactive controls have a minimum 44px target. Compact metadata may be
  visually smaller but must not be the sole hit target.
- Studio's complex builder has an explicit supported/degraded mobile policy:
  content, status, review, and quick edits may be available; a compressed full
  desktop canvas must not be implied without evidence.

The desktop concept is evidence of intent only. The 375px rules above are
implementation requirements, not proof that a mobile screen has been rendered.

## 4. Accessibility, content truth, and motion

- Meet WCAG AA contrast for text, controls, focus indicators, charts, and
  status states in the shipped contexts. Verify contrast; do not infer it from
  these reference colors.
- Preserve a logical heading order, landmark structure, skip link, real-link
  navigation, `aria-current`, keyboard-visible focus, and a non-tooltip-only
  route to critical help.
- Pair every state color with text, icon, pattern, or position. Fixture,
  planned, denied, degraded, empty, and error states must be plainly labelled.
- Motion is functional: 120–180ms for small feedback and 180–240ms for
  entering/reordering state, with interruption-safe completion. Under
  `prefers-reduced-motion`, replace transitions with immediate state changes;
  never use ambient grid animation.
- Do not present a source, tenant boundary, workflow state, metric, citation,
  or AI result as live unless the exact product path can prove it. Concept and
  fixture labels remain visible in demos and static previews.

## 5. Surface mapping

| Surface | Luminous Grid expression | Ownership boundary |
| --- | --- | --- |
| Public | Light canvas, spacious 12-column composition, cobalt structural edges, outlined content cards, and small amber provenance cues. Public pages prioritize reading and clear calls to action over dense controls. | Nexora public/brand primitives and schema-block renderers. |
| Studio | Shared color/type/spacing semantics with compact density, strong information grouping, and clear selected/focus states. Tables, forms, trees, menus, overlays, and data states stay utilitarian rather than becoming marketing cards. | Owned Nexora Studio wrappers around Ant Design; feature code does not directly style Ant Design internals. |
| AI and knowledge | Source/citation state is first-class. Amber identifies a provenance marker; cobalt identifies the active action or selection. Answers expose citation, authorization, no-evidence, cancellation, and provider-degraded states without decorative confidence signals. | Owned AI adapters and citation components; library UI never owns domain IDs, permissions, SSE state, redaction, or safe rendering rules. |
| Builder | Cobalt selection geometry and quiet bordered frames make navigator, canvas, inspector, version, and publish relationships explicit. | Owned builder primitives; do not force canvas geometry into generic library components. |

## 6. Ant Design token and wrapper boundary

The design system maps semantic roles into Ant Design only through the owned
Studio provider and wrapper layer. `color.action.primary` is the source for
`colorPrimary`; text, background, border, radius, control height, and focus
aliases map from the corresponding Nexora semantic tokens. Component-specific
aliases are snapshotted so upstream defaults cannot silently change the visual
contract.

Ant Design owns accessible data-dense mechanics (forms, tables, tree,
navigation, overlays, upload shell, and notifications). Nexora wrappers own
semantic props, domain states, labels, focus treatment, density variants, and
the supported token map. No feature may target private Ant Design selectors,
scatter `ConfigProvider` instances, or introduce a second full component
system. Public and builder surfaces use owned primitives; they must not import
Studio wrappers merely to borrow a visual treatment.

## 7. Component contracts

| Component | Contract |
| --- | --- |
| `AppShell` / `Grid` | Supplies landmark regions and the responsive 12/8/4-column grid. It prevents horizontal overflow and does not encode page-specific data. |
| `PublicHeader` | Uses real navigation links, visible active context, a keyboard-operable mobile menu, and 44px targets. |
| `InformationCard` | Uses `surface.base`, 1px semantic border, 10px radius, optional low tonal shadow, and explicit title/body/action slots. It never invents KPI evidence. |
| `ActionButton` | Has primary, secondary, tertiary, disabled, loading, and focus states. Primary is cobalt; destructive behavior uses a separately verified semantic danger token. |
| `ProvenanceMarker` | A narrow amber edge or label paired with readable source/state text. It is not a clickable color-only indicator and does not claim verification. |
| `StatusLabel` | Announces planned, fixture, live, denied, degraded, empty, saving, conflict, and error state with text plus a non-color cue. |
| `StudioField` / `StudioTable` | Owned Ant Design wrappers expose semantic density, validation, empty/loading/error, permissions, and keyboard behavior; consumers do not reach raw library internals. |
| `Citation` / `AIResponse` | Shows source title/location, permission at open time, safe failure/no-answer states, and an owned safe-content boundary. It never renders provider HTML or hidden reasoning. |
| `BuilderFrame` | Keeps selection synchronized among navigator, canvas, and inspector without focus theft; supports pointer and keyboard alternatives. |

## 8. Stitch evidence quarantine — strict rule

Stitch exports are untrusted reference evidence, never product source. The
HTML, screenshot, prompt, and project metadata under `.stitch/directions/**`
and `assets/designs/**` may be inspected offline only. No remote asset, CDN,
Tailwind configuration, script, dependency instruction, component code, font,
image, event handler, inline executable URL, or external import from a Stitch
export may be copied into implementation.

Any candidate font or asset must separately pass ownership/license, local
hosting, security, performance, Vietnamese-coverage, and accessibility review.
Exports are not executed on an authenticated or product origin. Production
components start from this semantic contract and owned implementation code, not
from generated HTML.
