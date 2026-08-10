"use client";

import type { ReactNode } from "react";

export function BuilderFrame({ navigator, canvas, inspector }: { navigator: ReactNode; canvas: ReactNode; inspector: ReactNode }) {
  return (
    <section className="nx-builder-frame" aria-label="Builder fixture">
      <aside aria-label="Navigator">{navigator}</aside>
      <div className="nx-builder-canvas">{canvas}</div>
      <aside aria-label="Inspector">{inspector}</aside>
    </section>
  );
}
