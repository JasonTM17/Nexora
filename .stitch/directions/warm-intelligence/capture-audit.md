# Capture and quarantine observations

## Actual export receipt

- Stitch returned a `DESKTOP` screen titled `Nexora Warm Intelligence - Landing
  Page`, declared at `2560×4720`.
- The downloaded PNG thumbnail is `278×512` (a scaled capture), so it is not
  asserted as a full-resolution browser screenshot.
- Stitch supplied both the screenshot and HTML export; neither was substituted
  or hand-authored locally. One whitespace-only line in the HTML was normalized
  before commit so the repository's diff check can pass; no executable or
  rendered content was changed.

## Safety and claim boundaries

- `public-home-concept.html` is quarantined design input, not production source.
  It references Google Fonts and `cdn.tailwindcss.com`, and includes an inline
  Tailwind configuration script; it must not be executed or copied into a
  product without the later owner’s sanitization and dependency review.
- The generated HTML contains the phrase `Tenant Isolation Verified`. That
  phrase is not accepted as an operational claim: the surrounding capture is
  explicitly concept-only/not-live evidence, and the repository makes no live
  tenant-isolation assertion from it.
- No mobile screen, live endpoint, provider connection, real tenant, metric,
  deployment, release asset, or product code was created or verified.
