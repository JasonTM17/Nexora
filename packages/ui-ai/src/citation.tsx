"use client";

export type CitationState = "available" | "denied" | "no-evidence";

export function Citation({ state, title }: { state: CitationState; title: string }) {
  if (state === "denied") return <p className="nx-citation nx-citation--denied">Source unavailable: permission is checked when opened.</p>;
  if (state === "no-evidence") return <p className="nx-citation">No supporting source is available for this fixture response.</p>;
  return <p className="nx-citation"><span aria-hidden="true">▌</span> Source fixture: {title}</p>;
}
