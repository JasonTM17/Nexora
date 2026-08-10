"use client";

import { useState } from "react";

/** A native button preserves Enter/Space activation and reports selection state. */
export function BuilderSelectionFixture() {
  const [selected, setSelected] = useState(true);

  return (
    <button
      aria-label="Canvas block: Hero section"
      aria-pressed={selected}
      className="nx-builder-selection"
      onClick={() => setSelected((value) => !value)}
      type="button"
    >
      <strong>Hero section</strong>
      <span>{selected ? "Selected. Press Enter or Space to toggle." : "Not selected. Press Enter or Space to select."}</span>
    </button>
  );
}
