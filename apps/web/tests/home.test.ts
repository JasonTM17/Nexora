import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

const app = (path: string) => readFileSync(resolve(import.meta.dirname, "..", path), "utf8");

describe("home workspace overview", () => {
  it("offers accessible routes without making a live-data claim", () => {
    const home = app("app/page.tsx");

    for (const route of ["/knowledge", "/studio", "/ai", "/builder", "/account"]) {
      expect(home).toContain(`href=\"${route}\"`);
    }
    expect(home).toContain('aria-label="Product principles"');
    expect(home).toContain('aria-labelledby="trust-rail-heading"');
    expect(home).toContain("does not claim a hosted tenant, connected provider, or live production telemetry");
    expect(home).not.toContain("Live dashboard");
  });

  it("keeps the overview usable at a compact viewport", () => {
    const css = app("app/globals.css");

    expect(css).toContain(".nx-trust-rail ol { grid-template-columns: 1fr; }");
    expect(css).toContain(".nx-nav { flex-wrap: nowrap;");
    expect(css).toContain("min-height: 44px");
  });
});
