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

  it("uses static public CSP and nonce-bound no-store CSP for private surfaces", () => {
    const config = app("next.config.ts");
    const proxy = app("proxy.ts");
    expect(config).toContain("script-src 'self' 'unsafe-inline'");
    expect(proxy).toContain("'strict-dynamic'");
    expect(proxy).toContain('requestHeaders.set("x-nonce", nonce)');
    expect(proxy).toContain("private, no-store, max-age=0");
    expect(config).toContain("frame-ancestors 'none'");
  });

  it("keeps the builder selection keyboard-operable and visibly focused", () => {
    const builder = app("../../packages/ui-builder/src/builder-selection-fixture.tsx");
    const css = app("app/globals.css");
    expect(builder).toContain('type="button"');
    expect(builder).toContain("aria-pressed={selected}");
    expect(css).toContain("button:focus-visible");
    expect(css).toContain(".nx-builder-selection");
  });

  it("keeps fixture language visible in every interactive surface", () => {
    for (const route of ["app/page.tsx", "app/studio/page.tsx", "app/ai/page.tsx", "app/builder/page.tsx"]) {
      expect(app(route)).toContain("fixture");
    }
  });
});
