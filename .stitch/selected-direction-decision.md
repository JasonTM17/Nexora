# M1-D02 selected-direction decision

**Decision date:** 2026-08-10
**Decision owner:** explicit user selection recorded for M1-D02
**Selected direction:** **M1-D01B — Luminous Grid**
**Status:** selected design contract and evidence receipt; not implementation,
release, provider configuration, or proof of live product behavior.

## Decision

The user selected **B — Luminous Grid** after the three-direction comparison.
M1-D02 therefore establishes `.stitch/DESIGN.md` as the canonical semantic
design contract for that direction. The selection does not erase A or C, does
not re-attribute their private Stitch provenance, and does not authorize screen
generation, remote Stitch edits, product code, package changes, deployment, or
release work.

## Integrated comparison receipt

All three packets remain bound inputs to this decision. SHA-256 values below
are direct local hashes at M1-D02 creation; they identify evidence, not a claim
that the evidence is production-ready.

| Direction | Bound local packet | Stitch project / design-system asset / screen | Evidence SHA-256 |
| --- | --- | --- | --- |
| A — Signal Atelier | `.stitch/directions/signal-atelier/**`; `assets/designs/signal-atelier/public-home-concept.png` | Private project `8293661789942295178`; screen and design-system asset were not exposed by the completed generation. | provenance `6D135E5A58969569F19D6335DA15F6CF980B17C31BD97049BD180598FB218D01`; HTML evidence wrapper `AB4B5E6B59D3C44B41FD592FE5890BECB8C1A1B7A664C2B2673D1E9BD38D7E45`; capture `7D127B92C4EB519C3CAC18D7896C9685BDBDD565A887DFE14DDC06336ED65287` |
| B — **Luminous Grid (selected)** | `.stitch/directions/luminous-grid/**`; `assets/designs/luminous-grid/public-home-concept.png`; `assets/designs/selected/luminous-grid-public-home-concept.png` | Private project `3311987604123916756`; design-system asset `assets/c9bb78aa40b74fb3b0f533055e20443a`; screen `6a9a5b3b5cdb44f28720c84e791ff739`. | provenance `7765206A0707E3485B510866B3B6C53F0A480518A1A74BFBCE3A4335D9979B34`; Stitch HTML export `C0E06D7DDC459683B7E89A76BFC5D2BB0ACAEF958668FDEF3D8AC5762D52A2E9`; source/selected capture `6B5B31E9BEBC17AF9923AB5072ADA1C84FD76E431FFE18A9F63075A007699E40` |
| C — Warm Intelligence | `.stitch/directions/warm-intelligence/**`; `assets/designs/warm-intelligence/public-home-concept.png` | Private project `3311987604123916756` used as a bounded generation container; design-system asset `assets/352aabff49234bf0863ea60593d188c0`; screen `938ac00b782a4bf59412c4f8ca82b038`. | provenance `0060B34C16BC3FDF452D79C061BFE3B070CD5714EBBF028D5B855D6E6AC1A115`; Stitch HTML export `D55E24F9848ED7A01A102A775B2435478644B14BB872CCE99D0E946EB71BCCE6`; capture `0B42D2991400C8025159D0C5529DF0D460F653EDCB4923676FCE3D2176CF2F0E` |

The selected copy is byte-identical to the B source capture. Its presence under
`assets/designs/selected/` is an indexable selection receipt, not a new design
asset or an implementation input.

## Objective comparison criteria

The comparison used the same public-home anchor and assessed these criteria;
none overrides the user's explicit selection.

| Criterion | A — Signal Atelier | B — Luminous Grid | C — Warm Intelligence |
| --- | --- | --- | --- |
| Public hierarchy and reading | Editorial signal rail and restrained display type. | Clear modular reading order with outlined cards and structural grid. | Warm sequential folio and content-led rhythm. |
| Dense Studio/Builder fit | Rail must prove compactness in dense professional contexts. | Strongest stated fit for modular architecture, builder, and observability contexts. | Must prove that its soft folio stays precise in dense controls. |
| Provenance/truth cue | State rail: Draft → Review → Published → Grounded. | Narrow amber marker and explicit source-aware panel. | Layered knowledge folio and source/version depth. |
| Distinctiveness and anti-clone risk | Avoid imitation through editorial balance. | Must be challenged for Vercel/Linear-like technical-grid similarity. | Avoid losing technical precision through warmth. |
| Responsive/a11y readiness | 375px behavior, font coverage, contrast, and reduced motion are specified but unverified. | 375px one-column collapse, 44px targets, font coverage, and contrast are specified but unverified. | 375px collapse, font coverage, and contrast are specified but unverified. |
| Evidence integrity | Private thumbnail/capture; no exposed screen ID or generated HTML. | Private screen/export with recorded asset and screen IDs. | Private screen/export in the shared bounded container; title is not re-attributed to C. |

## Known limitations and non-claims

- Each direction is planned concept evidence only. None proves an application,
  responsive browser rendering, a connected source, tenant isolation, AI
  behavior, accessibility conformance, performance, deployment, or release.
- No 375px mobile screen was generated or rendered for any selected-direction
  evidence. The contract records the required collapse; implementation must
  test it.
- Generated font names and visual contrast are reference observations only.
  Licensing, local hosting, performance, Vietnamese glyph coverage, and
  measured contrast remain implementation gates.
- A's completed generation exposed neither screen ID nor generated HTML; its
  local HTML is an offline evidence wrapper. C used B's existing private project
  as a bounded container after creation failures and is not re-attributed to
  that project title.
- B and C exports contain remote-resource patterns. They remain quarantined;
  no export code, remote asset/CDN, Tailwind configuration, or runtime
  dependency is approved for reuse.

## Links and continuation boundary

- Canonical contract: [`.stitch/DESIGN.md`](./DESIGN.md)
- Selected evidence index: [`assets/designs/selected/README.md`](../assets/designs/selected/README.md)
- B provenance: [`luminous-grid/provenance.json`](./directions/luminous-grid/provenance.json)
- A provenance: [`signal-atelier/provenance.json`](./directions/signal-atelier/provenance.json)
- C provenance: [`warm-intelligence/provenance.json`](./directions/warm-intelligence/provenance.json)

The next authorized activity is exact-head dual review of this bounded commit.
Any later product implementation must consume the semantic contract through its
own scoped work, with independent responsive, accessibility, visual, security,
and dependency evidence.
