import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";

const app = (path: string) => readFileSync(resolve(import.meta.dirname, "..", path), "utf8");

describe("frontend foundation guardrails", () => {
  it("keeps canonical semantic tokens and mobile layout rules", () => {
    const tokens = app("../../packages/design-tokens/src/tokens.css");
    const css = app("app/globals.css");
    expect(tokens).toContain("--nx-color-action-primary: #2457d6");
    expect(tokens).toContain("--nx-color-provenance: #c97916");
    expect(css).toContain("@media (max-width: 767px)");
    expect(css).toContain("min-height: 44px");
  });

  it("applies stricter no-store headers to non-public surfaces", () => {
    const config = app("next.config.ts");
    expect(config).toContain("private, no-store, max-age=0");
    expect(config).toContain("frame-ancestors 'none'");
  });

  it("keeps fixture language visible in every interactive surface", () => {
    for (const route of ["app/page.tsx", "app/studio/page.tsx", "app/ai/page.tsx", "app/builder/page.tsx"]) {
      expect(app(route)).toContain("fixture");
    }
  });
});
