# Observed direction characteristics

These characteristics were returned with the generated concept as an emergent
Stitch design system. They are review inputs only; this file does **not**
create the canonical `.stitch/DESIGN.md` authority.

## Visual rules

- Base: cool white-blue `#F7F9FC`; cards use white surfaces and quiet slate
  `#D6DFEE`/`#C3C5D7` boundaries.
- Ink: high-legibility graphite `#182233` (the returned token resolves to
  `#191C1E`); cobalt `#2457D6` is the primary action and grid anchor.
- Provenance: amber `#C97916` is reserved for a narrow card-edge marker and
  source-aware metadata, not used as decorative fill.
- Type: Hanken Grotesk provides the display/headline voice, Work Sans carries
  body text, and JetBrains Mono is limited to technical micro labels. Runtime
  font loading and Vietnamese glyph coverage remain future validation work,
  not a claim from this concept.
- Geometry: 12 desktop columns with 20px gutters and 40px margins; 8 tablet
  columns and 4 mobile columns. Cards are 10px soft-industrial rectangles with
  1px borders and, where elevation is needed, one `0 2px 4px` low-opacity tonal
  shadow.
- Responsive rule: below 768px the design system declares four columns,
  12px gutters, and 16px margins. The prompt further requires the 375px version
  to stack content into a single scroll column and replace desktop header
  controls with a menu control.

## Fit and follow-up questions

The result gives reviewers a useful alternative to Signal Atelier: it is a
card-led architecture surface rather than an editorial story with a provenance
rail. Before selection, reviewers should test the actual 375px screen,
validate the three fonts and colour contrast in the future application, and
decide whether the deliberate metadata density is appropriate for public
audiences.
