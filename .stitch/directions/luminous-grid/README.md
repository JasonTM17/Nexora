# M1-D01B — Luminous Grid

## Status

**Planned design-direction evidence only.** This packet records one private
Stitch concept for comparison and review. It is not a product implementation,
a canonical design system, provider configuration, or evidence of live data.

## Direction

Luminous Grid is a pragmatic, modular visual direction for Nexora. It uses a
cool white-blue canvas, calm cobalt structural anchors, and a strictly
rationed amber provenance marker. Its signature is a 12-column content grid:
clear, square-ish cards make tenant boundaries, content assembly, and handoff
states legible without turning illustrative fixtures into operational claims.

The generated public-home concept demonstrates the direction through:

- a sans-serif, implementation-minded hierarchy rather than Signal Atelier's
  editorial typography and signal rail;
- outlined modular cards with labelled planned, fixture, and not-live content;
- an explicit source-awareness panel that says there are no connected sources
  or live data; and
- a specified 375px rule: header controls collapse to a menu control and every
  desktop grid section stacks to one column without horizontal overflow. The
  single desktop capture does not itself verify that mobile rendering.

## Packet contents

| File | Purpose |
| --- | --- |
| [enhanced-prompt.md](./enhanced-prompt.md) | Exact structured prompt sent before generation. |
| [observed-design-system.md](./observed-design-system.md) | Emergent direction-level rules returned by Stitch; not canonical tokens. |
| [provenance.json](./provenance.json) | Private project, generation, export, and capture receipt. |
| [public-home-concept.html](./public-home-concept.html) | Downloaded Stitch HTML export; evidence only, not product source. |
| [public-home-concept.png](../../../assets/designs/luminous-grid/public-home-concept.png) | Downloaded Stitch desktop capture. |

## Boundary

Only `.stitch/directions/luminous-grid/**` and
`assets/designs/luminous-grid/**` are changed by this packet. A later
selected-direction owner may assess it beside the quarantined alternatives;
that owner alone can create canonical `.stitch/DESIGN.md` or any product/token
implementation.
