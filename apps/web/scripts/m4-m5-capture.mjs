// Captures README evidence for M4-M5 surfaces: knowledge workspace, AI chat,
// admin feature flags, analytics, notifications, experiments, search.
// Prerequisites: `next build` then `next start -p 3100 -H 127.0.0.1`.
// Run: node apps/web/scripts/m4-m5-capture.mjs
// Outputs: assets/readme/m4-*.png

import { mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { chromium } from "@playwright/test";

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), "../../..");
const outDir = resolve(repoRoot, "assets/readme");
mkdirSync(outDir, { recursive: true });

const BASE = process.env.NEXORA_README_CAPTURE_URL ?? "http://127.0.0.1:3100";

// M4-M5 surfaces to capture (deterministic fixture data)
const surfaces = [
  { path: "/knowledge", name: "knowledge" },
  { path: "/ai", name: "ai-chat" },
  { path: "/search?q=test", name: "search" },
  { path: "/admin/feature-flags", name: "feature-flags" },
  { path: "/admin/analytics", name: "analytics" },
  { path: "/admin/notifications", name: "notifications" },
  { path: "/admin/experiments", name: "experiments" },
];

const browser = await chromium.launch();
const context = await browser.newContext({
  viewport: { width: 1440, height: 900 },
  deviceScaleFactor: 2,
});
const page = await context.newPage();

// Wait for app to be ready
await page.goto(`${BASE}/`, { waitUntil: "networkidle" });
await page.waitForSelector(".nx-wordmark", { timeout: 15000 });

for (const surface of surfaces) {
  await page.goto(`${BASE}${surface.path}`, { waitUntil: "networkidle" });
  await page.waitForTimeout(500); // Let states settle
  await page.screenshot({ path: resolve(outDir, `${surface.name}.png`) });
  console.log(`Captured: ${surface.name}`);
}

await browser.close();
console.log(`\nDone. ${surfaces.length} captures saved to assets/readme/`);
