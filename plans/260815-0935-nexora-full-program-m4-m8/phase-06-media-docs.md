---
phase: 6
title: "Cross-cutting: Media + Docs"
status: pending
priority: P2
effort: "3-5 days"
dependencies: [1]
---

# Phase 6: Cross-cutting Media + Docs

## Overview
Produce full media suite: screenshots, GIFs, diagrams for README and all docs. Run in parallel with implementation phases.

## Requirements
- [ ] README media current (tour GIF + surface screenshots)
- [ ] Architecture diagrams (SVG/PNG) in docs/architecture/
- [ ] Security threat model diagram
- [ ] UX wireflow captures
- [ ] Per-service README screenshots where applicable
- [ ] Media manifest reconciling to exact SHA

## Implementation Steps

1. **Capture workflow**
   - Use existing `apps/web/scripts/readme-capture.mjs`
   - Build web: `pnpm exec next build && pnpm exec next start -p 3100`
   - Capture all surfaces including new M4/M5 surfaces
   - Convert webm → GIF via `tools/readme-gif.sh`

2. **Architecture diagrams**
   - System overview (C4 context)
   - Data flow diagram
   - Trust boundary diagram
   - Module dependency diagram
   - Use Mermaid or excalidraw → export SVG/PNG

3. **Per-surface captures**
   - Home, Studio, AI, Builder (done — verify current)
   - Knowledge workspace (new)
   - Admin dashboard (new after M5)
   - Analytics dashboard (new after M5)

4. **Media manifest**
   - `docs/media-manifest.md` mapping file → SHA → description
   - Reconcile to exact release head

## Todo
- [ ] Capture all web surfaces
- [ ] Architecture diagrams
- [ ] Per-surface screenshots
- [ ] Convert to GIF
- [ ] Media manifest

## Success Criteria
- All docs have relevant media
- Media manifest complete
- GIF ≤ reasonable size (< 5MB)

## Commit Plan
```
docs(media): capture all web surfaces
docs(media): add architecture diagrams
docs(media): add media manifest
docs(media): update README with new surfaces
```
